/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.utils

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import org.linphone.R
import org.linphone.databinding.DialogAccountModesExplanationBinding
import org.linphone.databinding.DialogAssistantAcceptConditionsAndPolicyBinding
import org.linphone.databinding.DialogAssistantCreateAccountConfirmPhoneNumberBinding
import org.linphone.databinding.DialogCancelContactChangesBinding
import org.linphone.databinding.DialogConfirmZrtpSasBinding
import org.linphone.databinding.DialogContactConfirmTrustCallBinding
import org.linphone.databinding.DialogContactTrustProcessBinding
import org.linphone.databinding.DialogDeleteContactBinding
import org.linphone.databinding.DialogEditGroupConversationSubjectBinding
import org.linphone.databinding.DialogManageAccountInternationalPrefixHelpBinding
import org.linphone.databinding.DialogPickNumberOrAddressBinding
import org.linphone.databinding.DialogRemoveAccountBinding
import org.linphone.databinding.DialogRemoveAllCallLogsBinding
import org.linphone.databinding.DialogRemoveCallLogsBinding
import org.linphone.databinding.DialogUpdateAvailableBinding
import org.linphone.ui.assistant.model.AcceptConditionsAndPolicyDialogModel
import org.linphone.ui.assistant.model.ConfirmPhoneNumberDialogModel
import org.linphone.ui.call.model.ZrtpSasConfirmationDialogModel
import org.linphone.ui.main.chat.model.ConversationEditSubjectDialogModel
import org.linphone.ui.main.contacts.model.NumberOrAddressPickerDialogModel
import org.linphone.ui.main.contacts.model.TrustCallDialogModel
import org.linphone.ui.main.history.model.ConfirmationDialogModel

class DialogUtils {
    companion object {
        @UiThread
        fun getAcceptConditionsAndPrivacyDialog(
            context: Context,
            viewModel: AcceptConditionsAndPolicyDialogModel
        ): Dialog {
            val binding: DialogAssistantAcceptConditionsAndPolicyBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_assistant_accept_conditions_and_policy,
                null,
                false
            )
            binding.viewModel = viewModel
            binding.message.movementMethod = LinkMovementMethod.getInstance()

            return getDialog(context, binding)
        }

        @UiThread
        fun getAccountCreationPhoneNumberConfirmationDialog(
            context: Context,
            viewModel: ConfirmPhoneNumberDialogModel
        ): Dialog {
            val binding: DialogAssistantCreateAccountConfirmPhoneNumberBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_assistant_create_account_confirm_phone_number,
                null,
                false
            )
            binding.viewModel = viewModel

