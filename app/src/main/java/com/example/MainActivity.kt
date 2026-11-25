package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.MontanteDatabase
import com.example.data.repository.MontanteRepository
import com.example.ui.screens.AnalyzerScreen
import com.example.ui.screens.DailyPicksScreen
import com.example.ui.screens.MontanteTrackerScreen
import com.example.ui.theme.MontanteStatTheme
import com.example.ui.viewmodel.MontanteViewModel

private const val ROUTE_PICKS = "daily_picks"
private const val ROUTE_MONTANTES = "montantes"
private const val ROUTE_ANALYZER = "analyzer"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navDestinations = listOf(
    NavDestinationItem(
        route = ROUTE_PICKS,
        labelRes = R.string.nav_daily_picks,
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
        testTag = "nav_picks"
    ),
    NavDestinationItem(
        route = ROUTE_MONTANTES,
        labelRes = R.string.nav_montantes,
        selectedIcon = Icons.AutoMirrored.Filled.TrendingUp,
        unselectedIcon = Icons.AutoMirrored.Outlined.TrendingUp,
        testTag = "nav_montantes"
    ),
    NavDestinationItem(
        route = ROUTE_ANALYZER,
        labelRes = R.string.nav_analyzer,
        selectedIcon = Icons.Filled.Functions,
        unselectedIcon = Icons.Outlined.Functions,
        testTag = "nav_analyzer"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = MontanteDatabase.getInstance(applicationContext)
        val repository = MontanteRepository(db.montanteDao())

        setContent {
            MontanteStatTheme(darkTheme = true) {
                val viewModel: MontanteViewModel = viewModel(
                    factory = MontanteViewModel.Factory(repository)
                )
                MontanteAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MontanteAppRoot(viewModel: MontanteViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_PICKS

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val dailyState by viewModel.dailyPicksState.collectAsStateWithLifecycle()
    val tierFilter by viewModel.tierFilter.collectAsStateWithLifecycle()
    val sportFilter by viewModel.sportFilter.collectAsStateWithLifecycle()
    val campaigns by viewModel.campaigns.collectAsStateWithLifecycle()
    val activeCampaign by viewModel.activeCampaign.collectAsStateWithLifecycle()
    val stepsByCampaign by viewModel.stepsByCampaign.collectAsStateWithLifecycle()
    val customInput by viewModel.customInput.collectAsStateWithLifecycle()
    val customResult by viewModel.customResult.collectAsStateWithLifecycle()
    val simulatorParams by viewModel.simulatorParams.collectAsStateWithLifecycle()
    val simulatedSteps by viewModel.simulatedSteps.collectAsStateWithLifecycle()

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_PICKS,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_PICKS) {
                        DailyPicksScreen(
                            uiState = dailyState,
                            tierFilter = tierFilter,
                            sportFilter = sportFilter,
                            activeCampaign = activeCampaign,
                            onTierFilterSelected = viewModel::setTierFilter,
                            onSportFilterSelected = viewModel::setSportFilter,
                            onRefresh = viewModel::refreshDailyMatches,
                            onEngageOption = { match, option, stake, odds ->
                                viewModel.engageOptionInMontante(match, option, stake, odds)
                            },
                            onNavigateToAnalyzer = { navigateTo(ROUTE_ANALYZER) }
                        )
                    }

                    composable(ROUTE_MONTANTES) {
                        MontanteTrackerScreen(
                            campaigns = campaigns,
                            activeCampaign = activeCampaign,
                            stepsByCampaign = stepsByCampaign,
                            simulatorParams = simulatorParams,
                            simulatedSteps = simulatedSteps,
                            onSelectCampaign = viewModel::selectCampaign,
                            onCreateCampaign = viewModel::createCampaign,
                            onDeleteCampaign = viewModel::deleteCampaign,
                            onAddManualStep = { campaignId, matchTitle, marketLabel, odds, prob, stake ->
                                viewModel.addManualStepToCampaign(
                                    campaignId = campaignId,
                                    matchTitle = matchTitle,
                                    marketLabel = marketLabel,
                                    odds = odds,
                                    probability = prob,
                                    stake = stake
                                )
                            },
                            onResolveStep = viewModel::resolveStep,
                            onUpdateSimulator = viewModel::updateSimulatorParams
                        )
                    }

                    composable(ROUTE_ANALYZER) {
                        AnalyzerScreen(
                            input = customInput,
                            result = customResult,
                            onUpdateInput = viewModel::updateCustomInput,
                            onEngageCustom = { matchTitle, marketLabel, odds, prob ->
                                viewModel.engageCustomPickInMontante(
                                    matchTitle = matchTitle,
                                    marketLabel = marketLabel,
                                    odds = odds,
                                    probability = prob
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
