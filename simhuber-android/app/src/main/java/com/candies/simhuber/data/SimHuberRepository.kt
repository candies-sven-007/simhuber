package com.candies.simhuber.data

import android.content.Context
import com.candies.simhuber.model.CallRecord
import com.candies.simhuber.model.CardLedgerItem
import com.candies.simhuber.model.EsimProfile
import com.candies.simhuber.model.EsimProfileState
import com.candies.simhuber.model.JournalLogEntry
import com.candies.simhuber.model.ProxyRoutingRule
import com.candies.simhuber.model.ReaderSourceType
import com.candies.simhuber.model.SimCardInfo
import com.candies.simhuber.model.SmsMessage
import com.candies.simhuber.model.VoHiveRelayNode
import com.candies.simhuber.model.VoWifiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class SimHuberUiState(
    val cards: List<SimCardInfo> = emptyList(),
    val selectedCardId: String = "",
    val esimProfiles: List<EsimProfile> = emptyList(),
    val routingRules: List<ProxyRoutingRule> = emptyList(),
    val relayNodes: List<VoHiveRelayNode> = emptyList(),
    val smsList: List<SmsMessage> = emptyList(),
    val callRecords: List<CallRecord> = emptyList(),
    val ledgerItems: List<CardLedgerItem> = emptyList(),
    val voWifiState: VoWifiState = VoWifiState.DISCONNECTED,
    val usbReaderConnected: Boolean = false,
    val usbReaderName: String = "未检测到 USB CCID 读卡器",
    val logs: List<JournalLogEntry> = emptyList()
)

