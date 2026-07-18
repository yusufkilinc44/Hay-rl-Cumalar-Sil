package com.hayirlicumalarsil

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hayirlicumalarsil.scan.ReminderScheduler
import com.hayirlicumalarsil.scan.ScanState
import com.hayirlicumalarsil.ui.screens.HomeScreen
import com.hayirlicumalarsil.ui.screens.OnboardingScreen
import com.hayirlicumalarsil.ui.screens.ResultsScreen
import com.hayirlicumalarsil.ui.screens.SettingsScreen
import com.hayirlicumalarsil.ui.screens.StatsScreen
import com.hayirlicumalarsil.ui.theme.HcsTheme

class MainActivity : ComponentActivity() {

    private var autoScanRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Cumartesi hatırlatıcısını kur (varsa yeniden kurar, zararsız).
        ReminderScheduler.schedule(this)
        autoScanRequested = intent?.getBooleanExtra(EXTRA_AUTO_SCAN, false) == true

        setContent {
            val vm: ScanViewModel = viewModel()
            val dark by vm.themeDark.collectAsStateWithLifecycle()
            val onboardingDone by vm.onboardingDone.collectAsStateWithLifecycle()
            HcsTheme(darkTheme = dark) {
                if (!onboardingDone) {
                    OnboardingScreen(onDone = { vm.completeOnboarding() })
                } else {
                    MainScreen(
                        vm = vm,
                        autoScan = autoScanRequested,
                        onAutoScanHandled = { autoScanRequested = false },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_AUTO_SCAN, false)) autoScanRequested = true
    }

    companion object {
        const val EXTRA_AUTO_SCAN = "auto_scan"
    }
}

private data class TabItem(val title: String, val icon: Int)

private val Tabs = listOf(
    TabItem("Ana Sayfa", R.drawable.ic_home),
    TabItem("Sonuçlar", R.drawable.ic_photo),
    TabItem("İstatistik", R.drawable.ic_bar_chart),
    TabItem("Ayarlar", R.drawable.ic_settings),
)

private fun hasMediaPermission(context: Context): Boolean {
    val perm = when {
        Build.VERSION.SDK_INT >= 33 -> Manifest.permission.READ_MEDIA_IMAGES
        else -> Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun MainScreen(
    vm: ScanViewModel = viewModel(),
    autoScan: Boolean = false,
    onAutoScanHandled: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val state by vm.state.collectAsStateWithLifecycle()
    val candidates by vm.candidates.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Bildirimden "Evet, tara" ile açıldıysa izin varsa taramayı başlat.
    LaunchedEffect(autoScan) {
        if (autoScan) {
            if (hasMediaPermission(context)) vm.startScan()
            onAutoScanHandled()
        }
    }

    // Geri tuşu: ana sayfada değilsek uygulamadan çıkmak yerine ana sayfaya dön.
    BackHandler(enabled = selectedTab != 0) { selectedTab = 0 }

    // Tarama bitince otomatik olarak sonuçlar sekmesine geç.
    var wasScanning by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state is ScanState.Scanning) {
            wasScanning = true
        } else if (wasScanning) {
            wasScanning = false
            if (state is ScanState.Results && candidates.isNotEmpty()) selectedTab = 1
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        icon = {
                            if (index == 1 && candidates.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError,
                                        ) { Text(candidates.size.toString()) }
                                    }
                                ) {
                                    Icon(painterResource(tab.icon), contentDescription = tab.title)
                                }
                            } else {
                                Icon(painterResource(tab.icon), contentDescription = tab.title)
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it / 6 * direction } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 6 * direction } + fadeOut())
            },
            label = "tabContent",
        ) { tab ->
            when (tab) {
                0 -> HomeScreen(vm = vm, onGoResults = { selectedTab = 1 })
                1 -> ResultsScreen(vm = vm)
                2 -> StatsScreen(vm = vm)
                else -> SettingsScreen(vm = vm)
            }
        }
    }
}
