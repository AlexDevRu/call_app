package com.example.learning_android_callapp_kulakov.ui.contacts

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.learning_android_callapp_kulakov.Utils
import com.example.learning_android_callapp_kulakov.models.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactsViewModel(private val app: Application): AndroidViewModel(app) {

    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts : LiveData<List<Contact>> = _contacts

    private val handler = Handler(Looper.getMainLooper())

    private val contactsContentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            readContacts()
        }
    }

    init {
        app.contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            contactsContentObserver
        )
    }

    override fun onCleared() {
        super.onCleared()
        app.contentResolver.unregisterContentObserver(contactsContentObserver)
    }

    fun readContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val idCol = ContactsContract.Contacts._ID
            val displayNameCol = ContactsContract.Contacts.DISPLAY_NAME
            val thumbnailCol = ContactsContract.Contacts.PHOTO_THUMBNAIL_URI

            val phonesMap = Utils.getPhoneNumbersForContacts(app)

            val projection = arrayOf(idCol, displayNameCol, thumbnailCol)

            val contactsList = ArrayList<Contact>()
            val contactsCursor = app.contentResolver?.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            if (contactsCursor != null) {
                val idIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val thumbnailIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
                while (contactsCursor.moveToNext()) {
                    val id = contactsCursor.getLong(idIndex)
                    val name = contactsCursor.getString(nameIndex)
                    val thumbnail = contactsCursor.getString(thumbnailIndex)
                    if (name != null) {
                        val contact = Contact(
                            id = id,
                            name = name,
                            avatar = thumbnail,
                            phoneNumber = phonesMap[id]
                        )
                        contactsList.add(contact)
                    }
                }
                _contacts.postValue(contactsList)
                contactsCursor.close()
            }
        }
    }

}