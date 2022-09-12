
package com.qamarq.keepcards

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import com.qamarq.keepcards.theme.WearAppTheme

@Composable
fun SettingsActivity(navController: NavController, context: Context) {
    val sharedPrefFile = "keepcardspref"
    val sharedPreferences: SharedPreferences = context.getSharedPreferences(sharedPrefFile,
        Context.MODE_PRIVATE)
    val syncWear = sharedPreferences.getBoolean("syncWear", true)
    WearAppTheme {
        val listState = rememberScalingLazyListState()
        Scaffold(
            timeText = {
                if (!listState.isScrollInProgress) {
                    TimeText()
                }
            },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
            positionIndicator = {
                PositionIndicator(
                    scalingLazyListState = listState
                )
            }
        ) {
            val contentModifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
            val iconModifier = Modifier
                .size(24.dp)
                .wrapContentSize(align = Alignment.Center)
            val firstItemModifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp, top = 20.dp)

            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                autoCentering = AutoCenteringParams(itemIndex = 0),
                state = listState
            ) {
                item { TextSettings(firstItemModifier) }
                item { ToggleChipSync(contentModifier, syncWear, sharedPreferences) }

//                item { AppInfoIcon(iconModifier) }
                item { AppInfoText1(contentModifier) }
                item { AppInfoText2(contentModifier) }
                item { ChipCheckUpdate(context) }
                item { ChipBack(contentModifier, iconModifier, navController) }
//                item { ButtonBack(contentModifier, iconModifier) }
            }
        }
    }
}