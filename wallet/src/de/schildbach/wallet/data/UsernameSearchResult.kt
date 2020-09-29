package de.schildbach.wallet.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UsernameSearchResult(val username: String,
                                val dashPayProfile: DashPayProfile,
                                var toContactRequest: DashPayContactRequest?,
                                var fromContactRequest: DashPayContactRequest?) : Parcelable {
    val requestSent: Boolean
        get() = toContactRequest != null
    val requestReceived: Boolean
        get() = fromContactRequest != null
    val isPendingRequest: Boolean
        get() = requestReceived && !requestSent

    val date: Long // milliseconds
        get() {
            return when (type) {
                Type.CONTACT_ESTABLISHED -> {
                    fromContactRequest!!.timestamp
                }
                Type.REQUEST_SENT -> {
                    toContactRequest!!.timestamp
                }
                Type.REQUEST_RECEIVED -> {
                    fromContactRequest!!.timestamp
                }
                Type.NO_RELATIONSHIP -> {
                    0.0
                }

            }.toLong()
        }

    val type: Type
        get() = when (requestSent to requestReceived) {
            false to false -> Type.NO_RELATIONSHIP
            true to true -> Type.CONTACT_ESTABLISHED
            false to true -> Type.REQUEST_RECEIVED
            true to false -> Type.REQUEST_SENT
            else -> throw IllegalStateException()
        }

    enum class Type {
        NO_RELATIONSHIP,
        REQUEST_SENT,
        REQUEST_RECEIVED,
        CONTACT_ESTABLISHED
    }
}