package com.candies.simhuber.vohive

import com.candies.simhuber.model.ProxyRoutingRule
import com.candies.simhuber.model.VoHiveRelayNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * VoHive2 远程中继与代理分流引擎：
 * 1. 通过局域网或 Tailscale IP 对接 OpenStick / Linux VoHive2 节点；
 * 2. 根据 SIM 卡的 MCC/MNC 自动匹配 SOCKS5 上游代理、Cloudflare TURN 与 TUN 模式。
 */
class VoHiveRelayClient {

    fun buildClientForRule(rule: ProxyRoutingRule?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)

        if (rule != null && rule.enabled && rule.upstreamType != "DIRECT") {
            val proxyType = if (rule.upstreamType == "SOCKS5") Proxy.Type.SOCKS else Proxy.Type.HTTP
            builder.proxy(Proxy(proxyType, InetSocketAddress(rule.proxyHost, rule.proxyPort)))
        }
        return builder.build()
    }

    fun matchRoutingRule(mcc: String, mnc: String, rules: List<ProxyRoutingRule>): ProxyRoutingRule? {
        return rules.firstOrNull { it.enabled && it.mcc == mcc && (it.mnc == "*" || it.mnc == mnc) }
            ?: rules.firstOrNull { it.enabled && it.mcc == "*" }
    }

    suspend fun probeRelayNode(node: VoHiveRelayNode): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val client = OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .build()

            val url = node.endpointUrl.trimEnd('/') + "/api/status"
            val reqBuilder = Request.Builder().url(url).get()
            if (node.apiToken.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${node.apiToken}")
            }
            client.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: ${resp.message}")
                }
                resp.body?.string().orEmpty().ifBlank { "OK (200)" }
            }
        }
    }
}
