package com.qamarq.keepcards

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.InputDeviceCompat
import androidx.core.view.MotionEventCompat
import androidx.core.view.ViewConfigurationCompat
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.google.android.gms.wearable.DataClient.OnDataChangedListener
import com.qamarq.keepcards.theme.WearAppTheme
import kotlinx.coroutines.*
import java.io.InputStream
import kotlin.coroutines.CoroutineContext


class MainActivity : ComponentActivity(), OnDataChangedListener, CoroutineScope {
    private val sharedPrefFile = "keepcardspref"
    var datapath = "/data_path"
    var connected = false
    var profileImg: Bitmap? = null
    @Composable
    fun Navigation() {
        val navController = rememberSwipeDismissableNavController()
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        val storedCards = sharedPreferences.getString("storedCards","").toString()
        SwipeDismissableNavHost(navController = navController, startDestination = Screen.MainScreen.route) {
            composable(route = Screen.SplashScreen.route) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                SplashActivity(false)
            }

            composable(route = Screen.MainScreen.route) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                MainScreen(message = storedCards, navController = navController)
            }

            composable(route = Screen.SettingsScreen.route) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                SettingsActivity(navController = navController, context = this@MainActivity)
            }

            composable(
                route = Screen.ShowCardScreen.route+"/{shopName}/{productId}/{cardType}",
                arguments = listOf(
                    navArgument("shopName") {
                        type = NavType.StringType
                        defaultValue = "Shop empty"
                        nullable = true
                    },
                    navArgument("productId") {
                        type = NavType.StringType
                        defaultValue = "1236789876"
                        nullable = true
                    },
                    navArgument("cardType") {
                        type = NavType.StringType
                        defaultValue = "barcode"
                        nullable = true
                    }
                )
            ) { entry ->
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                ShowCardActivity(
                    navController = navController,
                    shopName = entry.arguments?.getString("shopName"),
                    productId = entry.arguments?.getString("productId"),
                    cardType = entry.arguments?.getString("cardType"),
                    this@MainActivity,
                    connected
                )
            }
        }
    }

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private suspend fun checkConnection(): Boolean {
        val time = System.currentTimeMillis()
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        val syncWear = sharedPreferences.getBoolean("syncWear", true)
        return if (syncWear) {
            sendData(this, "check_connection|$time")
            var sec = 0
            while (!connected) {
                delay(1000L)
                sec++
                if (sec == 10) break
            }
            connected
        } else {
            false
        }
    }

    private fun onResult(state: Boolean) {
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        val syncWear = sharedPreferences.getBoolean("syncWear", true)
        if (syncWear) {
            if (state) {
                setContent { SplashActivity(true) }
                Handler(Looper.getMainLooper()).postDelayed({
//                    val time = System.currentTimeMillis()
//                    sendData(this, "give_me_cards|$time")
                    val time = System.currentTimeMillis()
                    sendData(this, "get_profile|$time")
                }, 500)
            } else {
                setContent { Navigation() }
            }
        } else {
            setContent { Navigation() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SplashActivity(false) }
        Handler(Looper.getMainLooper()).postDelayed({
            launch {
                val state = checkConnection()
                onResult(state)
            }
        }, 500)
    }

    lateinit var listState: ScalingLazyListState
    @Composable
    fun MainScreen(message: String?, navController: NavController) {
        var emptyCards = false
        if (message == "") emptyCards = true
        val list = message?.split("#")
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        val syncWear = sharedPreferences.getBoolean("syncWear", true)
        WearAppTheme {
            listState = rememberScalingLazyListState()
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

                ScalingLazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
//                    autoCentering = AutoCenteringParams(itemIndex = 0),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    state = listState
                ) {
                    if (syncWear) {
                        item { ButtonSync(contentModifier, iconModifier, this@MainActivity, connected) }
                    }
                    item { TextAppName() }
                    if (emptyCards) {
                        item { NewEmptyCard(contentModifier, iconModifier, "sync", this@MainActivity) }
                    } else {
                        var cardsCount = 0
                        list?.forEachIndexed { _, s ->
                            val currentShop = s.split("|")
                            var shopName = ""
                            var clientId = ""
                            var cardType = ""
                            currentShop.forEachIndexed { index, txt ->
                                when (index) {
                                    0 -> {shopName = txt}
                                    1 -> {clientId = txt}
                                    2 -> {cardType = txt}
                                }
                            }
                            if (cardType != "") {
                                cardsCount++
                                item { NewCard(contentModifier, iconModifier, cardType, shopName, clientId, navController) }
                            }
                        }
                        if (cardsCount == 0) {
                            item { NewEmptyCard(contentModifier, iconModifier, "empty", this@MainActivity) }
                        }
                    }
                    val sharedPreferences: SharedPreferences = this@MainActivity.getSharedPreferences(sharedPrefFile,
                        Context.MODE_PRIVATE)
                    val globalUsername = sharedPreferences.getString("userName", "").toString()
                    val globalEmail = sharedPreferences.getString("userEmail", "").toString()
//                    item { MainBottomButtons(firstItemModifier, iconModifier, navController, this@MainActivity, connected) }
                    item { MainBottomChipPrimary(iconModifier, this@MainActivity, connected) }
                    item { MainBottomChipSecondary(iconModifier, navController) }
                    item { MainBottomChipTertiary(iconModifier, globalUsername, globalEmail) }
                }
            }
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)) {
            runBlocking {
                val delta = -event.getAxisValue(MotionEventCompat.AXIS_SCROLL) *
                        ViewConfigurationCompat.getScaledVerticalScrollFactor(
                            ViewConfiguration.get(baseContext), baseContext
                        )
                if (delta < 127) {
                    listState.scrollBy(-100f)
                } else {
                    listState.scrollBy(100f)
                }
            }
        }
        return super.onGenericMotionEvent(event)
    }

    @Preview(
        widthDp = WEAR_PREVIEW_DEVICE_WIDTH_DP,
        heightDp = WEAR_PREVIEW_DEVICE_HEIGHT_DP,
        apiLevel = WEAR_PREVIEW_API_LEVEL,
        uiMode = WEAR_PREVIEW_UI_MODE,
        backgroundColor = WEAR_PREVIEW_BACKGROUND_COLOR_BLACK,
        showBackground = WEAR_PREVIEW_SHOW_BACKGROUND
    )
    @Composable
    fun WearAppPreview() {
        Navigation()
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)

        for (event in dataEventBuffer) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                if (datapath == path) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val message = dataMapItem.dataMap.getString("message")
                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
