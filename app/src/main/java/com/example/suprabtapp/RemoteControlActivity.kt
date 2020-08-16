package com.example.suprabtapp

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.*
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.remote_control_layout.*
import java.io.IOException
import java.util.*


class RemoteControlActivity : AppCompatActivity() {

    companion object {
        var preset1 = "OFF"
        var preset2 = "OFF"
        var currentBright = ""
        var currentColor = ""
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        lateinit var spinner: Spinner
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String
        var brightCommandMap = mapOf<String, String>(
            "D20" to "1%",
            "D200" to "5%",
            "D400" to "10%",
            "D800" to "20%",
            "D1200" to "30%",
            "D1600" to "40%",
            "D2000" to "50%",
            "D2400" to "60%",
            "D2800" to "70%",
            "D3200" to "80%",
            "D3600" to "90%",
            "D4000" to "100%"
        )
        var presetCommandMap = mutableMapOf<String, MutableList<String>>(
            "Preset1" to mutableListOf<String>("D4000"),
            "Preset2" to mutableListOf<String>("D4000"),
            "Preset3" to mutableListOf<String>("D4000")
        )
        var presetArray = arrayOf<String>("Preset1", "Preset2", "Preset3")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.remote_control_layout)
        m_address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS)

        spinner = findViewById(R.id.presetSpinner)
        val spinnerAdapter = ArrayAdapter(this,  android.R.layout.simple_spinner_item, presetArray)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        ConnectToDevice(this).execute()

        rc_off.setOnClickListener { sendCommand("P0") }
        rc_on.setOnClickListener { sendCommand("P1") }
        rc_preset1.setOnClickListener { sendPresetCommand(presetArray[0]) }
        rc_preset2.setOnClickListener { sendPresetCommand(presetArray[1]) }
        rc_preset3.setOnClickListener { sendPresetCommand(presetArray[2]) }
        rc_3000k.setOnClickListener { sendCommand("C20") }
        rc_3500k.setOnClickListener { sendCommand("C800") }
        rc_4000k.setOnClickListener { sendCommand("C1600") }
        rc_4500k.setOnClickListener { sendCommand("C2400") }
        rc_5000k.setOnClickListener { sendCommand("C3200") }
        rc_5700k.setOnClickListener { sendCommand("C4000") }

        rc_add_bright.setOnClickListener {
            var found = false
            var brightVal = "D2000"
            for(key in brightCommandMap.keys){
                if(found) {
                    brightVal = key
                    break;
                }
                found = (key == currentBright)
            }
            sendCommand(brightVal)
        }

        rc_sub_bright.setOnClickListener {
            var brightVal = "D2000"
            var prevbrightVal = "D2000"
            for(key in brightCommandMap.keys){
                if(key == currentBright) {
                    brightVal = prevbrightVal
                    break;
                }
                prevbrightVal = key
            }
            sendCommand(brightVal)
        }
        rc_savePresetBtn.setOnClickListener { savePreset() }
        rc_disconnect.setOnClickListener { disconnect() }
    }

    private fun sendCommand(input: String) {
        if (m_bluetoothSocket != null) {
            try{
                vibratePhone()
                if(input.startsWith("D")) {
                    currentBright = input
                    if(brightCommandMap.containsKey(currentBright))
                        rc_bright_value.text = brightCommandMap[input]
                }
                else if(input.startsWith("C"))
                    currentColor = input
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch(e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun sendPresetCommand(presetTxt: String) {
        val presetCommands = presetCommandMap[presetTxt]
        if (presetCommands != null) {
            for(command in presetCommands)
                sendCommand(command)
        }
    }

    private fun savePreset(){
        vibratePhone()
        val selectedPreset = spinner.selectedItem.toString()
        val currentSetting = mutableListOf<String>()
        if(currentBright.isNotBlank()){
            currentSetting.add(currentBright)
        }
        if(currentColor.isNotBlank()){
            currentSetting.add(currentColor)
        }
        if(currentSetting.size <= 0) {
            Toast.makeText(this, "No Setting was set", Toast.LENGTH_LONG).show()
            return;
        }
        presetCommandMap[selectedPreset] = currentSetting
        Toast.makeText(this, "Selected setting saved as $selectedPreset", Toast.LENGTH_LONG).show()
    }

    private fun vibratePhone() {
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(100)
    }

    private fun disconnect() {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {
        private var connectSuccess: Boolean = true
        private val context: Context
        init {
            this.context = c
        }
        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "please wait")
        }

        override fun doInBackground(vararg p0: Void?): String? {
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                Log.i("data", "couldn't connect")
            } else {
                m_isConnected = true
            }
            m_progress.dismiss()
        }
    }

}
