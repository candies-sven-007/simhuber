package com.candies.simhuber.ccid

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android USB Host CCID (Chip Card Interface Device, Class 0x0B) 驱动实现。
 * 支持 PC_to_RDR_IccPowerOn / PC_to_RDR_IccPowerOff / PC_to_RDR_XfrBlock，
 * 以及基础 SIM 文件系统 (EF_ICCID 2FE2, EF_IMSI 6F07) 与 USIM AUTHENTICATE (00 88 00 81) APDU。
 */
class UsbCcidDriver(private val context: Context) {

    companion object {
        const val ACTION_USB_PERMISSION = "com.candies.simhuber.USB_PERMISSION"
        private const val USB_CLASS_SMART_CARD = 0x0B

        private const val PC_TO_RDR_ICC_POWER_ON: Byte = 0x62
        private const val PC_TO_RDR_ICC_POWER_OFF: Byte = 0x63
        private const val PC_TO_RDR_XFR_BLOCK: Byte = 0x6F
        private const val RDR_TO_PC_DATA_BLOCK: Byte = 0x80.toByte()
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connection: UsbDeviceConnection? = null
    private var ccidInterface: UsbInterface? = null
    private var bulkIn: UsbEndpoint? = null
    private var bulkOut: UsbEndpoint? = null
    private var seqCounter: Byte = 0

    data class ApduResponse(val data: ByteArray, val sw1: Int, val sw2: Int) {
        val swHex: String get() = "%02X%02X".format(sw1, sw2)
        val isSuccess: Boolean get() = (sw1 == 0x90 && sw2 == 0x00) || sw1 == 0x61
    }

    fun findCcidDevices(): List<UsbDevice> {
        return usbManager.deviceList.values.filter { device ->
            (0 until device.interfaceCount).any { idx ->
                device.getInterface(idx).interfaceClass == USB_CLASS_SMART_CARD
            }
        }
    }

    fun hasPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)

