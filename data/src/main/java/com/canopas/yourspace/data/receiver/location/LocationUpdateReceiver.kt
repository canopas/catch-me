package com.canopas.yourspace.data.receiver.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

const val ACTION_LOCATION_UPDATE = "action.LOCATION_UPDATE"
const val MINIMUM_DISTANCE_TO_UPDATE_LOCATION = 10

@AndroidEntryPoint
class LocationUpdateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var locationService: ApiLocationService

    @Inject
    lateinit var authService: AuthService
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        LocationResult.extractResult(intent)?.let { locationResult ->
            scope.launch {
                try {
                    locationResult.locations.map { extractedLocation ->
                        locationService.getLastLocation(authService.currentUser?.id ?: "")
                            .let { lastLocation ->
                                if (lastLocation != null) {
                                    val distance = FloatArray(1)
                                    Location.distanceBetween(
                                        lastLocation.latitude,
                                        lastLocation.longitude,
                                        extractedLocation.latitude,
                                        extractedLocation.longitude,
                                        distance
                                    )
                                    // Verify if the distance is greater than 10 meters
                                    if (distance[0] > MINIMUM_DISTANCE_TO_UPDATE_LOCATION) {
                                        locationService.saveCurrentLocation(
                                            authService.currentUser?.id ?: "",
                                            extractedLocation.latitude,
                                            extractedLocation.longitude,
                                            Date().time
                                        )
                                    } else {
                                        return@map
                                    }
                                } else {
                                    locationService.saveCurrentLocation(
                                        authService.currentUser?.id ?: "",
                                        extractedLocation.latitude,
                                        extractedLocation.longitude,
                                        Date().time
                                    )
                                }
                            }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error while saving location")
                }
            }
        }
    }
}
