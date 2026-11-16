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
import com.candies.simhuber.model.EsimProfile
import com.candies.simhuber.model.EsimProfileState
import com.candies.simhuber.model.ProxyRoutingRule
import com.candies.simhuber.model.SmsMessage
import com.candies.simhuber.model.VoWifiState
import com.candies.simhuber.vohive.VoHiveRelayClient
import com.candies.simhuber.vowifi.VoWifiEngine
import com.candies.simhuber.vowifi.VoWifiForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
                repo.appendLog("CCID", "INFO", "已连接读卡器 $devName | ATR=$atr")
                ccidDriver.readIccid().onSuccess { iccid ->
                    repo.appendLog("CCID", "INFO", "读取到物理卡槽 ICCID: $iccid")
                }
            }.onFailure { err ->
                repo.setUsbStatus(false, "连接失败: $devName")
                repo.appendLog("CCID", "ERROR", "读卡器初始化失败: ${err.message}")
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
            repo.appendLog("VoWiFi", "INFO", "已断开 VoWiFi / IMS 注册")
            return
        }

        val card = current.cards.firstOrNull { it.id == current.selectedCardId } ?: current.cards.firstOrNull()
        if (card == null) {
            repo.appendLog("VoWiFi", "ERROR", "当前无可用 SIM 卡槽")
            return
        }
        val rule = relayClient.matchRoutingRule(card.mcc, card.mnc, current.routingRules)
        voWifiJob?.cancel()
        voWifiJob = viewModelScope.launch {
            voWifiEngine.startRegistrationSequence(card, rule).collect { step ->
                repo.setVoWifiState(step.state)
                repo.appendLog("VoWiFi", "INFO", step.log)
                if (step.state == VoWifiState.REGISTERED) {
                    VoWifiForegroundService.startOrUpdate(
                        getApplication(),
                        "${card.carrierName} VoWiFi 已注册"
                    )
                }
            }
        }
    }

    fun probeRelayNode(nodeId: String) {
        val node = uiState.value.relayNodes.firstOrNull { it.id == nodeId } ?: return
        viewModelScope.launch {
            repo.appendLog("VoHive2", "INFO", "正在探测远端节点: ${node.endpointUrl}")
            relayClient.probeRelayNode(node).onSuccess { body ->
                repo.updateRelayNodeStatus(nodeId, true, "在线 ($body)")
                repo.appendLog("VoHive2", "INFO", "远端节点 ${node.name} 响应正常")
            }.onFailure { err ->
                repo.updateRelayNodeStatus(nodeId, false, "离线: ${err.message?.take(36)}")
                repo.appendLog("VoHive2", "WARN", "远端节点探测失败: ${err.message}")
            }
        }
    }

    fun enableEsimProfile(iccid: String) {
        val apdu = lpadEngine.buildEnableProfileApdu(iccid)
        repo.appendLog("LPAd", "INFO", "构造 EnableProfile APDU: $apdu")
        if (uiState.value.usbReaderConnected) {
            viewModelScope.launch(Dispatchers.IO) {
                lpadEngine.selectIsdr()
                ccidDriver.transmitApdu(apdu.hexToBytes()).onSuccess { resp ->
                    repo.appendLog("LPAd", "INFO", "eUICC 响应 SW=${resp.swHex} DATA=${resp.data.toHexString()}")
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
        repo.appendLog("LPAd", "WARN", "构造 DeleteProfile APDU: $apdu")
        repo.deleteEsimProfile(iccid)
    }

    fun sendRawApdu(apduHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bytes = apduHex.hexToBytes()
                if (!uiState.value.usbReaderConnected) {
                    repo.appendLog("APDU", "WARN", "未连接实体读卡器，仅校验格式成功 (${bytes.size} bytes)")
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
        repo.appendLog("IMS", "INFO", "已通过 VoWiFi 发送短信至 $peer")
    }

    fun placeVoWifiCall(number: String) {
        voWifiEngine.validateDialNumber(number).onSuccess { normalized ->
            val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            repo.addCallRecord(
                CallRecord(
                    id = "call_${System.currentTimeMillis()}",
                    cardId = uiState.value.selectedCardId,
                    peerNumber = normalized,
                    direction = "OUTBOUND",
                    durationSec = 0,
                    timestamp = now
                )
            )
            repo.appendLog("IMS", "INFO", "发起 VoWiFi SIP INVITE -> $normalized")
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
                expiryDate = expiry.ifBlank { "2027-01-01" }
            )
        )
    }
}
