package com.example.suprabtapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.scan_and_pair_activity.*
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class ScanAndPairActivity: AppCompatActivity() {

    private var bAdapter: BluetoothAdapter? = null
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1
    private var bReceiver: BroadcastReceiver? = null
    private val discoveredDeviceList : ArrayList<BluetoothDevice> = ArrayList()
    private val discoveredDeviceNameList : ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_and_pair_activity)

        bAdapter = BluetoothAdapter.getDefaultAdapter()
        if(bAdapter == null) {
            Toast.makeText(this, "this device doesn't support bluetooth", Toast.LENGTH_LONG).show()
            return
        }
        if(!bAdapter!!.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }
        if (bAdapter!!.isDiscovering) {
            bAdapter!!.cancelDiscovery();
        }


        registerBluetoothReceiver()

        var result = bAdapter!!.startDiscovery()


        if(!result) {
            Toast.makeText(this, "Unable to scan devices", Toast.LENGTH_LONG).show()
            return
        }
        else {
            // Show Loading Gif
        }

    }

    private fun initializeListView() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, discoveredDeviceNameList)
        discoveredList.adapter = adapter
        select_device_list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val device: BluetoothDevice = discoveredDeviceList[position]
            val connectThread = ConnectThread(bAdapter, device)
            connectThread.start()
        }
    }

    private fun registerBluetoothReceiver() {
        bReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    discoveredDeviceNameList.add(device.name)
                    discoveredDeviceList.add(device)
                }
                initializeListView()
            }
        }
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bReceiver, filter)

    }

    private class ConnectThread(bAdapter: BluetoothAdapter?, device: BluetoothDevice) : Thread() {
        private val bSocket: BluetoothSocket?
        private val bDevice: BluetoothDevice
        private val adapter: BluetoothAdapter?
        override fun run() {
            // Cancel discovery because it will slow down the connection
            adapter!!.cancelDiscovery()
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                bSocket!!.connect()
            } catch (connectException: IOException) {
                // Unable to connect; close the socket and get out
                try {
                    bSocket!!.close()
                } catch (closeException: IOException) {
                }
                return
            }
            return
        }

        /** Will cancel an in-progress connection, and close the socket  */
        fun cancel() {
            try {
                bSocket!!.close()
            } catch (e: IOException) {
            }
        }

        init {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            var tmp: BluetoothSocket? = null
            bDevice = device
            adapter = bAdapter

            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(R.string.MyUUID.toString() ))
            } catch (e: IOException) {
            }
            bSocket = tmp
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bReceiver)
    }
}