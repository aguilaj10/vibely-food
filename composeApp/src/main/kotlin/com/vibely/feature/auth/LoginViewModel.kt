package com.vibely.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibely.feature.auth.domain.model.Credentials
import com.vibely.feature.auth.usecase.LoginUseCase
import com.vibely.navigation.AppNavKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the login screen.
 *
 * @property email Current value of the email field.
 * @property password Current value of the password field.
 * @property isLoading True while a sign-in request is in flight.
 * @property errorMessage Human-readable error shown below the Sign In button, or null.
 * @property isSubmitEnabled True when both fields are non-blank and no request is in flight.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isSubmitEnabled: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}

/**
 * ViewModel for the login screen.
 * Exposes [uiState] for the UI and [navigationEvents] for one-shot navigation.
 */
class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<AppNavKey>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    /** Updates the email field and clears any displayed error. */
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    /** Updates the password field and clears any displayed error. */
    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    /** Initiates a sign-in request if [LoginUiState.isSubmitEnabled] is true. */
    fun onSignIn() {
        val state = _uiState.value
        if (!state.isSubmitEnabled) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = loginUseCase(Credentials(state.email, state.password))
            result
                .onSuccess { _navigationEvents.emit(AppNavKey.FloorPlan) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Sign in failed. Please try again.",
                        )
                    }
                }
        }
    }
}
