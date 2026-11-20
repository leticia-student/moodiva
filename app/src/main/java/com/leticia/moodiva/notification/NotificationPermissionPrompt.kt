package com.leticia.moodiva.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.leticia.moodiva.R
import org.koin.compose.koinInject

/**
 * Pedido da permissão de notificação no Android 13+ (RF24, RN35).
 *
 * Antes do pedido do sistema, um diálogo explica o motivo. "Agora não" ou negar não
 * mudam nada no app, e a pergunta não se repete. Abaixo do Android 13 a permissão
 * não existe (é concedida na instalação), então nada aparece.
 */
@Composable
fun NotificationPermissionPrompt(prefs: NotificationPermissionPrefs = koinInject()) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val alreadyGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    var visible by remember { mutableStateOf(!alreadyGranted && !prefs.wasAsked) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    if (!visible) return
    val close = {
        prefs.wasAsked = true
        visible = false
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(stringResource(R.string.chat_notification_title)) },
        text = { Text(stringResource(R.string.chat_notification_text)) },
        confirmButton = {
            TextButton(
                onClick = {
                    close()
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
            ) { Text(stringResource(R.string.chat_notification_allow)) }
        },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.chat_notification_deny)) } },
    )
}
