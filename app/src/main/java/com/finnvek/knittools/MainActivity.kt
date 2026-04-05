package com.finnvek.knittools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.ui.navigation.KnitToolsNavHost
import com.finnvek.knittools.ui.screens.settings.SettingsViewModel
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val prefs by settingsViewModel.preferences.collectAsStateWithLifecycle()

            KnitToolsTheme(themeMode = prefs.themeMode) {
                val isDark =
                    when (prefs.themeMode) {
                        ThemeMode.DARK -> true
                        ThemeMode.LIGHT -> false
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    }

                val noiseModifier =
                    if (isDark) {
                        val noiseBitmap = ImageBitmap.imageResource(R.drawable.noise_texture)
                        val noiseBrush =
                            ShaderBrush(
                                ImageShader(noiseBitmap, TileMode.Repeated, TileMode.Repeated),
                            )
                        Modifier.drawWithContent {
                            drawContent()
                            drawRect(
                                brush = noiseBrush,
                                alpha = 0.025f,
                                blendMode = BlendMode.Overlay,
                            )
                        }
                    } else {
                        Modifier
                    }

                Surface(modifier = Modifier.fillMaxSize().then(noiseModifier)) {
                    KnitToolsNavHost()
                }
            }
        }
    }
}
