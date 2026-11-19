package com.leticia.moodiva.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leticia.moodiva.R
import com.leticia.moodiva.presentation.components.MoodivaLogo
import com.leticia.moodiva.presentation.theme.Dimens
import com.leticia.moodiva.presentation.theme.MoodivaTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthRoute(viewModel: AuthViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AuthScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmationChange = viewModel::onConfirmationChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onToggleMode = viewModel::onToggleMode,
        onSubmit = viewModel::onSubmit,
    )
}

/** Login e cadastro locais na mesma tela (RF01, RF02). */
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = Dimens.ContentMaxWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.SectionSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            MoodivaLogo(large = true)
            Text(
                text = stringResource(R.string.auth_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(if (uiState.isSignUp) R.string.auth_sign_up_title else R.string.auth_sign_in_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = Dimens.MediumSpacing),
            )

            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.auth_email)) },
                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { { Text(stringResource(it)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth(),
            )

            PasswordField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.auth_password),
                error = uiState.passwordError,
                isVisible = uiState.isPasswordVisible,
                onToggleVisibility = onTogglePasswordVisibility,
                imeAction = if (uiState.isSignUp) ImeAction.Next else ImeAction.Done,
                onImeAction = { if (uiState.isSignUp) focusManager.moveFocus(FocusDirection.Down) else onSubmit() },
            )

            if (uiState.isSignUp) {
                PasswordField(
                    value = uiState.confirmation,
                    onValueChange = onConfirmationChange,
                    label = stringResource(R.string.auth_confirm_password),
                    error = uiState.confirmationError,
                    isVisible = uiState.isPasswordVisible,
                    onToggleVisibility = onTogglePasswordVisibility,
                    imeAction = ImeAction.Done,
                    onImeAction = onSubmit,
                )
            }

            uiState.formError?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    Text(
                        text = stringResource(message),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(Dimens.ContentPadding),
                    )
                }
            }

            Button(
                onClick = onSubmit,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = Dimens.ProgressStrokeWidth,
                        modifier = Modifier.size(Dimens.IconSmall),
                    )
                } else {
                    Text(stringResource(if (uiState.isSignUp) R.string.auth_sign_up_action else R.string.auth_sign_in_action))
                }
            }
            TextButton(onClick = onToggleMode) {
                Text(stringResource(if (uiState.isSignUp) R.string.auth_switch_to_sign_in else R.string.auth_switch_to_sign_up))
            }
            Text(
                text = stringResource(R.string.auth_local_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: Int?,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
        trailingIcon = {
            TextButton(onClick = onToggleVisibility) {
                Text(stringResource(if (isVisible) R.string.auth_hide_password else R.string.auth_show_password))
            }
        },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@PreviewLightDark
@Composable
private fun AuthSignInPreview() {
    MoodivaTheme {
        Surface {
            AuthScreen(
                uiState = AuthUiState(email = "ana@mail.com", formError = R.string.auth_error_invalid_credentials),
                onEmailChange = {}, onPasswordChange = {}, onConfirmationChange = {},
                onTogglePasswordVisibility = {}, onToggleMode = {}, onSubmit = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AuthSignUpPreview() {
    MoodivaTheme {
        Surface {
            AuthScreen(
                uiState = AuthUiState(isSignUp = true, passwordError = R.string.auth_error_password_short),
                onEmailChange = {}, onPasswordChange = {}, onConfirmationChange = {},
                onTogglePasswordVisibility = {}, onToggleMode = {}, onSubmit = {},
            )
        }
    }
}
