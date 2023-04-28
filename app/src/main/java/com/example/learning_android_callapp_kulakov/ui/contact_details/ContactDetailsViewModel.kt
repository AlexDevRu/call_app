package com.example.learning_android_callapp_kulakov.ui.contact_details

import android.app.Application
import android.provider.ContactsContract
import androidx.lifecycle.*
import com.example.learning_android_callapp_kulakov.Utils
import com.example.learning_android_callapp_kulakov.models.Contact
import com.example.learning_android_callapp_kulakov.models.ContactDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ContactDetailsViewModel(
    private val app: Application,
    savedStateHandle: SavedStateHandle
): AndroidViewModel(app) {

    private val _contact = MutableLiveData<ContactDetails>()
    val contact : LiveData<ContactDetails> = _contact

    val phoneNumber: String
        get() = contact.value?.phoneNumber.orEmpty()

    init {
        val contactId = savedStateHandle.get<Long>("CONTACT_ID")
        Timber.e("contactId - $contactId")

        viewModelScope.launch(Dispatchers.IO) {
            val idCol = ContactsContract.Contacts._ID
            val displayNameCol = ContactsContract.Contacts.DISPLAY_NAME
            val hasPhoneNumberCol = ContactsContract.Contacts.HAS_PHONE_NUMBER
            val photoCol = ContactsContract.Contacts.PHOTO_URI
            val thumbnailCol = ContactsContract.Contacts.PHOTO_THUMBNAIL_URI

            val projection = arrayOf(idCol, displayNameCol, hasPhoneNumberCol, photoCol, thumbnailCol)

            val contactsCursor = app.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                ContactsContract.Contacts._ID + "=$contactId",
                null,
                null
            )

            if (contactsCursor != null && contactsCursor.count > 0) {
                val idIndex = contactsCursor.getColumnIndex(idCol)
                val nameIndex = contactsCursor.getColumnIndex(displayNameCol)
                val hasPhoneIndex = contactsCursor.getColumnIndex(hasPhoneNumberCol)
                val photoIndex = contactsCursor.getColumnIndex(photoCol)
                val thumbnailIndex = contactsCursor.getColumnIndex(thumbnailCol)

                if (contactsCursor.moveToFirst()) {
                    val id = contactsCursor.getLong(idIndex)
                    val name = contactsCursor.getString(nameIndex)
                    val photo = contactsCursor.getString(photoIndex)
                    val thumbnail = contactsCursor.getString(thumbnailIndex)
                    val hasPhone = contactsCursor.getInt(hasPhoneIndex)
                    if (name != null) {
                        val contact = Contact(
                            id = id,
                            name = name,
                            avatar = photo
                        )

                        val phoneNumber = if (hasPhone == 0) "" else {
                            val phones = app.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=$id",
                                null,
                                null
                            )
                            if (phones != null) {
                                phones.moveToFirst()
                                val phoneNumber = phones.getString(phones.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                phones.close()
                                phoneNumber
                            } else ""
                        }

                        val calls = if (phoneNumber.isNotEmpty())
                            Utils.getCallsByPhoneNumber(app.contentResolver, phoneNumber, thumbnail, name)
                        else
                            emptyList()

                        val contactDetails = ContactDetails(contact, phoneNumber, calls)
                        _contact.postValue(contactDetails)
                    }
                }

                contactsCursor.close()
            }
        }
    }
}
