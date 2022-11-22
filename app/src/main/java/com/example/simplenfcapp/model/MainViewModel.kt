package com.example.simplenfcapp.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    private val TAG = "MainViewModel"


    private val _tagData = MutableLiveData<String>()
    val tagData: LiveData<String>
        get() = _tagData

    fun getTagData(data: String) {
        _tagData.value = data
        Log.i(TAG,"_tagData: ${_tagData.value.toString()}")
    }
}