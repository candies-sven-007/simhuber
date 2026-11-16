package com.candies.simhuber.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.candies.simhuber.data.SimHuberUiState
import com.candies.simhuber.ui.theme.AccentAmber
import com.candies.simhuber.ui.theme.AccentCyan
import com.candies.simhuber.ui.theme.AccentEmerald
import com.candies.simhuber.ui.theme.CardBorder
import com.candies.simhuber.ui.theme.CardSurface
import com.candies.simhuber.ui.theme.TextPrimary
import com.candies.simhuber.ui.theme.TextSecondary

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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("VoHive2 代理分流规则 (MCC -> SOCKS5 / TURN / TUN)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        items(state.routingRules) { rule ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${rule.name} (MCC ${rule.mcc})", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${rule.upstreamType} ${rule.proxyHost}:${rule.proxyPort}", color = AccentCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("CF TURN: ${if (rule.enableCloudflareTurn) "开启" else "关闭"} | TUN: ${if (rule.enableTunMode) "开启" else "关闭"}", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(checked = rule.enabled, onCheckedChange = { onToggleRule(rule.id) })
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("新增 MCC 分流规则", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = mccInput, onValueChange = { mccInput = it }, label = { Text("MCC") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = ruleName, onValueChange = { ruleName = it }, label = { Text("规则名称") }, modifier = Modifier.weight(2f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = hostInput, onValueChange = { hostInput = it }, label = { Text("SOCKS5 Host") }, modifier = Modifier.weight(2f), singleLine = true)
                        OutlinedTextField(value = portInput, onValueChange = { portInput = it }, label = { Text("端口") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = turnEnabled, onCheckedChange = { turnEnabled = it })
                        Text("Cloudflare TURN", color = TextPrimary, fontSize = 12.sp)
                        Checkbox(checked = tunEnabled, onCheckedChange = { tunEnabled = it })
                        Text("TUN 模式", color = TextPrimary, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            onAddRule(ruleName, mccInput, hostInput, portInput.toIntOrNull() ?: 10808, turnEnabled, tunEnabled)
                            ruleName = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black)
                    ) {
                        Text("保存分流规则", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("SIMHub 订阅与保号账本", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        items(state.ledgerItems) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.cardLabel} · ${item.carrier}", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${item.monthlyCost} ${item.currency}", color = AccentEmerald, fontWeight = FontWeight.Bold)
                    }
                    Text("套餐: ${item.planName} | 到期: ${item.expiryDate} (剩余 ${item.daysRemaining} 天)", color = TextSecondary, fontSize = 12.sp)
                    LinearProgressIndicator(
                        progress = { (item.daysRemaining.coerceIn(0, 365) / 365f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (item.daysRemaining < 45) AccentAmber else AccentEmerald
                    )
                    if (item.notes.isNotBlank()) Text(item.notes, color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("添加保号账本记录", color = TextPrimary, fontWeight = FontWeight.SemiBold)
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
                        Text("添加到账本", color = AccentCyan)
                    }
                }
            }
        }
    }
}
