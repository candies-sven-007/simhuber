package com.candies.simhuber.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReaderSourceType {
    USB_CCID,
    BLUETOOTH_CCID,
    VOHIVE2_RELAY,
    SIMULATOR
}

@Serializable
enum class EsimProfileState {
    ENABLED,
    DISABLED
}

@Serializable
enum class VoWifiState {
    DISCONNECTED,
    RESOLVING_EPDG,
    IKE_SA_INIT,
    EAP_AKA_AUTH,
    IMS_REGISTERING,
    REGISTERED,
    ERROR
}

@Serializable
data class SimCardInfo(
    val id: String,
    val label: String,
    val iccid: String,
    val imsi: String,
    val mcc: String,
    val mnc: String,
    val countryCode: String,
    val carrierName: String,
    val phoneNumber: String = "",
    val balanceText: String = "",
    val expiryText: String = "",
    val isEsimChip: Boolean = false,
    val eid: String? = null,
    val sourceType: ReaderSourceType = ReaderSourceType.USB_CCID,
    val cardThemeIndex: Int = 0
)

@Serializable
data class EsimProfile(
    val iccid: String,
    val nickname: String,
    val providerName: String,
    val state: EsimProfileState,
    val mccMnc: String,
    val isdpAid: String = "A0000005591010FFFFFFFF8900000100"
)

@Serializable
data class CarrierPresetInfo(
    val id: String,
    val bundleName: String,
    val plmn: String,
    val epdgAddress: String?,
    val imsDomain: String?,
    val apn: String?,
    val ipStack: String?,
    val ikeCipher: String,
    val natKeepaliveSeconds: Int
)

@Serializable
data class MccMncEntry(
    val mcc: String,
    val mnc: String,
    val countryCode: String,
    val operatorName: String,
    val brand: String,
    val bands: String
)

@Serializable
data class ProxyRoutingRule(
    val id: String,
    val name: String,
    val mcc: String,
    val mnc: String = "*",
    val upstreamType: String = "SOCKS5",
    val proxyHost: String = "127.0.0.1",
    val proxyPort: Int = 10808,
    val proxyUser: String = "",
    val proxyPass: String = "",
    val enableCloudflareTurn: Boolean = true,
    val enableTunMode: Boolean = false,
    val enabled: Boolean = true
)

@Serializable
data class VoHiveRelayNode(
    val id: String,
    val name: String,
    val deviceGeneration: String = "OpenStick-410",
    val endpointUrl: String,
    val apiToken: String = "",
    val connected: Boolean = false,
    val remoteModemStatus: String = "未探测",
    val lastSyncTime: String = "--"
)

@Serializable
data class ImsHealthEvent(
    val id: String,
    val timestamp: String,
    val deviceGeneration: String, // 按设备代际隔离: USB-CCID / OpenStick-410 / VPS-Relay
    val stage: String,            // ePDG / IKEv2 / EAP-AKA / SIP-REG / KEEPALIVE
    val latencyMs: Int,
    val healthy: Boolean,
    val detail: String
)

@Serializable
data class SmsMessage(
    val id: String,
    val cardId: String,
    val peerNumber: String,
    val body: String,
    val timestamp: String,
    val isIncoming: Boolean,
    val encoding: String = "UCS2",
    val archivedFromVoHive: Boolean = false
)

@Serializable
data class CallRecord(
    val id: String,
    val cardId: String,
    val peerNumber: String,
    val direction: String,
    val durationSec: Int,
    val timestamp: String,
    val hasRecording: Boolean = false,
    val recordingFileName: String? = null
)

@Serializable
data class CardLedgerItem(
    val id: String,
    val cardLabel: String,
    val iccidSuffix: String,
    val carrier: String,
    val planName: String,
    val monthlyCost: String,
    val currency: String,
    val renewalDate: String,
    val expiryDate: String,
    val daysRemaining: Int = 180,
    val notes: String = ""
)

@Serializable
data class JournalLogEntry(
    val timestamp: String,
    val tag: String,
    val level: String,
    val message: String
)
