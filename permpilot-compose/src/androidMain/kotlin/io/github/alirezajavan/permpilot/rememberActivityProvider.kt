package io.github.alirezajavan.permpilot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun rememberActivityProvider(): ActivityProvider {
    return remember { ActivityProvider.create() }
}