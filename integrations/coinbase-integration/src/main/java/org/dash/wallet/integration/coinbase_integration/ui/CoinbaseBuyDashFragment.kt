/*
 * Copyright 2021 Dash Core Group.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.dash.wallet.integration.coinbase_integration.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.bitcoinj.core.Coin
import org.dash.wallet.common.ui.enter_amount.EnterAmountFragment
import org.dash.wallet.common.ui.enter_amount.EnterAmountViewModel
import org.dash.wallet.common.ui.viewBinding
import org.dash.wallet.common.util.GenericUtils
import org.dash.wallet.common.util.safeNavigate
import org.dash.wallet.integration.coinbase_integration.R
import org.dash.wallet.integration.coinbase_integration.databinding.FragmentCoinbaseBuyDashBinding
import org.dash.wallet.integration.coinbase_integration.databinding.KeyboardHeaderViewBinding
import org.dash.wallet.integration.coinbase_integration.model.CoinbasePaymentMethod
import org.dash.wallet.integration.coinbase_integration.model.ReviewBuyOrderModel
import org.dash.wallet.integration.coinbase_integration.viewmodels.CoinbaseBuyDashViewModel
import org.dash.wallet.integration.coinbase_integration.viewmodels.CoinbaseServicesViewModel

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class CoinbaseBuyDashFragment: Fragment(R.layout.fragment_coinbase_buy_dash) {
    private val binding by viewBinding(FragmentCoinbaseBuyDashBinding::bind)
        private val viewModel by viewModels<CoinbaseBuyDashViewModel>()
    private val amountViewModel by activityViewModels<EnterAmountViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            val fragment = EnterAmountFragment.newInstance(
                isMaxButtonVisible = false
            )
            val headerBinding = KeyboardHeaderViewBinding.inflate(layoutInflater, null, false)
            fragment.setViewDetails(getString(R.string.button_continue), headerBinding.root)

            parentFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.enter_amount_fragment_placeholder, fragment)
            }
        }

        setupPaymentMethodPayment()
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        amountViewModel.selectedExchangeRate.observe(viewLifecycleOwner) { rate ->
            binding.toolbarSubtitle.text = getString(
                R.string.exchange_rate_template,
                Coin.COIN.toPlainString(),
                GenericUtils.fiatToString(rate.fiat)
            )
        }
        amountViewModel.onContinueEvent.observe(viewLifecycleOwner) { pair ->

            //viewModel.onContinueClicked(pair.first,pair.second)
            Log.i("COINBASELOG", "fiat: ${pair.second}, coin: ${pair.first} ${ binding.paymentMethodPicker.paymentMethods[binding.paymentMethodPicker.selectedMethodIndex]}")
           // safeNavigate(CoinbaseBuyDashFragmentDirections.buyDashToOrderReview(ReviewBuyOrderModel()))
        }
    }

    private fun setupPaymentMethodPayment() {
        arguments?.let {
            val coinbasePaymentMethods = CoinbaseBuyDashFragmentArgs.fromBundle(it).paymentMethods
            val paymentMethods = coinbasePaymentMethods
                .filter { it.allowBuy }
                .map {
                    val type = paymentMethodTypeFromCoinbaseType(it.type)
                    val nameAccountPair = splitNameAndAccount(it.name)
                    PaymentMethod(
                        nameAccountPair.first,
                        nameAccountPair.second,
                        "", // set "Checking" to get "****1234 • Checking" in subtitle
                        paymentMethodType = type
                    )
                }
            binding.paymentMethodPicker.paymentMethods = paymentMethods
        }

    }

    private fun splitNameAndAccount(nameAccount: String?): Pair<String, String> {
        nameAccount?.let {
            val match = "(\\d+)?\\s?[a-z]?\\*+".toRegex().find(nameAccount)
            match?.range?.first?.let { index ->
                val name = nameAccount.substring(0, index).trim(' ', '-', ',', ':')
                val account = nameAccount.substring(index, nameAccount.length).trim()
                return Pair(name, account)
            }
        }

        return Pair("", "")
    }

    private fun paymentMethodTypeFromCoinbaseType(type: String): PaymentMethodType {
        return when (type) {
            "fiat_account" -> PaymentMethodType.Fiat
            "secure3d_card", "worldpay_card", "credit_card", "debit_card" -> PaymentMethodType.Card
            "ach_bank_account", "sepa_bank_account",
            "ideal_bank_account", "eft_bank_account", "interac" -> PaymentMethodType.BankAccount
            "bank_wire" -> PaymentMethodType.WireTransfer
            "paypal_account" -> PaymentMethodType.PayPal
            else -> PaymentMethodType.Unknown
        viewModel.getPaymentMethods()
        viewModel.activePaymentMethods.observe(viewLifecycleOwner){
            binding.paymentMethodPicker.paymentMethods = it
        }
    }
}