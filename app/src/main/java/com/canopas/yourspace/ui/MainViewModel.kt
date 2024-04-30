package com.canopas.yourspace.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.user.ApiUserSession
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.fcm.KEY_NOTIFICATION_TYPE
import com.canopas.yourspace.domain.fcm.NotificationChatConst
import com.canopas.yourspace.domain.fcm.NotificationPlaceConst
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val navigator: AppNavigator,
    private val appDispatcher: AppDispatcher,
    private val apiUserService: ApiUserService,
    private val authService: AuthService,
    private val spaceRepository: SpaceRepository
) : ViewModel() {

    val navActions = navigator.navigationChannel
    private var listenSessionJob: Job? = null
    private val _state = MutableStateFlow(MainScreenState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (userPreferences.isIntroShown()) {
                if (userPreferences.currentUser == null) {
                    _state.value = state.value.copy(initialRoute = AppDestinations.signIn.path)
                } else if (!userPreferences.isOnboardShown()) {
                    _state.value = state.value.copy(initialRoute = AppDestinations.onboard.path)
                }
            } else {
                _state.value = state.value.copy(initialRoute = AppDestinations.intro.path)
            }

            userPreferences.currentUserSessionState.collectLatest { userSession ->
                listenSessionJob?.cancel()
                listenUserSession(userSession)
            }
        }
    }

    private suspend fun listenUserSession(userSession: ApiUserSession?) {
        listenSessionJob = viewModelScope.launch(appDispatcher.IO) {
            userSession?.let {
                apiUserService.getUserSessionFlow(it.user_id, it.id).collectLatest { session ->
                    if (session != null && !session.session_active) {
                        _state.value = state.value.copy(isSessionExpired = true)
                    }
                }
            }
        }
    }

    fun signOut() = viewModelScope.launch {
        authService.signOut()
        navigator.navigateTo(
            AppDestinations.signIn.path,
            clearStack = true
        )
        _state.value = state.value.copy(isSessionExpired = false)
    }

    fun handleIntentData(intent: Intent?) {
        val type = intent?.getStringExtra(KEY_NOTIFICATION_TYPE) ?: return
        if (type == NotificationChatConst.NOTIFICATION_TYPE_CHAT) {
            val threadId = intent.getStringExtra(NotificationChatConst.KEY_THREAD_ID)
            if (!threadId.isNullOrEmpty()) {
                navigateToMessages(threadId)
            }
        } else if (type == NotificationPlaceConst.NOTIFICATION_TYPE_NEW_PLACE_ADDED) {
            val spaceId = intent.getStringExtra(NotificationPlaceConst.KEY_SPACE_ID)
            if (!spaceId.isNullOrEmpty()) navigateToPlaceList(spaceId)
        }
    }

    private fun navigateToMessages(threadId: String) = viewModelScope.launch(appDispatcher.IO) {
        delay(500)
        navigator.navigateTo(AppDestinations.ThreadMessages.messages(threadId).path)
    }

    private fun navigateToPlaceList(spaceId: String) = viewModelScope.launch(appDispatcher.IO) {
        try {
            if (spaceRepository.currentSpaceId == spaceId) {
                delay(500)
                navigator.navigateTo(AppDestinations.places.path)
                return@launch
            }
            _state.value = state.value.copy(verifyingSpace = true, showSpaceNotFoundPopup = false)
            val space = spaceRepository.getSpace(spaceId)

            if (space != null) {
                spaceRepository.currentSpaceId = spaceId
                _state.value = state.value.copy(verifyingSpace = false)
                navigator.navigateTo(AppDestinations.places.path, popUpToRoute = AppDestinations.home.path)
            } else {
                _state.value =
                    state.value.copy(showSpaceNotFoundPopup = true, verifyingSpace = false)
            }
        } catch (e: Exception) {
            _state.value = state.value.copy(verifyingSpace = false, showSpaceNotFoundPopup = true)
            Timber.e(e, "Error verifying space")
        }
    }

    fun dismissSpaceNotFoundPopup() {
        _state.value = state.value.copy(showSpaceNotFoundPopup = false)
    }
}

data class MainScreenState(
    val isSessionExpired: Boolean = false,
    val initialRoute: String = AppDestinations.home.path,
    val verifyingSpace: Boolean = false,
    val showSpaceNotFoundPopup: Boolean = false
)
