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
package org.linphone.ui.main.viewmodel

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.R
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.RegistrationState
import org.linphone.core.tools.Log
import org.linphone.utils.AppUtils
import org.linphone.utils.Event
import org.linphone.utils.LinphoneUtils

class MainViewModel @UiThread constructor() : ViewModel() {
    companion object {
        private const val TAG = "[Main ViewModel]"

        const val NONE = 0
        const val NON_DEFAULT_ACCOUNT_NOTIFICATIONS = 5
        const val NON_DEFAULT_ACCOUNT_NOT_CONNECTED = 10
        const val NETWORK_NOT_REACHABLE = 19
        const val SINGLE_CALL = 20
        const val MULTIPLE_CALLS = 21
    }

    val showAlert = MutableLiveData<Boolean>()

    val alertLabel = MutableLiveData<String>()

    val alertIcon = MutableLiveData<Int>()

    val atLeastOneCall = MutableLiveData<Boolean>()

    val callsStatus = MutableLiveData<String>()

    val defaultAccountRegistrationErrorEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val changeSystemTopBarColorEvent: MutableLiveData<Event<Int>> by lazy {
        MutableLiveData<Event<Int>>()
    }

    val goBackToCallEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val openDrawerEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    private var defaultAccountRegistrationFailed = false

    private val alertsList = arrayListOf<Pair<Int, String>>()

    private var alertJob: Job? = null

    private val coreListener = object : CoreListenerStub() {
        @WorkerThread
        override fun onLastCallEnded(core: Core) {
            Log.i("$TAG Last call ended, removing in-call 'alert'")
            removeAlert(SINGLE_CALL)
            atLeastOneCall.postValue(false)
        }

        override fun onFirstCallStarted(core: Core) {
            Log.i("$TAG First call started, adding in-call 'alert'")
            updateCallAlert()
            atLeastOneCall.postValue(true)
        }

        @WorkerThread
        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
            if (
                core.callsNb > 1 && (
                    LinphoneUtils.isCallEnding(call.state) ||
                        LinphoneUtils.isCallIncoming(call.state) ||
                        LinphoneUtils.isCallOutgoing(call.state)
                    )
            ) {
                updateCallAlert()
            } else if (core.callsNb == 1) {
                callsStatus.postValue(LinphoneUtils.callStateToString(call.state))
            }
        }

        @WorkerThread
        override fun onNetworkReachable(core: Core, reachable: Boolean) {
            Log.i("$TAG Network is ${if (reachable) "reachable" else "not reachable"}")
            if (!reachable) {
                val label = AppUtils.getString(R.string.network_not_reachable)
                addAlert(NETWORK_NOT_REACHABLE, label)
            } else {
                removeAlert(NETWORK_NOT_REACHABLE)
            }
        }

