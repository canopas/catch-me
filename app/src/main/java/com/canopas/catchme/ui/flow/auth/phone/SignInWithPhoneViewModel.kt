package com.canopas.catchme.ui.flow.auth.phone

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.auth.FirebaseAuthService
import com.canopas.catchme.data.service.auth.PhoneAuthState
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppDestinations.OtpVerificationNavigation
import com.canopas.catchme.ui.navigation.AppNavigator
import com.canopas.catchme.ui.navigation.KEY_RESULT
import com.canopas.catchme.ui.navigation.RESULT_OKAY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SignInWithPhoneViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val fbAuthService: FirebaseAuthService,
    private val authService: AuthService,
    private val dispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(SignInWithPhoneState())
    val state: StateFlow<SignInWithPhoneState> = _state

    fun onPhoneChange(phone: String) {
        _state.value = _state.value.copy(phone = phone, enableNext = phone.length > 3)
    }

    fun onCodeChange(code: String) {
        _state.value = _state.value.copy(code = code)
    }

    fun verifyPhoneNumber(context: Context) = viewModelScope.launch(dispatcher.IO) {
        _state.emit(_state.value.copy(verifying = true))

        val phone = _state.value.code + _state.value.phone

        fbAuthService.verifyPhoneNumber(context, phone)
            .collect { result ->
                when (result) {
                    is PhoneAuthState.VerificationCompleted -> {
                        val firebaseIdToken =
                            fbAuthService.signInWithPhoneAuthCredential(result.credential)
                        val isNewUser =
                            authService.verifiedPhoneLogin(firebaseIdToken, _state.value.phone)
                        appNavigator.navigateBack(
                            route = AppDestinations.signIn.path,
                            result = mapOf(
                                KEY_RESULT to RESULT_OKAY,
                                EXTRA_RESULT_IS_NEW_USER to isNewUser
                            )
                        )
                        _state.emit(_state.value.copy(verifying = false))
                    }

                    is PhoneAuthState.VerificationFailed -> {
                        Timber.e(result.e, "Unable to send OTP, verification failed")
                        _state.emit(
                            _state.value.copy(
                                verifying = false,
                                error = result.e.message
                            )
                        )
                    }

                    is PhoneAuthState.CodeSent -> {
                        _state.emit(
                            _state.value.copy(
                                verifying = false,
                                verificationId = result.verificationId
                            )
                        )

                        appNavigator.navigateTo(
                            OtpVerificationNavigation.otpVerification(
                                verificationId = result.verificationId,
                                phoneNo = phone
                            ).path
                        )
                    }
                }
            }
    }

    fun popBack() {
        appNavigator.navigateBack()
    }

    fun showCountryPicker(show: Boolean = true) {
        _state.value = _state.value.copy(showCountryPicker = show)
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }
}

data class SignInWithPhoneState(
    val code: String = "",
    val phone: String = "",
    val verifying: Boolean = false,
    val verificationId: String? = null,
    val error: String? = null,
    val enableNext: Boolean = false,
    val showCountryPicker: Boolean = false
)
