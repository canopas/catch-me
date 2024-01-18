package com.canopas.catchme.ui.flow.auth.methods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.auth.FirebaseAuthService
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.MainNavigator
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SignInMethodViewModel @Inject constructor(
    private val navigator: MainNavigator,
    private val firebaseAuth: FirebaseAuthService,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SignInMethodScreenState())
    val state: StateFlow<SignInMethodScreenState> = _state

    fun signInWithPhone() = viewModelScope.launch {
        navigator.navigateTo(AppDestinations.phoneSignIn.path)
    }

    fun proceedGoogleSignIn(account: GoogleSignInAccount) =
        viewModelScope.launch(appDispatcher.IO) {
            _state.emit(_state.value.copy(showGoogleLoading = true))
            try {
                val firebaseToken = firebaseAuth.signInWithGoogleAuthCredential(account.idToken)
                val isNewUSer = authService.verifiedGoogleLogin(firebaseToken, account)
                onSignUp(isNewUSer)
                _state.emit(_state.value.copy(showGoogleLoading = false))
            } catch (e: Exception) {
                Timber.e(e, "Failed to sign in with google")
                _state.emit(
                    _state.value.copy(
                        showGoogleLoading = false,
                        error = "Failed to sign in with google"
                    )
                )
            }
        }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }

    fun onSignUp(isNewUSer: Boolean) = viewModelScope.launch(appDispatcher.MAIN) {
        if (isNewUSer) {
            navigator.navigateTo(
                AppDestinations.onboard.path,
                popUpToRoute = AppDestinations.signIn.path,
                inclusive = true
            )
        } else {
            userPreferences.setOnboardShown(true)
            navigator.navigateTo(
                AppDestinations.enablePermissions.path,
                popUpToRoute = AppDestinations.signIn.path,
                inclusive = true
            )
        }
    }
}

data class SignInMethodScreenState(
    val showGoogleLoading: Boolean = false,
    val error: String? = null
)
