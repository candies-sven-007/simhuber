package com.candies.simhuber.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.WifiCalling3
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.candies.simhuber.data.SimHuberUiState
import com.candies.simhuber.model.CarrierPresetInfo
import com.candies.simhuber.model.SimCardInfo
import com.candies.simhuber.model.VoWifiState
import com.candies.simhuber.ui.theme.AccentAmber
import com.candies.simhuber.ui.theme.AccentCyan
import com.candies.simhuber.ui.theme.AccentEmerald
import com.candies.simhuber.ui.theme.AccentRose
import com.candies.simhuber.ui.theme.CardBorder
import com.candies.simhuber.ui.theme.CardSurface
import com.candies.simhuber.ui.theme.CardSurfaceElevated
import com.candies.simhuber.ui.theme.TextPrimary
import com.candies.simhuber.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    state: SimHuberUiState,
    matchedPreset: CarrierPresetInfo?,
    onScanUsb: () -> Unit,
    onTriggerSelfHeal: () -> Unit,
    onToggleSelfHeal: () -> Unit,
    onSelectCard: (String) -> Unit,
    onToggleVoWifi: () -> Unit,
    onProbeNode: (String) -> Unit
) {
    val isVoWifiActive = state.voWifiState == VoWifiState.REGISTERED

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    label = if (isVoWifiActive) "VoWiFi 在线" else "VoWiFi 待机",
                    color = if (isVoWifiActive) AccentEmerald else AccentAmber,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = if (state.usbReaderConnected) "CCID 已接" else "CCID 未接",
                    color = if (state.usbReaderConnected) AccentCyan else TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "预设 ${state.totalPresetCount}",
                    color = AccentCyan,
                    modifier = Modifier.weight(0.9f)
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.cards) { card ->
                    SimVisualCard(
                        card = card,
                        selected = card.id == state.selectedCardId,
                        onClick = { onSelectCard(card.id) }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WifiCalling3, contentDescription = null, tint = AccentCyan)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("VoWiFi / IMS 通道控制", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("状态: ${state.voWifiState.name}", color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    if (matchedPreset != null) {
                        Surface(
                            color = CardSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("命中预设: ${matchedPreset.bundleName} (PLMN ${matchedPreset.plmn})", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("ePDG: ${matchedPreset.epdgAddress ?: "3GPP 自动解析"}", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("IKEv2: ${matchedPreset.ikeCipher} | Keepalive ${matchedPreset.natKeepaliveSeconds}s", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onToggleVoWifi,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVoWifiActive) AccentRose else AccentCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(if (isVoWifiActive) "断开 VoWiFi" else "一键注册 VoWiFi", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onScanUsb,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("扫描 CCID", color = TextPrimary)
                        }
                    }
                    Text("读卡器: ${state.usbReaderName}", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        // VoHive2 0.3.5: IMS 健康度诊断时间线 & USB 端口自愈
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = AccentEmerald)
                            Spacer(Modifier.width(8.dp))
                            Text("IMS 健康度时间线 & 设备自愈", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Switch(checked = state.usbSelfHealEnabled, onCheckedChange = { onToggleSelfHeal() })
                    }

                    state.imsTimeline.take(4).forEach { ev ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("[${ev.deviceGeneration}] ${ev.stage} · ${ev.detail}", color = if (ev.healthy) TextPrimary else AccentRose, fontSize = 11.sp)
                            }
                            Text("${ev.latencyMs}ms", color = if (ev.healthy) AccentEmerald else AccentAmber, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    OutlinedButton(
                        onClick = onTriggerSelfHeal,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
                    ) {
                        Text("执行 USB 端口自愈 / QMI 断线恢复", color = AccentAmber, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text("VoHive2 远程中继节点 (Tailscale / LAN)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        items(state.relayNodes) { node ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(node.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(node.endpointUrl, color = AccentCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("${node.remoteModemStatus} · ${node.lastSyncTime}", color = TextSecondary, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { onProbeNode(node.id) }, border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))) {
                        Text("探测", color = AccentCyan)
                    }
                }
            }
        }

        item {
            Text("实时 Journal 内核日志", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Surface(
                color = Color(0xFF06090D),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.logs.take(10).forEach { log ->
                        val tagColor = when (log.level) {
                            "ERROR" -> AccentRose
                            "WARN" -> AccentAmber
                            else -> AccentEmerald
                        }
                        Text(
                            text = "[${log.timestamp}] ${log.tag}: ${log.message}",
                            color = tagColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = CardSurface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SimVisualCard(card: SimCardInfo, selected: Boolean, onClick: () -> Unit) {
    val gradient = when (card.cardThemeIndex % 3) {
        0 -> Brush.linearGradient(listOf(Color(0xFF0B3954), Color(0xFF087E8B)))
        1 -> Brush.linearGradient(listOf(Color(0xFF3A1C71), Color(0xFFD76D77)))
        else -> Brush.linearGradient(listOf(Color(0xFF134E5E), Color(0xFF2E7D32)))
    }

    Card(
        modifier = Modifier.width(272.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) AccentCyan else Color.White.copy(alpha = 0.15f))
    ) {
        Box(modifier = Modifier.background(gradient).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Color.Black.copy(alpha = 0.28f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "${card.countryCode} · ${card.sourceType.name}",
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Icon(Icons.Default.SimCard, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
                }

                Text(card.carrierName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(card.phoneNumber.ifBlank { card.label }, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontFamily = FontFamily.Monospace)

                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("余额 / 套餐", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                        Text(card.balanceText.ifBlank { "--" }, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("有效期至", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                        Text(card.expiryText.ifBlank { "--" }, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                Text("ICCID: ${card.iccid}", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
