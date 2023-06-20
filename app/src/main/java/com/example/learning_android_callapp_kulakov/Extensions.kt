package com.example.learning_android_callapp_kulakov

import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.widget.EditText

object Extensions {
    fun EditText.pop() {
        val text = text?.toString().orEmpty()
        if (text.isNotEmpty()) {
            setText(text.dropLast(1))
            setSelection(text.length - 1)
        }
    }

    fun Context.isDarkThemeOn(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
    }
}