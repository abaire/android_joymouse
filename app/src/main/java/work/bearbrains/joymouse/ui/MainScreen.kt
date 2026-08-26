package work.bearbrains.joymouse.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import work.bearbrains.joymouse.R
import work.bearbrains.joymouse.input.ActionConfig
import work.bearbrains.joymouse.input.JoystickAction

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  isServiceEnabled: Boolean = false,
  actionConfig: ActionConfig = ActionConfig.DEFAULT,
  onLaunchAccessibilitySettings: () -> Unit,
  onNavigateToConfig: () -> Unit = {},
) {
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(id = R.string.operating_instructions_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      item {
        ActionsTableCard(actionConfig = actionConfig)
      }

      item {
        Spacer(modifier = Modifier.height(LayoutTokens.COLUMN_ROW_SPACING))
      }
    }
  }
}

private data class ActionTableRow(
  val name: String,
  val description: String,
  @param:DrawableRes val iconRes: Int? = null,
  val buttonGroups: List<List<String>> = emptyList(),
  val directButtonText: String? = null,
)

@Composable
private fun ActionsTableCard(
  actionConfig: ActionConfig,
  modifier: Modifier = Modifier,
) {
  val rows = buildActionTableRows(actionConfig)
  var selectedRowForExplanation by remember { mutableStateOf<ActionTableRow?>(null) }

  OutlinedCard(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.outlinedCardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Table Header
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(id = R.string.table_header_action),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(0.46f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(id = R.string.table_header_buttons),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(0.54f),
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // Table Rows
      rows.forEachIndexed { index, row ->
        val rowBackground =
          if (index % 2 == 1) MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f)
          else Color.Transparent

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clickable { selectedRowForExplanation = row }
              .background(rowBackground)
              .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Action Name & Icon
          Row(
            modifier = Modifier.weight(0.46f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (row.iconRes != null) {
              Icon(
                painter = painterResource(id = row.iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            Text(
              text = row.name,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Trigger Buttons
          Box(
            modifier = Modifier.weight(0.54f),
            contentAlignment = Alignment.CenterStart,
          ) {
            ActionTriggerCell(row = row)
          }
        }

        if (index < rows.size - 1) {
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 0.5.dp,
          )
        }
      }
    }
  }

  selectedRowForExplanation?.let { row ->
    AlertDialog(
      onDismissRequest = { selectedRowForExplanation = null },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (row.iconRes != null) {
            Icon(
              painter = painterResource(id = row.iconRes),
              contentDescription = null,
              modifier = Modifier.size(24.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
          }
          Text(text = row.name, style = MaterialTheme.typography.titleLarge)
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Text(
            text = row.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = stringResource(id = R.string.action_dialog_trigger_label),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            ActionTriggerCell(row = row)
          }
        }
      },
      confirmButton = {
        Button(onClick = { selectedRowForExplanation = null }) {
          Text(stringResource(id = R.string.button_ok))
        }
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionTriggerCell(
  row: ActionTableRow,
  modifier: Modifier = Modifier,
) {
  if (row.directButtonText != null) {
    ControllerKeyBadge(text = row.directButtonText, modifier = modifier)
  } else if (row.buttonGroups.isEmpty()) {
    Text(
      text = stringResource(id = R.string.config_unassigned),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.outline,
      fontStyle = FontStyle.Italic,
      modifier = modifier,
    )
  } else {
    FlowRow(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
      row.buttonGroups.forEachIndexed { groupIndex, group ->
        if (groupIndex > 0) {
          Text(
            text = "or",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 2.dp),
          )
        }
        group.forEachIndexed { buttonIndex, buttonName ->
          if (buttonIndex > 0) {
            Text(
              text = "+",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.align(Alignment.CenterVertically),
            )
          }
          ControllerKeyBadge(
            text = buttonName,
            modifier = Modifier.align(Alignment.CenterVertically),
          )
        }
      }
    }
  }
}

@Composable
private fun ControllerKeyBadge(
  text: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    )
  }
}

@Composable
private fun buildActionTableRows(actionConfig: ActionConfig): List<ActionTableRow> {
  fun bindingGroups(action: JoystickAction): List<List<String>> {
    val binding = actionConfig.actionBindings[action] ?: return emptyList()
    return ActionConfig.getBindingButtonGroups(binding, actionConfig)
  }

  return listOf(
    // 1. Move cursor
    ActionTableRow(
      name = stringResource(id = R.string.action_name_move_cursor),
      description = stringResource(id = R.string.action_desc_move_cursor),
      directButtonText = ActionConfig.getMouseControlDisplayName(actionConfig),
    ),
    // 2. Activate at cursor
    ActionTableRow(
      name = stringResource(id = R.string.action_name_activate_at_cursor),
      description = stringResource(id = R.string.action_desc_activate_at_cursor),
      buttonGroups = bindingGroups(JoystickAction.ACTIVATE),
    ),
    // 3. Move cursor faster
    ActionTableRow(
      name = stringResource(id = R.string.action_name_move_cursor_faster),
      description = stringResource(id = R.string.action_desc_move_cursor_faster),
      buttonGroups = bindingGroups(JoystickAction.FAST_CURSOR),
    ),
    // 4. Toggle drag/fling
    ActionTableRow(
      name = stringResource(id = R.string.action_name_toggle_drag_fling),
      description = stringResource(id = R.string.action_desc_toggle_drag_fling),
      buttonGroups = bindingGroups(JoystickAction.TOGGLE_GESTURE),
    ),
    // 5. Back
    ActionTableRow(
      name = stringResource(id = R.string.action_name_back),
      description = stringResource(id = R.string.action_desc_back),
      iconRes = R.drawable.ic_android_back,
      buttonGroups = bindingGroups(JoystickAction.BACK),
    ),
    // 6. Home
    ActionTableRow(
      name = stringResource(id = R.string.action_name_home),
      description = stringResource(id = R.string.action_desc_home),
      iconRes = R.drawable.ic_android_home,
      buttonGroups = bindingGroups(JoystickAction.HOME),
    ),
    // 7. Recent
    ActionTableRow(
      name = stringResource(id = R.string.action_name_recent),
      description = stringResource(id = R.string.action_desc_recent),
      iconRes = R.drawable.ic_android_recents,
      buttonGroups = bindingGroups(JoystickAction.RECENTS),
    ),
    // 8. Fling up
    ActionTableRow(
      name = stringResource(id = R.string.action_name_fling_up),
      description = stringResource(id = R.string.action_desc_fling_up),
      buttonGroups = bindingGroups(JoystickAction.SWIPE_UP),
    ),
    // 9. Fling down
    ActionTableRow(
      name = stringResource(id = R.string.action_name_fling_down),
      description = stringResource(id = R.string.action_desc_fling_down),
      buttonGroups = bindingGroups(JoystickAction.SWIPE_DOWN),
    ),
    // 10. Fling left
    ActionTableRow(
      name = stringResource(id = R.string.action_name_fling_left),
      description = stringResource(id = R.string.action_desc_fling_left),
      buttonGroups = bindingGroups(JoystickAction.SWIPE_LEFT),
    ),
    // 11. Fling right
    ActionTableRow(
      name = stringResource(id = R.string.action_name_fling_right),
      description = stringResource(id = R.string.action_desc_fling_right),
      buttonGroups = bindingGroups(JoystickAction.SWIPE_RIGHT),
    ),
    // 12. Move to next display
    ActionTableRow(
      name = stringResource(id = R.string.action_name_move_to_next_display),
      description = stringResource(id = R.string.action_desc_move_to_next_display),
      buttonGroups = bindingGroups(JoystickAction.CYCLE_DISPLAY_FORWARD),
    ),
    // 13. Move to previous display
    ActionTableRow(
      name = stringResource(id = R.string.action_name_move_to_previous_display),
      description = stringResource(id = R.string.action_desc_move_to_previous_display),
      buttonGroups = bindingGroups(JoystickAction.CYCLE_DISPLAY_BACKWARD),
    ),
    // 14. Select active controller
    ActionTableRow(
      name = stringResource(id = R.string.action_name_select_active_controller),
      description = stringResource(id = R.string.action_desc_select_active_controller),
      buttonGroups = bindingGroups(JoystickAction.SELECT_PRIMARY_DEVICE),
    ),
    // 15. Toggle JoyMouse
    ActionTableRow(
      name = stringResource(id = R.string.action_name_toggle_joymouse),
      description = stringResource(id = R.string.action_desc_toggle_joymouse),
      buttonGroups =
        if (actionConfig.toggleChord.isNotEmpty()) {
          listOf(actionConfig.toggleChord.map { ActionConfig.getButtonDisplayName(it) })
        } else {
          emptyList()
        },
    ),
  )
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
