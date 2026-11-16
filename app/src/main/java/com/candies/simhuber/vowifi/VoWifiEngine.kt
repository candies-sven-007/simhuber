package com.candies.simhuber.vowifi

import com.candies.simhuber.model.CarrierPresetInfo
import com.candies.simhuber.model.ProxyRoutingRule
import com.candies.simhuber.model.SimCardInfo
import com.candies.simhuber.model.VoWifiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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

    fun validateDialNumber(number: String): Result<String> {
        val normalized = number.replace(" ", "").replace("-", "")
        if (normalized.isBlank()) return Result.failure(IllegalArgumentException("号码不能为空"))
        if (normalized in setOf("110", "119", "120", "112", "911", "999")) {
            return Result.failure(IllegalStateException("VoWiFi 通道不支持紧急呼叫，请使用系统蜂窝拨号"))
        }
        if (normalized.startsWith("+86") || normalized.startsWith("0086")) {
            return Result.failure(IllegalStateException("合规策略限制：已屏蔽拨打中国大陆 (+86) 号码"))
        }
        return Result.success(normalized)
    }

    data class RegistrationStep(val state: VoWifiState, val log: String)

    fun startRegistrationSequence(
        card: SimCardInfo,
        rule: ProxyRoutingRule?,
        preset: CarrierPresetInfo?
    ): Flow<RegistrationStep> = flow {
        val epdg = preset?.epdgAddress ?: buildEpdgFqdn(card.mcc, card.mnc)
        val ims = preset?.imsDomain ?: buildImsDomain(card.mcc, card.mnc)
        val ikeProfile = preset?.ikeCipher ?: "aes256 / sha256 / modp2048"
        val keepalive = preset?.natKeepaliveSeconds ?: 20
        val proxyDesc = if (rule != null && rule.enabled) {
            "${rule.upstreamType} ${rule.proxyHost}:${rule.proxyPort} (TURN=${rule.enableCloudflareTurn}, TUN=${rule.enableTunMode})"
        } else {
            "DIRECT"
        }

        emit(RegistrationStep(VoWifiState.RESOLVING_EPDG, "匹配预设 [${preset?.bundleName ?: "3GPP默认"}] ePDG: $epdg | 出口: $proxyDesc"))
        delay(400)
        emit(RegistrationStep(VoWifiState.IKE_SA_INIT, "IKE_SA_INIT 协商 ($ikeProfile | Keepalive=${keepalive}s)"))
        delay(400)
        emit(RegistrationStep(VoWifiState.EAP_AKA_AUTH, "通过 ${card.sourceType.name} 转发 EAP-AKA 鉴权 (IMSI: ${card.imsi.take(6)}***)"))
        delay(500)
        emit(RegistrationStep(VoWifiState.IMS_REGISTERING, "IPsec SA 建立，向 $ims 发起 SIP REGISTER (APN=${preset?.apn ?: "ims"})"))
        delay(400)
        emit(RegistrationStep(VoWifiState.REGISTERED, "VoWiFi 注册成功！${card.carrierName} 已就绪"))
    }
}
