package com.example.suprabtapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_remote_logs.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RemoteLogs : AppCompatActivity() {

    companion object {
        lateinit var logTxt: ArrayList<String>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote_logs)

        val deviceName = intent.getStringExtra(MainActivity.EXTRA_NAME)
        logs_device_name.text = deviceName
        loadSavedLogs(deviceName)
        showRemoteLogs()
    }

    private fun loadSavedLogs(deviceName: String){
        val fileName = deviceName + "_logs.txt"
        val fi = File(this.filesDir, fileName)
        fi.createNewFile()
        val fin = openFileInput(fileName)
        val inputStreamReader = InputStreamReader(fin)
        val bufferedReader = BufferedReader(inputStreamReader)
        val sb = StringBuilder()
        var line: String?
        logTxt = ArrayList()
        while (bufferedReader.readLine().also { line = it } != null) {
            logTxt.add(line.toString())
        }
        inputStreamReader.close()
    }

    private fun showRemoteLogs(){
        if(logTxt.isNullOrEmpty()){
            Toast.makeText(this, "No logs available for this device", Toast.LENGTH_LONG).show()
            return
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, logTxt.reversed())
        logList.adapter = adapter
    }
}