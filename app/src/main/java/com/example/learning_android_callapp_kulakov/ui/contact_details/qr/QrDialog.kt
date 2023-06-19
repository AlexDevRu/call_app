package com.example.learning_android_callapp_kulakov.ui.contact_details.qr

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.example.learning_android_callapp_kulakov.Utils.toPx
import com.example.learning_android_callapp_kulakov.models.ContactInfo
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder


class QrDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val imageView = ImageView(requireContext())

        val contact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(CONTACT_ARG, ContactInfo::class.java)
        } else {
            requireArguments().getParcelable(CONTACT_ARG)!!
        }

        val json = Gson().toJson(contact)
        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.encodeBitmap(json, BarcodeFormat.QR_CODE, 200.toPx, 200.toPx)
        imageView.setImageBitmap(bitmap)

        return AlertDialog.Builder(requireContext())
            .setTitle("Scan QR")
            .setView(imageView)
            .create()
    }

    companion object {
        private const val CONTACT_ARG = "CONTACT"

        fun createInstance(contact: ContactInfo) = QrDialog().apply {
            arguments = bundleOf(CONTACT_ARG to contact)
        }
    }

}