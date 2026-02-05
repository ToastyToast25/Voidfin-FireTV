package org.jellyfin.androidtv.ui.playlist

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.playlistsApi
import org.jellyfin.sdk.model.api.CreatePlaylistDto
import timber.log.Timber
import java.util.UUID

/**
 * XML-based dialog for creating a new playlist.
 * Uses proper D-pad navigation for Android TV remotes.
 */
class CreatePlaylistDialogFragment : DialogFragment() {

	private var itemId: UUID? = null
	private var apiClient: ApiClient? = null
	private var onPlaylistCreated: (() -> Unit)? = null
	private var onBackPressed: (() -> Unit)? = null
	private var backButtonPressed = false

	companion object {
		fun newInstance(
			itemId: UUID,
			apiClient: ApiClient,
			onPlaylistCreated: () -> Unit,
			onBackPressed: () -> Unit
		): CreatePlaylistDialogFragment {
			return CreatePlaylistDialogFragment().apply {
				this.itemId = itemId
				this.apiClient = apiClient
				this.onPlaylistCreated = onPlaylistCreated
				this.onBackPressed = onBackPressed
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NO_FRAME, R.style.Theme_Jellyfin_Dialog)
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val dialog = super.onCreateDialog(savedInstanceState)
		dialog.window?.apply {
			requestFeature(Window.FEATURE_NO_TITLE)
			setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			setDimAmount(0.6f)
			addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
		}
		dialog.setOnKeyListener { _, keyCode, event ->
			if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
				backButtonPressed = true
				dismiss()
				true
			} else {
				false
			}
		}
		return dialog
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		return inflater.inflate(R.layout.dialog_create_playlist, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val playlistNameInput = view.findViewById<EditText>(R.id.playlist_name_input)
		val publicSwitchContainer = view.findViewById<View>(R.id.public_playlist_container)
		val publicSwitchLabel = view.findViewById<android.widget.TextView>(R.id.public_playlist_label)
		val publicSwitch = view.findViewById<Switch>(R.id.public_playlist_switch)
		val cancelButton = view.findViewById<Button>(R.id.button_cancel)
		val backButton = view.findViewById<Button>(R.id.button_back)
		val createButton = view.findViewById<Button>(R.id.button_create)

		publicSwitchContainer.setOnFocusChangeListener { _, hasFocus ->
			val textColor = if (hasFocus) {
				resources.getColor(R.color.button_default_highlight_text, null)
			} else {
				resources.getColor(android.R.color.black, null)
			}
			publicSwitchLabel.setTextColor(textColor)
		}

		publicSwitchContainer.setOnClickListener {
			publicSwitch.isChecked = !publicSwitch.isChecked
		}

		playlistNameInput.setOnEditorActionListener { _, actionId, _ ->
			if (actionId == EditorInfo.IME_ACTION_NEXT) {
				publicSwitchContainer.requestFocus()
				true
			} else {
				false
			}
		}

		cancelButton.setOnClickListener {
			dismiss()
		}

		backButton.setOnClickListener {
			backButtonPressed = true
			dismiss()
		}

		createButton.setOnClickListener {
			val playlistName = playlistNameInput.text.toString().trim()
			if (playlistName.isBlank()) {
				Toast.makeText(
					requireContext(),
					R.string.msg_enter_playlist_name,
					Toast.LENGTH_SHORT
				).show()
				playlistNameInput.requestFocus()
				return@setOnClickListener
			}

			val isPublic = publicSwitch.isChecked
			createPlaylist(playlistName, isPublic)
		}

		playlistNameInput.requestFocus()
	}

	override fun onStart() {
		super.onStart()
		dialog?.window?.apply {
			setLayout(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
			setGravity(Gravity.CENTER)
		}
	}

	override fun onDismiss(dialog: android.content.DialogInterface) {
		super.onDismiss(dialog)
		if (backButtonPressed) {
			onBackPressed?.invoke()
		}
	}

	private fun createPlaylist(name: String, isPublic: Boolean) {
		val api = apiClient ?: return
		val id = itemId ?: return

		CoroutineScope(Dispatchers.Main).launch {
			try {
				withContext(Dispatchers.IO) {
					val createRequest = CreatePlaylistDto(
						name = name,
						ids = listOf(id),
						users = emptyList(),
						isPublic = isPublic,
					)
					api.playlistsApi.createPlaylist(createRequest)
				}
				Toast.makeText(
					requireContext(),
					R.string.msg_playlist_created,
					Toast.LENGTH_SHORT
				).show()
				dismiss()
				onPlaylistCreated?.invoke()
			} catch (e: Exception) {
				Timber.e(e, "Failed to create playlist")
				Toast.makeText(
					requireContext(),
					R.string.msg_failed_to_create_playlist,
					Toast.LENGTH_SHORT
				).show()
			}
		}
	}
}
