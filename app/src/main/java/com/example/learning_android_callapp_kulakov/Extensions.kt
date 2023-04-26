package com.example.learning_android_callapp_kulakov

import android.widget.EditText

object Extensions {
    fun EditText.pop() {
        val text = text?.toString().orEmpty()
        if (text.isNotEmpty()) {
            setText(text.dropLast(1))
            setSelection(text.length - 1)
        }
    }
}