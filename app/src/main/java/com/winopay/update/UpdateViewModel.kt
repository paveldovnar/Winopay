package com.winopay.update

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "UpdateViewModel"

    private val _updateInfo = MutableStateFlow<UpdateChecker.UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateChecker.UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _checkError = MutableStateFlow<String?>(null)
    val checkError: StateFlow<String?> = _checkError.asStateFlow()

    private val _showNoUpdateDialog = MutableStateFlow(false)
    val showNoUpdateDialog: StateFlow<Boolean> = _showNoUpdateDialog.asStateFlow()

    fun checkForUpdates(botToken: String, chatUsername: String) {
        if (_isChecking.value) {
            Log.d(TAG, "UPDATE|CHECK|SKIP|already_checking")
            return
        }

        viewModelScope.launch {
            try {
                _isChecking.value = true
                _checkError.value = null
                _showNoUpdateDialog.value = false
                
                Log.i(TAG, "UPDATE|CHECK|START|bot=${botToken.take(10)}...|chat=$chatUsername")

                val update = UpdateChecker.checkForUpdate(botToken, chatUsername)
                
                if (update != null) {
                    Log.i(TAG, "UPDATE|CHECK|FOUND|version=${update.versionName}(${update.versionCode})")
                    _updateInfo.value = update
                    _showNoUpdateDialog.value = false
                } else {
                    Log.i(TAG, "UPDATE|CHECK|UP_TO_DATE")
                    _updateInfo.value = null
                    _showNoUpdateDialog.value = true
                }

            } catch (e: Exception) {
                Log.e(TAG, "UPDATE|CHECK|ERROR|${e.javaClass.simpleName}|${e.message}", e)
                _updateInfo.value = null
                _checkError.value = "Failed to check for updates: ${e.message}"
            } finally {
                _isChecking.value = false
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    fun dismissCheckStatus() {
        _showNoUpdateDialog.value = false
        _checkError.value = null
    }

    fun clearUpdate() {
        _updateInfo.value = null
    }
}
