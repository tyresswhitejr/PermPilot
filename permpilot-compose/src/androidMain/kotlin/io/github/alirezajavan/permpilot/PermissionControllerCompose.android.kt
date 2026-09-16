package io.github.alirezajavan.permpilot

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest

@Composable
actual fun rememberPermissionController(persistence: PermissionPersistence?): PermissionController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activityProvider = rememberActivityProvider()
    val controller =
        remember {
            PermissionController.create(
                context.applicationContext,
                scope = scope,
                activityProvider = activityProvider,
                persistence = persistence,
            ) as AndroidPermissionController
        }

    // shouldShowRequestPermissionRationale is an Activity-only API;
    // Notify activityProvider of latest activity this composition is attached to
    val activity = context.findActivity()
    DisposableEffect(activity) {
        activity?.let(activityProvider::update)
        onDispose {
            activity?.let(activityProvider::clear)
        }
    }

    UseAndroidPermissionController(controller)

    return controller
}

@Composable
actual fun UsePermissionController(controller: PermissionController) {
    UseAndroidPermissionController(controller as AndroidPermissionController)
}

@Composable
private fun UseAndroidPermissionController(controller: AndroidPermissionController) {
    var currentMultiCallback by remember { mutableStateOf<((Map<String, Boolean>) -> Unit)?>(null) }
    var currentHealthCallback by remember { mutableStateOf<((Set<String>) -> Unit)?>(null) }

    val multiLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            currentMultiCallback?.invoke(results)
            currentMultiCallback = null
        }

    val healthLauncher =
        rememberLauncherForActivityResult(
            contract =
                androidx.health.connect.client.PermissionController
                    .createRequestPermissionResultContract(),
        ) { results ->
            currentHealthCallback?.invoke(results)
            currentHealthCallback = null
        }

    LaunchedEffect(controller) {
        controller.multiRequestFlow.collectLatest { request ->
            when (request) {
                is PermissionRequest.Runtime -> {
                    currentMultiCallback = request.onResult
                    multiLauncher.launch(request.permissions)
                }

                is PermissionRequest.Health -> {
                    currentHealthCallback = request.onResult
                    healthLauncher.launch(request.permissions)
                }
            }
        }
    }

    // Special permissions (and PermanentlyDenied runtime ones) are only ever resolved via a
    // Settings redirect, which never calls back into request() -- ON_RESUME after returning from
    // that Settings screen is the only signal available that the state may have changed.
    ObserveLifecycleResume { controller.refreshAll() }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }