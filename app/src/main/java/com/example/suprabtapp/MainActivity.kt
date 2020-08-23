package com.example.suprabtapp

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var bAdapter: BluetoothAdapter? = null
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1

    companion object {
        const val EXTRA_ADDRESS: String = "Device_address"
        const val EXTRA_NAME: String = "Device_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bAdapter = BluetoothAdapter.getDefaultAdapter()
        if(bAdapter == null) {
            Toast.makeText(this, "this device doesn't support bluetooth", Toast.LENGTH_LONG).show()
            return
        }
        if(!bAdapter!!.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        select_device_refresh.setOnClickListener{ pairedDeviceList() }

//        device_discover.setOnClickListener{ showDiscoverPage() }
    }

    private fun pairedDeviceList() {
        pairedDevices = bAdapter!!.bondedDevices
        val deviceList : ArrayList<BluetoothDevice> = ArrayList()
        val deviceNameList : ArrayList<String> = ArrayList()
        if (pairedDevices.isNotEmpty()) {
            for (device: BluetoothDevice in pairedDevices) {
//                if(device.name.startsWith("HC-")) {
                    deviceList.add(device)
                    deviceNameList.add(device.name)
                    Log.i("device", "" + device.name)
//                }
            }
        } else {
            Toast.makeText(this, "no paired bluetooth devices found", Toast.LENGTH_LONG).show()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNameList)
        select_device_list.adapter = adapter
        select_device_list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val device: BluetoothDevice = deviceList[position]
            val address: String = device.address

            val intent = Intent(this, RemoteControlActivity::class.java)
            intent.putExtra(EXTRA_ADDRESS, address)
            intent.putExtra(EXTRA_NAME, device.name)
            startActivity(intent)
        }
    }

    private fun showDiscoverPage() {
        val intent = Intent(this, ScanAndPairActivity::class.java)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                if (bAdapter!!.isEnabled) {
                    Toast.makeText(this,"Bluetooth has been enabled", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Bluetooth has been disabled", Toast.LENGTH_LONG).show()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this,"Bluetooth enabling has been canceled", Toast.LENGTH_LONG).show()
            }
        }
    }
}