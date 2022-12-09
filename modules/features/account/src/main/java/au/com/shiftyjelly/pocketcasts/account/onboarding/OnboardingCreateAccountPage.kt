package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingCreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.EmailAndPasswordFields
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OnboardingCreateAccountPage(
    onBackPressed: () -> Unit,
    onAccountCreated: () -> Unit,
) {

    val viewModel = hiltViewModel<OnboardingCreateAccountViewModel>()
    val state by viewModel.stateFlow.collectAsState()

    LaunchedEffect(Unit) { viewModel.onShown() }
    BackHandler {
        viewModel.onBackPressed()
        onBackPressed()
    }

    val view = LocalView.current
    @Suppress("NAME_SHADOWING")
    val onAccountCreated = {
        UiUtil.hideKeyboard(view)
        onAccountCreated()
    }

    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.create_account),
            onNavigationClick = {
                viewModel.onBackPressed()
                onBackPressed()
            }
        )

        Column(
            Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {

            EmailAndPasswordFields(
                email = state.email,
                password = state.password,
                showEmailError = state.showEmailError,
                showPasswordError = state.showPasswordError,
                showPasswordErrorMessage = false,
                enabled = state.enableSubmissionFields,
                onDone = { viewModel.createAccount(onAccountCreated) },
                onUpdateEmail = viewModel::updateEmail,
                onUpdatePassword = viewModel::updatePassword,
                isCreatingAccount = true,
                modifier = Modifier.padding(16.dp)
            )

            TextP40(
                text = "• ${stringResource(LR.string.profile_create_password_requirements)}",
                color = if (state.showPasswordError) {
                    MaterialTheme.theme.colors.support05
                } else {
                    MaterialTheme.theme.colors.primaryText02
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            )

            state.serverErrorMessage?.let {
                TextP40(
                    text = it,
                    color = MaterialTheme.theme.colors.support05,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            RowButton(
                text = stringResource(LR.string.onboarding_create_account),
                enabled = state.enableSubmissionFields,
                onClick = { viewModel.createAccount(onAccountCreated) },
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingCreateAccountPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingCreateAccountPage(
            onBackPressed = {},
            onAccountCreated = {},
        )
    }
}
