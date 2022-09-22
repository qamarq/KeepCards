package com.qamarq.keepcards

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.window.SplashScreen
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Confirmation
import androidx.wear.compose.material.dialog.Dialog
import com.qamarq.keepcards.theme.WearAppColorPalette
import com.qamarq.keepcards.theme.WearAppTheme

@Composable
fun ButtonExample(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            modifier = Modifier
                .size(ButtonDefaults.LargeButtonSize)
                .padding(top = 30.dp),
            onClick = { /* ... */ },
        ) {
            Icon(
                imageVector = Icons.Rounded.DashboardCustomize,
                contentDescription = "triggers phone action",
                modifier = iconModifier
            )
        }
    }
}

@Composable
fun ButtonBack(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    navController: NavController
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
            onClick = { navController.navigate(Screen.MainScreen.route) },
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "triggers phone action",
                modifier = iconModifier
            )
        }
    }
}

var firstTimeAlert = true
@Composable
fun ButtonSync(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    context: Context,
    connectionState: Boolean
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
//        var showDialog by remember { mutableStateOf(false) }
//        Dialog(showDialog = showDialog, onDismissRequest = { showDialog = false }) {
//            Confirmation(
//                onTimeout = {
//                    showDialog = false
//                    val time = System.currentTimeMillis()
//                    MainActivity.sendData(context, "give_me_cards|$time")
//                },
//                icon = {
//                    // Initially, animation is static and shown at the start position (atEnd = false).
//                    // Then, we use the EffectAPI to trigger a state change to atEnd = true,
//                    // which plays the animation from start to end.
//                    var atEnd by remember { mutableStateOf(false) }
//                    DisposableEffect(Unit) {
//                        atEnd = true
//                        onDispose {}
//                    }
//                    Image(
//                        colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
//                        painter = painterResource(id = R.drawable.ic_baseline_sync_24),
//                        contentDescription = stringResource(R.string.phone_sync),
//                        modifier = Modifier.size(48.dp)
//                    )
//                },
//                durationMillis = 3000,
//            ) {
//                Text(text = stringResource(R.string.phone_sync), textAlign = TextAlign.Center)
//            }
//        }
        var showDialog2 by remember { mutableStateOf(false) }
        Dialog(showDialog = showDialog2, onDismissRequest = { showDialog2 = false }) {
            Confirmation(
                onTimeout = {
                    showDialog2 = false
                },
                icon = {
                    DisposableEffect(Unit) {
                        onDispose {}
                    }
                    Image(
                        colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
                        painter = painterResource(id = R.drawable.ic_twotone_phonelink_erase_24),
                        contentDescription = stringResource(R.string.phone_sync),
                        modifier = Modifier.size(48.dp)
                    )
                },
                durationMillis = 3000,
            ) {
                Text(text = "Brak połączenia", textAlign = TextAlign.Center)
            }
        }
        var showDialog by remember { mutableStateOf(false) }
        if (connectionState) {
            CompactChip(
                onClick = {
                    showDialog = true
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "triggers meditation action",
                        modifier = iconModifier
                    )
                },
                colors = ChipDefaults.primaryChipColors(),
            )
        }

        Dialog(showDialog = showDialog, onDismissRequest = { showDialog = false }) {
            Confirmation(
                onTimeout = {
                    showDialog = false
                    val time = System.currentTimeMillis()
                    MainActivity.sendData(context, "add_card|$time") },
                icon = {
                    DisposableEffect(Unit) {
                        onDispose {}
                    }
                    Image(
                        colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
                        painter = painterResource(id = R.drawable.ic_twotone_smartphone_24),
                        contentDescription = stringResource(R.string.open_on_phone),
                        modifier = Modifier.size(48.dp)
                    )
                },
                durationMillis = 3000,
            ) {
                Text(text = stringResource(R.string.open_on_phone), textAlign = TextAlign.Center)
            }
        }
        if (firstTimeAlert) {
            firstTimeAlert = false
            Handler(Looper.getMainLooper()).postDelayed({
                if (!connectionState) showDialog2 = true
            }, 1000)
        }
    }
}

