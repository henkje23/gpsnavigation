package com.hendrik.gpsnavigation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersion: String,
    val minimumVersion: String,
    val downloadUrl: String
)

object UpdateManager {

    private const val UPDATE_URL =
        "https://raw.githubusercontent.com/henkje23/gps-navigation/main/update.json"

    suspend fun checkForUpdate(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val connection =
                    URL(UPDATE_URL).openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                connection.disconnect()

                val json = JSONObject(response)

                UpdateInfo(
                    latestVersion = json.getString("latestVersion"),
                    minimumVersion = json.getString("minimumVersion"),
                    downloadUrl = json.getString("downloadUrl")
                )

            } catch (e: Exception) {
                null
            }
        }
    }

    fun compareVersions(
        currentVersion: String,
        otherVersion: String
    ): Int {

        val current =
            currentVersion
                .split(".")
                .map { it.toIntOrNull() ?: 0 }

        val other =
            otherVersion
                .split(".")
                .map { it.toIntOrNull() ?: 0 }

        for (
        i in 0 until maxOf(
            current.size,
            other.size
        )
        ) {

            val currentPart =
                current.getOrElse(i) { 0 }

            val otherPart =
                other.getOrElse(i) { 0 }

            if (currentPart < otherPart) {
                return -1
            }

            if (currentPart > otherPart) {
                return 1
            }
        }

        return 0
    }

    fun downloadAndInstall(
        context: Context,
        downloadUrl: String
    ) {

        Thread {

            try {

                val connection =
                    URL(downloadUrl)
                        .openConnection() as HttpURLConnection

                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                val apkFile =
                    java.io.File(
                        context.cacheDir,
                        "gps-navigation-update.apk"
                    )

                android.os.Handler(
                    android.os.Looper.getMainLooper()
                ).post {

                    android.widget.Toast.makeText(
                        context,
                        "APK wordt gedownload...",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                connection.inputStream.use { input ->

                    apkFile.outputStream().use { output ->

                        input.copyTo(output)
                    }
                }

                connection.disconnect()

                android.os.Handler(
                    android.os.Looper.getMainLooper()
                ).post {

                    android.widget.Toast.makeText(
                        context,
                        "✅ APK gedownload!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                val apkUri =
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )

                val intent =
                    android.content.Intent(
                        android.content.Intent.ACTION_INSTALL_PACKAGE
                    ).apply {

                        data = apkUri

                        addFlags(
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        addFlags(
                            android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    }

                android.os.Handler(
                    android.os.Looper.getMainLooper()
                ).post {

                    android.widget.Toast.makeText(
                        context,
                        "📦 Installatiescherm openen...",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                context.startActivity(intent)

            } catch (e: Exception) {

                android.os.Handler(
                    android.os.Looper.getMainLooper()
                ).post {

                    android.widget.Toast.makeText(
                        context,
                        "❌ Update downloaden mislukt.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }
}