package work.bearbrains.joymouse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import work.bearbrains.joymouse.R
import work.bearbrains.joymouse.input.ActionConfig
import work.bearbrains.joymouse.input.ShiftModifier

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  isServiceEnabled: Boolean = false,
  actionConfig: ActionConfig = ActionConfig.DEFAULT,
  onLaunchAccessibilitySettings: () -> Unit,
  onNavigateToConfig: () -> Unit = {},
) {
  val shiftButtonFriendly = ActionConfig.getFriendlyButtonName(actionConfig.shiftButton)
  val altButtonFriendly = ActionConfig.getFriendlyButtonName(actionConfig.altButton)

  Column(
    modifier = modifier.padding(LayoutTokens.COLUMN_PADDING),
    verticalArrangement = Arrangement.spacedBy(LayoutTokens.COLUMN_ROW_SPACING),
  ) {
    Spacer(modifier = Modifier.height(LayoutTokens.COLUMN_ROW_SPACING))

    ServiceStatusCard(
      isServiceEnabled = isServiceEnabled,
      onLaunchAccessibilitySettings = onLaunchAccessibilitySettings,
      onNavigateToConfig = onNavigateToConfig,
    )

    LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(LayoutTokens.COLUMN_ROW_SPACING),
    ) {
      item {
        Text(
          text = stringResource(id = R.string.operating_instructions_section_title),
          style = MaterialTheme.typography.headlineMedium
        )
      }
      item {
        Text(
          text = stringResource(id = R.string.operating_instructions, shiftButtonFriendly)
        )
      }
      item {
        Text(
          text =
            stringResource(
              id = R.string.operating_instructions_primary_button,
              altButtonFriendly,
            )
        )
      }
      item {
        Text(
          text =
            stringResource(
              id = R.string.operating_instructions_toggle_chord_template,
              ActionConfig.formatChord(actionConfig.toggleChord),
            )
        )
      }

      item {
        Text(
          text = stringResource(id = R.string.operating_instructions_actions_section_title),
          style = MaterialTheme.typography.headlineSmall
        )
      }

      // Group actions by modifier
      val unshiftedActions =
        ActionConfig.REMAPPABLE_ACTIONS.filter {
          actionConfig.actionBindings[it]?.modifier == ShiftModifier.NONE &&
            (actionConfig.actionBindings[it]?.keyCodes?.isNotEmpty() == true)
        }
      val shiftActions =
        ActionConfig.REMAPPABLE_ACTIONS.filter {
          actionConfig.actionBindings[it]?.modifier == ShiftModifier.SHIFT &&
            (actionConfig.actionBindings[it]?.keyCodes?.isNotEmpty() == true)
        }
      val altActions =
        ActionConfig.REMAPPABLE_ACTIONS.filter {
          actionConfig.actionBindings[it]?.modifier == ShiftModifier.ALT &&
            (actionConfig.actionBindings[it]?.keyCodes?.isNotEmpty() == true)
        }
      val dualShiftActions =
        ActionConfig.REMAPPABLE_ACTIONS.filter {
          actionConfig.actionBindings[it]?.modifier == ShiftModifier.ALTSHIFT &&
            (actionConfig.actionBindings[it]?.keyCodes?.isNotEmpty() == true)
        }

      if (unshiftedActions.isNotEmpty()) {
        item {
          Text(
            text =
              stringResource(
                id = R.string.operating_instructions_actions_section_unshifted_title,
                shiftButtonFriendly,
                altButtonFriendly,
              ),
            fontWeight = FontWeight.Bold,
          )
        }
        items(unshiftedActions) { action ->
          val binding = actionConfig.actionBindings[action]!!
          Text(
            text =
              stringResource(
                id = R.string.operating_instructions_action_item_template,
                ActionConfig.formatBinding(binding),
                ActionConfig.getActionHelpDescription(action),
              )
          )
        }
      }

      if (shiftActions.isNotEmpty()) {
        item {
          Text(
            text =
              stringResource(
                id = R.string.operating_instructions_actions_section_single_modifier_title,
                shiftButtonFriendly,
              ),
            fontWeight = FontWeight.Bold,
          )
        }
        items(shiftActions) { action ->
          val binding = actionConfig.actionBindings[action]!!
          Text(
            text =
              stringResource(
                id = R.string.operating_instructions_action_item_template,
                ActionConfig.formatBinding(binding),
                ActionConfig.getActionHelpDescription(action),
              )
          )
        }
      }

      if (altActions.isNotEmpty()) {
        item {
          Text(
            text =
              stringResource(
                id = R.string.operating_instructions_actions_section_single_modifier_title,
                altButtonFriendly,
              ),
            fontWeight = FontWeight.Bold,
          )
        }
        items(altActions) { action ->
          val binding = actionConfig.actionBindings[action]!!
          Text(
            text =
              stringResource(
                id = R.string.operating_instructions_action_item_template,
                ActionConfig.formatBinding(binding),
                ActionConfig.getActionHelpDescription(action),
              )
          )
        }
      }

      if (dualShiftActions.isNotEmpty()) {
        item {
          Text(
            text =
              stringResource(
                id = R.string.operating_instructions_actions_section_dual_modifier_title,
                shiftButtonFriendly,
                altButtonFriendly,
              ),
            fontWeight = FontWeight.Bold,
          )
        }
        items(dualShiftActions) { action ->
          val binding = actionConfig.actionBindings[action]!!
          Text(
            text =
              stringResource(
                id = R.string.operating_instructions_action_item_template,
                ActionConfig.formatBinding(binding),
                ActionConfig.getActionHelpDescription(action),
              )
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(LayoutTokens.COLUMN_ROW_SPACING))
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceStatusCard(
  isServiceEnabled: Boolean,
  onLaunchAccessibilitySettings: () -> Unit,
  onNavigateToConfig: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isServiceEnabled) {
    Card(
      modifier = modifier.fillMaxWidth(),
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
      FlowRow(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = LayoutTokens.COLUMN_PADDING, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = stringResource(id = R.string.service_status_enabled),
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.align(Alignment.CenterVertically),
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(vertical = 4.dp),
        ) {
          Button(
            onClick = onNavigateToConfig,
            modifier = Modifier.align(Alignment.CenterVertically),
          ) {
            Text(stringResource(id = R.string.button_configure_actions))
          }
          OutlinedButton(
            onClick = onLaunchAccessibilitySettings,
            modifier = Modifier.align(Alignment.CenterVertically),
          ) {
            Text(stringResource(id = R.string.button_manage_accessibility_settings))
          }
        }
      }
    }
  } else {
    Card(
      modifier = modifier.fillMaxWidth(),
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
      Column(
        modifier = Modifier.padding(LayoutTokens.COLUMN_PADDING),
        verticalArrangement = Arrangement.spacedBy(LayoutTokens.COLUMN_ROW_SPACING),
      ) {
        Text(
          text = stringResource(id = R.string.enable_service_instructions),
          style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(onClick = onLaunchAccessibilitySettings) {
            Text(stringResource(id = R.string.button_open_accessibility_settings))
          }
          Spacer(modifier = Modifier.padding(horizontal = 4.dp))
          OutlinedButton(onClick = onNavigateToConfig) {
            Text(stringResource(id = R.string.button_configure_actions))
          }
        }
      }
    }
  }
}
