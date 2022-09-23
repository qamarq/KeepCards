package com.qamarq.keepcards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.qamarq.keepcards.theme.WearAppTheme
import java.util.*

@Composable
fun ShowCardActivity(navController: NavController, shopName: String?, productId: String?, cardType: String?, context: Context, phoneConnected: Boolean) {
    var globalBitmap: Bitmap? = null

    if (cardType == "barcode") {
        try {
            val hintMap = Hashtable<EncodeHintType, ErrorCorrectionLevel>()
            hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L
            val codeWriter: com.google.zxing.Writer
            codeWriter = Code128Writer()
            val byteMatrix: BitMatrix =
                codeWriter.encode(productId, BarcodeFormat.CODE_128, 800, 300, hintMap)
            val width = byteMatrix.width
            val height = byteMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (i in 0 until width) {
                for (j in 0 until height) {
                    bitmap.setPixel(i, j, if (byteMatrix[i, j]) Color.BLACK else Color.WHITE)
                }
            }
            globalBitmap = bitmap
        } catch (e: Exception) {
            Log.d("fsdfdsfdsfds", "Errrorr")
        }
    } else {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(productId, BarcodeFormat.QR_CODE, 400, 400)

            val w = bitMatrix.width
            val h = bitMatrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    pixels[y * w + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }

            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h)

            globalBitmap = bitmap
        } catch (e: Exception) {
            Log.d("fsdfdsfdsfds", "Errrorr")
        }
    }

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
                item {
                    if (shopName != null) {
                        ShopNameText(firstItemModifier, shopName)
                    }
                }
                item {
                    if (globalBitmap != null) {
                        BarcodeCard(globalBitmap)
                    }
                }
//                item { ButtonBackShowCard(
//                    lastItemModifier,
//                    iconModifier,
//                    navController,
//                    context,
//                    shopName.toString(),
//                    productId.toString(),
//                    cardType.toString(),
//                    phoneConnected
//                ) }
                item { ShowCardButtonOpenPhone(iconModifier, context, shopName.toString(), productId.toString(), cardType.toString(), phoneConnected) }
                item { ShowCardButtonArchive(iconModifier, context, shopName.toString(), productId.toString(), cardType.toString(), navController) }
                item { ShowCardButtonDelete(iconModifier, context, productId.toString(), navController) }
//                item { ShowCardButtonBack(iconModifier, navController) }
            }
        }
    }
}