@Composable
fun ButtonBackShowCard(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    navController: NavController,
    context: Context,
    shopName: String,
    productId: String,
    cardType: String,
    phoneConnected: Boolean
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        var showDialog by remember { mutableStateOf(false) }
        if (phoneConnected) {
            Button(
                modifier = Modifier
                    .padding(5.dp)
                    .size(ButtonDefaults.DefaultButtonSize),
                onClick = {
                    showDialog = true
                    val time = System.currentTimeMillis()
                    MainActivity.sendData(context, "open_card|$shopName|$productId|$cardType|$time")
                },
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = "triggers phone action",
                    modifier = iconModifier
                )
            }
        }

        Button(
            modifier = Modifier
                .padding(5.dp)
                .size(ButtonDefaults.DefaultButtonSize),
            onClick = { navController.navigate(Screen.MainScreen.route) },
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "triggers phone action",
                modifier = iconModifier
            )
        }

        Dialog(showDialog = showDialog, onDismissRequest = { showDialog = false }) {
            Confirmation(
                onTimeout = { showDialog = false },
                icon = {
                    // Initially, animation is static and shown at the start position (atEnd = false).
                    // Then, we use the EffectAPI to trigger a state change to atEnd = true,
                    // which plays the animation from start to end.
                    DisposableEffect(Unit) {
                        onDispose {}
                    }
                    Image(
                        colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
                        painter = painterResource(id = R.drawable.ic_twotone_smartphone_24),
                        contentDescription = stringResource(R.string.open_on_phone),
                        modifier = Modifier.size(48.dp)
                    )
                },
                durationMillis = 3000,
            ) {
                Text(text = stringResource(R.string.open_on_phone), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ShowCardButtonOpenPhone(
    iconModifier: Modifier = Modifier,
    context: Context,
    shopName: String,
    productId: String,
    cardType: String,
    phoneConnected: Boolean
) {
    var showDialog by remember { mutableStateOf(false) }
    if (phoneConnected) {
        Chip(
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth(),
            onClick = {
                showDialog = true
                val time = System.currentTimeMillis()
                MainActivity.sendData(context, "open_card|$shopName|$productId|$cardType|$time")
            },
            label = {
                Text(
                    text = "Otwórz na telefonie",
                    maxLines = 1,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.SendToMobile,
                    contentDescription = "Otwórz na telefonie",
                    modifier = iconModifier
                )
            },
            colors = ChipDefaults.primaryChipColors(),
        )
    }

    Dialog(showDialog = showDialog, onDismissRequest = { showDialog = false }) {
        Confirmation(
            onTimeout = { showDialog = false },
            icon = {
                DisposableEffect(Unit) {
                    onDispose {}
                }
                Image(
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
                    painter = painterResource(id = R.drawable.ic_twotone_smartphone_24),
                    contentDescription = stringResource(R.string.open_on_phone),
                    modifier = Modifier.size(48.dp)
                )
            },
            durationMillis = 3000,
        ) {
            Text(text = stringResource(R.string.open_on_phone), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ShowCardButtonArchive(
    iconModifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    Dialog(showDialog = showDialog, onDismissRequest = { showDialog = false }) {
        Alert(
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Archive,
                    contentDescription = "Archive",
                    modifier = Modifier.size(28.dp).wrapContentSize(align = Alignment.Center),
                )
            },
            title = { Text("Archiwizacja", textAlign = TextAlign.Center) },
            negativeButton = {
                Button(
                    colors = ButtonDefaults.secondaryButtonColors(),
                    onClick = {
                        showDialog = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "triggers phone action",
                        modifier = iconModifier
                    )
                }
            },
            positiveButton = {
                Button(onClick = {
                    showDialog = false
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Done,
                        contentDescription = "triggers phone action",
                        modifier = iconModifier
                    )
                }
            },
            contentPadding =
            PaddingValues(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 32.dp),
        ) {
            Text(
                text = "Czy chcesz zarchiwizować tą kartę?",
                textAlign = TextAlign.Center
            )
        }
    }

    Chip(
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth(),
        onClick = {
            showDialog = true
        },
        label = {
            Text(
                text = "Archiwizuj",
                maxLines = 1,
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Archive,
                contentDescription = "Archiwizuj",
                modifier = iconModifier
            )
        },
        colors = ChipDefaults.secondaryChipColors(),
    )
}

@Composable
fun ShowCardButtonDelete(
    iconModifier: Modifier = Modifier,
    context: Context,
    productId: String,
    navController: NavController
) {
    var showDialog by remember { mutableStateOf(false) }
    var showDialog2 by remember { mutableStateOf(false) }
    Dialog(showDialog = showDialog, onDismissRequest = { showDialog = false }) {
        Alert(
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(28.dp).wrapContentSize(align = Alignment.Center),
                )
            },
            title = { Text("Usuwanie", textAlign = TextAlign.Center) },
            negativeButton = {
                Button(
                    colors = ButtonDefaults.secondaryButtonColors(),
                    onClick = {
                        showDialog = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "triggers phone action",
                        modifier = iconModifier
                    )
                }
            },
            positiveButton = {
                Button(onClick = {
                    showDialog = false
                    val time = System.currentTimeMillis()
                    MainActivity.sendData(context, "delete_card|$productId|$time")
                    showDialog2 = true
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Done,
                        contentDescription = "triggers phone action",
                        modifier = iconModifier
                    )
                }
            },
            contentPadding =
            PaddingValues(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 32.dp),
        ) {
            Text(
                text = "Czy na pewno chcesz usunąć tą kartę?",
                textAlign = TextAlign.Center
            )
        }
    }

    Dialog(showDialog = showDialog2, onDismissRequest = { showDialog2 = false }) {
        Confirmation(
            onTimeout = {
                showDialog2 = false
                navController.navigate(Screen.MainScreen.route)
            },
            icon = {
                DisposableEffect(Unit) {
                    onDispose {}
                }
                Image(
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.secondary),
                    painter = painterResource(id = R.drawable.ic_baseline_done_24),
                    contentDescription = stringResource(R.string.open_on_phone),
                    modifier = Modifier.size(48.dp)
                )
            },
            durationMillis = 2000,
        ) {
            Text(text = "Usunięto kartę", textAlign = TextAlign.Center)
        }
    }

    Chip(
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth(),
        onClick = {
            showDialog = true
        },
        label = {
            Text(
                text = "Usuń kartę",
                maxLines = 1,
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Usuń kartę",
                modifier = iconModifier
            )
        },
        colors = ChipDefaults.secondaryChipColors(),
    )
}

@Composable
fun ShowCardButtonBack(
    iconModifier: Modifier = Modifier,
    navController: NavController,
) {
    Chip(
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth(),
        onClick = {
            navController.navigate(Screen.MainScreen.route)
        },
        label = {
            Text(
                text = "Powrót",
                maxLines = 1,
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Powrót",
                modifier = iconModifier
            )
        },
        colors = ChipDefaults.secondaryChipColors(),
    )
}

@Composable
fun MainBottomChipPrimary(
    iconModifier: Modifier = Modifier,
    context: Context,
    isConnected: Boolean
) {
    var showDialog by remember { mutableStateOf(false) }
    if (isConnected) {
        Chip(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(),
            onClick = {
                showDialog = true
                val time = System.currentTimeMillis()
                MainActivity.sendData(context, "give_me_cards|$time")
            },
            label = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.sync),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = stringResource(R.string.sync),
                    modifier = iconModifier
                )
            },
            colors = ChipDefaults.primaryChipColors(),
        )
    }

    Dialog(showDialog = showDialog, onDismissRequest = { showDialog = false }) {
        Confirmation(
            onTimeout = { showDialog = false },
            icon = {
                DisposableEffect(Unit) {
                    onDispose {}
                }
                Image(
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
                    painter = painterResource(id = R.drawable.ic_baseline_sync_24),
                    contentDescription = stringResource(R.string.phone_sync),
                    modifier = Modifier.size(48.dp)
                )
            },
            durationMillis = 3000,
        ) {
            Text(text = stringResource(R.string.phone_sync), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun MainBottomChipSecondary(
    iconModifier: Modifier = Modifier,
    navController: NavController,
) {
    Chip(
        modifier = Modifier
            .padding(top = 5.dp)
            .fillMaxWidth(),
        onClick = {
            navController.navigate(Screen.SettingsScreen.route)
        },
        label = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.settings),
                maxLines = 1,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings),
                modifier = iconModifier
            )
        },
        colors = ChipDefaults.secondaryChipColors(),
    )
}

