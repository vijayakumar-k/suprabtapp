package com.example.suprabtapp

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.remote_control_layout.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class RemoteControlActivity : AppCompatActivity() {

    companion object {
        var currentBright = ""
        var currentColor = ""
        var isPowerOn = false
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        lateinit var spinner: Spinner
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String
        lateinit var m_name: String

        var colorCommandMap = mapOf<String, String>(
            "C20" to "3000K",
            "C800" to "3500K",
            "C1600" to "4000K",
            "C2400" to "4500K",
            "C3200" to "5000K",
            "C4000" to "5700K"
        )

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
        var presetCommandMap = mutableMapOf<String, String>(
            "Preset1" to "D4000",
            "Preset2" to "D4000",
            "Preset3" to "D4000"
        )
        var presetArray = arrayOf<String>("Preset1", "Preset2", "Preset3")
        var presetFilenames = mutableMapOf<String, String>(
            "Preset1" to "preset1.txt",
            "Preset2" to "preset2txt",
            "Preset3" to "preset3.txt"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.remote_control_layout)
        m_address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS)
        m_name = intent.getStringExtra(MainActivity.EXTRA_NAME)
        remote_toolbar.title = m_name;

        spinner = findViewById(R.id.presetSpinner)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, presetArray)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        ConnectToDevice(this).execute()

        loadSavedPresetSettings()
        setRemoteListeners()
    }

    private fun loadSavedPresetSettings(){
        for(preset in presetArray){
            val fileName = presetFilenames[preset]
            val fi = File(this.filesDir, fileName)
            fi.createNewFile()
            this.openFileInput(fileName).use { stream ->
                val text = stream.bufferedReader().use {
                    it.readText()
                }
                if(text.isNotBlank()){
                    presetCommandMap[preset] = text
                }
            }

        }
    }

    private fun setRemoteListeners(){
        rc_power.setOnClickListener { powerCommand() }
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
        rc_logs.setOnClickListener{ showRemoteLogs() }
    }

    private fun sendCommand(input: String) {
        if (m_bluetoothSocket != null && input.isNotBlank()) {
            try{
                vibratePhone()
                if(input.startsWith("D")) {
                    currentBright = input
                    if(brightCommandMap.containsKey(currentBright))
                        rc_bright_value.text = brightCommandMap[input]
                }
                else if(input.startsWith("C")) {
                    currentColor = input
                }
                val log = constructLog(input)
                if(log.isNotBlank()) {
                    logCommand(log)
                }
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun powerCommand() {
        if(isPowerOn){
            sendCommand("P0")
            rc_power.setBackgroundResource(R.drawable.ic_power_off)
            Toast.makeText(this, "Light is switched off", Toast.LENGTH_LONG).show()
        } else {
            sendCommand("P1")
            rc_power.setBackgroundResource(R.drawable.ic_power_on)
            Toast.makeText(this, "Light is switched on", Toast.LENGTH_LONG).show()
        }
        isPowerOn = !isPowerOn
    }

    private fun sendPresetCommand(presetTxt: String) {
        val presetCommands = presetCommandMap[presetTxt]
        if (presetCommands != null) {
            val arrayCommands = presetCommands.split(',').toTypedArray()
            for(command in arrayCommands)
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
        saveToPresetFile(selectedPreset, currentSetting.joinToString())
        Toast.makeText(this, "Selected setting saved as $selectedPreset", Toast.LENGTH_LONG).show()
    }

    private fun saveToPresetFile(presetId: String, command: String){
        val fileName = presetFilenames[presetId]
        val fOut: FileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE)
        fOut.write(command.toByteArray())
        fOut.close()
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

    private fun logCommand(log: String){
        val fileName = m_name + "_logs.txt"
        val fi = File(this.filesDir, fileName)
        fi.createNewFile()
        val fos: FileOutputStream = FileOutputStream(fi, true)
        val bw = BufferedWriter(OutputStreamWriter(fos))
        bw.write(log)
        bw.newLine()
        bw.close()
    }

    private fun constructLog(command: String): String{
        var log: String = ""
        when {
            command == "P0" -> {
                log += "Switched OFF"
            }
            command == "P1" -> {
                log += "Switched ON"
            }
            command.startsWith("C") -> {
                log += "Color set as " + colorCommandMap[command]
            }
            command.startsWith("D") -> {
                log += "Brightness changed to " + brightCommandMap[command]
            }
        }
        val time = getCurrentTime()
        if(time.isNotBlank())
            log += " at $time"
        return log
    }

    private fun getCurrentTime(): String{
        val pattern = "MMM d, yyyy HH:mm:ss a "
        val simpleDateFormat = SimpleDateFormat(pattern)
        return simpleDateFormat.format(Date())
    }

    private fun showRemoteLogs() {
        val intent = Intent(this, RemoteLogs::class.java)
        intent.putExtra(MainActivity.EXTRA_NAME, m_name)
        startActivity(intent)
    }

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {
        private var connectSuccess: Boolean = true
        private val context: Context = c
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
