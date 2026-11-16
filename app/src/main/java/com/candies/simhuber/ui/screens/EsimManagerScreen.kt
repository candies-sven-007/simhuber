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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.candies.simhuber.model.EsimProfileState

@Composable
fun EsimManagerScreen(
    state: SimHuberUiState,
    onEnableProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onDownloadLpa: (String, String) -> Unit,
    onSendApdu: (String) -> Unit
) {
    var lpaInput by remember { mutableStateOf("") }
    var nickInput by remember { mutableStateOf("") }
    var apduInput by remember { mutableStateOf("00A4040010A0000005591010FFFFFFFF8900000100") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("下载 eSIM Profile (LPAd / eSTK)", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = lpaInput,
                        onValueChange = { lpaInput = it },
                        label = { Text("LPA:1\$SM-DP+\$MatchingID") },,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nickInput,
                        onValueChange = { nickInput = it },
                        label = { Text("备注名称 (如 Globe PH)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(onClick = {
                        onDownloadLpa(lpaInput, nickInput)
                        lpaInput = ""
                        nickInput = ""
                    }) {
                        Text("解析并下载 Profile")
                    }
                }
            }
        }

        item {
            Text("卡内 Profile 列表", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        items(state.esimProfiles) { profile ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.nickname, fontWeight = FontWeight.Bold)
                        Text("${profile.providerName} | MCC/MNC: ${profile.mccMnc}", style = MaterialTheme.typography.bodySmall)
                        Text("ICCID: ${profile.iccid}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (profile.state == EsimProfileState.ENABLED) {
                            Text("已启用", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        } else {
                            OutlinedButton(onClick = { onEnableProfile(profile.iccid) }) {
                                Text("启用")
                            }
                        }
                        TextButton(onClick = { onDeleteProfile(profile.iccid) }) {
                            Text("删卡", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("原始 APDU 调试台", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = apduInput,
                        onValueChange = { apduInput = it },
                        label = { Text("Hex APDU") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedButton(onClick = { onSendApdu(apduInput) }) {
                        Text("发送 APDU 至 CCID")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
