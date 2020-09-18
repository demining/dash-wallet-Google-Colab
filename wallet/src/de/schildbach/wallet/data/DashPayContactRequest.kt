package de.schildbach.wallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.dashevo.dpp.document.Document
import org.dashevo.dpp.util.HashUtils

@Entity(tableName = "dashpay_contact_request")
data class DashPayContactRequest(@PrimaryKey val entropy: String,
                                 val userId: String,
                                 val toUserId: String,
                                 val privateData: ByteArray?,
                                 val encryptedPublicKey: ByteArray,
                                 val senderKeyIndex: Int,
                                 val recipientKeyIndex: Int,
                                 val timestamp: Double,
                                 val hidden: Boolean, // is the request from another user hidden (local)
                                 val dateAdded: Long  // when was this request created or accepted (local)
) {
    companion object {
        fun fromDocument(document: Document): DashPayContactRequest {
            val timestamp: Double = if (document.createdAt != null) document.createdAt!!.toDouble() else 0.00
            val privateData = if (document.data.containsKey("privateData"))
                HashUtils.byteArrayFromString(document.data["privateData"] as String)
            else null
            val entropy = document.ownerId + document.data["toUserId"] as String
            return DashPayContactRequest(entropy, document.ownerId, document.data["toUserId"] as String,
                    privateData,
                    HashUtils.byteArrayFromString(document.data["encryptedPublicKey"] as String),
                    document.data["senderKeyIndex"] as Int,
                    document.data["recipientKeyIndex"] as Int,
                    timestamp, false, 0)
        }
    }
}