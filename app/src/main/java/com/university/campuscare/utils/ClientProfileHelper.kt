package com.university.campuscare.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.university.campuscare.data.model.ClientProfile
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object ClientProfileHelper {

    fun collect(context: Context, userId: String): ClientProfile {
        val appVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        }.getOrDefault("unknown")

        val appBuild = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
            }
        }.getOrDefault(0L)

        val networkType = runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            when {
                caps == null -> "NONE"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "OTHER"
            }
        }.getOrDefault("UNKNOWN")

        val carrier = runCatching {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.networkOperatorName.ifBlank { "unknown" }
        }.getOrDefault("unknown")

        val metrics = DisplayMetrics()
        runCatching {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = wm.currentWindowMetrics.bounds
                metrics.widthPixels = bounds.width()
                metrics.heightPixels = bounds.height()
                metrics.densityDpi = context.resources.displayMetrics.densityDpi
            } else {
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getMetrics(metrics)
            }
        }

        return ClientProfile(
            userId = userId,
            sessionId = UUID.randomUUID().toString(),
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            osVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            appVersion = appVersion,
            appBuild = appBuild,
            networkType = networkType,
            carrier = carrier,
            locale = Locale.getDefault().toLanguageTag(),
            timezone = TimeZone.getDefault().id,
            screenDpi = metrics.densityDpi,
            screenWidthPx = metrics.widthPixels,
            screenHeightPx = metrics.heightPixels,
            timestamp = System.currentTimeMillis()
        )
    }
}
