package com.everit.hucurity_test_app

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.UUID

class ClientThread(
    private val device: BluetoothDevice,
    private val uuid: UUID,
    private val context: Context
) : Thread() {
    private var socket: BluetoothSocket? = null

    init {
        try {
            // 디바이스와 소켓 초기화
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("ClientThread", "Socket creation failed")
            }
            socket = device.createRfcommSocketToServiceRecord(uuid)
        } catch (e: IOException) {
            Log.e("ClientThread", "Socket creation failed", e)
        }
    }

    override fun run() {
        try {
            // Bluetooth 권한 확인
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("ClientThread", "Bluetooth permission not granted")
                return
            }

            // 소켓 연결 시도
            socket?.connect()
            Log.d("ClientThread", "Connected to server")

            // 데이터 전송
            val outputStream = socket?.outputStream
            val dataToSend = byteArrayOf(1)  // 보낼 데이터 정의

            outputStream?.write(dataToSend)
            outputStream?.flush()  // 데이터가 모두 전송될 수 있도록 flush() 호출
            Log.d("ClientThread", "Data sent: ${dataToSend[0]}")

        } catch (e: IOException) {
            Log.e("ClientThread", "Could not connect or send data", e)
        } finally {
            // 연결된 소켓 닫기
            socket?.close()
            Log.d("ClientThread", "Socket closed")
        }
    }

    fun cancel() {
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e("ClientThread", "Could not close client socket", e)
        }
    }
}