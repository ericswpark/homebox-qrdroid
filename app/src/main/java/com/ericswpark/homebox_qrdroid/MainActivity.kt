package com.ericswpark.homebox_qrdroid

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ericswpark.homebox_qrdroid.settings.SettingsActivity
import com.ericswpark.homebox_qrdroid.settings.SettingsRepository
import com.ericswpark.homebox_qrdroid.ui.theme.HomeboxqrdroidTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.io.OutputStream
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(this)
        enableEdgeToEdge()
        setContent {
            HomeboxqrdroidTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopBar()
                    }
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        settingsRepository = settingsRepository,
                        generateQrCode = ::generateQrCode,
                        saveQrCode = ::saveQrCodeToStorage
                    )
                }
            }
        }
    }

    private fun generateQrCode(content: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return bitmap
    }

    private fun saveQrCodeToStorage(bitmap: Bitmap, displayName: String) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HomeboxQRDroid")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            val out: OutputStream? = resolver.openOutputStream(it)
            out?.let {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.close()
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(it, values, null, null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(modifier: Modifier = Modifier, context: android.content.Context = LocalContext.current) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("homebox-qrdroid") },
        actions = {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More"
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                        showMenu = false
                    }
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    settingsRepository: SettingsRepository,
    generateQrCode: (String) -> Bitmap,
    saveQrCode: (Bitmap, String) -> Unit
) {
    var assetId by remember { mutableStateOf("") }
    var cableMode by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val homeboxServerUrl by settingsRepository.homeboxServerUrl.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextField(
            value = assetId,
            onValueChange = { newText ->
                if (newText.all { it.isDigit() || it == '-' || it == '.' }) {
                    assetId = newText
                }
            },
            label = { Text("Enter asset ID (e.g. 000-456)") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused) {
                        assetId = assetId.replace(".", "")
                    }
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        if (qrCodeBitmap != null) {
            Image(
                bitmap = qrCodeBitmap!!.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                contentDescription = "QR Code",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cable")
            Switch(
                checked = cableMode,
                onCheckedChange = { cableMode = it }
            )
        }
        Button(
            onClick = {
                coroutineScope.launch {
                    val url = homeboxServerUrl
                    if (!url.isNullOrBlank() && assetId.isNotBlank()) {
                        val qrContent = "$url/a/$assetId"
                        val bitmap = generateQrCode(qrContent)
                        qrCodeBitmap = bitmap
                        saveQrCode(bitmap, "homebox-qrdroid-$assetId.png")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate")
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun MainScreenPreview() {
//    HomeboxqrdroidTheme {
//        Scaffold(
//            modifier = Modifier.fillMaxSize(),
//            topBar = {
//                TopBar()
//            }
//        ) { innerPadding ->
//            MainScreen(modifier = Modifier.padding(innerPadding))
//        }
//    }
//}
