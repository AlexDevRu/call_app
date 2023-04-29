package com.example.learning_android_callapp_kulakov

import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.example.learning_android_callapp_kulakov.models.Call
import com.example.learning_android_callapp_kulakov.ui.edit.EditContactViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


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

    suspend fun editContact(contentResolver: ContentResolver, contactId: Long, name: String, phoneNumber: String, image: EditContactViewModel.Image?): Unit = withContext(Dispatchers.IO) {
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
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name)
                .build()
        )

        if (image != null) {
            val imageCursor = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Photo._ID),
                ContactsContract.CommonDataKinds.Photo.CONTACT_ID + "=$contactId AND ${ContactsContract.RawContacts.Data.MIMETYPE} = ?",
                arrayOf(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE), null
            )
            Timber.d("imageId = ${imageCursor?.count}")
            imageCursor?.moveToFirst()
            val imageId = imageCursor?.getString(imageCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Photo._ID))
            imageCursor?.close()

            Timber.d("imageId = $imageId")

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

            cpo.add(
                ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        ContactsContract.CommonDataKinds.Photo._ID + " = $imageId AND ${ContactsContract.RawContacts.Data.MIMETYPE} = ?",
                        arrayOf(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    )
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, byteArray)
                    .build()
            )
        }

        contentResolver.applyBatch(ContactsContract.AUTHORITY, cpo)
    }

    suspend fun insertContact(contentResolver: ContentResolver, name: String, phoneNumber: String, imageUri: Uri?): Unit = withContext(Dispatchers.IO) {
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
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name)
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

    fun getImageUri(contentResolver: ContentResolver, image: Bitmap): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            saveImageInQ(contentResolver, image)
        else
            saveImageInLegacy(image)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageInQ(contentResolver: ContentResolver, bitmap: Bitmap) : Uri? {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var imageUri: Uri? = null
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        contentResolver.also { resolver ->
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        }

        fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }

        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)

        if (imageUri != null)
            contentResolver.update(imageUri!!, contentValues, null, null)

        return imageUri
    }

    private fun saveImageInLegacy(bitmap: Bitmap) : Uri {
        val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val image = File(imagesDir, "IMG_${System.currentTimeMillis()}.jpg")
        val fos = FileOutputStream(image)
        fos.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        return Uri.fromFile(image)
    }
}