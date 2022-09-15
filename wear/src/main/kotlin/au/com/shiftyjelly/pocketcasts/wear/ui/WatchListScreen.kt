package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.theme.theme
import au.com.shiftyjelly.pocketcasts.wear.ui.podcasts.PodcastsScreen
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.profile.R as PR

object WatchListScreen {
    const val route = "watch_list_screen"
}

@Composable
fun WatchListScreen(navController: NavHostController?) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = stringResource(LR.string.app_name)
            )
        }

        item {
            WatchListChip(
                titleRes = LR.string.player_tab_playing_wide,
                iconRes = IR.drawable.ic_play_all,
                secondaryLabel = "A Really Long Podcast Name", // TODO
                onClick = { navController?.navigate(NowPlayingScreen.route) },
            )
        }

        item {
            UpNextChip(
                navController = navController,
                numInUpNext = 100
            )
        }

        item {
            WatchListChip(
                titleRes = LR.string.podcasts,
                iconRes = IR.drawable.ic_podcasts,
                onClick = { navController?.navigate(PodcastsScreen.route) }
            )
        }

        item {
            WatchListChip(
                titleRes = LR.string.filters,
                iconRes = IR.drawable.ic_filters,
                onClick = { navController?.navigate(FiltersScreen.route) }
            )
        }

        item {
            WatchListChip(
                titleRes = LR.string.downloads,
                iconRes = IR.drawable.ic_download,
                onClick = { navController?.navigate(DownloadsScreen.route) }
            )
        }

        item {
            WatchListChip(
                titleRes = LR.string.profile_navigation_files,
                iconRes = PR.drawable.ic_file,
                onClick = { navController?.navigate(FilesScreen.route) }
            )
        }
    }
}

@Composable
private fun WatchListChip(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    secondaryLabel: String? = null,
    onClick: () -> Unit
) {
    val title = stringResource(titleRes)
    Chip(
        onClick = onClick,
        colors = ChipDefaults.chipColors(),
        label = {
            Text(title)
        },
        secondaryLabel = {
            if (secondaryLabel != null) {
                Text(
                    text = secondaryLabel,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun UpNextChip(navController: NavHostController?, numInUpNext: Int) {
    val title = stringResource(LR.string.up_next)
    Chip(
        onClick = { navController?.navigate(UpNextScreen.route) },
        colors = ChipDefaults.chipColors(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight()
        ) {
            Box(
                modifier = Modifier.wrapContentSize(align = Alignment.Center),
                content = {
                    Icon(
                        painter = painterResource(IR.drawable.ic_upnext),
                        contentDescription = title
                    )
                }
            )

            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.theme.colors.primaryIcon02Active)
            ) {
                val num = if (numInUpNext < 100) numInUpNext.toString() else "99+"
                Text(
                    text = num,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
private fun WatchListPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    WearAppTheme(themeType) {
        WatchListScreen(navController = null)
    }
}
