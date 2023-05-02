package com.example.learning_android_callapp_kulakov.ui.add_contact

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.learning_android_callapp_kulakov.Utils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class AddContactViewModel(private val app: Application): AndroidViewModel(app) {

    private val _goBack = MutableSharedFlow<Unit>()
    val goBack : SharedFlow<Unit> = _goBack

    private val _uri = MutableLiveData<Uri>()
    val uri : LiveData<Uri> = _uri

    fun setUri(uri: Uri) {
        _uri.value = uri
    }

    fun insertContact(givenName: String, familyName: String, middleName: String, phoneNumber: String, email: String, address: String) {
        viewModelScope.launch {
            Utils.insertContact(app.contentResolver, givenName, familyName, middleName, phoneNumber, email, address, uri.value)
            _goBack.emit(Unit)
        }
    }

}