package work.bearbrains.joymouse

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi

private data class Permission(
    val androidPermission: String,
    @DrawableRes val icon: Int,
    @StringRes val iconDescription: Int,
    @StringRes val label: Int,
    @StringRes val description: Int
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    overlayEnabled: Boolean,
    modifier: Modifier = Modifier,
    onLaunchAccessibilitySettings: () -> Unit,
    onEnableOverlay: () -> Unit
) {
  Column(
      modifier = modifier.padding(LayoutTokens.COLUMN_PADDING),
      verticalArrangement = Arrangement.spacedBy(LayoutTokens.COLUMN_ROW_SPACING),
  ) {
    Spacer(modifier = Modifier.height(LayoutTokens.COLUMN_ROW_SPACING))

    Text(
        text = stringResource(id = R.string.enable_service_instructions),
        style = MaterialTheme.typography.titleMedium,
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
      Button(onClick = { onLaunchAccessibilitySettings() }) {
        Text(stringResource(id = R.string.button_open_accessibility_settings))
      }
    }

    Text(
      text = stringResource(id = R.string.permission_header),
      style = MaterialTheme.typography.titleMedium,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          painter = painterResource(id = R.drawable.highlight_mouse_cursor_24px),
          contentDescription = stringResource(R.string.icon_description_mouse_overlay),
          modifier = Modifier.size(24.dp),
      )

      Spacer(modifier = Modifier.width(8.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = stringResource(id = R.string.permission_manage_overlay_label),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(id = R.string.permission_manage_overlay_description),
            style = MaterialTheme.typography.bodySmall,
        )
      }

      Checkbox(
          checked = overlayEnabled,
          onCheckedChange = { onLaunchAccessibilitySettings() },
          colors = CheckboxDefaults.colors(uncheckedColor = Color.Gray),
          modifier = Modifier.padding(horizontal = LayoutTokens.BORDER_SIZE),
      )
    }
  }
}