@Composable
fun MainBottomButtons(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    navController: NavController,
    context: Context,
    isConnected: Boolean
) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isConnected) {
            Button(
                modifier = Modifier
                    .padding(6.dp)
                    .size(ButtonDefaults.LargeButtonSize),
                onClick = {
                    showDialog = true
                    val time = System.currentTimeMillis()
                    MainActivity.sendData(context, "add_card|$time")
                },
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "triggers phone action",
                    modifier = iconModifier
                )
            }
        }
        Button(
            modifier = Modifier
                .padding(6.dp)
                .size(ButtonDefaults.LargeButtonSize),
            onClick = { navController.navigate(Screen.SettingsScreen.route) },
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "triggers phone action",
                modifier = iconModifier
            )
        }
    }

    Dialog(showDialog = showDialog, onDismissRequest = { showDialog = false }) {
        Confirmation(
            onTimeout = { showDialog = false },
            icon = {
                DisposableEffect(Unit) {
                    onDispose {}
                }
                Image(
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
                    painter = painterResource(id = R.drawable.ic_twotone_smartphone_24),
                    contentDescription = stringResource(R.string.open_on_phone),
                    modifier = Modifier.size(48.dp)
                )
            },
            durationMillis = 3000,
        ) {
            Text(text = stringResource(R.string.open_on_phone), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun TextExample(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.device_shape)
    )
}

@Composable
fun ShopNameText(modifier: Modifier = Modifier, shopName: String) {
    Text(
        modifier = modifier,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = shopName
    )
}

@Composable
fun AppInfoIcon(iconModifier: Modifier) {
//    Image(
//        painter = painterResource(id = R.drawable.ic_baseline_sync_problem_24),
//        contentDescription = stringResource(R.string.app_name),
//        modifier = Modifier
//            .size(54.dp)
//            .padding(top = 14.dp, bottom = 6.dp)
//    )
    Image(
        imageVector = Icons.Rounded.Info,
        contentDescription = stringResource(R.string.info),
        modifier = iconModifier
    )
}

@Composable
fun AppInfoText1() {
    Text(
        text = "Numer kompilacji",
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp, top = 6.dp),
        fontWeight = FontWeight.Medium)
}

