package com.canopas.catchme.ui.flow.home.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.models.user.UserInfo
import com.canopas.catchme.data.repository.SpaceRepository
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.data.utils.AppDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val spaceRepository: SpaceRepository,
    private val userPreferences: UserPreferences,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(MapScreenState())
    val state = _state.asStateFlow()

    private var locationJob: Job? = null

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(currentUserId = userPreferences.currentUser?.id)
            userPreferences.currentSpaceState.collectLatest {
                locationJob?.cancel()
                listenMemberLocation()
            }
        }
    }

    private fun listenMemberLocation() {
        locationJob = viewModelScope.launch(appDispatcher.IO) {
            spaceRepository.getMemberWithLocation().collectLatest {
                _state.emit(_state.value.copy(members = it))
            }
        }
    }
}

data class MapScreenState(
    val members: List<UserInfo> = emptyList(),
    val currentUserId: String? = ""
)
