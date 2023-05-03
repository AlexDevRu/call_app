package com.example.learning_android_callapp_kulakov

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import com.example.learning_android_callapp_kulakov.models.Call
import com.example.learning_android_callapp_kulakov.ui.edit.EditContactViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream


object Utils {

    fun doCall(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$phoneNumber")
        context.startActivity(intent)
    }

    fun sendSms(context: Context, phoneNumber: String) {
        val uri = Uri.fromParts("sms", phoneNumber, null)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    suspend fun getCallsByPhoneNumber(contentResolver: ContentResolver, phoneNumber: String, thumbnail: String?, displayName: String) = withContext(Dispatchers.IO) {
        val regex = Regex("\\D")
        val phoneOnlyDigits = phoneNumber.replace(regex, "")

        val idCol = CallLog.Calls._ID
        val dateCol = CallLog.Calls.DATE
        val numberCol = CallLog.Calls.NUMBER
        val typeCol = CallLog.Calls.TYPE
        val durationCol = CallLog.Calls.DURATION

        val projection = arrayOf(idCol, dateCol, typeCol, durationCol)

        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            "$numberCol='$phoneOnlyDigits' or $numberCol='$phoneNumber'",
            null,
            "$dateCol desc"
        ) ?: return@withContext emptyList()

        val idColIdx = cursor.getColumnIndex(idCol)
        val dateColIdx = cursor.getColumnIndex(dateCol)
        val typeColIdx = cursor.getColumnIndex(typeCol)
        val durationColIdx = cursor.getColumnIndex(durationCol)

        val calls = mutableListOf<Call>()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColIdx)
            val timestamp = cursor.getLong(dateColIdx)
            val type = cursor.getInt(typeColIdx)
            val duration = cursor.getLong(durationColIdx)

