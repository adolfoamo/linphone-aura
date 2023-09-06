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
package org.linphone.ui.assistant.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.R
import org.linphone.core.tools.Log
import org.linphone.databinding.AssistantLoginFragmentBinding
import org.linphone.ui.assistant.AssistantActivity
import org.linphone.ui.assistant.viewmodel.AccountLoginViewModel
import org.linphone.ui.main.fragment.GenericFragment
import org.linphone.utils.PhoneNumberUtils

@UiThread
class LoginFragment : GenericFragment() {
    companion object {
        private const val TAG = "[Login Fragment]"
    }

    private lateinit var binding: AssistantLoginFragmentBinding

    private val viewModel: AccountLoginViewModel by navGraphViewModels(
        R.id.loginFragment
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AssistantLoginFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun goBack() {
        requireActivity().finish()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.setBackClickListener {
            goBack()
        }

        binding.setRegisterClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            findNavController().navigate(action)
        }

        binding.setForgottenPasswordClickListener {
            try {
                val url = "https://subscribe.linphone.org"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            } catch (ise: IllegalStateException) {
                Log.e("$TAG Can't start ACTION_VIEW intent, IllegalStateException: $ise")
            }
        }

        binding.setQrCodeClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToQrCodeScannerFragment()
            findNavController().navigate(action)
        }

        binding.setThirdPartySipAccountLoginClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToThirdPartySipAccountWarningFragment()
            findNavController().navigate(action)
        }

        viewModel.showPassword.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                delay(50)
                binding.password.setSelection(binding.password.text?.length ?: 0)
            }
        }

        viewModel.accountLoggedInEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG Account successfully logged-in, leaving assistant")
                goBack()
            }
        }

        viewModel.accountLoginErrorEvent.observe(viewLifecycleOwner) {
            it.consume { message ->
                Log.e("$TAG Failed to log in account [$message]")
                // TODO FIXME: don't use message from callback
                (requireActivity() as AssistantActivity).showRedToast(
                    message,
                    R.drawable.warning_circle
                )
            }
        }

        coreContext.postOnCoreThread {
            val prefix = PhoneNumberUtils.getDeviceInternationalPrefix(requireContext())
            viewModel.internationalPrefix.postValue(prefix)
        }
    }

    override fun onResume() {
        super.onResume()

        // In case we come back from QrCodeScannerFragment
        val white = ContextCompat.getColor(
            requireContext(),
            R.color.white
        )
        requireActivity().window.navigationBarColor = white
    }
}
