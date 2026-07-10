package ai.nextgpu.agent.ui

import androidx.compose.runtime.*

// The State Holder
class OverlayState {
    val overlays = mutableStateListOf<@Composable () -> Unit>()
}

// The CompositionLocal (This makes the portal accessible anywhere)
val LocalAppOverlay = staticCompositionLocalOf<OverlayState> {
    error("OverlayState not provided")
}

// The Wrapper Component you will use in HubScreen
@Composable
fun AppPortal(content: @Composable () -> Unit) {
    val overlayState = LocalAppOverlay.current

    // When this component is added to the UI, it sends its content to the root.
    // When it's removed (e.g., showDialog becomes false), it cleans up after itself.
    DisposableEffect(content) {
        overlayState.overlays.add(content)
        onDispose { overlayState.overlays.remove(content) }
    }
}