package com.candies.simhuber.data

import android.content.Context
import com.candies.simhuber.model.CarrierPresetInfo
import com.candies.simhuber.model.MccMncEntry
import org.json.JSONArray
import org.json.JSONObject

/**
 * 加载内置的 1,343 条 VoWiFi 运营商预设与 3,082 条 MCC/MNC 数据库。
 */
class CarrierPresetStore(private val context: Context) {

    val presets: List<CarrierPresetInfo> by lazy { loadCarrierPresets() }
    val mccMncList: List<MccMncEntry> by lazy { loadMccMnc() }

    fun findPresetByPlmn(mcc: String, mnc: String): CarrierPresetInfo? {
        val plmn = mcc + mnc
        return presets.firstOrNull { it.plmn == plmn }
            ?: presets.firstOrNull { it.plmn.startsWith(mcc) }
    }

    fun searchPresets(query: String, limit: Int = 30): List<CarrierPresetInfo> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return presets.take(limit)
        return presets.filter {
            it.bundleName.lowercase().contains(q) ||
                it.plmn.contains(q) ||
                (it.epdgAddress?.lowercase()?.contains(q) == true)
        }.take(limit)
    }

    fun searchOperators(query: String, limit: Int = 30): List<MccMncEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return mccMncList.take(limit)
        return mccMncList.filter {
            (it.mcc + it.mnc).contains(q) ||
                it.countryCode.lowercase().contains(q) ||
                it.brand.lowercase().contains(q) ||
                it.operatorName.lowercase().contains(q)
        }.take(limit)
    }

    private fun loadCarrierPresets(): List<CarrierPresetInfo> {
        return runCatching {
            val raw = context.assets.open("vowifi/carrier_presets.json").bufferedReader().use { it.readText() }
            val root = JSONObject(raw)
            val arr = root.getJSONArray("presets")
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val enc = o.optJSONArray("ikeEncryption")?.optString(0) ?: "aes256"
                    val intg = o.optJSONArray("ikeIntegrity")?.optString(0) ?: "sha256"
                    val dh = o.optJSONArray("ikeDH")?.optString(0) ?: "modp2048"
                    add(
                        CarrierPresetInfo(
                            id = o.optString("id"),
                            bundleName = o.optString("bundleName"),
                            plmn = o.optString("plmn"),
                            epdgAddress = o.optString("epdgAddress").takeIf { it.isNotBlank() && it != "null" },
                            imsDomain = o.optString("imsDomain").takeIf { it.isNotBlank() && it != "null" },
                            apn = o.optString("apn").takeIf { it.isNotBlank() && it != "null" },
                            ipStack = o.optString("ipStack").takeIf { it.isNotBlank() && it != "null" },
                            ikeCipher = "$enc / $intg / $dh",
                            natKeepaliveSeconds = o.optInt("natKeepaliveSeconds", 20)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun loadMccMnc(): List<MccMncEntry> {
        return runCatching {
            val raw = context.assets.open("data/mcc_mnc.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val row = arr.getJSONArray(i)
                    add(
                        MccMncEntry(
                            mcc = row.optString(0),
                            mnc = row.optString(1),
                            countryCode = row.optString(2),
                            operatorName = row.optString(3),
                            brand = row.optString(4),
                            bands = row.optString(6)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
