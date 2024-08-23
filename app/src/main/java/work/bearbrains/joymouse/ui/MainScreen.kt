package work.bearbrains.joymouse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    LazyColumn(
      modifier = modifier.padding(LayoutTokens.COLUMN_PADDING),
      verticalArrangement = Arrangement.spacedBy(LayoutTokens.COLUMN_ROW_SPACING),
    ) {
      item {
        Text(
          text = stringResource(id = R.string.operating_instructions_section_title),
          style = MaterialTheme.typography.headlineMedium
        )
      }
      item { Text(text = stringResource(id = R.string.operating_instructions)) }
      item { Text(text = stringResource(id = R.string.operating_instructions_primary_button)) }
      item { Text(text = stringResource(id = R.string.operating_instructions_toggle_chord)) }

      item {
        Text(
          text = stringResource(id = R.string.operating_instructions_actions_section_title),
          style = MaterialTheme.typography.headlineSmall
        )
      }

      item {
        Text(
          text =
            stringResource(id = R.string.operating_instructions_actions_section_unshifted_title),
          fontWeight = FontWeight.Bold,
        )
      }

      item { Text(text = stringResource(id = R.string.operating_instructions_action_back)) }
      item { Text(text = stringResource(id = R.string.operating_instructions_action_home)) }
      item { Text(text = stringResource(id = R.string.operating_instructions_action_start)) }
      item { Text(text = stringResource(id = R.string.operating_instructions_action_activate)) }

      item {
        Text(
          text =
            stringResource(id = R.string.operating_instructions_actions_section_left_shift_title),
          fontWeight = FontWeight.Bold,
        )
      }
      item {
        Text(text = stringResource(id = R.string.operating_instructions_action_display_backward))
      }
      item {
        Text(text = stringResource(id = R.string.operating_instructions_action_display_forward))
      }

      item {
        Text(
          text =
            stringResource(id = R.string.operating_instructions_actions_section_right_shift_title),
          fontWeight = FontWeight.Bold,
        )
      }

      item { Text(text = stringResource(id = R.string.operating_instructions_action_swipe_up)) }
      item { Text(text = stringResource(id = R.string.operating_instructions_action_swipe_down)) }
      item { Text(text = stringResource(id = R.string.operating_instructions_action_swipe_left)) }
      item { Text(text = stringResource(id = R.string.operating_instructions_action_swipe_right)) }
      item {
        Text(text = stringResource(id = R.string.operating_instructions_action_toggle_gesture))
      }
    }
  }
}
