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
    val carrierName: String,
    val isEsimChip: Boolean = false,
    val eid: String? = null,
    val sourceType: ReaderSourceType = ReaderSourceType.USB_CCID,
    val atrHex: String = "",
    val pinLocked: Boolean = false,
    val pinRemainingAttempts: Int = 3
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
data class ProxyRoutingRule(
    val id: String,
    val name: String,
    val mcc: String,
    val mnc: String = "*",
    val upstreamType: String = "SOCKS5", // SOCKS5 | HTTP | DIRECT
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
    val endpointUrl: String, // e.g. http://100.64.0.12:8080 (Tailscale) or LAN IP
    val apiToken: String = "",
    val connected: Boolean = false,
    val remoteModemStatus: String = "未连接",
    val lastSyncTime: String = "--"
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
    val direction: String, // INBOUND | OUTBOUND | MISSED
    val durationSec: Int,
    val timestamp: String
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
    val notes: String = ""
)

@Serializable
data class JournalLogEntry(
    val timestamp: String,
    val tag: String,
    val level: String, // INFO | WARN | ERROR
    val message: String
)
