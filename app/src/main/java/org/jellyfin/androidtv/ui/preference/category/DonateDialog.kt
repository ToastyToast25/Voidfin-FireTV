package org.jellyfin.androidtv.ui.preference.category

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import org.jellyfin.androidtv.R

fun showDonateDialog(context: Context) {
	val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_donate, null)
	
	val titleText = dialogView.findViewById<TextView>(R.id.donate_title)
	val messageText = dialogView.findViewById<TextView>(R.id.donate_message)
	val qrCodeImage = dialogView.findViewById<ImageView>(R.id.donate_qr_code)
	val thanksText = dialogView.findViewById<TextView>(R.id.donate_thanks)
	
	titleText.text = context.getString(R.string.donate_dialog_title)
	messageText.text = context.getString(R.string.donate_dialog_message)
	thanksText.text = context.getString(R.string.donate_dialog_thanks)
	qrCodeImage.setImageResource(R.drawable.qr_code)
	
	AlertDialog.Builder(context)
		.setView(dialogView)
		.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
		.create()
		.show()
}
