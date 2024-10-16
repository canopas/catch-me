package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.JourneyRoute
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.utils.Config
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiJourneyService @Inject constructor(
    db: FirebaseFirestore
) {
    private val userRef = db.collection(Config.FIRESTORE_COLLECTION_USERS)

    // App crashes sometimes because of the empty userId string passed to document().
    // java.lang.IllegalArgumentException: Invalid document reference.
    // Document references must have an even number of segments, but users has 1
    // https://stackoverflow.com/a/51195713/22508023 [Explanation can be found in comments]
    private fun journeyRef(userId: String) =
        userRef.document(userId.takeIf { it.isNotBlank() } ?: "null")
            .collection(Config.FIRESTORE_COLLECTION_USER_JOURNEYS)

    suspend fun saveCurrentJourney(
        userId: String,
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double? = null,
        toLongitude: Double? = null,
        routeDistance: Double? = null,
        routeDuration: Long? = null,
        routes: List<JourneyRoute> = emptyList(),
        createdAt: Long? = null,
        updateAt: Long? = null
    ): String {
        val docRef = journeyRef(userId).document()

        val journey = LocationJourney(
            id = docRef.id,
            user_id = userId,
            from_latitude = fromLatitude,
            from_longitude = fromLongitude,
            to_latitude = toLatitude,
            to_longitude = toLongitude,
            route_distance = routeDistance,
            route_duration = routeDuration,
            routes = routes,
            created_at = createdAt ?: System.currentTimeMillis(),
            update_at = updateAt ?: System.currentTimeMillis()
        )

        docRef.set(journey).await()

        return docRef.id
    }

    suspend fun updateLastLocationJourney(userId: String, journey: LocationJourney) {
        try {
            journeyRef(userId).document(journey.id).set(journey).await()
        } catch (e: Exception) {
            Timber.e(e, "Error while updating last location journey")
        }
    }

    suspend fun getLastJourneyLocation(userId: String) = try {
        journeyRef(userId).whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
            .get().await().documents.firstOrNull()?.toObject(LocationJourney::class.java)
    } catch (e: Exception) {
        Timber.e(e, "Error while getting last location journey")
        null
    }

    suspend fun getMoreJourneyHistory(
        userId: String,
        from: Long?
    ): List<LocationJourney> {
        if (from == null) {
            return journeyRef(userId).whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(20)
                .get().await().documents.mapNotNull { it.toObject<LocationJourney>() }
        }
        return journeyRef(userId).whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .whereLessThan("created_at", from)
            .limit(20)
            .get().await().documents.mapNotNull { it.toObject<LocationJourney>() }
    }

    suspend fun getJourneyHistory(
        userId: String,
        from: Long? = null,
        to: Long? = null
    ): List<LocationJourney> {
        if (from == null) {
            return journeyRef(userId).whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(20)
                .get().await().documents.mapNotNull { it.toObject<LocationJourney>() }
        } else if (to == null) {
            return journeyRef(userId).whereEqualTo("user_id", userId)
                .whereGreaterThanOrEqualTo("created_at", from)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(20)
                .get().await().documents.mapNotNull { it.toObject<LocationJourney>() }
        } else {
            return journeyRef(userId).whereEqualTo("user_id", userId)
                .whereGreaterThanOrEqualTo("created_at", from)
                .whereLessThanOrEqualTo("created_at", to)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(20)
                .get().await().documents.mapNotNull { it.toObject<LocationJourney>() }
        }
    }

    suspend fun getLocationJourneyFromId(userId: String, journeyId: String): LocationJourney? {
        return journeyRef(userId).document(journeyId).get().await()
            .toObject(LocationJourney::class.java)
    }
}
