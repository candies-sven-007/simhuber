package com.candies.simhuber.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.candies.simhuber.data.SimHuberUiState

@Composable
fun RoutingAndLedgerScreen(
    state: SimHuberUiState,
    onToggleRule: (String) -> Unit,
    onAddRule: (String, String, String, Int, Boolean, Boolean) -> Unit,
    onAddLedger: (String, String, String, String, String, String) -> Unit
) {
    var ruleName by remember { mutableStateOf("") }
    var mccInput by remember { mutableStateOf("515") }
    var hostInput by remember { mutableStateOf("127.0.0.1") }
    var portInput by remember { mutableStateOf("10808") }
    var turnEnabled by remember { mutableStateOf(true) }
    var tunEnabled by remember { mutableStateOf(false) }

    var cardLabel by remember { mutableStateOf("") }
    var carrierInput by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("VoHive2 代理分流规则 (MCC -> SOCKS5 / TURN / TUN)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        items(state.routingRules) { rule ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${rule.name} (MCC ${rule.mcc})", fontWeight = FontWeight.Bold)
                        Text("${rule.upstreamType} ${rule.proxyHost}:${rule.proxyPort}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        Text("CF TURN: ${if (rule.enableCloudflareTurn) "开" else "关"} | TUN: ${if (rule.enableTunMode) "开" else "关"}", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = rule.enabled, onCheckedChange = { onToggleRule(rule.id) })
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("新增分流规则", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = mccInput, onValueChange = { mccInput = it }, label = { Text("MCC") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = ruleName, onValueChange = { ruleName = it }, label = { Text("备注") }, modifier = Modifier.weight(2f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = hostInput, onValueChange = { hostInput = it }, label = { Text("SOCKS5 Host") }, modifier = Modifier.weight(2f), singleLine = true)
                        OutlinedTextField(value = portInput, onValueChange = { portInput = it }, label = { Text("端口") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = turnEnabled, onCheckedChange = { turnEnabled = it })
                        Text("Cloudflare TURN")
                        Checkbox(checked = tunEnabled, onCheckedChange = { tunEnabled = it })
                        Text("TUN 模式")
                    }
                    Button(onClick = {
                        onAddRule(ruleName, mccInput, hostInput, portInput.toIntOrNull() ?: 10808, turnEnabled, tunEnabled)
                        ruleName = ""
                    }) {
                        Text("添加分流规则")
                    }
                }
            }
        }

        item {
            Text("SIMHub 卡片账本与保号提醒", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        items(state.ledgerItems) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.cardLabel} (${item.carrier})", fontWeight = FontWeight.Bold)
                        Text("${item.monthlyCost} ${item.currency}", fontWeight = FontWeight.SemiBold)
                    }
                    Text("套餐: ${item.planName} | 到期: ${item.expiryDate}", style = MaterialTheme.typography.bodySmall)
                    if (item.notes.isNotBlank()) Text(item.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("记一笔卡套餐", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cardLabel, onValueChange = { cardLabel = it }, label = { Text("卡名") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = carrierInput, onValueChange = { carrierInput = it }, label = { Text("运营商") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = costInput, onValueChange = { costInput = it }, label = { Text("月费") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedButton(onClick = {
                        onAddLedger(cardLabel, carrierInput, "Prepaid", costInput, "USD", "2027-12-01")
                        cardLabel = ""
                        carrierInput = ""
                        costInput = ""
                    }) {
                        Text("添加到账本")
                    }
                }
            }
        }
    }
}