        @WorkerThread
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            when (state) {
                RegistrationState.Failed -> {
                    if (account == core.defaultAccount) {
                        Log.e("$TAG Default account registration failed!")
                        defaultAccountRegistrationFailed = true
                        defaultAccountRegistrationErrorEvent.postValue(Event(true))
                    } else if (core.isNetworkReachable) {
                        Log.e("$TAG Non-default account registration failed!")
                        val label = AppUtils.getString(
                            R.string.connection_error_for_non_default_account
                        )
                        addAlert(SINGLE_CALL, label)
                    }
                }
                RegistrationState.Ok -> {
                    if (account == core.defaultAccount && defaultAccountRegistrationFailed) {
                        Log.i("$TAG Default account is now registered")
                        defaultAccountRegistrationFailed = false
                        defaultAccountRegistrationErrorEvent.postValue(Event(false))
                    } else {
                        // If no call and no account is in Failed state, hide top bar
                        val found = core.accountList.find {
                            it.state == RegistrationState.Failed
                        }
                        if (found == null) {
                            removeAlert(NON_DEFAULT_ACCOUNT_NOT_CONNECTED)
                        }
                    }
                }
                else -> {}
            }
        }
    }

    init {
        defaultAccountRegistrationFailed = false
        showAlert.value = false

        coreContext.postOnCoreThread { core ->
            core.addListener(coreListener)

            if (!core.isNetworkReachable) {
                Log.w("$TAG Network is not reachable!")
                val label = AppUtils.getString(R.string.network_not_reachable)
                addAlert(NETWORK_NOT_REACHABLE, label)
            }

            if (core.callsNb > 0) {
                updateCallAlert()
            }
            atLeastOneCall.postValue(core.callsNb > 0)
        }
    }

    @UiThread
    override fun onCleared() {
        super.onCleared()

        coreContext.postOnCoreThread { core ->
            core.removeListener(coreListener)
        }
    }

    @UiThread
    fun closeTopBar() {
        showAlert.value = false
    }

    @UiThread
    fun onTopBarClicked() {
        if (atLeastOneCall.value == true) {
            goBackToCallEvent.value = Event(true)
        } else {
            openDrawerEvent.value = Event(true)
        }
    }

    @WorkerThread
    private fun updateCallAlert() {
        val core = coreContext.core
        val callsNb = core.callsNb
        if (callsNb == 1) {
            removeAlert(MULTIPLE_CALLS)

            val currentCall = core.currentCall ?: core.calls.firstOrNull()
            if (currentCall != null) {
                val contact = coreContext.contactsManager.findContactByAddress(
                    currentCall.remoteAddress
                )
                val label = if (contact != null) {
                    contact.name ?: LinphoneUtils.getDisplayName(currentCall.remoteAddress)
                } else {
                    val conferenceInfo = coreContext.core.findConferenceInformationFromUri(
                        currentCall.remoteAddress
                    )
                    conferenceInfo?.subject ?: LinphoneUtils.getDisplayName(
                        currentCall.remoteAddress
                    )
                }
                addAlert(SINGLE_CALL, label)
                callsStatus.postValue(LinphoneUtils.callStateToString(currentCall.state))
            }
        } else if (callsNb > 1) {
            removeAlert(SINGLE_CALL)

            addAlert(
                MULTIPLE_CALLS,
                AppUtils.getFormattedString(R.string.calls_count_label, callsNb)
            )
            callsStatus.postValue("") // TODO: improve ?
        }
    }

    @WorkerThread
    private fun addAlert(type: Int, label: String) {
        val found = alertsList.find {
            it.first == type
        }
        if (found == null) {
            cancelAlertJob()
            val alert = Pair(type, label)
            alertsList.add(alert)
            updateDisplayedAlert()
        } else {
            Log.w("$TAG There is already an alert with type [$type], skipping...")
        }
    }

    @WorkerThread
    private fun removeAlert(type: Int) {
        val found = alertsList.find {
            it.first == type
        }
        if (found != null) {
            cancelAlertJob()
            alertsList.remove(found)
            updateDisplayedAlert()
        } else {
            Log.w("$TAG Failed to remove alert with type [$type], not found in current alerts list")
        }
    }

    @WorkerThread
    private fun cancelAlertJob() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                alertJob?.cancelAndJoin()
                alertJob = null
            }
        }
    }

    @WorkerThread
    private fun updateDisplayedAlert() {
        // Sort alerts by priority
        alertsList.sortByDescending {
            it.first
        }

        val maxedPriorityAlert = alertsList.firstOrNull()
        if (maxedPriorityAlert == null) {
            Log.i("$TAG No alert to display")
            showAlert.postValue(false)
            changeSystemTopBarColorEvent.postValue(Event(NONE))
        } else {
            val type = maxedPriorityAlert.first
            val label = maxedPriorityAlert.second
            Log.i("$TAG Max priority alert right now is [$type]")
            when (type) {
                NON_DEFAULT_ACCOUNT_NOTIFICATIONS, NON_DEFAULT_ACCOUNT_NOT_CONNECTED -> {
                    alertIcon.postValue(R.drawable.bell_simple)
                }
                NETWORK_NOT_REACHABLE -> {
                    alertIcon.postValue(R.drawable.wifi_slash)
                }
                SINGLE_CALL, MULTIPLE_CALLS -> {
                    alertIcon.postValue(R.drawable.phone)
                }
            }
            alertLabel.postValue(label)

            coreContext.postOnMainThread {
                val delayMs = if (type == SINGLE_CALL) 1000L else 0L
                alertJob = viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        delay(delayMs)
                        withContext(Dispatchers.Main) {
                            showAlert.value = true
                            changeSystemTopBarColorEvent.value = Event(type)
                        }
                    }
                }
            }
        }
    }
}
