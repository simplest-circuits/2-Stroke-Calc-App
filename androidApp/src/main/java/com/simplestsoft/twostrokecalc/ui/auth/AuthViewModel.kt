package com.simplestsoft.twostrokecalc.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.viewModelScope
import com.simplestsoft.twostrokecalc.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val forgotEmailSent: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private fun authErrorMessage(t: Throwable?) = AuthExceptionMessage.getMessage(appContext, t)

    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    fun clearError() {
        _state.update { it.copy(error = null, forgotEmailSent = false) }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = authRepository.signInWithEmail(email, password)
            _state.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::authErrorMessage),
                )
            }
        }
    }

    fun register(email: String, password: String, displayName: String?) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = authRepository.register(email, password, displayName)
            _state.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::authErrorMessage),
                )
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, forgotEmailSent = false) }
            val result = authRepository.sendPasswordReset(email)
            _state.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::authErrorMessage),
                    forgotEmailSent = result.isSuccess,
                )
            }
        }
    }

    fun googleSignIn(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = authRepository.signInWithGoogle(context)
            _state.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::authErrorMessage),
                )
            }
        }
    }
}
