package com.brenda.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var discoveredDevices: MutableList<BluetoothDevice>
    private lateinit var deviceListView: ListView
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private var bluetoothSocket: BluetoothSocket? = null

    private val PERMISSION_REQUEST_BLUETOOTH_CONNECT = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize list of devices and adapter
        discoveredDevices = mutableListOf()
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        deviceListView = findViewById(R.id.deviceListView)
        deviceListView.adapter = deviceListAdapter

        // Set item click listener for device selection
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = discoveredDevices[position]
            connectToDevice(selectedDevice)
        }

        // Button to start device discovery
        val scanButton: Button = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            discoverDevices()
        }
    }

    private fun discoverDevices() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                PERMISSION_REQUEST_BLUETOOTH_CONNECT
            )
            return
        }

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        bluetoothAdapter.startDiscovery()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connectToDevice(device: BluetoothDevice) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID

                // Verificar el estado del adaptador Bluetooth
                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Bluetooth no disponible", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Cerrar cualquier conexión Bluetooth existente
                bluetoothSocket?.close()

                // Crear nuevo BluetoothSocket y conectar
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return@launch
                }
//                bluetoothSocket?.connectTimeout = 30000
                bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()


                    // Conexión exitosa
                } catch (e: IOException) {
                    // Error al conectar
                    e.printStackTrace()
                }
//                bluetoothSocket?.isConnected
            }
        }





    override fun onStart() {
        super.onStart()
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(bluetoothReceiver)
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return
            intent ?: return

            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    if (!discoveredDevices.contains(device)) {
                        discoveredDevices.add(device)
                        if (ActivityCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return
                        }
                        deviceListAdapter.add(device.name ?: "Dispositivo sin nombre")
                        deviceListAdapter.notifyDataSetChanged() // Update the ListView
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start device discovery
                discoverDevices()
            } else {
                Toast.makeText(this, "Permiso denegado para escanear Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
