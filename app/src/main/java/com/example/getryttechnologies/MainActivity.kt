package com.example.getryttechnologies

import android.app.ActivityManager
import android.content.Context
import android.os.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.getryttechnologies.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class MainActivity : AppCompatActivity() {

    private val mainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mainBinding.root)

        // Initial load
        updateBatteryLevel()
        updateRamUsage()
        updateStorageUsage()

        mainBinding.refreshButton.setOnClickListener {
            // Show ProgressBar and disable the button to prevent multiple clicks
            mainBinding.progressBar.visibility = View.VISIBLE
            mainBinding.refreshButton.isEnabled = false

            // Simulate a delay for loading (1.5 seconds)
            Handler(Looper.getMainLooper()).postDelayed({
                // Update data
                updateBatteryLevel()
                updateRamUsage()
                updateStorageUsage()

                // Hide ProgressBar and enable the button again
                mainBinding.progressBar.visibility = View.GONE
                mainBinding.refreshButton.isEnabled = true
            }, 1500) // Delay for 1.5 seconds
        }
    }

    // ✅ Fetch & Display Battery Level
    private fun updateBatteryLevel() {
        try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            mainBinding.batteryTextView.text = "Battery Level: $batteryLevel%"
        } catch (e: Exception) {
            mainBinding.batteryTextView.text = "Battery Level: Unknown"
        }
    }

    // ✅ Fetch & Display RAM Usage
    private fun updateRamUsage() {
        try {
            val totalRamGB = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)
                (memoryInfo.totalMem / (1024 * 1024 * 1024)).toInt()
            } else {
                val file = File("/proc/meminfo")
                val reader = BufferedReader(FileReader(file))
                val line = reader.readLine()
                reader.close()
                val totalRamKB = line.replace(Regex("[^0-9]"), "").toLong()
                (totalRamKB / (1024 * 1024)).toInt()
            }

            mainBinding.ramTextView.text = "RAM Usage: ${formatRAMSize(totalRamGB)}"
        } catch (e: Exception) {
            e.printStackTrace()
            mainBinding.ramTextView.text = "RAM Usage: Unknown"
        }
    }

    // 🔹 Format RAM to standard hardware values
    private fun formatRAMSize(ramGB: Int): String {
        return when {
            ramGB <= 2 -> "2 GB"
            ramGB in 3..4 -> "4 GB"
            ramGB in 5..6 -> "6 GB"
            ramGB in 7..8 -> "8 GB"
            ramGB in 9..12 -> "12 GB"
            ramGB in 13..16 -> "16 GB"
            ramGB in 17..24 -> "24 GB"
            ramGB in 25..32 -> "32 GB"
            else -> "$ramGB GB"
        }
    }

    private fun updateStorageUsage() {
        try {
            val internalStorage = getStorageDetails(Environment.getDataDirectory()) // Internal Storage

            // Fetch External Storage if available (For API 24+)
            val externalStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val externalDirs = getExternalFilesDirs(null)
                externalDirs?.getOrNull(1)?.let { getStorageDetails(it) } ?: StorageInfo(0, 0, 0)
            } else {
                getStorageDetails(Environment.getExternalStorageDirectory())
            }

            // Display Storage Data
            val storageText = StringBuilder()
            storageText.append("Internal: ${internalStorage.used} GB used / ${internalStorage.total} GB total\n")
            if (externalStorage.total > 0) {
                storageText.append("External: ${externalStorage.used} GB used / ${externalStorage.total} GB total")
            }

            mainBinding.storageTextView.text = storageText.toString().trim()
        } catch (e: Exception) {
            mainBinding.storageTextView.text = "Storage: Unable to fetch"
            e.printStackTrace()
        }
    }

    private fun getStorageDetails(path: File?): StorageInfo {
        return try {
            if (path == null) return StorageInfo(0, 0, 0)

            val statFs = StatFs(path.absolutePath)
            val totalBytes = statFs.totalBytes
            val freeBytes = statFs.availableBytes
            val usedBytes = totalBytes - freeBytes

            val totalGB = totalBytes / (1024 * 1024 * 1024)
            val usedGB = usedBytes / (1024 * 1024 * 1024)

            StorageInfo(totalGB, usedGB, freeBytes / (1024 * 1024 * 1024))
        } catch (e: Exception) {
            StorageInfo(0, 0, 0) // Return 0 if something goes wrong
        }
    }

    // ✅ Storage Data Model
    data class StorageInfo(val total: Long, val used: Long, val free: Long)

}
