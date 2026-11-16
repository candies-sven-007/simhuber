package com.candies.simhuber.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun VoWifiCommScreen(
    state: SimHuberUiState,
    onSendSms: (String, String) -> Unit,
    onPlaceCall: (String, Boolean) -> Unit,
    onToggleAutoRecord: () -> Unit,
    onTogglePlayRecording: (String) -> Unit
) {
    var peerNumber by remember { mutableStateOf("+63917") }
    var smsText by remember { mutableStateOf("") }

    val dialRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("+", "0", "⌫")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("VoWiFi 拨号盘 & 通话录音", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("自动录音", color = TextSecondary, fontSize = 12.sp)
                            Switch(checked = state.autoCallRecording, onCheckedChange = { onToggleAutoRecord() })
                        }
                    }

                    OutlinedTextField(
                        value = peerNumber,
                        onValueChange = { peerNumber = it },
                        label = { Text("对方号码 (+86 与紧急号码已自动拦截)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    dialRows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { key ->
                                OutlinedButton(
                                    onClick = {
                                        peerNumber = if (key == "⌫") peerNumber.dropLast(1) else peerNumber + key
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    border = BorderStroke(1.dp, CardBorder)
                                ) {
                                    Text(key, color = TextPrimary, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = smsText,
                        onValueChange = { smsText = it },
                        label = { Text("短信内容 (UCS2 / GSM-7)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onPlaceCall(peerNumber, false) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color.Black)
                        ) {
                            Text("呼叫", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onPlaceCall(peerNumber, true) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.6f))
                        ) {
                            Text("手动录音拨打", color = AccentAmber, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                onSendSms(peerNumber, smsText)
                                smsText = ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black)
                        ) {
                            Text("发短信", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("通话历史与录音回放", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        items(state.callRecords) { call ->
            val playing = state.activePlayingCallId == call.id
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${call.direction} · ${call.peerNumber}", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("${call.timestamp} · 时长 ${call.durationSec}s", color = TextSecondary, fontSize = 12.sp)
                            call.recordingFileName?.let {
                                Text("录音文件: $it", color = AccentAmber, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        if (call.hasRecording) {
                            IconButton(onClick = { onTogglePlayRecording(call.id) }) {
                                Icon(
                                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = AccentCyan
                                )
                            }
                        }
                    }
                    if (playing) {
                        LinearProgressIndicator(progress = { 0.45f }, modifier = Modifier.fillMaxWidth(), color = AccentCyan)
                    }
                }
            }
        }

        item {
            Text("短信收件箱 (含 VoHive2 归档)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        items(state.smsList) { sms ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = (if (sms.isIncoming) "收自 " else "发往 ") + sms.peerNumber,
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(sms.timestamp, color = TextSecondary, fontSize = 11.sp)
                    }
                    Text(sms.body, color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}
