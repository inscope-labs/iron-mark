package com.inscopelabs.abx.ironmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inscopelabs.abx.ironmark.ui.screens.ConsoleScreen
import com.inscopelabs.abx.ironmark.ui.screens.JoinerScreen
import com.inscopelabs.abx.ironmark.ui.screens.OutputManagerScreen
import com.inscopelabs.abx.ironmark.ui.screens.ScriptStudioScreen
import com.inscopelabs.abx.ironmark.ui.screens.SplitterScreen
import com.inscopelabs.abx.ironmark.ui.theme.CyanPrimary
import com.inscopelabs.abx.ironmark.ui.theme.DarkBackground
import com.inscopelabs.abx.ironmark.ui.theme.DarkBorder
import com.inscopelabs.abx.ironmark.ui.theme.DarkSurface
import com.inscopelabs.abx.ironmark.ui.theme.IronMarkTheme
import com.inscopelabs.abx.ironmark.ui.theme.TextMuted
import com.inscopelabs.abx.ironmark.viewmodel.IronMarkViewModel

enum class IronMarkTab(val title: String, val icon: ImageVector) {
    SPLITTER("Splitter", Icons.Default.CallSplit),
    JOINER("Joiner", Icons.Default.MergeType),
    STUDIO("Studio", Icons.Default.Code),
    OUTPUTS("Outputs", Icons.Default.FolderZip),
    CONSOLE("Console", Icons.Default.Terminal)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IronMarkTheme {
                val viewModel: IronMarkViewModel = viewModel()
                var selectedTabIndex by remember { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_navigation_bar")
                        ) {
                            IronMarkTab.values().forEachIndexed { index, tab ->
                                val isSelected = selectedTabIndex == index
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedTabIndex = index },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DarkBackground,
                                        selectedTextColor = CyanPrimary,
                                        indicatorColor = CyanPrimary,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTabIndex) {
                            0 -> SplitterScreen(viewModel = viewModel)
                            1 -> JoinerScreen(viewModel = viewModel)
                            2 -> ScriptStudioScreen(viewModel = viewModel)
                            3 -> OutputManagerScreen(viewModel = viewModel)
                            4 -> ConsoleScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
