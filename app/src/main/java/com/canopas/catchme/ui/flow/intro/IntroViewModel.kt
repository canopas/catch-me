package com.canopas.catchme.ui.flow.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    private val userPreferences: UserPreferences, private val navigator: AppNavigator
) : ViewModel() {

    fun completedIntro() = viewModelScope.launch {
        userPreferences.setIntroShown(true)
        navigator.navigateTo(
            AppDestinations.signIn.path,
            popUpToRoute = AppDestinations.intro.path,
            inclusive = true
        )
    }
}