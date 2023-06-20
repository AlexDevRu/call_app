package com.example.learning_android_callapp_kulakov.ui.call

import android.app.Application
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learning_android_callapp_kulakov.Utils
import com.example.learning_android_callapp_kulakov.models.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class CallViewModel(
    private val app: Application
): AndroidViewModel(app) {

    private val _duration = MutableLiveData(0L)
    val duration : LiveData<Long> = _duration

    private val _contact = MutableLiveData<Contact?>()
    val contact : LiveData<Contact?> = _contact

    private var job: Job? = null

    fun getContact(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val idCol = ContactsContract.Contacts._ID
            val displayNameCol = ContactsContract.Contacts.DISPLAY_NAME
            val thumbnailCol = ContactsContract.Contacts.PHOTO_THUMBNAIL_URI

            val phonesMap = Utils.getPhoneNumbersForContacts(app.contentResolver)

            val regex = Regex("\\D")
            val digitPhone = phoneNumber.replace(regex, "")
            val contactId = phonesMap.entries.find { (_, phone) -> phone?.replace(regex, "") == digitPhone }?.key

            val projection = arrayOf(displayNameCol, thumbnailCol)

            val contactsCursor = app.contentResolver?.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                "$idCol = $contactId",
                null,
                null
            )

            if (contactsCursor != null) {
                val nameIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val thumbnailIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
                if (contactsCursor.moveToFirst()) {
                    val name = contactsCursor.getString(nameIndex)
                    val thumbnail = contactsCursor.getString(thumbnailIndex)
                    Timber.d("CONTACT FOUND - $name")
                    val contact = Contact(
                        id = contactId!!,
                        name = name,
                        avatar = thumbnail,
                        phoneNumber = phoneNumber
                    )
                    _contact.postValue(contact)
                }
                contactsCursor.close()
            }
        }
    }

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