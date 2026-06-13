package com.example.dianpinghelper

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.content.getSystemService
import com.example.dianpinghelper.data.SettingsRepository
import com.example.dianpinghelper.model.ShopInfo
import com.example.dianpinghelper.ui.screen.*
import com.example.dianpinghelper.ui.theme.DianpingHelperTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val settingsRepo: SettingsRepository get() = DianpingApp.instance.settingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查是否通过分享进来的
        val sharedText = parseSharedText(intent)

        setContent {
            DianpingHelperTheme {
                MainContent(
                    initialSharedText = sharedText,
                    settingsRepo = settingsRepo,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 重新处理分享文本：重启 Activity 以刷新状态
        val sharedText = parseSharedText(intent)
        if (sharedText != null) {
            // 简单处理：重启 Activity
            intent.removeExtra(Intent.EXTRA_TEXT)
            recreate()
        }
    }

    private fun parseSharedText(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            return intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        return null
    }
}

/**
 * App 主内容（页面路由 + 状态管理）
 */
@Composable
private fun MainContent(
    initialSharedText: String?,
    settingsRepo: SettingsRepository,
) {
    // 页面路由
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    // 当前待处理的文本
    var currentText by remember { mutableStateOf(initialSharedText ?: "") }
    // 当前待收藏的店铺
    var currentShop by remember { mutableStateOf<ShopInfo?>(null) }
    // 收藏模式
    var currentMode by remember { mutableStateOf(FavoriteMode.WEBVIEW) }
    // 收藏历史
    val history by settingsRepo.favoriteHistoryFlow.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    // 如果有分享文本，直接进入提取页
    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            currentText = initialSharedText
            currentScreen = Screen.Extract
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    // ── 页面路由 ──
    when (currentScreen) {
        Screen.Home -> {
            HomeScreen(
                history = history,
                onPasteClipboard = {
                    val clipboard = context.getSystemService<ClipboardManager>()
                    val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                    if (clipText.isNotBlank()) {
                        currentText = clipText
                        currentScreen = Screen.Extract
                    }
                },
                onManualInput = {
                    currentText = ""
                    currentScreen = Screen.ManualInput
                },
                onItemClick = { shop ->
                    currentShop = shop
                    currentMode = FavoriteMode.WEBVIEW
                    currentScreen = Screen.WebView
                },
                onItemDelete = { index ->
                    scope.launch { settingsRepo.removeFavorite(index) }
                },
                onReceiveShare = { text ->
                    currentText = text
                    currentScreen = Screen.Extract
                },
            )
        }

        Screen.ManualInput -> {
            ManualInputScreen(
                initialText = currentText,
                onConfirm = { text ->
                    currentText = text
                    currentScreen = Screen.Extract
                },
                onBack = { currentScreen = Screen.Home },
            )
        }

        Screen.Extract -> {
            ExtractScreen(
                initialText = currentText,
                onConfirm = { shop, mode ->
                    currentShop = shop
                    currentMode = mode
                    currentScreen = Screen.WebView
                },
                onBack = { currentScreen = Screen.Home },
            )
        }

        Screen.WebView -> {
            val shop = currentShop
            if (shop != null) {
                WebScreen(
                    shop = shop,
                    mode = currentMode,
                    onBack = { currentScreen = Screen.Extract },
                    onSuccess = {
                        // 保存收藏记录
                        scope.launch { settingsRepo.addFavorite(shop) }
                        currentScreen = Screen.Home
                    },
                )
            } else {
                LaunchedEffect(Unit) { currentScreen = Screen.Home }
            }
        }
    }
}

/** 页面枚举 */
private enum class Screen {
    Home,
    ManualInput,
    Extract,
    WebView,
}
