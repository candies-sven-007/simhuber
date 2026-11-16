package com.candies.simhuber

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.candies.simhuber.ccid.UsbCcidDriver
import com.candies.simhuber.ui.SimHuberViewModel
import com.candies.simhuber.ui.screens.DashboardScreen
import com.candies.simhuber.ui.screens.EsimManagerScreen
import com.candies.simhuber.ui.screens.RoutingAndLedgerScreen
import com.candies.simhuber.ui.screens.VoWifiCommScreen
import com.candies.simhuber.ui.theme.SIMHuberTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SimHuberViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbCcidDriver.ACTION_USB_PERMISSION -> {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (granted && device != null) {
                        viewModel.onUsbPermissionGranted(device)
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> viewModel.scanUsbReaders()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissionLauncher.launch(perms.toTypedArray())

        val filter = IntentFilter().apply {
            addAction(UsbCcidDriver.ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        }
        ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        setContent {
            SIMHuberTheme {
                val state by viewModel.uiState.collectAsState()
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }

                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("SIMHuber · VoHive2 × SIMHub") })
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                label = { Text("总览") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.SimCard, contentDescription = null) },
                                label = { Text("eSIM") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Call, contentDescription = null) },
                                label = { Text("通话/短信") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                                label = { Text("分流/账本") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> DashboardScreen(
                                state = state,
                                onScanUsb = { viewModel.scanUsbReaders() },
                                onSelectCard = { viewModel.selectCard(it) },
                                onToggleVoWifi = { viewModel.toggleVoWifi() },
                                onProbeNode = { viewModel.probeRelayNode(it) }
                            )
                            1 -> EsimManagerScreen(
                                state = state,
                                onEnableProfile = { viewModel.enableEsimProfile(it) },
                                onDeleteProfile = { viewModel.deleteEsimProfile(it) },
                                onDownloadLpa = { lpa, nick -> viewModel.downloadEsimByLpa(lpa, nick) },
                                onSendApdu = { viewModel.sendRawApdu(it) }
                            )
                            2 -> VoWifiCommScreen(
                                state = state,
                                onSendSms = { peer, msg -> viewModel.sendSms(peer, msg) },
                                onPlaceCall = { viewModel.placeVoWifiCall(it) }
                            )
                            3 -> RoutingAndLedgerScreen(
                                state = state,
                                onToggleRule = { viewModel.toggleRoutingRule(it) },
                                onAddRule = { name, mcc, host, port, turn, tun ->
                                    viewModel.addRoutingRule(name, mcc, host, port, turn, tun)
                                },
                                onAddLedger = { label, carrier, plan, cost, cur, exp ->
                                    viewModel.addLedgerItem(label, carrier, plan, cost, cur, exp)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        viewModel.ccidDriver.close()
    }
}
