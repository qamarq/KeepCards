
package com.qamarq.keepcards

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.qamarq.keepcards.theme.WearAppTheme

@Composable
fun SplashActivity(showSpinner: Boolean) {
    WearAppTheme {
        Scaffold(
            timeText = {},
            vignette = {},
            positionIndicator = {}
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                if (showSpinner) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp).padding(top = 19.dp),
                        strokeWidth = 2.dp)
                }
            }
        }
    }
}