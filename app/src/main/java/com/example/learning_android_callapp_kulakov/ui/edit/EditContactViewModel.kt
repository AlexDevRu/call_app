package com.example.learning_android_callapp_kulakov.ui.edit

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.*
import com.example.learning_android_callapp_kulakov.Utils
import com.example.learning_android_callapp_kulakov.models.Contact
import com.example.learning_android_callapp_kulakov.models.ContactDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class EditContactViewModel(
    private val app: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {

    private val _contact = MutableLiveData<ContactDetails>()
    val contact: LiveData<ContactDetails> = _contact

    private val _goBack = MutableSharedFlow<Unit>()
    val goBack : SharedFlow<Unit> = _goBack

    private var contactId = 0L

    private val _uri = MutableLiveData<Image>()
    val uri : LiveData<Image> = _uri

    sealed interface Image {
        class Camera(val bitmap: Bitmap) : Image
        class Gallery(val uri: Uri) : Image
    }

    init {
        savedStateHandle.get<Long>(CONTACT_ID)?.let {
            contactId = it
            getContactDetails(it)
        }
    }

    fun setUri(image: Image) {
        _uri.value = image
    }

    private fun getContactDetails(contactId: Long) {
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
                    //lookupKey = contactsCursor.getString(lookupKeyIndex)

                    if (name != null) {
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
                                val phoneNumber = phones.getString(phones.getColumnIndexOrThrow(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER))
                                phones.close()
                                phoneNumber
                            } else ""
                        }

                        val calls = if (phoneNumber.isNotEmpty())
                            Utils.getCallsByPhoneNumber(app.contentResolver, phoneNumber, thumbnail, name)
                        else
                            emptyList()

                        val contact = Contact(
                            id = id,
                            name = name,
                            avatar = photo,
                            phoneNumber = phoneNumber
                        )

                        val contactDetails = ContactDetails(contact, calls)
                        _contact.postValue(contactDetails)
                    }
                }

                contactsCursor.close()
            }
        }
    }

    fun saveChanges(name: String, phoneNumber: String) {
        viewModelScope.launch {
            Utils.editContact(app.contentResolver, contact.value!!.contact.id, name, phoneNumber, uri.value)
            _goBack.emit(Unit)
        }
    }

    companion object {
        const val CONTACT_ID = "CONTACT_ID"
    }
}