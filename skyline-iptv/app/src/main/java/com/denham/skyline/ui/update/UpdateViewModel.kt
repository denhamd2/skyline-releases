package com.denham.skyline.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denham.skyline.data.repo.UpdateRepository
import kotlinx.coroutines.launch

class UpdateViewModel(private val updateRepository: UpdateRepository) : ViewModel() {
    val updateState = updateRepository.state

    /** [silent] true for the automatic check on launch, so failures don't nag. */
    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            updateRepository.checkForUpdates(silent)
        }
    }

    fun downloadAndInstall(version: String) {
        viewModelScope.launch {
            updateRepository.downloadAndInstall(version)
        }
    }

    fun dismiss() {
        updateRepository.dismiss()
    }
}