@Composable
fun AppInfoText2(modifier: Modifier = Modifier) {
    Text(
        text = "Wersja " + BuildConfig.VERSION_NAME,
        textAlign = TextAlign.Center,
        modifier = modifier,
        fontSize = 13.sp,
        fontWeight = FontWeight.Light
    )
}

@Composable
fun BarcodeCard(globalBitmap: Bitmap) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Image(bitmap = globalBitmap.asImageBitmap(), contentDescription = null,modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(10.dp)
            ))
    }
}

@Composable
fun TextSettings(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        textAlign = TextAlign.Center,
        color = Color.Gray,
        text = stringResource(R.string.settings)
    )
}

@Composable
fun TextAppName() {
    Text(
        modifier = Modifier.padding(bottom = 4.dp).fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = stringResource(id = R.string.your_cards)
    )
}

@Composable
fun NewCard(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    type: String,
    shop: String,
    clientId: String,
    navController: NavController
) {
    AppCard(
        modifier = modifier,
        appImage = {
            if (type == "barcode") {
                Icon(
                    painter = painterResource(id = R.drawable.ic_barcode_scanner_fill0_wght400_grad0_opsz24),
                    contentDescription = "triggers open message action",
                    modifier = iconModifier
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.QrCode,
                    contentDescription = "triggers open message action",
                    modifier = iconModifier
                )
            }
        },
        appName = {
            if (type == "barcode") {
                Text(stringResource(R.string.barcode))
            } else {
                Text(stringResource(R.string.qrcode))
            }},
        time = { Text("") },
        title = { Text(shop) },
        onClick = { navController.navigate(Screen.ShowCardScreen.withArgs(shop, clientId, type)) }
    ) {
        Text(stringResource(R.string.click_open))
    }


//    Chip(
//        modifier = modifier,
//        enabled = true,
//        onClick = { navController.navigate(Screen.ShowCardScreen.withArgs(shop, clientId, type)) },
//        label = {
//            Text(
//                text = shop,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
//        },
//        secondaryLabel= {
//            if (type == "barcode") {
//                Text(
//                    text = stringResource(R.string.barcode),
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            } else {
//                Text(
//                    text = stringResource(R.string.qrcode),
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//        },
//        icon = {
//            if (type == "barcode") {
//                Icon(
//                    painter = painterResource(id = R.drawable.ic_barcode_scanner_fill0_wght400_grad0_opsz24),
//                    contentDescription = "triggers open message action",
//                    modifier = iconModifier
//                )
//            } else {
//                Icon(
//                    imageVector = Icons.Rounded.QrCode,
//                    contentDescription = "triggers open message action",
//                    modifier = iconModifier
//                )
//            }
//        },
//    )
}

