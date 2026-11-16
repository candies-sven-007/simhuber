package com.candies.simhuber.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.candies.simhuber.data.SimHuberUiState
import com.candies.simhuber.model.VoWifiState

@Composable
fun DashboardScreen(
    state: SimHuberUiState,
    onScanUsb: () -> Unit,
    onSelectCard: (String) -> Unit,
    onToggleVoWifi: () -> Unit,
    onProbeNode: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("VoWiFi / IMS 通道", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("当前状态: ${state.voWifiState.name}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onToggleVoWifi) {
                            val active = state.voWifiState == VoWifiState.REGISTERED || state.voWifiState == VoWifiState.EAP_AKA_AUTH
                            Text(if (active) "断开 VoWiFi" else "一键注册 VoWiFi")
                        }
                        OutlinedButton(onClick = onScanUsb) {
                            Text("扫描 USB CCID")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("读卡器: ${state.usbReaderName}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Text("卡槽与数据源切换", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.cards.forEach { card ->
                    FilterChip(
                        selected = card.id == state.selectedCardId,
                        onClick = { onSelectCard(card.id) },
                        label = { Text(card.label) }
                    )
                }
            }
        }

        item {
            val activeCard = state.cards.firstOrNull { it.id == state.selectedCardId }
            if (activeCard != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${activeCard.carrierName} (${activeCard.sourceType.name})", fontWeight = FontWeight.Bold)
                        Text("ICCID: ${activeCard.iccid}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        Text("IMSI: ${activeCard.imsi} | MCC/MNC: ${activeCard.mcc}/${activeCard.mnc}", style = MaterialTheme.typography.bodySmall)
                        activeCard.eid?.let {
                            Text("eUICC EID: $it", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        item {
            Text("VoHive2 远程中继节点 (LAN / Tailscale)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        items(state.relayNodes) { node ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(node.name, fontWeight = FontWeight.SemiBold)
                        Text(node.endpointUrl, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        Text("状态: ${node.remoteModemStatus} (${node.lastSyncTime})", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { onProbeNode(node.id) }) {
                        Text("探测")
                    }
                }
            }
        }

        item {
            Text("实时 Journal 日志", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        items(state.logs.take(15)) { log ->
            Text(
                text = "[${log.timestamp}] ${log.tag}/${log.level}: ${log.message}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 15.sp
            )
        }
    }
}
