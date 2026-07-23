package ai.nextgpu.agent.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window

@Composable
fun SplashWindow() {

    Window(
        onCloseRequest = {},
        undecorated = true,
        resizable = false
    ) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painterResource("images/logo-splash-screen.png"),
                    null
                )

                Spacer(Modifier.height(20.dp))

                Text("Starting NextGPU...")

                Spacer(Modifier.height(10.dp))

                CircularProgressIndicator()
            }
        }
    }
}