@Composable
fun NewEmptyCard(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    type: String,
    context: Context
) {
    var showDialog by remember { mutableStateOf(false) }
    Dialog(showDialog = showDialog, onDismissRequest = { showDialog = false }) {
        Confirmation(
            onTimeout = { showDialog = false },
            icon = {
                DisposableEffect(Unit) {
                    onDispose {}
                }
                Image(
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
                    painter = painterResource(id = R.drawable.ic_twotone_smartphone_24),
                    contentDescription = stringResource(R.string.open_on_phone),
                    modifier = Modifier.size(48.dp)
                )
            },
            durationMillis = 3000,
        ) {
            Text(text = stringResource(R.string.open_on_phone), textAlign = TextAlign.Center)
        }
    }
    AppCard(
        modifier = modifier,
        appImage = {
            if (type == "sync") {
                Icon(
                    imageVector = Icons.Rounded.SyncProblem,
                    contentDescription = "triggers open message action",
                    modifier = iconModifier
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "triggers open message action",
                    modifier = iconModifier
                )
            }
        },
        appName = {
            if (type == "sync") {
                Text(stringResource(R.string.sync_error))
            } else {
                Text(stringResource(R.string.add_cards))
            }},
        time = { Text("") },
        title = {
            if (type == "sync") {
                Text(stringResource(R.string.phone_connection))
            } else {
                Text(stringResource(R.string.empty_cards))
            }
        },
        onClick = {
            showDialog = true
            val time = System.currentTimeMillis()
            MainActivity.sendData(context, "add_card|$time")
        }
    ) {
        if (type == "sync") {
            Text(stringResource(R.string.sync_error_desc))
        } else {
            Text(stringResource(R.string.add_cards_desc))
        }
    }
}

@Composable
fun ChipBack(
    modifier: Modifier = Modifier,
    navController: NavController
) {
//    Chip(
//        modifier = modifier,
//        onClick = {
//            navController.navigate(Screen.MainScreen.route)
//        },
//        label = {
//            Text(
//                text = "Wróć do strony głównej",
//                maxLines = 2,
//                overflow = TextOverflow.Ellipsis
//            )
//        },
//        icon = {
//            Icon(
//                imageVector = Icons.Rounded.ArrowBack,
//                contentDescription = stringResource(R.string.back),
//                modifier = iconModifier
//            )
//        },
//        colors = ChipDefaults.secondaryChipColors(),
//    )
    Chip(
        modifier = modifier,
        onClick = {
            navController.navigate(Screen.MainScreen.route)
        },
        label = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                text = "Powrót",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = ChipDefaults.secondaryChipColors(),
    )
}

@Composable
fun ChipCheckUpdate(
    context: Context
) {
    var showDialog by remember { mutableStateOf(false) }
    Dialog(showDialog = showDialog, onDismissRequest = { showDialog = false }) {
        Confirmation(
            onTimeout = {
                showDialog = false
                val time = System.currentTimeMillis()
                MainActivity.sendData(context, "check_update|$time")
            },
            icon = {
                DisposableEffect(Unit) {
                    onDispose {}
                }
                Image(
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.secondary),
                    painter = painterResource(id = R.drawable.ic_baseline_done_24),
                    contentDescription = stringResource(R.string.open_on_phone),
                    modifier = Modifier.size(48.dp)
                )
            },
            durationMillis = 2000,
        ) {
            Text(text = stringResource(R.string.open_on_phone), textAlign = TextAlign.Center)
        }
    }
    Chip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, top = 12.dp),
        onClick = {
            showDialog = true
        },
        label = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.check_update),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = ChipDefaults.primaryChipColors(),
    )
}

@Composable
fun ChipSettings(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    navController: NavController
) {
    Chip(
        modifier = modifier,
        onClick = { navController.navigate(Screen.SettingsScreen.route) },
        label = {
            Text(
                text = stringResource(id = R.string.settings),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "triggers meditation action",
                modifier = iconModifier
            )
        },
    )
}

