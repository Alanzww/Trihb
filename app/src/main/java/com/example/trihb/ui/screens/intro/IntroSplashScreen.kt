package com.example.trihb.ui.screens.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.trihb.R
import kotlinx.coroutines.delay

/**
 * Abertura: exclusivamente a logo TriHub centralizada, animada de forma declarativa
 * (fade-in 800 ms → pausa 400 ms → fade-out 800 ms), sem arquivos XML de animação.
 */
@Composable
fun IntroSplashScreen(onIntroConcluida: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val aoConcluir by rememberUpdatedState(onIntroConcluida)

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing))
        delay(400)
        alpha.animateTo(0f, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing))
        aoConcluir()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.trihb_logo),
            contentDescription = "Logotipo TriHub",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(180.dp)
                .graphicsLayer {
                    this.alpha = alpha.value
                    // leve "respiro" de escala acompanhando o fade
                    val escala = 0.92f + 0.08f * alpha.value
                    scaleX = escala
                    scaleY = escala
                },
        )
    }
}
