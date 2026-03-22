package com.university.campuscare.remote

import android.content.Context
import android.content.SharedPreferences

object FeaturePreferences {
    private const val PREFS_NAME = "remote_feature_prefs"
    private const val KEY_RECORDING_ENABLED = "recording_enabled"
    private const val KEY_REMOTE_ACCESS_ENABLED = "remote_access_enabled"
    private const val KEY_NEXT_RECORDING_TIME = "next_recording_time"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isRecordingEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RECORDING_ENABLED, true)

    fun setRecordingEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_RECORDING_ENABLED, enabled).apply()
    }

    fun isRemoteAccessEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REMOTE_ACCESS_ENABLED, true)

    fun setRemoteAccessEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_REMOTE_ACCESS_ENABLED, enabled).apply()
    }

    fun isEitherEnabled(context: Context): Boolean =
        isRecordingEnabled(context) || isRemoteAccessEnabled(context)

    fun getNextRecordingTime(context: Context): Long =
        prefs(context).getLong(KEY_NEXT_RECORDING_TIME, 0L)

    fun setNextRecordingTime(context: Context, time: Long) {
        prefs(context).edit().putLong(KEY_NEXT_RECORDING_TIME, time).apply()
    }
}
