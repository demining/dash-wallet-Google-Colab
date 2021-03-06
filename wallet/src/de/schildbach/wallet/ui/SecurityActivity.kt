/*
 * Copyright 2019 Dash Core Group
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

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.CompoundButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import de.schildbach.wallet.WalletApplication
import de.schildbach.wallet.ui.backup.BackupWalletDialogFragment
import de.schildbach.wallet.util.FingerprintHelper
import de.schildbach.wallet_test.R
import kotlinx.android.synthetic.main.activity_security.*
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.Wallet
import org.dash.wallet.common.BuildConfig
import org.dash.wallet.common.services.analytics.AnalyticsConstants
import org.dash.wallet.common.services.analytics.FirebaseAnalyticsServiceImpl

class SecurityActivity : BaseMenuActivity(), AbstractPINDialogFragment.WalletProvider {

    private lateinit var fingerprintHelper: FingerprintHelper
    private lateinit var checkPinSharedModel: CheckPinSharedModel
    private val analytics = FirebaseAnalyticsServiceImpl.getInstance()

    companion object {
        private const val AUTH_REQUEST_CODE_BACKUP = 1
        private const val ENABLE_FINGERPRINT_REQUEST_CODE = 2
        private const val FINGERPRINT_ENABLED_REQUEST_CODE = 3
        private const val AUTH_REQUEST_CODE_VIEW_RECOVERYPHRASE = 4
        private const val AUTH_REQUEST_CODE_ADVANCED_SECURITY = 5
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_security
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.security_title)
        val hideBalanceOnLaunch = findViewById<SwitchCompat>(R.id.hide_balance_switch)
        hideBalanceOnLaunch.isChecked = configuration.hideBalance
        hideBalanceOnLaunch.setOnCheckedChangeListener { _, hideBalanceOnLaunch ->
            configuration.hideBalance = hideBalanceOnLaunch
            analytics.logEvent(
                if (hideBalanceOnLaunch) {
                    AnalyticsConstants.Security.AUTOHIDE_BALANCE_ON
                } else {
                    AnalyticsConstants.Security.AUTOHIDE_BALANCE_OFF
                }, bundleOf()
            )
        }

        val checkPinSharedModel: CheckPinSharedModel = ViewModelProvider(this)[CheckPinSharedModel::class.java]
        checkPinSharedModel.onCorrectPinCallback.observe(this, Observer<Pair<Int?, String?>> { (requestCode, pin) ->
            when (requestCode) {
                AUTH_REQUEST_CODE_BACKUP -> {
                    BackupWalletDialogFragment.show(supportFragmentManager)
                    //BackupWalletActivity.start(this)
                }
                ENABLE_FINGERPRINT_REQUEST_CODE -> {
                    if (pin != null) {
                        EnableFingerprintDialog.show(pin, FINGERPRINT_ENABLED_REQUEST_CODE,
                                supportFragmentManager)
                        // TODO: move to FINGERPRINT_ENABLED_REQUEST_CODE case when the bug
                        // TODO: that's preventing it from getting called is resolved
                        analytics.logEvent(AnalyticsConstants.Security.FINGERPRINT_ON, bundleOf())
                    }
                }
                FINGERPRINT_ENABLED_REQUEST_CODE -> {
                    updateFingerprintSwitchSilently(fingerprintHelper.isFingerprintEnabled)
                    configuration.enableFingerprint = fingerprintHelper.isFingerprintEnabled
                }
                AUTH_REQUEST_CODE_ADVANCED_SECURITY -> {
                    analytics.logEvent(AnalyticsConstants.Security.ADVANCED_SECURITY, bundleOf())
                    startActivity(Intent(this, AdvancedSecurityActivity::class.java))
                }
            }
        })

        val decryptSeedSharedModel : DecryptSeedSharedModel = ViewModelProvider(this)[DecryptSeedSharedModel::class.java]
        decryptSeedSharedModel.onDecryptSeedCallback.observe(this) { (requestCode, seed) ->
            when (requestCode) {
                AUTH_REQUEST_CODE_VIEW_RECOVERYPHRASE -> {
                    startViewSeedActivity(seed)
                }
            }
        }

        //Fingerprint group and switch setup
        fingerprintHelper = FingerprintHelper(this)
        if (fingerprintHelper.init()) {
            fingerprint_auth_group.visibility = VISIBLE
            fingerprint_auth_switch.isChecked = fingerprintHelper.isFingerprintEnabled
            fingerprint_auth_switch.setOnCheckedChangeListener(fingerprintSwitchListener)
            configuration.enableFingerprint = fingerprintHelper.isFingerprintEnabled
        } else {
            fingerprint_auth_group.visibility = GONE
        }

        if (BuildConfig.DEBUG) {
            backup_wallet.visibility = VISIBLE
        }
    }

    private val fingerprintSwitchListener= CompoundButton.OnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            CheckPinDialog.show(this, ENABLE_FINGERPRINT_REQUEST_CODE)
            updateFingerprintSwitchSilently(false)
        } else {
            analytics.logEvent(AnalyticsConstants.Security.FINGERPRINT_OFF, bundleOf())
            fingerprintHelper.clear()
            configuration.enableFingerprint = false
        }
    }

    private fun updateFingerprintSwitchSilently(checked: Boolean) {
        fingerprint_auth_switch.setOnCheckedChangeListener(null)
        fingerprint_auth_switch.isChecked = checked
        fingerprint_auth_switch.setOnCheckedChangeListener(fingerprintSwitchListener)
    }

    fun backupWallet(view: View) {
        CheckPinDialog.show(this, AUTH_REQUEST_CODE_BACKUP, true)
    }

    fun viewRecoveryPhrase(view: View) {
        DecryptSeedWithPinDialog.show(this, AUTH_REQUEST_CODE_VIEW_RECOVERYPHRASE, true)
    }

    fun changePin(view: View) {
        analytics.logEvent(AnalyticsConstants.Security.CHANGE_PIN, bundleOf())
        startActivity(SetPinActivity.createIntent(this, R.string.wallet_options_encrypt_keys_change, true))
    }

    fun openAdvancedSecurity(view: View) {
        CheckPinDialog.show(this, AUTH_REQUEST_CODE_ADVANCED_SECURITY, true)
    }

    fun resetWallet(view: View) {
        ResetWalletDialog.newInstance().show(supportFragmentManager, "reset_wallet_dialog")
    }

    // required by UnlockWalletDialogFragment
    override fun onWalletUpgradeComplete(password: String?) {

    }

    override fun getWallet(): Wallet {
        return WalletApplication.getInstance().wallet
    }

    private fun startViewSeedActivity(seed : DeterministicSeed?) {
        analytics.logEvent(AnalyticsConstants.Security.VIEW_RECOVERY_PHRASE, bundleOf())
        val mnemonicCode = seed!!.mnemonicCode
        val seedArray = mnemonicCode!!.toTypedArray()
        val intent = ViewSeedActivity.createIntent(this, seedArray)
        startActivity(intent)
    }
}
