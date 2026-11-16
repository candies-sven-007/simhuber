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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.candies.simhuber.model.CarrierPresetInfo
import com.candies.simhuber.model.EsimProfileState
import com.candies.simhuber.ui.theme.AccentCyan
import com.candies.simhuber.ui.theme.AccentEmerald
import com.candies.simhuber.ui.theme.AccentRose
import com.candies.simhuber.ui.theme.CardBorder
import com.candies.simhuber.ui.theme.CardSurface
import com.candies.simhuber.ui.theme.TextPrimary
import com.candies.simhuber.ui.theme.TextSecondary

@Composable
fun EsimManagerScreen(
    state: SimHuberUiState,
    onSearchPresets: (String) -> List<CarrierPresetInfo>,
    onEnableProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onDownloadLpa: (String, String) -> Unit,
    onSendApdu: (String) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) }
    var lpaInput by remember { mutableStateOf("") }
    var nickInput by remember { mutableStateOf("") }
    var presetQuery by remember { mutableStateOf("") }
    var apduInput by remember { mutableStateOf("00A4040010A0000005591010FFFFFFFF8900000100") }

    val matchedPresets = remember(presetQuery) { onSearchPresets(presetQuery) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = subTab == 0, onClick = { subTab = 0 }, label = { Text("eSIM 卡槽") })
            FilterChip(selected = subTab == 1, onClick = { subTab = 1 }, label = { Text("预设库 (${state.totalPresetCount})") })
            FilterChip(selected = subTab == 2, onClick = { subTab = 2 }, label = { Text("APDU 终端") })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (subTab) {
                0 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("下载 eSIM Profile (LPAd / eSTK)", color = TextPrimary, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = lpaInput,
                                    onValueChange = { lpaInput = it },
                                    label = { Text("LPA 激活码 (SM-DP+ / MatchingID)") },
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
                                Button(
                                    onClick = {
                                        onDownloadLpa(lpaInput, nickInput)
                                        lpaInput = ""
                                        nickInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black)
                                ) {
                                    Text("解析并写入 Profile", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    items(state.esimProfiles) { profile ->
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
                                    Text(profile.nickname, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text("${profile.providerName} · PLMN ${profile.mccMnc}", color = TextSecondary, fontSize = 12.sp)
                                    Text("ICCID: ${profile.iccid}", color = AccentCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (profile.state == EsimProfileState.ENABLED) {
                                        Text("已启用", color = AccentEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    } else {
                                        OutlinedButton(onClick = { onEnableProfile(profile.iccid) }) {
                                            Text("启用", color = AccentCyan)
                                        }
                                    }
                                    TextButton(onClick = { onDeleteProfile(profile.iccid) }) {
                                        Text("删除", color = AccentRose)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        OutlinedTextField(
                            value = presetQuery,
                            onValueChange = { presetQuery = it },
                            label = { Text("搜索运营商 / PLMN (如 51502 / vodafone / tmobile)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    items(matchedPresets) { preset ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("${preset.bundleName} (PLMN: ${preset.plmn})", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("ePDG: ${preset.epdgAddress ?: "3GPP 默认"}", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("IKEv2: ${preset.ikeCipher} · APN=${preset.apn ?: "ims"} · ${preset.ipStack ?: "dual"}", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                else -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("原始 APDU 调试控制台", color = TextPrimary, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = apduInput,
                                    onValueChange = { apduInput = it },
                                    label = { Text("Hex APDU 指令") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Button(
                                    onClick = { onSendApdu(apduInput) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black)
                                ) {
                                    Text("发送至 USB CCID", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
