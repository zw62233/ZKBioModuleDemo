package com.armatura.biomodule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armatura.biomodule.util.HidHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class BaseRegisterViewModel : ViewModel() {

    fun exitStandByMode() {
        viewModelScope.launch(Dispatchers.IO) {
            HidHelper.exitStandByMode()
        }
    }

    fun enterStandByMode() {
        viewModelScope.launch(Dispatchers.IO) {
            HidHelper.enterStandByMode()
        }
    }
}