            val lastCall = calls.lastOrNull()
            if (lastCall != null && lastCall.callType == type && lastCall.phoneNumber == displayName) {
                calls.removeLast()
                calls.add(lastCall.copy(count = lastCall.count + 1))
            } else {
                val call = Call(
                    id = id,
                    timestamp = timestamp,
                    phoneNumber = displayName,
                    callType = type,
                    avatar = thumbnail,
                    duration = duration,
                    count = 1
                )
                calls.add(call)
            }
        }

        cursor.close()

        calls
    }

    fun shareContact(context: Context, lookupKey: String, contactName: String) {
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = ContactsContract.Contacts.CONTENT_VCARD_TYPE
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.putExtra(Intent.EXTRA_SUBJECT, contactName)
        context.startActivity(intent)
    }

    suspend fun getPhoneNumbersForContacts(contentResolver: ContentResolver) : Map<Long, String?> = withContext(Dispatchers.IO) {
        val idCol = ContactsContract.CommonDataKinds.Phone.CONTACT_ID
        val contactNumberCol = ContactsContract.CommonDataKinds.Phone.NUMBER

        val projection = arrayOf(idCol, contactNumberCol)

        val phonesMap = hashMapOf<Long, String?>()

        val phonesCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection,null,null, null
        ) ?: return@withContext phonesMap

        val idIndex = phonesCursor.getColumnIndexOrThrow(idCol)
        val numberIndex = phonesCursor.getColumnIndexOrThrow(contactNumberCol)

        while (phonesCursor.moveToNext()) {
            val phoneNumber = phonesCursor.getString(numberIndex)
            val contactId = phonesCursor.getLong(idIndex)
            phonesMap[contactId] = phoneNumber
        }

        phonesCursor.close()

        phonesMap
    }

    suspend fun editContact(
        contentResolver: ContentResolver,
        contactId: Long,
        givenName: String, familyName: String, middleName: String, phoneNumber: String, email: String, address: String,
        image: EditContactViewModel.Image?
    ): Unit = withContext(Dispatchers.IO) {
        val cpo = arrayListOf<ContentProviderOperation>()

        val phonesCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone._ID),
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=$contactId",
            null, null
        )
        phonesCursor?.moveToFirst()
        val phoneId = phonesCursor?.getString(phonesCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID))
        phonesCursor?.close()
        cpo.add(
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(
                    ContactsContract.CommonDataKinds.Phone._ID + " = $phoneId AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                )
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )

        val nameCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.StructuredName._ID),
            ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID + "=$contactId",
            null, null
        )
        nameCursor?.moveToFirst()
        val nameId = nameCursor?.getString(nameCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName._ID))
        nameCursor?.close()
        cpo.add(
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(
                    ContactsContract.CommonDataKinds.StructuredName._ID + " = $nameId AND ${ContactsContract.RawContacts.Data.MIMETYPE} = ?",
                    arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, givenName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, familyName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, middleName)
                .build()
        )

        val emailCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email._ID),
            ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=$contactId",
            null, null
        )
        emailCursor?.moveToFirst()
        val emailId = emailCursor?.getString(emailCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email._ID))
        emailCursor?.close()
        cpo.add(
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(
                    ContactsContract.CommonDataKinds.Email._ID + " = $emailId AND ${ContactsContract.RawContacts.Data.MIMETYPE} = ?",
                    arrayOf(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                )
                .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_MOBILE)
                .build()
        )

        val addressCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.StructuredPostal._ID),
            ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + "=$contactId",
            null, null
        )
        addressCursor?.moveToFirst()
        val addressId = addressCursor?.getString(addressCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal._ID))
        addressCursor?.close()
        cpo.add(
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(
                    ContactsContract.CommonDataKinds.StructuredPostal._ID + " = $addressId AND ${ContactsContract.RawContacts.Data.MIMETYPE} = ?",
                    arrayOf(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.DATA, address)
                .build()
        )

        if (image != null) {
            val byteArray = when (image) {
                is EditContactViewModel.Image.Camera -> {
                    val baos = ByteArrayOutputStream()
                    image.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    baos.toByteArray()
                }
                is EditContactViewModel.Image.Gallery -> {
                    imageUriToBytes(contentResolver, image.uri)
                }
            }

            val imageCursor = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Photo._ID),
                ContactsContract.CommonDataKinds.Photo.CONTACT_ID + "=$contactId AND ${ContactsContract.RawContacts.Data.MIMETYPE} = ?",
                arrayOf(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE), null
            )
            Timber.d("imageId = ${imageCursor?.count}")
            val exist = imageCursor?.moveToFirst() == true

            if (exist) {
                val imageId = imageCursor?.getString(imageCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Photo._ID))
                Timber.d("imageId = $imageId")
                cpo.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            ContactsContract.CommonDataKinds.Photo._ID + " = $imageId AND ${ContactsContract.RawContacts.Data.MIMETYPE} = ?",
                            arrayOf(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        )
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, byteArray)
                        .build()
                )
            } else {
                val c = contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts._ID),
                    ContactsContract.RawContacts.CONTACT_ID + " = ?",
                    arrayOf(contactId.toString()),
                    null
                )

                var rawContactId = 0
                Timber.d("c count = ${c?.count}")
                if (c != null && c.moveToFirst()) {
                    rawContactId = c.getInt(c.getColumnIndexOrThrow(ContactsContract.RawContacts._ID))
                    c.close()
                }

                cpo.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, byteArray)
                        .build()
                )
            }

            imageCursor?.close()
        }

        contentResolver.applyBatch(ContactsContract.AUTHORITY, cpo)
    }

    suspend fun insertContact(contentResolver: ContentResolver, givenName: String, familyName: String, middleName: String, phoneNumber: String, email: String, address: String, imageUri: Uri?): Unit = withContext(Dispatchers.IO) {
        val cpo = arrayListOf<ContentProviderOperation>()
        val rawContactId = 0
        cpo.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )
        cpo.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, givenName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, familyName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, middleName)
                .build()
        )
        cpo.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )

        cpo.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_MOBILE)
                .build()
        )

        cpo.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.DATA, address)
                .build()
        )

        if (imageUri != null) {
            val imageBytes = imageUriToBytes(contentResolver, imageUri)
            cpo.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, imageBytes)
                    .build()
            )
        }

        contentResolver.applyBatch(ContactsContract.AUTHORITY, cpo)
    }

    private fun imageUriToBytes(contentResolver: ContentResolver, uri: Uri) : ByteArray {
        val baos = ByteArrayOutputStream()
        val bmp = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        return baos.toByteArray()
    }

    suspend fun getContactEmail(contentResolver: ContentResolver, contactId: Long) = withContext(Dispatchers.IO) {
        var email = ""
        val emailCur = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.DATA),
            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
            arrayOf(contactId.toString()),
            null
        )
        if (emailCur != null && emailCur.moveToFirst()) {
            email =
                emailCur.getString(emailCur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DATA))
            emailCur.close()
        }
        email
    }

    suspend fun getContactAddress(contentResolver: ContentResolver, contactId: Long) = withContext(Dispatchers.IO) {
        val addressCur = contentResolver.query(
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.StructuredPostal.DATA),
            ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + " = ?",
            arrayOf(contactId.toString()),
            null
        )

        var address = ""
        if (addressCur != null && addressCur.moveToFirst()) {
            address =
                addressCur.getString(addressCur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.DATA))
            addressCur.close()
        }

        address
    }

    suspend fun getContactName(contentResolver: ContentResolver, contactId: Long) = withContext(Dispatchers.IO) {
        val nameCur = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
            ),
            ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID + " = ?",
            arrayOf(contactId.toString()),
            null
        )

        var givenName = ""
        var familyName = ""
        var middleName = ""
        if (nameCur != null && nameCur.moveToFirst()) {
            givenName =
                nameCur.getString(nameCur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME))
            familyName =
                nameCur.getString(nameCur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME))
            middleName =
                nameCur.getString(nameCur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME))
            nameCur.close()
        }

        Triple(givenName, familyName, middleName)
    }
}