package com.ericswpark.homebox_qrdroid

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.ericswpark.homebox_qrdroid.settings.SettingsActivity
import com.ericswpark.homebox_qrdroid.settings.SettingsRepository
import com.ericswpark.homebox_qrdroid.ui.theme.HomeboxqrdroidTheme
import com.ericswpark.homebox_qrdroid.utils.generateQrCode
import com.ericswpark.homebox_qrdroid.utils.saveQrCodeToStorage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(this)
        enableEdgeToEdge()
        setContent {
            HomeboxqrdroidTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(), topBar = {
                        TopBar()
                    }) { innerPadding ->
                    val homeboxServerUrl by settingsRepository.homeboxServerUrl.collectAsState(
                        initial = null
                    )
                    val trimQrCodeQuietZone by settingsRepository.trimQrCodeQuietZone.collectAsState(
                        initial = false
                    )
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        homeboxServerUrl = homeboxServerUrl,
                        trimQrCodeQuietZone = trimQrCodeQuietZone,
                        generateQrCode = ::generateQrCode,
                        saveQrCode = { bitmap, displayName ->
                            saveQrCodeToStorage(this, bitmap, displayName)
                        })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(modifier: Modifier = Modifier, context: Context = LocalContext.current) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("homebox-qrdroid") }, actions = {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    imageVector = Icons.Default.MoreVert, contentDescription = "More"
                )
            }
            DropdownMenu(
                expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Settings") }, onClick = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                    showMenu = false
                })
            }
        }, modifier = modifier
    )
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    homeboxServerUrl: String?,
    trimQrCodeQuietZone: Boolean,
    generateQrCode: (String, String, Boolean) -> Bitmap,
    saveQrCode: (Bitmap, String) -> Unit
) {
    var assetId by remember { mutableStateOf("") }
    var cableMode by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(), contentAlignment = Alignment.Center
            ) {
                QRCodePlaceholder()
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cable")
            Switch(
                checked = cableMode, onCheckedChange = { cableMode = it })
        }
        Button(
            onClick = {
                coroutineScope.launch {
                    if (!homeboxServerUrl.isNullOrBlank() && assetId.isNotBlank()) {
                        val assetIdDigits = assetId.filter { it.isDigit() }
                        val paddedAssetId = assetIdDigits.padStart(6, '0')
                        val formattedAssetId =
                            "${paddedAssetId.take(3)}-${paddedAssetId.substring(3)}"
                        val qrContent = "$homeboxServerUrl/a/$formattedAssetId"
                        val bitmap = generateQrCode(qrContent, formattedAssetId, trimQrCodeQuietZone)

                        val finalBitmap = if (cableMode) {
                            val padding = 80
                            val newBitmap = createBitmap(
                                bitmap.width * 2 + padding,
                                bitmap.height,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = android.graphics.Canvas(newBitmap)
                            canvas.drawColor(Color.WHITE)
                            canvas.drawBitmap(bitmap, 0f, 0f, null)
                            canvas.drawBitmap(bitmap, (bitmap.width + padding).toFloat(), 0f, null)
                            newBitmap
                        } else {
                            bitmap
                        }

                        qrCodeBitmap = finalBitmap
                        saveQrCode(finalBitmap, "label-$formattedAssetId.png")
                    }
                }
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate")
        }
    }
}

@Composable
fun QRCodePlaceholder() {
    Icon(
        imageVector = Icons.Default.QrCode,
        contentDescription = "QR Code",
        modifier = Modifier.fillMaxSize(0.8f),
        tint = LocalContentColor.current.copy(alpha = 0.4f)
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    HomeboxqrdroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(), topBar = {
                TopBar()
            }) { innerPadding ->
            MainScreen(
                modifier = Modifier.padding(innerPadding),
                homeboxServerUrl = "https://homebox.example.com",
                trimQrCodeQuietZone = false,
                generateQrCode = { _, _, _ -> createBitmap(512, 512, Bitmap.Config.ARGB_8888) },
                saveQrCode = { _, _ -> })
        }
    }
}
