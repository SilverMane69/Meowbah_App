package com.kawaii.meowbah.ui.screens.merch

import androidx.lifecycle.ViewModel
import com.kawaii.meowbah.data.SampleMerchData
import com.kawaii.meowbah.data.model.MerchItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MerchViewModel : ViewModel() {

    private val _merchItems = MutableStateFlow<List<MerchItem>>(emptyList())
    val merchItems: StateFlow<List<MerchItem>> = _merchItems

    init {
        loadMerchItems()
    }

    private fun loadMerchItems() {
        // For now, we are loading directly from SampleMerchData
        // In a real app, this might come from a repository, database, or network call
        _merchItems.value = SampleMerchData.items
    }

    fun getMerchItemById(id: String): MerchItem? {
        return _merchItems.value.find { it.id == id }
    }
}
