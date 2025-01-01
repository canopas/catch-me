package com.canopas.yourspace.data.storage.bufferedkeystore

import com.canopas.yourspace.data.models.user.ApiSenderKeyRecord
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.storage.database.SenderKeyDao
import com.canopas.yourspace.data.storage.database.SenderKeyEntity
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_USER_SENDER_KEY_RECORD
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * An in-memory sender key store with persistent and server backup.
 */
@Singleton
class BufferedSenderKeyStore @Inject constructor(
    db: FirebaseFirestore,
    @Named("sender_key_dao") private val senderKeyDao: SenderKeyDao,
    private val userPreferences: UserPreferences
) : SignalServiceSenderKeyStore {

    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)

    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId.ifBlank { "null" })
            .collection(FIRESTORE_COLLECTION_SPACE_MEMBERS)

    private fun spaceSenderKeyRecordRef(spaceId: String, userId: String) =
        spaceMemberRef(spaceId)
            .document(userId.ifBlank { "null" })
            .collection(FIRESTORE_COLLECTION_USER_SENDER_KEY_RECORD)

    private val inMemoryStore: MutableMap<StoreKey, SenderKeyRecord> = mutableMapOf()
    private val sharedWithAddresses: MutableSet<SignalProtocolAddress> = mutableSetOf()

    private suspend fun saveSenderKeyToServer(senderKeyRecord: ApiSenderKeyRecord) {
        val currentUser = userPreferences.currentUser ?: return
        spaceSenderKeyRecordRef(senderKeyRecord.distributionId, currentUser.id)
            .document(senderKeyRecord.distributionId).set(senderKeyRecord).await()
    }

    override fun storeSenderKey(
        sender: SignalProtocolAddress,
        distributionId: UUID,
        record: SenderKeyRecord
    ) {
        val key = StoreKey(sender, distributionId)
        if (inMemoryStore.containsKey(key)) {
            Timber.d("Sender key already exists for $sender and $distributionId")
            return
        }
        inMemoryStore[key] = record

        runBlocking {
            senderKeyDao.insertSenderKey(
                SenderKeyEntity(
                    address = sender.name,
                    deviceId = sender.deviceId,
                    distributionId = distributionId.toString(),
                    record = record.serialize()
                )
            )

            val senderKeyRecord = ApiSenderKeyRecord(
                address = sender.name,
                deviceId = sender.deviceId,
                distributionId = distributionId.toString(),
                record = Blob.fromBytes(record.serialize())
            )
            saveSenderKeyToServer(senderKeyRecord)
        }
    }

    override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: UUID): SenderKeyRecord? {
        val key = StoreKey(sender, distributionId)
        return inMemoryStore[key] ?: runBlocking {
            senderKeyDao.getSenderKeyRecord(
                address = sender.name,
                deviceId = sender.deviceId,
                distributionId = distributionId.toString()
            )?.let {
                inMemoryStore[key] = it
                it
            } ?: fetchSenderKeyFromServer(sender)?.also {
                inMemoryStore[key] = it
            }
        }
    }

    private suspend fun fetchSenderKeyFromServer(sender: SignalProtocolAddress): SenderKeyRecord? {
        val currentUser = userPreferences.currentUser ?: return null
        return spaceSenderKeyRecordRef(sender.name.toString(), currentUser.id)
            .document(sender.name.toString()).get().await().toObject(ApiSenderKeyRecord::class.java)?.let {
                try {
                    SenderKeyRecord(it.record.toBytes())
                } catch (e: Exception) {
                    Timber.e(e, "Failed to deserialize sender key record")
                    null
                }
            }
    }

    override fun getSenderKeySharedWith(distributionId: DistributionId?): MutableSet<SignalProtocolAddress> {
        throw UnsupportedOperationException("Should not happen during the intended usage pattern of this class")
    }

    override fun markSenderKeySharedWith(
        distributionId: DistributionId?,
        addresses: Collection<SignalProtocolAddress?>?
    ) {
        throw UnsupportedOperationException("Should not happen during the intended usage pattern of this class")
    }

    override fun clearSenderKeySharedWith(addresses: Collection<SignalProtocolAddress?>?) {
        addresses?.forEach { address ->
            address?.let { sharedWithAddresses.add(it) }
        }
    }

    data class StoreKey(val address: SignalProtocolAddress, val distributionId: UUID)
}