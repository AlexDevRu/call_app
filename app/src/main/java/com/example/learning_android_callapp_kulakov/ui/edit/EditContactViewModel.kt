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

    private val _uri = MutableLiveData<Uri>()
    val uri : LiveData<Uri> = _uri

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

    fun setUri(image: Uri) {
        _uri.value = image
    }

    private fun getContactDetails(contactId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val idCol = ContactsContract.Contacts._ID
            val displayNameCol = ContactsContract.Contacts.DISPLAY_NAME
            val hasPhoneNumberCol = ContactsContract.Contacts.HAS_PHONE_NUMBER
            val photoCol = ContactsContract.Contacts.PHOTO_URI

            val projection = arrayOf(idCol, displayNameCol, hasPhoneNumberCol, photoCol)

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

                if (contactsCursor.moveToFirst()) {
                    val id = contactsCursor.getLong(idIndex)
                    val name = contactsCursor.getString(nameIndex)
                    val photo = contactsCursor.getString(photoIndex)
                    val hasPhone = contactsCursor.getInt(hasPhoneIndex)

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

                        val contact = Contact(
                            id = id,
                            name = name,
                            avatar = photo,
                            phoneNumber = phoneNumber
                        )

                        val contactName = Utils.getContactName(app.contentResolver, id)

                        val contactDetails = ContactDetails(
                            contact = contact,
                            givenName = contactName.first.orEmpty(),
                            familyName = contactName.second.orEmpty(),
                            middleName = contactName.third.orEmpty(),
                            email = Utils.getContactEmail(app.contentResolver, id),
                            address = Utils.getContactAddress(app.contentResolver, id),
                            calls = emptyList()
                        )
                        _contact.postValue(contactDetails)
                    }
                }

                contactsCursor.close()
            }
        }
    }

    fun saveChanges(givenName: String, familyName: String, middleName: String, phoneNumber: String, email: String, address: String) {
        viewModelScope.launch {
            val image = if (uri.value != null) Image.Gallery(uri.value!!) else null
            Utils.editContact(app.contentResolver, contact.value!!.contact.id, givenName, familyName, middleName, phoneNumber, email, address, image)
            _goBack.emit(Unit)
        }
    }

    companion object {
        const val CONTACT_ID = "CONTACT_ID"
    }
}