    fun requestPermission(device: UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val intent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags)
        usbManager.requestPermission(device, intent)
    }

    fun open(device: UsbDevice): Result<String> {
        close()
        val intf = (0 until device.interfaceCount)
            .map { device.getInterface(it) }
            .firstOrNull { it.interfaceClass == USB_CLASS_SMART_CARD }
            ?: return Result.failure(IllegalStateException("未找到 CCID (0x0B) 接口"))

        var epIn: UsbEndpoint? = null
        var epOut: UsbEndpoint? = null
        for (i in 0 until intf.endpointCount) {
            val ep = intf.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_IN) epIn = ep else epOut = ep
            }
        }
        if (epIn == null || epOut == null) {
            return Result.failure(IllegalStateException("CCID Bulk IN/OUT 端点不完整"))
        }

        val conn = usbManager.openDevice(device)
            ?: return Result.failure(SecurityException("无法打开 USB 设备（请检查权限）"))
        if (!conn.claimInterface(intf, true)) {
            conn.close()
            return Result.failure(IllegalStateException("无法声明 USB CCID 接口"))
        }

        connection = conn
        ccidInterface = intf
        bulkIn = epIn
        bulkOut = epOut
        return powerOn()
    }

    fun powerOn(): Result<String> {
        val header = buildCcidHeader(PC_TO_RDR_ICC_POWER_ON, 0, byteArrayOf(0x00, 0x00, 0x00))
        val resp = transferCcid(header) ?: return Result.failure(IllegalStateException("CCID 上电超时或无响应"))
        val atr = extractDataBlock(resp)
        return Result.success(atr.toHexString())
    }

    fun powerOff() {
        val header = buildCcidHeader(PC_TO_RDR_ICC_POWER_OFF, 0, byteArrayOf(0x00, 0x00, 0x00))
        transferCcid(header)
    }

    fun transmitApdu(apdu: ByteArray): Result<ApduResponse> {
        val header = buildCcidHeader(PC_TO_RDR_XFR_BLOCK, apdu.size, byteArrayOf(0x00, 0x00, 0x00))
        val packet = header + apdu
        val resp = transferCcid(packet) ?: return Result.failure(IllegalStateException("APDU 传输失败"))
        val block = extractDataBlock(resp)
        if (block.size < 2) {
            return Result.failure(IllegalStateException("APDU 响应长度异常: ${block.size}"))
        }
        val sw1 = block[block.size - 2].toInt() and 0xFF
        val sw2 = block[block.size - 1].toInt() and 0xFF
        val data = block.copyOfRange(0, block.size - 2)

        // 自动处理 61 XX (GET RESPONSE)
        if (sw1 == 0x61) {
            val getRespApdu = byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, sw2.toByte())
            return transmitApdu(getRespApdu).map { next ->
                ApduResponse(data + next.data, next.sw1, next.sw2)
            }
        }
        return Result.success(ApduResponse(data, sw1, sw2))
    }

    fun readIccid(): Result<String> {
        // SELECT MF (3F00) -> SELECT EF_ICCID (2FE2) -> READ BINARY (10 bytes)
        transmitApdu("00A40004023F00".hexToBytes()).onFailure { return Result.failure(it) }
        transmitApdu("00A40004022FE2".hexToBytes()).onFailure { return Result.failure(it) }
        val read = transmitApdu("00B000000A".hexToBytes()).getOrElse { return Result.failure(it) }
        return Result.success(decodeBcdSwapped(read.data))
    }

    fun authenticateEapAka(randHex: String, autnHex: String): Result<String> {
        val rand = randHex.hexToBytes()
        val autn = autnHex.hexToBytes()
        val payload = byteArrayOf(rand.size.toByte()) + rand + byteArrayOf(autn.size.toByte()) + autn
        val apdu = byteArrayOf(0x00, 0x88.toByte(), 0x00, 0x81.toByte(), payload.size.toByte()) + payload
        return transmitApdu(apdu).map { it.data.toHexString() }
    }

    fun close() {
        runCatching { powerOff() }
        ccidInterface?.let { intf -> connection?.releaseInterface(intf) }
        connection?.close()
        connection = null
        ccidInterface = null
        bulkIn = null
        bulkOut = null
    }

    private fun buildCcidHeader(msgType: Byte, length: Int, specific: ByteArray): ByteArray {
        val buf = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(msgType)
        buf.putInt(length)
        buf.put(0x00) // Slot 0
        buf.put(seqCounter++)
        buf.put(specific[0])
        buf.put(specific[1])
        buf.put(specific[2])
        return buf.array()
    }

    private fun transferCcid(outPacket: ByteArray): ByteArray? {
        val conn = connection ?: return null
        val epOut = bulkOut ?: return null
        val epIn = bulkIn ?: return null

        val sent = conn.bulkTransfer(epOut, outPacket, outPacket.size, 5000)
        if (sent < 0) return null

        val inBuffer = ByteArray(1024)
        val received = conn.bulkTransfer(epIn, inBuffer, inBuffer.size, 5000)
        if (received < 10) return null
        return inBuffer.copyOfRange(0, received)
    }

    private fun extractDataBlock(ccidResp: ByteArray): ByteArray {
        if (ccidResp.size < 10 || ccidResp[0] != RDR_TO_PC_DATA_BLOCK) return ByteArray(0)
        val len = ByteBuffer.wrap(ccidResp, 1, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val end = (10 + len).coerceAtMost(ccidResp.size)
        return ccidResp.copyOfRange(10, end)
    }

    private fun decodeBcdSwapped(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            val low = b.toInt() and 0x0F
            val high = (b.toInt() ushr 4) and 0x0F
            if (low <= 9) sb.append(low)
            if (high <= 9) sb.append(high)
        }
        return sb.toString()
    }
}

fun String.hexToBytes(): ByteArray {
    val clean = replace(" ", "").replace("\n", "").trim()
    require(clean.length % 2 == 0) { "Hex 字符串长度必须为偶数" }
    return ByteArray(clean.length / 2) { i ->
        clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }
