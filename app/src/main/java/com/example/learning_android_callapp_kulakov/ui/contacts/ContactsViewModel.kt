package com.example.learning_android_callapp_kulakov.ui.contacts

import android.app.Application
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.learning_android_callapp_kulakov.models.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactsViewModel(private val app: Application): AndroidViewModel(app) {

    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts : LiveData<List<Contact>> = _contacts

    fun readContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val idCol = ContactsContract.Contacts._ID
            val displayNameCol = ContactsContract.Contacts.DISPLAY_NAME
            val thumbnailCol = ContactsContract.Contacts.PHOTO_THUMBNAIL_URI

            val projection = arrayOf(idCol, displayNameCol, thumbnailCol)

            val contactsList = ArrayList<Contact>()
            val contactsCursor = app.contentResolver?.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            if (contactsCursor != null && contactsCursor.count > 0) {
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
                            avatar = thumbnail
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