@Composable
fun ToggleChipExample(modifier: Modifier = Modifier) {
    var checked by remember { mutableStateOf(true) }
    ToggleChip(
        modifier = modifier,
        checked = checked,
        toggleControl = {
            Icon(
                imageVector = ToggleChipDefaults.switchIcon(checked = checked),
                contentDescription = if (checked) "On" else "Off"
            )
        },
        onCheckedChange = {
            checked = it
        },
        label = {
            Text(
                text = "Sound",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}


@Composable
fun ToggleChipSync(modifier: Modifier = Modifier, syncSettings: Boolean, settingsPrefs: SharedPreferences) {
    var checked by remember { mutableStateOf(syncSettings) }
    ToggleChip(
        modifier = modifier,
        checked = checked,
        toggleControl = {
            Icon(
                imageVector = ToggleChipDefaults.switchIcon(checked = checked),
                contentDescription = if (checked) "On" else "Off"
            )
        },
        onCheckedChange = {
            checked = it
            val editor: SharedPreferences.Editor = settingsPrefs.edit()
            editor.putBoolean("syncWear", it)
            editor.apply()
        },
        secondaryLabel = {
            Text("z telefonem", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        label = {
            Text(
                text = stringResource(R.string.sync),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )

//    SplitToggleChip(
//        modifier = modifier,
//        label = {
//            Text("Synchronizacja", maxLines = 1, overflow = TextOverflow.Ellipsis)
//        },
//        secondaryLabel = {
//            Text("z telefonem", maxLines = 1, overflow = TextOverflow.Ellipsis)
//        },
//        checked = checked,
//        toggleControl = {
//            Icon(
//                imageVector = ToggleChipDefaults.switchIcon(checked = checked),
//                contentDescription = if (checked) "On" else "Off"
//            )
//        },
//        onCheckedChange = {
//            checked = it
//            val editor: SharedPreferences.Editor = settingsPrefs.edit()
//            editor.putBoolean("syncWear", it)
//            editor.apply()
//        },
//        onClick = {
//            /* Do something */
//        },
//        enabled = true,
//    )
}

@Composable
fun StartOnlyTextComposables() {
    Text(
        modifier = Modifier.fillMaxSize(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.device_shape)
    )
}

@Preview(
    group = "Starter",
    widthDp = WEAR_PREVIEW_ELEMENT_WIDTH_DP,
    heightDp = WEAR_PREVIEW_ELEMENT_HEIGHT_DP,
    apiLevel = WEAR_PREVIEW_API_LEVEL,
    uiMode = WEAR_PREVIEW_UI_MODE,
    backgroundColor = WEAR_PREVIEW_BACKGROUND_COLOR_BLACK,
    showBackground = WEAR_PREVIEW_SHOW_BACKGROUND
)
@Composable
fun StartOnlyTextComposablesPreview() {
    WearAppTheme {
        StartOnlyTextComposables()
    }
}

@Preview(
    group = "Button",
    widthDp = WEAR_PREVIEW_ELEMENT_WIDTH_DP,
    heightDp = WEAR_PREVIEW_ELEMENT_HEIGHT_DP,
    apiLevel = WEAR_PREVIEW_API_LEVEL,
    uiMode = WEAR_PREVIEW_UI_MODE,
    backgroundColor = WEAR_PREVIEW_BACKGROUND_COLOR_BLACK,
    showBackground = WEAR_PREVIEW_SHOW_BACKGROUND
)
@Composable
fun ButtonExamplePreview() {
    WearAppTheme {
        ButtonExample(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            iconModifier = Modifier
                .size(24.dp)
                .wrapContentSize(align = Alignment.Center)
        )
    }
}

@Preview(
    group = "Text",
    widthDp = WEAR_PREVIEW_ROW_WIDTH_DP,
    heightDp = WEAR_PREVIEW_ROW_HEIGHT_DP,
    apiLevel = WEAR_PREVIEW_API_LEVEL,
    uiMode = WEAR_PREVIEW_UI_MODE,
    backgroundColor = WEAR_PREVIEW_BACKGROUND_COLOR_BLACK,
    showBackground = WEAR_PREVIEW_SHOW_BACKGROUND
)
@Composable
fun TextExamplePreview() {
    WearAppTheme {
        TextExample(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}

@Preview(
    group = "Toggle Chip",
    widthDp = WEAR_PREVIEW_ROW_WIDTH_DP,
    heightDp = WEAR_PREVIEW_ROW_HEIGHT_DP,
    apiLevel = WEAR_PREVIEW_API_LEVEL,
    uiMode = WEAR_PREVIEW_UI_MODE,
    backgroundColor = WEAR_PREVIEW_BACKGROUND_COLOR_BLACK,
    showBackground = WEAR_PREVIEW_SHOW_BACKGROUND
)
@Composable
fun ToggleChipExamplePreview() {
    WearAppTheme {
        ToggleChipExample(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}