            return getDialog(context, binding)
        }

        @UiThread
        fun getAccountInternationalPrefixHelpDialog(context: Context): Dialog {
            val binding: DialogManageAccountInternationalPrefixHelpBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_manage_account_international_prefix_help,
                null,
                false
            )
            val dialog = getDialog(context, binding)

            binding.setDismissClickListener {
                dialog.dismiss()
            }

            return dialog
        }

        @UiThread
        fun getAccountModeExplanationDialog(
            context: Context,
            defaultMode: Boolean
        ): Dialog {
            lateinit var dialog: Dialog

            val binding: DialogAccountModesExplanationBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_account_modes_explanation,
                null,
                false
            )
            binding.defaultMode = defaultMode
            binding.setDismissClickListener {
                dialog.dismiss()
            }

            dialog = getDialog(context, binding)
            return dialog
        }

        @UiThread
        fun getConfirmAccountRemovalDialog(
            context: Context,
            viewModel: ConfirmationDialogModel,
            displayName: String
        ): Dialog {
            val binding: DialogRemoveAccountBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_remove_account,
                null,
                false
            )
            binding.viewModel = viewModel
            binding.title.text = context.getString(
                R.string.dialog_remove_account_title,
                displayName
            )

            return getDialog(context, binding)
        }

        @UiThread
        fun getNumberOrAddressPickerDialog(
            context: Context,
            viewModel: NumberOrAddressPickerDialogModel
        ): Dialog {
            val binding: DialogPickNumberOrAddressBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_pick_number_or_address,
                null,
                false
            )
            binding.viewModel = viewModel

            return getDialog(context, binding)
        }

        @UiThread
        fun getContactTrustCallConfirmationDialog(
            context: Context,
            viewModel: TrustCallDialogModel
        ): Dialog {
            val binding: DialogContactConfirmTrustCallBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_contact_confirm_trust_call,
                null,
                false
            )
            binding.viewModel = viewModel

            return getDialog(context, binding)
        }

        @UiThread
        fun getContactTrustProcessExplanationDialog(context: Context): Dialog {
            val binding: DialogContactTrustProcessBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_contact_trust_process,
                null,
                false
            )
            val dialog = getDialog(context, binding)

            binding.setDismissClickListener {
                dialog.dismiss()
            }

            return dialog
        }

        @UiThread
        fun getDeleteContactConfirmationDialog(
            context: Context,
            viewModel: ConfirmationDialogModel,
            contactName: String
        ): Dialog {
            val binding: DialogDeleteContactBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_delete_contact,
                null,
                false
            )
            binding.viewModel = viewModel
            binding.title.text = context.getString(
                R.string.dialog_contact_delete_title,
                contactName
            )

            return getDialog(context, binding)
        }

        @UiThread
        fun getRemoveCallLogsConfirmationDialog(
            context: Context,
            viewModel: ConfirmationDialogModel
        ): Dialog {
            val binding: DialogRemoveCallLogsBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_remove_call_logs,
                null,
                false
            )
            binding.viewModel = viewModel

            return getDialog(context, binding)
        }

        @UiThread
        fun getRemoveAllCallLogsConfirmationDialog(
            context: Context,
            viewModel: ConfirmationDialogModel
        ): Dialog {
            val binding: DialogRemoveAllCallLogsBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_remove_all_call_logs,
                null,
                false
            )
            binding.viewModel = viewModel

            return getDialog(context, binding)
        }

        @UiThread
        fun getCancelContactChangesConfirmationDialog(
            context: Context,
            viewModel: ConfirmationDialogModel
        ): Dialog {
            val binding: DialogCancelContactChangesBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_cancel_contact_changes,
                null,
                false
            )
            binding.viewModel = viewModel

            return getDialog(context, binding)
        }

        @UiThread
        fun getEditConversationSubjectDialog(
            context: Context,
            viewModel: ConversationEditSubjectDialogModel
        ): Dialog {
            val binding: DialogEditGroupConversationSubjectBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_edit_group_conversation_subject,
                null,
                false
            )
            binding.viewModel = viewModel

            return getDialog(context, binding)
        }

        @UiThread
        fun getUpdateAvailableDialog(
            context: Context,
            viewModel: ConfirmationDialogModel,
            message: String
        ): Dialog {
            val binding: DialogUpdateAvailableBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_update_available,
                null,
                false
            )
            binding.viewModel = viewModel
            binding.message.text = message

            return getDialog(context, binding)
        }

        @UiThread
        fun getZrtpSasConfirmationDialog(
            context: Context,
            viewModel: ZrtpSasConfirmationDialogModel
        ): Dialog {
            val binding: DialogConfirmZrtpSasBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_confirm_zrtp_sas,
                null,
                false
            )
            binding.viewModel = viewModel

            return getDialog(context, binding)
        }

        @UiThread
        private fun getDialog(context: Context, binding: ViewDataBinding): Dialog {
            val dialog = Dialog(context, R.style.Theme_LinphoneDialog)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(binding.root)

            dialog.window
                ?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
            val d: Drawable = ColorDrawable(
                AppUtils.getColor(R.color.gray_main2_800)
            )
            d.alpha = 102
            dialog.window?.setBackgroundDrawable(d)
            return dialog
        }
    }
}
