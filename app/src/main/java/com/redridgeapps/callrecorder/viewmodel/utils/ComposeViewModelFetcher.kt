package com.redridgeapps.callrecorder.viewmodel.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.redridgeapps.callrecorder.viewmodel.ComposeViewModelStores
import javax.inject.Inject
import kotlin.reflect.KClass

class ComposeViewModelFetcher(
    private val composeViewModelStores: ComposeViewModelStores,
    private val viewModelFactory: DaggerViewModelFactory
) {

    fun <T : ViewModel> fetch(key: String, kClass: KClass<T>): T {

        val viewModelStore = composeViewModelStores.getViewModelStore(key)
        val viewModelProvider = ViewModelProvider(viewModelStore, viewModelFactory)

        return viewModelProvider.get(kClass.java)
    }
}

class ComposeViewModelFetcherFactory @Inject constructor(
    private val viewModelFactory: DaggerViewModelFactory
) {
    fun create(composeViewModelStores: ComposeViewModelStores): ComposeViewModelFetcher {
        return ComposeViewModelFetcher(composeViewModelStores, viewModelFactory)
    }
}
