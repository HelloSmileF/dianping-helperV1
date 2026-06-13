package com.example.dianpinghelper.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dianpinghelper.extractor.ShopInfoExtractor
import com.example.dianpinghelper.model.ShopInfo
import com.example.dianpinghelper.ui.theme.SuccessGreen
import com.example.dianpinghelper.ui.theme.WarningOrange

/**
 * 提取结果确认页
 *
 * @param initialText 用户输入的原始文本
 * @param onConfirm 用户确认后执行收藏
 * @param onBack 返回
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractScreen(
    initialText: String,
    onConfirm: (ShopInfo, mode: FavoriteMode) -> Unit,
    onBack: () -> Unit,
) {
    // 提取结果
    var shopInfo by remember { mutableStateOf(ShopInfoExtractor.extract(initialText)) }
    var editedName by remember { mutableStateOf(shopInfo?.name ?: "") }
    var editedAddress by remember { mutableStateOf(shopInfo?.address ?: "") }
    var showManualInput by remember { mutableStateOf(shopInfo == null) }
    var selectedMode by remember { mutableStateOf(FavoriteMode.WEBVIEW) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("确认店铺信息") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── 未提取到信息 ──
            if (shopInfo == null && !showManualInput) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = WarningOrange,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "未自动提取到店铺信息",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "请手动输入店铺名称",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showManualInput = true }) {
                            Text("手动输入")
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onBack) {
                            Text("返回修改文本")
                        }
                    }
                }
                return@Scaffold
            }

            // ── 店铺信息卡片 ──
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "店铺信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(16.dp))

                    // 店铺名称
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("店铺名称 *") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(Modifier.height(12.dp))

                    // 地址
                    OutlinedTextField(
                        value = editedAddress,
                        onValueChange = { editedAddress = it },
                        label = { Text("地址（可选）") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )

                    // 来源信息
                    if (shopInfo != null) {
                        val platform = ShopInfoExtractor.detectPlatform(shopInfo!!.sourceText)
                        if (platform != null) {
                            Spacer(Modifier.height(8.dp))
                            AssistChip(
                                onClick = {},
                                label = { Text("来源: $platform") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 收藏方式选择 ──
            Text(
                "收藏方式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))

            // WebView 模式（内嵌网页）
            ModeCard(
                selected = selectedMode == FavoriteMode.WEBVIEW,
                onClick = { selectedMode = FavoriteMode.WEBVIEW },
                icon = Icons.Default.Public,
                title = "WebView 自动收藏",
                description = "在 App 内打开大众点评网页版，自动搜索并收藏",
                badge = "推荐",
            )

            Spacer(Modifier.height(8.dp))

            // App 跳转模式
            ModeCard(
                selected = selectedMode == FavoriteMode.APP,
                onClick = { selectedMode = FavoriteMode.APP },
                icon = Icons.Default.OpenInNew,
                title = "跳转大众点评 App",
                description = "跳转到大众点评 App 搜索店铺，需要手动点击收藏",
            )

            Spacer(Modifier.height(24.dp))

            // ── 确认按钮 ──
            Button(
                onClick = {
                    val info = ShopInfo(
                        name = editedName.trim(),
                        address = editedAddress.trim(),
                    )
                    onConfirm(info, selectedMode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = editedName.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "加入收藏夹",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    badge: String? = null,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Medium)
                    if (badge != null) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SuccessGreen.copy(alpha = 0.15f),
                        ) {
                            Text(
                                badge,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessGreen,
                                fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.85f,
                            )
                        }
                    }
                }
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/** 收藏模式 */
enum class FavoriteMode {
    WEBVIEW,  // App 内 WebView 自动收藏
    APP,      // 跳转大众点评 App
}