class SimHuberRepository(context: Context) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val stateFile = File(context.filesDir, "simhuber_state.json")
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<SimHuberUiState> = _state.asStateFlow()

    private fun loadInitialState(): SimHuberUiState {
        if (stateFile.exists()) {
            runCatching {
                return json.decodeFromString<SimHuberUiState>(stateFile.readText())
            }
        }
        return defaultSeedState()
    }

    private fun persist() {
        runCatching {
            stateFile.writeText(json.encodeToString(_state.value))
        }
    }

    fun appendLog(tag: String, level: String, message: String) {
        val entry = JournalLogEntry(timeFmt.format(Date()), tag, level, message)
        _state.update { s ->
            s.copy(logs = (listOf(entry) + s.logs).take(120))
        }
    }

    fun selectCard(cardId: String) {
        _state.update { it.copy(selectedCardId = cardId) }
        persist()
    }

    fun setUsbStatus(connected: Boolean, name: String) {
        _state.update { it.copy(usbReaderConnected = connected, usbReaderName = name) }
    }

    fun setVoWifiState(newState: VoWifiState) {
        _state.update { it.copy(voWifiState = newState) }
    }

    fun toggleEsimProfile(iccid: String) {
        _state.update { s ->
            val updated = s.esimProfiles.map { p ->
                if (p.iccid == iccid) {
                    p.copy(state = EsimProfileState.ENABLED)
                } else {
                    p.copy(state = EsimProfileState.DISABLED)
                }
            }
            s.copy(esimProfiles = updated)
        }
        appendLog("LPAd", "INFO", "已切换激活 eSIM Profile: ...${iccid.takeLast(6)}")
        persist()
    }

    fun addEsimProfile(profile: EsimProfile) {
        _state.update { s -> s.copy(esimProfiles = listOf(profile) + s.esimProfiles) }
        appendLog("LPAd", "INFO", "已下载新 eSIM Profile: ${profile.nickname} (${profile.providerName})")
        persist()
    }

    fun deleteEsimProfile(iccid: String) {
        _state.update { s -> s.copy(esimProfiles = s.esimProfiles.filterNot { it.iccid == iccid }) }
        appendLog("LPAd", "WARN", "已删除 eSIM Profile: ...${iccid.takeLast(6)}")
        persist()
    }

    fun addRoutingRule(rule: ProxyRoutingRule) {
        _state.update { s -> s.copy(routingRules = listOf(rule) + s.routingRules) }
        appendLog("VoHive2", "INFO", "新增 MCC ${rule.mcc} 分流规则 -> ${rule.upstreamType} ${rule.proxyHost}:${rule.proxyPort}")
        persist()
    }

    fun toggleRoutingRule(ruleId: String) {
        _state.update { s ->
            s.copy(routingRules = s.routingRules.map {
                if (it.id == ruleId) it.copy(enabled = !it.enabled) else it
            })
        }
        persist()
    }

    fun updateRelayNodeStatus(nodeId: String, connected: Boolean, statusText: String) {
        _state.update { s ->
            s.copy(relayNodes = s.relayNodes.map {
                if (it.id == nodeId) it.copy(connected = connected, remoteModemStatus = statusText, lastSyncTime = timeFmt.format(Date()))
                else it
            })
        }
        persist()
    }

    fun addSms(sms: SmsMessage) {
        _state.update { s -> s.copy(smsList = listOf(sms) + s.smsList) }
        persist()
    }

    fun addCallRecord(record: CallRecord) {
        _state.update { s -> s.copy(callRecords = listOf(record) + s.callRecords) }
        persist()
    }

    fun addLedgerItem(item: CardLedgerItem) {
        _state.update { s -> s.copy(ledgerItems = listOf(item) + s.ledgerItems) }
        persist()
    }

    private fun defaultSeedState(): SimHuberUiState {
        return SimHuberUiState(
            cards = listOf(
                SimCardInfo(
                    id = "card_estk_1",
                    label = "eSTKme Air (主槽)",
                    iccid = "89630220260011223344",
                    imsi = "515021234567890",
                    mcc = "515",
                    mnc = "02",
                    carrierName = "Globe Telecom (PH)",
                    isEsimChip = true,
                    eid = "89049032001001234567890123456789",
                    sourceType = ReaderSourceType.USB_CCID
                ),
                SimCardInfo(
                    id = "card_voda_de",
                    label = "OpenStick VoHive2 中继",
                    iccid = "89492000260099887766",
                    imsi = "262029876543210",
                    mcc = "262",
                    mnc = "02",
                    carrierName = "Vodafone Germany",
                    isEsimChip = false,
                    sourceType = ReaderSourceType.VOHIVE2_RELAY
                )
            ),
            selectedCardId = "card_estk_1",
            esimProfiles = listOf(
                EsimProfile("89630220260011223344", "Globe PH 保号卡", "Globe Telecom", EsimProfileState.ENABLED, "51502"),
                EsimProfile("89441000260055667788", "CMLink UK 漫游", "China Mobile UK", EsimProfileState.DISABLED, "23410"),
                EsimProfile("89601800260044332211", "U Mobile MY", "U Mobile", EsimProfileState.DISABLED, "50218")
            ),
            routingRules = listOf(
                ProxyRoutingRule("rule_515", "菲律宾 Globe 专用出口", "515", "02", "SOCKS5", "127.0.0.1", 10808, enableCloudflareTurn = true, enableTunMode = false),
                ProxyRoutingRule("rule_262", "德国 Vodafone 住宅代理", "262", "*", "SOCKS5", "100.64.0.10", 10809, enableCloudflareTurn = true, enableTunMode = true)
            ),
            relayNodes = listOf(
                VoHiveRelayNode("node_openstick", "OpenStick Snapdragon 410 (Tailscale)", "http://100.64.0.12:8080", connected = false, remoteModemStatus = "待探测")
            ),
            smsList = listOf(
                SmsMessage("sms_1", "card_estk_1", "Globe", "Welcome to Globe VoWiFi! Your balance is PHP 120.00.", "11:20", isIncoming = true, archivedFromVoHive = true),
                SmsMessage("sms_2", "card_voda_de", "+491722270333", "Vodafone Sicherheits-Code: 482910", "10:05", isIncoming = true, archivedFromVoHive = true)
            ),
            callRecords = listOf(
                CallRecord("call_1", "card_estk_1", "+639171234567", "OUTBOUND", 42, "昨天 21:15")
            ),
            ledgerItems = listOf(
                CardLedgerItem("ldg_1", "Globe PH", "3344", "Globe Telecom", "Prepaid 余额保号", "10", "PHP", "每月 15 日", "2027-05-15", "需每 180 天有余额变动"),
                CardLedgerItem("ldg_2", "Vodafone DE", "7766", "Vodafone", "CallYa Classic", "0", "EUR", "按量计费", "2027-11-01", "VoWiFi 接码专用")
            ),
            logs = listOf(
                JournalLogEntry("11:40:00", "SIMHuber", "INFO", "初始化完成：已加载 USB CCID 驱动与 VoHive2 分流模块")
            )
        )
    }
}
