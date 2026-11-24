package com.example.trihb.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.trihb.data.repository.TriHubRepository
import com.example.trihb.sensor.ActivitySensorManager
import com.example.trihb.ui.screens.auth.AuthViewModel
import com.example.trihb.ui.screens.chat.ChatViewModel
import com.example.trihb.ui.screens.dieta.DietaViewModel
import com.example.trihb.ui.screens.feed.FeedViewModel
import com.example.trihb.ui.screens.perfil.PerfilViewModel
import com.example.trihb.ui.screens.progresso.ProgressoViewModel

/**
 * Fábrica única de ViewModels (injeção de dependência manual).
 * Uso nas telas: `viewModel(factory = AppViewModelProvider.Factory)`.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { AuthViewModel(repository()) }
        initializer { DietaViewModel(repository(), sensor()) }
        initializer { ProgressoViewModel(repository(), sensor()) }
        initializer { FeedViewModel(repository()) }
        initializer { ChatViewModel(repository(), sensor()) }
        initializer { PerfilViewModel(repository()) }
    }
}

private fun CreationExtras.application(): Application = checkNotNull(this[APPLICATION_KEY])
private fun CreationExtras.repository() = TriHubRepository.getInstance(application())
private fun CreationExtras.sensor() = ActivitySensorManager.getInstance(application())
