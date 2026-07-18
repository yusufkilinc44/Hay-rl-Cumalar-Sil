package com.hayirlicumalarsil

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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.hayirlicumalarsil.scan.ScanState
import com.hayirlicumalarsil.ui.screens.HomeScreen
import com.hayirlicumalarsil.ui.screens.ResultsScreen
import com.hayirlicumalarsil.ui.screens.SettingsScreen
import com.hayirlicumalarsil.ui.screens.StatsScreen
import com.hayirlicumalarsil.ui.theme.HcsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HcsTheme {
                MainScreen()
            }
        }
    }
}

private data class TabItem(val title: String, val icon: Int)

private val Tabs = listOf(
    TabItem("Ana Sayfa", R.drawable.ic_home),
    TabItem("Sonuçlar", R.drawable.ic_photo),
    TabItem("İstatistik", R.drawable.ic_bar_chart),
    TabItem("Ayarlar", R.drawable.ic_settings),
)

@Composable
fun MainScreen(vm: ScanViewModel = viewModel()) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val state by vm.state.collectAsStateWithLifecycle()
    val candidates by vm.candidates.collectAsStateWithLifecycle()

    // Geri tuşu: ana sayfada değilsek uygulamadan çıkmak yerine ana sayfaya dön.
    BackHandler(enabled = selectedTab != 0) { selectedTab = 0 }

    // Tarama bitince otomatik olarak sonuçlar sekmesine geç
    var wasScanning by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state is ScanState.Scanning) {
            wasScanning = true
        } else if (wasScanning) {
            wasScanning = false
            if (state is ScanState.Results && candidates.isNotEmpty()) selectedTab = 1
        }
    }

    // Sonuçlar sekmesine girince (ya da oradayken aday sayısı değişince) o an
    // mevcut tüm adayları seçili yap.
    LaunchedEffect(selectedTab, candidates.size) {
        if (selectedTab == 1 && candidates.isNotEmpty()) vm.selectAll()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(tab.title) },
                        icon = {
                            if (index == 1 && candidates.isNotEmpty()) {
                                BadgedBox(
                                    badge = { Badge { Text(candidates.size.toString()) } }
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
