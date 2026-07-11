package com.simplestsoft.twostrokecalc.ui.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.data.auth.AuthRepository
import com.simplestsoft.twostrokecalc.data.remote.AccountApiService
import com.simplestsoft.twostrokecalc.ui.auth.AuthExceptionMessage
import com.simplestsoft.twostrokecalc.ui.util.NetworkExceptionMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val passwordChanged: Boolean = false,
    val deleteInProgress: Boolean = false,
    val accountDeleted: Boolean = false,
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authRepository: AuthRepository,
    private val accountApiService: AccountApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state = _state.asStateFlow()

    fun changePassword(current: String, newPass: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, message = null, passwordChanged = false) }
            val result = authRepository.updatePassword(current, newPass)
            _state.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let { error ->
                        AuthExceptionMessage.getMessage(appContext, error)
                    },
                    passwordChanged = result.isSuccess,
                )
            }
        }
    }

    fun clearFeedback() {
        _state.update {
            it.copy(message = null, error = null, passwordChanged = false)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    deleteInProgress = true,
                    error = null,
                    message = null,
                    passwordChanged = false,
                    accountDeleted = false,
                )
            }
            runCatching {
                val response = accountApiService.deleteOwnAccount()
                if (!response.isSuccessful) {
                    error("Account deletion failed (${response.code()})")
                }
                authRepository.signOut()
            }.onSuccess {
                _state.update { it.copy(deleteInProgress = false, accountDeleted = true) }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        deleteInProgress = false,
                        error = NetworkExceptionMessage.resolve(
                            appContext,
                            e,
                            appContext.getString(R.string.auth_error_generic),
                        ),
                        accountDeleted = false,
                    )
                }
            }
        }
    }

    fun consumeAccountDeletedFlag() {
        _state.update { it.copy(accountDeleted = false) }
    }
}
