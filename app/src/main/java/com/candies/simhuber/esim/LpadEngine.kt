package com.candies.simhuber.esim

import com.candies.simhuber.ccid.UsbCcidDriver
import com.candies.simhuber.ccid.hexToBytes
import com.candies.simhuber.ccid.toHexString

/**
 * GSMA SGP.22 eUICC LPAd (Local Profile Assistant) 核心 APDU 构建与管理引擎。
 * 兼容 eSTKme Air / 5ber / Xesim / 9eSIM 等可插拔 eSIM 卡。
 */
class LpadEngine(private val ccidDriver: UsbCcidDriver?) {

    companion object {
        const val ISDR_AID = "A0000005591010FFFFFFFF8900000100"
        private const val APDU_STORE_DATA_HEADER = "80E29100"
    }

    fun selectIsdr(): Result<String> {
        val driver = ccidDriver ?: return Result.failure(IllegalStateException("未连接物理读卡器"))
        val lenHex = "%02X".format(ISDR_AID.length / 2)
        val apdu = "00A40400$lenHex$ISDR_AID".hexToBytes()
        return driver.transmitApdu(apdu).map { it.data.toHexString() }
    }

    fun buildGetEuiccInfo1Apdu(): String {
        // ES10b.GetEUICCInfo1: Tag BF20 Len 00
        val payload = "BF2000"
        return "${APDU_STORE_DATA_HEADER}%02X$payload".format(payload.length / 2)
    }

    fun buildGetProfilesInfoApdu(): String {
        // ES10c.GetProfilesInfo: Tag BF2D Len 00
        val payload = "BF2D00"
        return "${APDU_STORE_DATA_HEADER}%02X$payload".format(payload.length / 2)
    }

    fun buildEnableProfileApdu(iccid: String): String {
        val swapped = encodeIccidBcdSwapped(iccid)
        // BF31 -> A0 (profileIdentifier) -> 5A 0A <iccid> + 81 01 01 (refreshFlag=true)
        val inner = "A00C5A0A${swapped}810101"
        val tlv = "BF31%02X$inner".format(inner.length / 2)
        return "${APDU_STORE_DATA_HEADER}%02X$tlv".format(tlv.length / 2)
    }

    fun buildDisableProfileApdu(iccid: String): String {
        val swapped = encodeIccidBcdSwapped(iccid)
        val inner = "A00C5A0A${swapped}810101"
        val tlv = "BF32%02X$inner".format(inner.length / 2)
        return "${APDU_STORE_DATA_HEADER}%02X$tlv".format(tlv.length / 2)
    }

    fun buildDeleteProfileApdu(iccid: String): String {
        val swapped = encodeIccidBcdSwapped(iccid)
        val inner = "5A0A$swapped"
        val tlv = "BF33%02X$inner".format(inner.length / 2)
        return "${APDU_STORE_DATA_HEADER}%02X$tlv".format(tlv.length / 2)
    }

    /**
     * 解析 LPA 激活码格式: LPA:1$<SM-DP+>$<MatchingID>[$<OID>[$<ConfirmCode>]]
     */
    data class ParsedActivationCode(
        val smdpAddress: String,
        val matchingId: String,
        val oid: String? = null
    )

    fun parseLpaUri(lpaUri: String): Result<ParsedActivationCode> {
        val trimmed = lpaUri.trim()
        if (!trimmed.startsWith("LPA:1$", ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("无效的 LPA 激活码，应以 LPA:1$ 开头"))
        }
        val parts = trimmed.substring(6).split("$")
        val smdp = parts.getOrNull(0).orEmpty()
        val matchingId = parts.getOrNull(1).orEmpty()
        if (smdp.isBlank() || matchingId.isBlank()) {
            return Result.failure(IllegalArgumentException("LPA 激活码缺少 SM-DP+ 地址或 MatchingID"))
        }
        return Result.success(ParsedActivationCode(smdp, matchingId, parts.getOrNull(2)))
    }

    private fun encodeIccidBcdSwapped(iccid: String): String {
        val padded = iccid.padEnd(20, 'F').take(20)
        val sb = StringBuilder()
        for (i in padded.indices step 2) {
            sb.append(padded[i + 1]).append(padded[i])
        }
        return sb.toString()
    }
}
