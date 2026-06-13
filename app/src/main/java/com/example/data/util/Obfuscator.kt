package com.example.data.util

import android.util.Base64

object Obfuscator {
    // Hidden URL: "https://raw.githubusercontent.com/surenpaleru-tech/divine-server/main/"
    private const val OBFUSCATED_DEFAULT_URL = "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3N1cmVucGFsZXJ1LXRlY2gvZGl2aW5lLXNlcnZlci9tYWluLw=="

    fun getDecodedUrl(): String {
        return try {
            val decodedBytes = Base64.decode(OBFUSCATED_DEFAULT_URL, Base64.DEFAULT)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "https://raw.githubusercontent.com/surenpaleru-tech/divine-server/main/"
        }
    }
}
