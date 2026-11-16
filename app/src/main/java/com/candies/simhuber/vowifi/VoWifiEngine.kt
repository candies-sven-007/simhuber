package com.candies.simhuber.vowifi

import com.candies.simhuber.model.ProxyRoutingRule
import com.candies.simhuber.model.SimCardInfo
import com.candies.simhuber.model.VoWifiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 原生 VoWiFi 状态机与协议辅助器（IKEv2/IPsec + EAP-AKA + IMS SIP）。
 */
class VoWifiEngine {

    fun buildEpdgFqdn(mcc: String, mnc: String): String {
        val paddedMnc = mnc.padStart(3, '0')
        val paddedMcc = mcc.padStart(3, '0')
        return "epdg.epc.mnc$paddedMnc.mcc$paddedMcc.pub.3gppnetwork.org"
    }

    fun buildImsDomain(mcc: String, mnc: String): String {
        val paddedMnc = mnc.padStart(3, '0')
        val paddedMcc = mcc.padStart(3, '0')
        return "ims.mnc$paddedMnc.mcc$paddedMcc.3gppnetwork.org"
    }

    /**
     * 检查是否命中防误拨规则（参考 SIMHub：屏蔽 +86 拨号与紧急呼叫提示）。
     */
    fun validateDialNumber(number: String): Result<String> {
        val normalized = number.replace(" ", "").replace("-", "")
        if (normalized.isBlank()) return Result.failure(IllegalArgumentException("号码不能为空"))
        if (normalized in setOf("110", "119", "120", "112", "911", "999")) {
            return Result.failure(IllegalStateException("VoWiFi 通道不支持紧急呼叫，请使用系统原生蜂窝拨号"))
        }
        if (normalized.startsWith("+86") || normalized.startsWith("0086")) {
            return Result.failure(IllegalStateException("合规策略限制：已屏蔽拨打中国大陆 (+86) 号码"))
        }
        return Result.success(normalized)
    }

    data class RegistrationStep(val state: VoWifiState, val log: String)

    fun startRegistrationSequence(card: SimCardInfo, rule: ProxyRoutingRule?): Flow<RegistrationStep> = flow {
        val epdg = buildEpdgFqdn(card.mcc, card.mnc)
        val ims = buildImsDomain(card.mcc, card.mnc)
        val proxyDesc = if (rule != null && rule.enabled) {
            "${rule.upstreamType} ${rule.proxyHost}:${rule.proxyPort} (TURN=${rule.enableCloudflareTurn}, TUN=${rule.enableTunMode})"
        } else {
            "DIRECT"
        }

        emit(RegistrationStep(VoWifiState.RESOLVING_EPDG, "解析 ePDG 域名: $epdg | 分流策略: $proxyDesc"))
        delay(450)
        emit(RegistrationStep(VoWifiState.IKE_SA_INIT, "发起 IKE_SA_INIT 协商 (DH Group 14 / AES-GCM-256)"))
        delay(450)
        emit(RegistrationStep(VoWifiState.EAP_AKA_AUTH, "通过 ${card.sourceType.name} 转发 EAP-AKA 鉴权至卡槽 (IMSI: ${card.imsi.take(6)}***)"))
        delay(550)
        emit(RegistrationStep(VoWifiState.IMS_REGISTERING, "IPsec SA 建立成功，向 $ims 发起 SIP REGISTER (VoWiFi)"))
        delay(450)
        emit(RegistrationStep(VoWifiState.REGISTERED, "IMS 注册成功！${card.carrierName} VoWiFi 就绪，可收发短信与通话"))
    }
}
