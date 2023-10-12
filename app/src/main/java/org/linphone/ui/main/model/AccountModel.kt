/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
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
package org.linphone.ui.main.model

import android.view.View
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.R
import org.linphone.core.Account
import org.linphone.core.AccountListenerStub
import org.linphone.core.ChatMessage
import org.linphone.core.ChatRoom
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.RegistrationState
import org.linphone.core.tools.Log
import org.linphone.utils.AppUtils
import org.linphone.utils.LinphoneUtils

class AccountModel @WorkerThread constructor(
    val account: Account,
    private val onMenuClicked: ((view: View, account: Account) -> Unit)? = null,
    private val onSetAsDefault: ((account: Account) -> Unit)? = null
) {
    companion object {
        private const val TAG = "[Account Model]"
    }

    val displayName = MutableLiveData<String>()

    val avatar = MutableLiveData<String>()

    val initials = MutableLiveData<String>()

    val registrationState = MutableLiveData<RegistrationState>()

    val registrationStateLabel = MutableLiveData<String>()

    val registrationStateSummary = MutableLiveData<String>()

    val isDefault = MutableLiveData<Boolean>()

    val showTrust = MutableLiveData<Boolean>()

    val notificationsCount = MutableLiveData<Int>()

    private val accountListener = object : AccountListenerStub() {
        @WorkerThread
        override fun onRegistrationStateChanged(
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            if (account == this@AccountModel.account) {
                Log.i(
                    "$TAG Account [${account.params.identityAddress?.asStringUriOnly()}] registration state changed: [$state]($message)"
                )
                update()
            }
        }
    }

    private val coreListener = object : CoreListenerStub() {
        @WorkerThread
        override fun onChatRoomRead(core: Core, chatRoom: ChatRoom) {
            computeNotificationsCount()
        }

        @WorkerThread
        override fun onMessagesReceived(
            core: Core,
            chatRoom: ChatRoom,
            messages: Array<out ChatMessage>
        ) {
            computeNotificationsCount()
        }
    }

    init {
        account.addListener(accountListener)
        coreContext.core.addListener(coreListener)

        showTrust.postValue(account.isInSecureMode())

        update()
    }

    @WorkerThread
    fun destroy() {
        coreContext.core.removeListener(coreListener)
        account.removeListener(accountListener)
    }

    @UiThread
    fun setAsDefault() {
        coreContext.postOnCoreThread { core ->
            core.defaultAccount = account

            for (friendList in core.friendsLists) {
                if (friendList.isSubscriptionsEnabled) {
                    Log.i(
                        "$TAG Default account has changed, refreshing friend list [${friendList.displayName}] subscriptions"
                    )
                    // friendList.updateSubscriptions() won't trigger a refresh unless a friend has changed
                    friendList.isSubscriptionsEnabled = false
                    friendList.isSubscriptionsEnabled = true
                }
            }
        }

        isDefault.value = true
        onSetAsDefault?.invoke(account)
    }

    @UiThread
    fun openMenu(view: View) {
        onMenuClicked?.invoke(view, account)
    }

    @UiThread
    fun refreshRegister() {
        coreContext.postOnCoreThread { core ->
            core.refreshRegisters()
        }
    }

    @WorkerThread
    private fun update() {
        Log.i(
            "$TAG Refreshing info for account [${account.params.identityAddress?.asStringUriOnly()}]"
        )

        val name = LinphoneUtils.getDisplayName(account.params.identityAddress)
        displayName.postValue(name)

        initials.postValue(AppUtils.getInitials(name))

        val pictureUri = account.params.pictureUri.orEmpty()
        if (pictureUri != avatar.value) {
            avatar.postValue(pictureUri)
            Log.d("$TAG Account picture URI is [$pictureUri]")
        }

        isDefault.postValue(coreContext.core.defaultAccount == account)
        computeNotificationsCount()

        val state = account.state
        registrationState.postValue(state)

        val label = when (state) {
            RegistrationState.None, RegistrationState.Cleared -> {
                AppUtils.getString(
                    R.string.drawer_menu_account_connection_status_cleared
                )
            }
            RegistrationState.Progress -> AppUtils.getString(
                R.string.drawer_menu_account_connection_status_progress
            )
            RegistrationState.Failed -> {
                AppUtils.getString(
                    R.string.drawer_menu_account_connection_status_failed
                )
            }
            RegistrationState.Ok -> {
                AppUtils.getString(
                    R.string.drawer_menu_account_connection_status_connected
                )
            }
            RegistrationState.Refreshing -> AppUtils.getString(
                R.string.drawer_menu_account_connection_status_refreshing
            )
            else -> "${account.state}"
        }
        registrationStateLabel.postValue(label)

        val summary = when (account.state) {
            RegistrationState.None, RegistrationState.Cleared -> AppUtils.getString(
                R.string.manage_account_status_cleared_summary
            )
            RegistrationState.Refreshing, RegistrationState.Progress -> AppUtils.getString(
                R.string.manage_account_status_progress_summary
            )
            RegistrationState.Failed -> AppUtils.getString(
                R.string.manage_account_status_failed_summary
            )
            RegistrationState.Ok -> AppUtils.getString(
                R.string.manage_account_status_connected_summary
            )
            else -> "${account.state}"
        }
        registrationStateSummary.postValue(summary)
    }

    @WorkerThread
    private fun computeNotificationsCount() {
        notificationsCount.postValue(account.unreadChatMessageCount + account.missedCallsCount)
    }
}

fun Account.isInSecureMode(): Boolean {
    // TODO FIXME: use real API when available
    return params.identityAddress?.domain == "sip.linphone.org"
}
