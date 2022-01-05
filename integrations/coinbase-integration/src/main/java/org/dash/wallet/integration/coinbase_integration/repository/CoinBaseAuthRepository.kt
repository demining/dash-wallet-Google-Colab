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
package org.dash.wallet.integration.coinbase_integration.repository

import org.dash.wallet.common.Configuration
import org.dash.wallet.integration.coinbase_integration.network.safeApiCall
import org.dash.wallet.integration.coinbase_integration.service.CoinBaseAuthApi
import javax.inject.Inject

const val TEMP_ACCESS_TOKEN = "a20ccd1c8576c8e8026236e36f66c8f7fb294ce8854e004ff220dd063f5d6fe0"
const val TEMP_REFRESH_TOKEN = "ced721da2ae9a66dfa615a65fd583b91bef304733c72ec9b519f71c26b6de254"
class CoinBaseAuthRepository @Inject constructor(
    private val api: CoinBaseAuthApi,
    private val userPreferences: Configuration
) {
    suspend fun getUserToken(
        code: String
    ) = safeApiCall {
        api.getToken(code = code).also {
            it.body()?.let {
                //saveAccessTokens(it.accessToken, it.refreshToken)
                saveAccessTokens(TEMP_ACCESS_TOKEN, TEMP_REFRESH_TOKEN)   // Temp values for refresh & access tokens
            }
        }
    }

    private fun saveAccessTokens(accessToken: String, refreshToken: String) {
        userPreferences.setLastCoinBaseAccessToken(accessToken)
        userPreferences.setLastCoinBaseRefreshToken(refreshToken)
    }

    fun isUserConnected() = userPreferences.lastCoinbaseAccessToken.isNullOrEmpty().not()

    fun getUserLastCoinbaseBalance(): String? = userPreferences.lastCoinbaseBalance
}