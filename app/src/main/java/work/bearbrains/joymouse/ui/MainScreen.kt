package work.bearbrains.joymouse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import work.bearbrains.joymouse.R

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
  }
}
