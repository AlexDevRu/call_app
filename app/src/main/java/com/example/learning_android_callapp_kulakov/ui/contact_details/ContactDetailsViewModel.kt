package com.example.learning_android_callapp_kulakov.ui.contact_details

import android.app.Application
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.*
import com.example.learning_android_callapp_kulakov.Utils
import com.example.learning_android_callapp_kulakov.models.Contact
import com.example.learning_android_callapp_kulakov.models.ContactDetails
import com.example.learning_android_callapp_kulakov.models.ContactInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class ContactDetailsViewModel(
    private val app: Application,
    savedStateHandle: SavedStateHandle
): AndroidViewModel(app) {

    private val _contact = MutableLiveData<ContactDetails>()
    val contact : LiveData<ContactDetails> = _contact

    private val _goBack = MutableSharedFlow<Unit>()
    val goBack : SharedFlow<Unit> = _goBack

    fun getContactInfo() = ContactInfo(
        givenName = contact.value!!.givenName,
        familyName = contact.value!!.familyName,
        middleName = contact.value!!.middleName,
        email = contact.value!!.email,
        address = contact.value!!.address,
        phoneNumber = contact.value!!.contact.phoneNumber,
    )

    val contactId: Long
        get() = contact.value?.contact?.id ?: 0L

    val phoneNumber: String
        get() = contact.value?.contact?.phoneNumber.orEmpty()

    val contactName: String
        get() = contact.value?.contact?.name.orEmpty()

    var lookupKey: String = ""
        private set

    init {
        val contactId = savedStateHandle.get<Long>("CONTACT_ID")
        Timber.e("contactId - $contactId")

        viewModelScope.launch(Dispatchers.IO) {
            val idCol = ContactsContract.Contacts._ID
            val displayNameCol = ContactsContract.Contacts.DISPLAY_NAME
            val hasPhoneNumberCol = ContactsContract.Contacts.HAS_PHONE_NUMBER
            val photoCol = ContactsContract.Contacts.PHOTO_URI
            val thumbnailCol = ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
            val lookupKeyCol = ContactsContract.Contacts.LOOKUP_KEY

            val projection = arrayOf(idCol, displayNameCol, hasPhoneNumberCol, photoCol, thumbnailCol, lookupKeyCol)

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
                val lookupKeyIndex = contactsCursor.getColumnIndex(lookupKeyCol)

                if (contactsCursor.moveToFirst()) {
                    val id = contactsCursor.getLong(idIndex)
                    val name = contactsCursor.getString(nameIndex)
                    val photo = contactsCursor.getString(photoIndex)
                    val thumbnail = contactsCursor.getString(thumbnailIndex)
                    val hasPhone = contactsCursor.getInt(hasPhoneIndex)
                    lookupKey = contactsCursor.getString(lookupKeyIndex)

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
                                val phoneNumber = phones.getString(phones.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                phones.close()
                                phoneNumber
                            } else ""
                        }

                        val calls = if (phoneNumber.isNotEmpty())
                            Utils.getCallsByPhoneNumber(app.contentResolver, phoneNumber, thumbnail, name)
                        else
                            emptyList()

                        var givenName: String? = null
                        var familyName: String? = null
                        var middleName: String? = null
                        val nameCursor = app.contentResolver.query(
                            ContactsContract.Data.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.StructuredName._ID),
                            ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID + "=$contactId",
                            null, null
                        )
                        nameCursor?.moveToFirst()
                        val nameId = nameCursor?.getString(nameCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName._ID))
                        nameCursor?.close()
                        val nameCursor2 = app.contentResolver.query(
                            ContactsContract.Data.CONTENT_URI,
                            arrayOf(
                                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                                ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
                            ),
                            ContactsContract.CommonDataKinds.StructuredName._ID + " = $nameId AND ${ContactsContract.RawContacts.Data.MIMETYPE} = ?",
                            arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
                            null
                        )
                        if (nameCursor2?.moveToFirst() == true) {
                            givenName = nameCursor2.getString(nameCursor2.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME))
                            familyName = nameCursor2.getString(nameCursor2.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME))
                            middleName = nameCursor2.getString(nameCursor2.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME))
                            nameCursor2.close()
                        }

                        val contact = Contact(
                            id = id,
                            name = name,
                            avatar = photo,
                            phoneNumber = phoneNumber
                        )

                        val contactDetails = ContactDetails(
                            contact = contact,
                            givenName = givenName.orEmpty(),
                            familyName = familyName.orEmpty(),
                            middleName = middleName.orEmpty(),
                            email = Utils.getContactEmail(app.contentResolver, id),
                            address = Utils.getContactAddress(app.contentResolver, id),
                            calls = calls
                        )
                        _contact.postValue(contactDetails)
                    }
                }

                contactsCursor.close()
            }
        }
    }

    fun delete() {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
            app.contentResolver.delete(uri, null, null)
            _goBack.emit(Unit)
        }
    }
}
