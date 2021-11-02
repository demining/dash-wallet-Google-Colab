/*
 * Copyright 2020 Dash Core Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.schildbach.wallet.ui

import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import de.schildbach.wallet.ui.dashpay.EditProfileViewModel
import de.schildbach.wallet_test.R
import kotlinx.android.synthetic.main.profile_picture_state_dialog.*
import de.schildbach.wallet.ui.dashpay.work.UpdateProfileError

class UploadProfilePictureStateDialog : DialogFragment() {

    private val showError: UpdateProfileError by lazy {
        UpdateProfileError.getByValue(requireArguments().getInt(ARG_SHOW_ERROR, UpdateProfileError.NO_ERROR.ordinal))!!
    }
    private lateinit var editProfileViewModel: EditProfileViewModel

    companion object {

        private const val ARG_SHOW_ERROR = "show_error"

        @JvmStatic
        fun newInstance(showError: UpdateProfileError = UpdateProfileError.NO_ERROR): UploadProfilePictureStateDialog {
            val fragment = UploadProfilePictureStateDialog()
            val args = Bundle()
            args.putInt(ARG_SHOW_ERROR, showError.ordinal)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.profile_picture_state_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editProfileViewModel = ViewModelProvider(requireActivity())
                .get(EditProfileViewModel::class.java)
        updateUiState(showError)

        val file = editProfileViewModel.profilePictureFile
        if (file != null) {
            Glide.with(requireActivity()).load(file)
                    .circleCrop().signature(ObjectKey(file.lastModified())).into(avatar)

        }

        try_again_btn.setOnClickListener {
            dismiss()
            if (showError != UpdateProfileError.AUTHENTICATION) {
                editProfileViewModel.uploadProfilePicture()
            }
        }
        cancel_btn.setOnClickListener {
            if (showError != UpdateProfileError.NO_ERROR) {
                dismiss()
            } else {
                cancelRequest()
            }
        }
    }

    private fun cancelRequest() {
        editProfileViewModel.cancelUploadRequest()
        dismiss()
    }

    override fun onStart() {
        super.onStart()
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog?.window?.setLayout(width, height)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun showError(error: UpdateProfileError) {
        updateUiState(error)
    }

    private fun updateUiState(error: UpdateProfileError = UpdateProfileError.NO_ERROR) {
        when (error) {
            UpdateProfileError.UPLOAD -> {
                icon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_error))
                title.setText(R.string.profile_picture_upload_error_title)
                subtitle.setText(R.string.profile_picture_upload_error_message)
                try_again_btn.visibility = View.VISIBLE
                cancel_btn.visibility = View.VISIBLE
            }
            UpdateProfileError.NO_ERROR -> {
                icon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_hourglass))
                (icon.drawable as AnimationDrawable).start()
                title.setText(R.string.profile_picture_uploading_title)
                subtitle.setText(R.string.profile_picture_uploading_message)
                try_again_btn.visibility = View.GONE
                cancel_btn.visibility = View.VISIBLE
            }
            UpdateProfileError.AUTHENTICATION -> {
                icon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_error))
                title.setText(
                        when (editProfileViewModel.storageService) {
                            EditProfileViewModel.ProfilePictureStorageService.GOOGLE_DRIVE -> R.string.select_source_google_drive
                            EditProfileViewModel.ProfilePictureStorageService.IMGUR -> R.string.edit_profile_imgur
                        }
                )
                subtitle.setText(R.string.google_drive_failed_authorization)
                try_again_btn.visibility = View.GONE
                cancel_btn.visibility = View.VISIBLE
            }
            else -> {
                // ignore
            }
        }
    }

}