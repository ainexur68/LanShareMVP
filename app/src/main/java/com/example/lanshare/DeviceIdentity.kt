package com.example.lanshare

import android.content.Context
import android.os.Build
import java.util.UUID

object DeviceIdentity {
    fun fingerprint(context: Context): String {
        val p = context.getSharedPreferences("identity", Context.MODE_PRIVATE)
        return p.getString("fingerprint", null) ?: UUID.randomUUID().toString().also {
            p.edit().putString("fingerprint", it).apply()
        }
    }

    fun alias(context: Context): String {
        val custom = context.getSharedPreferences("identity", Context.MODE_PRIVATE).getString("alias", null)
        return custom ?: (Build.MODEL ?: "Android")
    }
}