//                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    if (message != null) {
                        val commands = listOf(
                            "give_me_cards",
                            "add_card",
                            "open_card",
                            "check_connection",
                            "check_update",
                            "archive_card",
                            "delete_card"
                        )
                        for (command in commands) {
                            if (message.contains(command)) return
                        }
                        if (message.contains("connection_success")) {
                            connected = true; return
                        }
                    }
                    editor.putString("storedCards", message)
                    editor.apply()
                    Handler(Looper.getMainLooper()).postDelayed({
                        setContent { Navigation() }
                    }, 500)
                } else if (path == "/user_data"){
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val message = dataMapItem.dataMap.getString("message")
                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
                    val currentShop = message?.split("|")
                    currentShop?.forEachIndexed { index, txt ->
                        when (index) {
                            0 -> {editor.putString("userName",txt)}
                            1 -> {editor.putString("userEmail",txt)}
                        }
                    }
                    editor.apply()
                    val time = System.currentTimeMillis()
                    sendData(this, "give_me_cards|$time")
                } else {
                    Log.e(TAG, "Unrecognized path: $path")
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                Log.v(TAG, "Data deleted : " + event.dataItem.toString())
            } else {
                Log.e(TAG, "Unknown data event Type = " + event.type)
            }
        }
    }

    private fun loadBitmapFromAsset(asset: Asset): Bitmap? {
        // convert asset into a file descriptor and block until it's ready
        val assetInputStream: InputStream? =
            Tasks.await(Wearable.getDataClient(this).getFdForAsset(asset))
                ?.inputStream

        return assetInputStream?.let { inputStream ->
            // decode the stream into a bitmap
            BitmapFactory.decodeStream(inputStream)
        } ?: run {
            Log.w(TAG, "Requested an unknown Asset.")
            null
        }
    }

    companion object {
        fun sendData(context: Context, s: String) {
            val dataMap = PutDataMapRequest.create("/data_path")
            dataMap.dataMap.putString("message", s)
            val request = dataMap.asPutDataRequest()
            request.setUrgent()
            val dataItemTask = Wearable.getDataClient(context).putDataItem(request)
            dataItemTask
                .addOnSuccessListener { p0 ->
                    Log.d(TAG,"Sending message was successful: $p0")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG,"Sending message failed: $e")
                }
        }

        private const val TAG = "Wear MainActivity"
    }
}
