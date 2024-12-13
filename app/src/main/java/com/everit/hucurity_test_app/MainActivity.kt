package com.everit.hucurity_test_app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.everit.hucurity_test_app.ui.theme.Hucurity_test_appTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var clientThread: ClientThread? = null

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    private var clientStatus = mutableStateOf("Idle")
    private var targetDeviceName = mutableStateOf("")
    private var targetDeviceMacAddress = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1001
            )
            return
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth is not supported or not enabled", Toast.LENGTH_LONG)
                .show()
            finish()
        }

        setContent {
            Hucurity_test_appTheme {
                ClientScreen(
                    onSendData = { sendData() },
                    status = clientStatus.value,
                    deviceName = targetDeviceName.value,
                    deviceMacAddress = targetDeviceMacAddress.value
                )
            }
        }
    }

    @Composable
    fun ClientScreen(
        onSendData: () -> Unit,
        status: String,
        deviceName: String,  // 대상 이름
        deviceMacAddress: String // 대상 MAC 주소
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Client Status: $status")

            Spacer(modifier = Modifier.height(16.dp))

            // 대상 이름과 MAC 주소 출력
            Text(text = "Target Device: $deviceName")
            Text(text = "MAC Address: $deviceMacAddress")

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onSendData) {
                Text("Send Data")
            }
        }
    }

    private fun sendData() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val pairedDevices = bluetoothAdapter.bondedDevices
        if (pairedDevices.isNotEmpty()) {
            val device = pairedDevices.first() // 연결하려는 첫 번째 기기 선택
            clientStatus.value = "Connecting to ${device.name}..."

            // 이름과 MAC 주소를 상태로 업데이트
            targetDeviceName.value = device.name
            targetDeviceMacAddress.value = device.address

            // 클라이언트 스레드 시작
            clientThread = ClientThread(device, uuid, this).apply {
                start()
            }
            clientStatus.value = "Data sent!"
        } else {
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clientThread?.cancel()
    }
}