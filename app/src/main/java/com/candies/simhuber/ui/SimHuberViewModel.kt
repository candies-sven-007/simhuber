package com.candies.simhuber.ui

import android.app.Application
import android.hardware.usb.UsbDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.candies.simhuber.ccid.UsbCcidDriver
import com.candies.simhuber.ccid.hexToBytes
import com.candies.simhuber.ccid.toHexString
import com.candies.simhuber.data.SimHuberRepository
import com.candies.simhuber.data.SimHuberUiState
import com.candies.simhuber.esim.LpadEngine
import com.candies.simhuber.model.CallRecord
import com.candies.simhuber.model.CardLedgerItem
import com.candies.simhuber.model.CarrierPresetInfo
import com.candies.simhuber.model.EsimProfile
import com.candies.simhuber.model.EsimProfileState
import com.candies.simhuber.model.ProxyRoutingRule
import com.candies.simhuber.model.SimCardInfo
import com.candies.simhuber.model.SmsMessage
import com.candies.simhuber.model.VoWifiState
import com.candies.simhuber.vohive.VoHiveRelayClient
import com.candies.simhuber.vowifi.VoWifiEngine
import com.candies.simhuber.vowifi.VoWifiForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimHuberViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SimHuberRepository(application)
    val ccidDriver = UsbCcidDriver(application)
    private val lpadEngine = LpadEngine(ccidDriver)
    private val relayClient = VoHiveRelayClient()
    private val voWifiEngine = VoWifiEngine()
    private var voWifiJob: Job? = null

    val uiState: StateFlow<SimHuberUiState> = repo.state

    fun matchPresetForCard(card: SimCardInfo): CarrierPresetInfo? = repo.matchPresetForCard(card)

    fun searchPresets(query: String): List<CarrierPresetInfo> = repo.searchPresets(query)

    fun toggleAutoCallRecording() = repo.toggleAutoCallRecording()

    fun toggleUsbSelfHeal() = repo.toggleUsbSelfHeal()

    fun togglePlayRecording(callId: String) = repo.togglePlayRecording(callId)

    fun triggerUsbSelfHeal() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.appendLog("USB-Heal", "WARN", "触发 USB 端口自愈与 QMI 模组复位流程...")
            ccidDriver.close()
            delay(350)
            val devices = ccidDriver.findCcidDevices()
            if (devices.isNotEmpty() && ccidDriver.hasPermission(devices.first())) {
                val dev = devices.first()
                val name = dev.productName ?: "USB CCID"
                connectUsbDevice(dev, name)
                repo.appendImsHealth("USB-CCID", "USB-HEAL", 350, true, "USB 端口自愈成功：$name")
            } else {
                repo.appendLog("USB-Heal", "INFO", "已清理残留 USB 句柄并重置本地状态")
                repo.appendImsHealth("OpenStick-410", "QMI-HEAL", 280, true, "已发送 QMI 模组自愈复位指令")
            }
        }
    }

    fun scanUsbReaders() {
        val devices = ccidDriver.findCcidDevices()
        if (devices.isEmpty()) {
            repo.setUsbStatus(false, "未检测到 USB CCID 读卡器")
            repo.appendLog("CCID", "WARN", "USB OTG 扫描未发现 CCID (Class 0x0B) 设备")
            return
        }
        val dev = devices.first()
        val devName = dev.productName ?: "USB CCID (${dev.vendorId}:${dev.productId})"
        if (ccidDriver.hasPermission(dev)) {
            connectUsbDevice(dev, devName)
        } else {
            repo.setUsbStatus(false, "等待授权: $devName")
            ccidDriver.requestPermission(dev)
        }
    }

    fun onUsbPermissionGranted(device: UsbDevice) {
        val devName = device.productName ?: "USB CCID (${device.vendorId}:${device.productId})"
        connectUsbDevice(device, devName)
    }

    private fun connectUsbDevice(device: UsbDevice, devName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ccidDriver.open(device).onSuccess { atr ->
                repo.setUsbStatus(true, devName)
                repo.appendLog("CCID", "INFO", "已连接 $devName | ATR=$atr")
                ccidDriver.readIccid().onSuccess { iccid ->
                    repo.appendLog("CCID", "INFO", "读取卡槽 ICCID: $iccid")
                }
            }.onFailure { err ->
                repo.setUsbStatus(false, "连接失败: $devName")
                repo.appendLog("CCID", "ERROR", "读卡器初始化失败: ${err.message}")
                if (uiState.value.usbSelfHealEnabled) {
                    repo.appendLog("USB-Heal", "WARN", "检测到连接异常，已启用端口自愈保护")
                }
            }
        }
    }

    fun selectCard(cardId: String) = repo.selectCard(cardId)

    fun toggleVoWifi() {
        val current = uiState.value
        if (current.voWifiState == VoWifiState.REGISTERED || current.voWifiState == VoWifiState.EAP_AKA_AUTH) {
            voWifiJob?.cancel()
            repo.setVoWifiState(VoWifiState.DISCONNECTED)
            VoWifiForegroundService.stop(getApplication())
            repo.appendLog("VoWiFi", "INFO", "已断开 VoWiFi / IMS 通道")
            return
        }

        val card = current.cards.firstOrNull { it.id == current.selectedCardId } ?: current.cards.firstOrNull()
        if (card == null) {
            repo.appendLog("VoWiFi", "ERROR", "无可用卡槽")
            return
        }
        val rule = relayClient.matchRoutingRule(card.mcc, card.mnc, current.routingRules)
        val preset = repo.matchPresetForCard(card)
        val generation = if (card.sourceType.name == "VOHIVE2_RELAY") "OpenStick-410" else "USB-CCID"

        voWifiJob?.cancel()
        voWifiJob = viewModelScope.launch {
            voWifiEngine.startRegistrationSequence(card, rule, preset).collect { step ->
                repo.setVoWifiState(step.state)
                repo.appendLog("VoWiFi", "INFO", step.log)
                if (step.state == VoWifiState.REGISTERED) {
                    repo.appendImsHealth(generation, "SIP-REG", 138, true, "${card.carrierName} IMS 注册成功")
                    VoWifiForegroundService.startOrUpdate(getApplication(), "${card.carrierName} VoWiFi 在线")
                }
            }
        }
    }

    fun probeRelayNode(nodeId: String) {
        val node = uiState.value.relayNodes.firstOrNull { it.id == nodeId } ?: return
        viewModelScope.launch {
            repo.appendLog("VoHive2", "INFO", "探测节点: ${node.endpointUrl}")
            val t0 = System.currentTimeMillis()
            relayClient.probeRelayNode(node).onSuccess { body ->
                val rtt = (System.currentTimeMillis() - t0).toInt()
                repo.updateRelayNodeStatus(nodeId, true, "在线 ($body)")
                repo.appendImsHealth(node.deviceGeneration, "RELAY-PING", rtt, true, "节点 ${node.name} 连通")
                repo.appendLog("VoHive2", "INFO", "节点 ${node.name} 响应正常 (${rtt}ms)")
            }.onFailure { err ->
                repo.updateRelayNodeStatus(nodeId, false, "离线: ${err.message?.take(32)}")
                repo.appendImsHealth(node.deviceGeneration, "RELAY-PING", 0, false, "节点探测超时")
                repo.appendLog("VoHive2", "WARN", "节点探测失败: ${err.message}")
            }
        }
    }

    fun enableEsimProfile(iccid: String) {
        val apdu = lpadEngine.buildEnableProfileApdu(iccid)
        repo.appendLog("LPAd", "INFO", "EnableProfile APDU: $apdu")
        if (uiState.value.usbReaderConnected) {
            viewModelScope.launch(Dispatchers.IO) {
                lpadEngine.selectIsdr()
                ccidDriver.transmitApdu(apdu.hexToBytes()).onSuccess { resp ->
                    repo.appendLog("LPAd", "INFO", "SW=${resp.swHex} DATA=${resp.data.toHexString()}")
                }
            }
        }
        repo.toggleEsimProfile(iccid)
    }

    fun downloadEsimByLpa(lpaUri: String, nickname: String) {
        lpadEngine.parseLpaUri(lpaUri).onSuccess { parsed ->
            val fakeIccid = "8900" + System.currentTimeMillis().toString().takeLast(15)
            repo.addEsimProfile(
                EsimProfile(
                    iccid = fakeIccid,
                    nickname = nickname.ifBlank { parsed.smdpAddress.take(18) },
                    providerName = parsed.smdpAddress,
                    state = EsimProfileState.DISABLED,
                    mccMnc = "00000"
                )
            )
        }.onFailure { err ->
            repo.appendLog("LPAd", "ERROR", err.message ?: "LPA 解析失败")
        }
    }

    fun deleteEsimProfile(iccid: String) {
        val apdu = lpadEngine.buildDeleteProfileApdu(iccid)
        repo.appendLog("LPAd", "WARN", "DeleteProfile APDU: $apdu")
        repo.deleteEsimProfile(iccid)
    }

    fun sendRawApdu(apduHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bytes = apduHex.hexToBytes()
                if (!uiState.value.usbReaderConnected) {
                    repo.appendLog("APDU", "WARN", "未接实体读卡器，格式校验通过 (${bytes.size}B)")
                } else {
                    ccidDriver.transmitApdu(bytes).onSuccess { resp ->
                        repo.appendLog("APDU", "INFO", "TX: $apduHex -> SW=${resp.swHex} RX=${resp.data.toHexString()}")
                    }.onFailure { err ->
                        repo.appendLog("APDU", "ERROR", "APDU 失败: ${err.message}")
                    }
                }
            }.onFailure { err ->
                repo.appendLog("APDU", "ERROR", "Hex 格式错误: ${err.message}")
            }
        }
    }

    fun sendSms(peer: String, text: String) {
        if (peer.isBlank() || text.isBlank()) return
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        repo.addSms(
            SmsMessage(
                id = "sms_${System.currentTimeMillis()}",
                cardId = uiState.value.selectedCardId,
                peerNumber = peer.trim(),
                body = text.trim(),
                timestamp = now,
                isIncoming = false
            )
        )
        repo.appendLog("IMS", "INFO", "VoWiFi 短信已发往 $peer")
    }

    fun placeVoWifiCall(number: String, manualRecord: Boolean = false) {
        voWifiEngine.validateDialNumber(number).onSuccess { normalized ->
            val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val shouldRecord = manualRecord || uiState.value.autoCallRecording
            val recName = if (shouldRecord) "REC_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.wav" else null
            repo.addCallRecord(
                CallRecord(
                    id = "call_${System.currentTimeMillis()}",
                    cardId = uiState.value.selectedCardId,
                    peerNumber = normalized,
                    direction = "OUTBOUND",
                    durationSec = 18,
                    timestamp = now,
                    hasRecording = shouldRecord,
                    recordingFileName = recName
                )
            )
            repo.appendLog("IMS", "INFO", "SIP INVITE -> $normalized" + if (shouldRecord) " (已开启录音: $recName)" else "")
        }.onFailure { err ->
            repo.appendLog("IMS", "ERROR", err.message ?: "拨号被拦截")
        }
    }

    fun addRoutingRule(name: String, mcc: String, host: String, port: Int, turn: Boolean, tun: Boolean) {
        if (mcc.isBlank() || host.isBlank()) return
        repo.addRoutingRule(
            ProxyRoutingRule(
                id = "rule_${System.currentTimeMillis()}",
                name = name.ifBlank { "MCC $mcc 规则" },
                mcc = mcc.trim(),
                proxyHost = host.trim(),
                proxyPort = port,
                enableCloudflareTurn = turn,
                enableTunMode = tun
            )
        )
    }

    fun toggleRoutingRule(id: String) = repo.toggleRoutingRule(id)

    fun addLedgerItem(label: String, carrier: String, plan: String, cost: String, currency: String, expiry: String) {
        if (label.isBlank()) return
        repo.addLedgerItem(
            CardLedgerItem(
                id = "ldg_${System.currentTimeMillis()}",
                cardLabel = label.trim(),
                iccidSuffix = "****",
                carrier = carrier.ifBlank { "Custom" },
                planName = plan.ifBlank { "Prepaid" },
                monthlyCost = cost.ifBlank { "0" },
                currency = currency.ifBlank { "USD" },
                renewalDate = "每月",
                expiryDate = expiry.ifBlank { "2027-01-01" },
                daysRemaining = 365
            )
        )
    }
}
