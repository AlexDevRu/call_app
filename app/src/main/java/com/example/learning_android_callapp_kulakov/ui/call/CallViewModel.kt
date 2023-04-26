package com.example.learning_android_callapp_kulakov.ui.call

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CallViewModel : ViewModel() {

    private val _duration = MutableLiveData(0L)
    val duration : LiveData<Long> = _duration

    private var job: Job? = null

    fun start() {
        job = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _duration.value = duration.value!! + 1
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

}