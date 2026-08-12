package com.denham.skyline.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.util.UnstableApi
import com.denham.skyline.SkylineApp
import com.denham.skyline.di.AppContainer

/** ViewModel wiring without a DI framework: pull the AppContainer from the app. */
@UnstableApi
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as SkylineApp).container
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
