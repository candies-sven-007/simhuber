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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.candies.simhuber.data.SimHuberUiState

@Composable
fun VoWifiCommScreen(
    state: SimHuberUiState,
    onSendSms: (String, String) -> Unit,
    onPlaceCall: (String) -> Unit
) {
    var peerNumber by remember { mutableStateOf("") }
    var smsText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("VoWiFi 短信与拨号", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = peerNumber,
                        onValueChange = { peerNumber = it },
                        label = { Text("对方号码 (如 +63917...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = smsText,
                        onValueChange = { smsText = it },
                        label = { Text("短信内容 (UCS2 / GSM-7)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            onSendSms(peerNumber, smsText)
                            smsText = ""
                        }) {
                            Text("发送短信")
                        }
                        OutlinedButton(onClick = { onPlaceCall(peerNumber) }) {
                            Text("VoWiFi 拨号")
                        }
                    }
                }
            }
        }

        item {
            Text("短信记录 (含 VoHive2 归档)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        items(state.smsList) { sms ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = (if (sms.isIncoming) "收自 " else "发往 ") + sms.peerNumber,
                            fontWeight = FontWeight.Bold
                        )
                        Text(sms.timestamp, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(sms.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Text("通话记录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        items(state.callRecords) { call ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${call.direction}: ${call.peerNumber}", fontWeight = FontWeight.Medium)
                    Text("${call.timestamp} (${call.durationSec}s)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
