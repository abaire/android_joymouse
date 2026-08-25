package work.bearbrains.joymouse.ui

import android.view.KeyEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import work.bearbrains.joymouse.R
import work.bearbrains.joymouse.input.ActionBinding
import work.bearbrains.joymouse.input.ActionConfig
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.ShiftModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
  actionConfig: ActionConfig,
  onSaveConfig: (ActionConfig) -> Unit,
  onResetDefaults: () -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var editingAction by remember { mutableStateOf<JoystickAction?>(null) }
  var isEditingShiftButton by remember { mutableStateOf(false) }
  var isEditingAltButton by remember { mutableStateOf(false) }
  var isEditingToggleChord by remember { mutableStateOf(false) }
  var showModifiersHelp by remember { mutableStateOf(false) }
  var showSpecialActionsHelp by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(id = R.string.config_screen_title)) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(id = R.string.button_back),
            )
          }
        },
        actions = {
          TextButton(onClick = onResetDefaults) {
            Text(stringResource(id = R.string.button_reset))
          }
        },
      )
    },
    modifier = modifier,
  ) { paddingValues ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = LayoutTokens.COLUMN_PADDING),
      verticalArrangement = Arrangement.spacedBy(LayoutTokens.COLUMN_ROW_SPACING),
    ) {
      item {
        Text(
          text = stringResource(id = R.string.config_screen_description),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(id = R.string.config_section_modifiers),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          IconButton(onClick = { showModifiersHelp = true }) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription =
                stringResource(id = R.string.config_info_modifiers_content_description),
            )
          }
        }
      }

      item {
        ModifierButtonCard(
          title = stringResource(id = R.string.config_modifier_shift_title),
          buttonName = ActionConfig.getButtonDisplayName(actionConfig.shiftButton),
          onClick = { isEditingShiftButton = true },
        )
      }

      item {
        ModifierButtonCard(
          title = stringResource(id = R.string.config_modifier_alt_title),
          buttonName = ActionConfig.getButtonDisplayName(actionConfig.altButton),
          onClick = { isEditingAltButton = true },
        )
      }

      item {
        ModifierButtonCard(
          title = stringResource(id = R.string.config_section_toggle_chord),
          buttonName = ActionConfig.formatChord(actionConfig.toggleChord),
          onClick = { isEditingToggleChord = true },
        )
      }

      item {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(id = R.string.config_section_special_actions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          IconButton(onClick = { showSpecialActionsHelp = true }) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription =
                stringResource(id = R.string.config_info_special_actions_content_description),
            )
          }
        }
      }

      items(ActionConfig.REMAPPABLE_ACTIONS) { action ->
        val binding = actionConfig.actionBindings[action] ?: ActionBinding()
        ActionMappingCard(
          action = action,
          binding = binding,
          onClick = { editingAction = action },
        )
      }

      item {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
          horizontalArrangement = Arrangement.Center,
        ) {
          OutlinedButton(onClick = onResetDefaults) {
            Text(stringResource(id = R.string.config_button_reset_defaults))
          }
        }
      }
    }
  }

  if (showModifiersHelp) {
    AlertDialog(
      onDismissRequest = { showModifiersHelp = false },
      title = { Text(stringResource(id = R.string.config_modifiers_info_title)) },
      text = { Text(stringResource(id = R.string.config_modifiers_info_description)) },
      confirmButton = {
        TextButton(onClick = { showModifiersHelp = false }) {
          Text(stringResource(id = R.string.button_ok))
        }
      },
    )
  }

  if (showSpecialActionsHelp) {
    AlertDialog(
      onDismissRequest = { showSpecialActionsHelp = false },
      title = { Text(stringResource(id = R.string.config_special_actions_info_title)) },
      text = { Text(stringResource(id = R.string.config_special_actions_info_description)) },
      confirmButton = {
        TextButton(onClick = { showSpecialActionsHelp = false }) {
          Text(stringResource(id = R.string.button_ok))
        }
      },
    )
  }

  if (isEditingShiftButton) {
    SelectSingleButtonDialog(
      title = stringResource(id = R.string.config_dialog_select_shift_title),
      currentButton = actionConfig.shiftButton,
      disabledButton = actionConfig.altButton,
      onDismiss = { isEditingShiftButton = false },
      onSelect = { newShiftButton ->
        if (newShiftButton != actionConfig.altButton) {
          onSaveConfig(actionConfig.copy(shiftButton = newShiftButton))
        }
        isEditingShiftButton = false
      },
    )
  }

  if (isEditingAltButton) {
    SelectSingleButtonDialog(
      title = stringResource(id = R.string.config_dialog_select_alt_title),
      currentButton = actionConfig.altButton,
      disabledButton = actionConfig.shiftButton,
      onDismiss = { isEditingAltButton = false },
      onSelect = { newAltButton ->
        if (newAltButton != actionConfig.shiftButton) {
          onSaveConfig(actionConfig.copy(altButton = newAltButton))
        }
        isEditingAltButton = false
      },
    )
  }

  editingAction?.let { action ->
    val currentBinding = actionConfig.actionBindings[action] ?: ActionBinding()
    RemapActionDialog(
      action = action,
      currentBinding = currentBinding,
      onDismiss = { editingAction = null },
      onSave = { newBinding ->
        val updatedMap = actionConfig.actionBindings.toMutableMap()
        updatedMap[action] = newBinding
        onSaveConfig(actionConfig.copy(actionBindings = updatedMap))
        editingAction = null
      },
    )
  }

  if (isEditingToggleChord) {
    EditToggleChordDialog(
      currentChord = actionConfig.toggleChord,
      onDismiss = { isEditingToggleChord = false },
      onSave = { newChord ->
        onSaveConfig(actionConfig.copy(toggleChord = newChord))
        isEditingToggleChord = false
      },
    )
  }
}

@Composable
private fun ModifierButtonCard(
  title: String,
  buttonName: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(LayoutTokens.COLUMN_PADDING),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = buttonName,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium,
        )
      }
      OutlinedButton(onClick = onClick) {
        Text(stringResource(id = R.string.button_change))
      }
    }
  }
}

@Composable
private fun ActionMappingCard(
  action: JoystickAction,
  binding: ActionBinding,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(LayoutTokens.COLUMN_PADDING),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = ActionConfig.getActionDisplayName(action),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (binding.modifier != ShiftModifier.NONE) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = MaterialTheme.shapes.small,
            ) {
              Text(
                text = binding.modifier.getDisplayName(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontWeight = FontWeight.Bold,
              )
            }
          }
          Text(
            text = ActionConfig.formatBinding(binding),
            style = MaterialTheme.typography.bodyMedium,
            color =
              if (binding.keyCodes.isEmpty()) MaterialTheme.colorScheme.outline
              else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      OutlinedButton(onClick = onClick) {
        Text(stringResource(id = R.string.button_remap))
      }
    }
  }
}

@Composable
private fun ControllerButtonLayout(
  selectedButtons: Set<Int>,
  onToggleKey: (Int) -> Unit,
  disabledButton: Int? = null,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Face buttons row (A, B, X, Y)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      listOf(
        KeyEvent.KEYCODE_BUTTON_A to "A",
        KeyEvent.KEYCODE_BUTTON_B to "B",
        KeyEvent.KEYCODE_BUTTON_X to "X",
        KeyEvent.KEYCODE_BUTTON_Y to "Y",
      ).forEach { (keyCode, label) ->
        val isSelected = selectedButtons.contains(keyCode)
        val isDisabled = disabledButton == keyCode
        FilterChip(
          selected = isSelected,
          enabled = !isDisabled,
          onClick = { onToggleKey(keyCode) },
          label = { Text(if (isDisabled) "$label (In use)" else label) },
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Center buttons (Select, Mode, Start - Xbox order)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      listOf(
        KeyEvent.KEYCODE_BUTTON_SELECT to "Select",
        KeyEvent.KEYCODE_BUTTON_MODE to "Mode",
        KeyEvent.KEYCODE_BUTTON_START to "Start",
      ).forEach { (keyCode, label) ->
        val isSelected = selectedButtons.contains(keyCode)
        val isDisabled = disabledButton == keyCode
        FilterChip(
          selected = isSelected,
          enabled = !isDisabled,
          onClick = { onToggleKey(keyCode) },
          label = { Text(if (isDisabled) "$label (In use)" else label) },
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Bumpers
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = stringResource(id = R.string.config_group_bumper),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(
          KeyEvent.KEYCODE_BUTTON_L1 to stringResource(id = R.string.config_button_left),
          KeyEvent.KEYCODE_BUTTON_R1 to stringResource(id = R.string.config_button_right),
        ).forEach { (keyCode, label) ->
          val isSelected = selectedButtons.contains(keyCode)
          val isDisabled = disabledButton == keyCode
          FilterChip(
            selected = isSelected,
            enabled = !isDisabled,
            onClick = { onToggleKey(keyCode) },
            label = { Text(if (isDisabled) "$label (In use)" else label) },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // Triggers
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = stringResource(id = R.string.config_group_trigger),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(
          KeyEvent.KEYCODE_BUTTON_L2 to stringResource(id = R.string.config_button_left),
          KeyEvent.KEYCODE_BUTTON_R2 to stringResource(id = R.string.config_button_right),
        ).forEach { (keyCode, label) ->
          val isSelected = selectedButtons.contains(keyCode)
          val isDisabled = disabledButton == keyCode
          FilterChip(
            selected = isSelected,
            enabled = !isDisabled,
            onClick = { onToggleKey(keyCode) },
            label = { Text(if (isDisabled) "$label (In use)" else label) },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // Thumbsticks
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = stringResource(id = R.string.config_group_thumbstick),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(
          KeyEvent.KEYCODE_BUTTON_THUMBL to stringResource(id = R.string.config_button_left),
          KeyEvent.KEYCODE_BUTTON_THUMBR to stringResource(id = R.string.config_button_right),
        ).forEach { (keyCode, label) ->
          val isSelected = selectedButtons.contains(keyCode)
          val isDisabled = disabledButton == keyCode
          FilterChip(
            selected = isSelected,
            enabled = !isDisabled,
            onClick = { onToggleKey(keyCode) },
            label = { Text(if (isDisabled) "$label (In use)" else label) },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // D-pad in cross formation
    Column(
      verticalArrangement = Arrangement.spacedBy(4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = stringResource(id = R.string.config_group_dpad),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.align(Alignment.Start),
      )

      // Up
      val isUpSelected = selectedButtons.contains(KeyEvent.KEYCODE_DPAD_UP)
      val isUpDisabled = disabledButton == KeyEvent.KEYCODE_DPAD_UP
      FilterChip(
        selected = isUpSelected,
        enabled = !isUpDisabled,
        onClick = { onToggleKey(KeyEvent.KEYCODE_DPAD_UP) },
        label = {
          Text(
            if (isUpDisabled) {
              "${stringResource(id = R.string.config_button_up)} (In use)"
            } else {
              stringResource(id = R.string.config_button_up)
            }
          )
        },
      )

      // Left & Right
      Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val isLeftSelected = selectedButtons.contains(KeyEvent.KEYCODE_DPAD_LEFT)
        val isLeftDisabled = disabledButton == KeyEvent.KEYCODE_DPAD_LEFT
        FilterChip(
          selected = isLeftSelected,
          enabled = !isLeftDisabled,
          onClick = { onToggleKey(KeyEvent.KEYCODE_DPAD_LEFT) },
          label = {
            Text(
              if (isLeftDisabled) {
                "${stringResource(id = R.string.config_button_left)} (In use)"
              } else {
                stringResource(id = R.string.config_button_left)
              }
            )
          },
        )

        val isRightSelected = selectedButtons.contains(KeyEvent.KEYCODE_DPAD_RIGHT)
        val isRightDisabled = disabledButton == KeyEvent.KEYCODE_DPAD_RIGHT
        FilterChip(
          selected = isRightSelected,
          enabled = !isRightDisabled,
          onClick = { onToggleKey(KeyEvent.KEYCODE_DPAD_RIGHT) },
          label = {
            Text(
              if (isRightDisabled) {
                "${stringResource(id = R.string.config_button_right)} (In use)"
              } else {
                stringResource(id = R.string.config_button_right)
              }
            )
          },
        )
      }

      // Down
      val isDownSelected = selectedButtons.contains(KeyEvent.KEYCODE_DPAD_DOWN)
      val isDownDisabled = disabledButton == KeyEvent.KEYCODE_DPAD_DOWN
      FilterChip(
        selected = isDownSelected,
        enabled = !isDownDisabled,
        onClick = { onToggleKey(KeyEvent.KEYCODE_DPAD_DOWN) },
        label = {
          Text(
            if (isDownDisabled) {
              "${stringResource(id = R.string.config_button_down)} (In use)"
            } else {
              stringResource(id = R.string.config_button_down)
            }
          )
        },
      )
    }
  }
}

@Composable
private fun SelectSingleButtonDialog(
  title: String,
  currentButton: Int,
  disabledButton: Int,
  onDismiss: () -> Unit,
  onSelect: (Int) -> Unit,
) {
  var selectedButton by remember { mutableStateOf(currentButton) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(id = R.string.config_dialog_select_button_help),
          style = MaterialTheme.typography.bodyMedium,
        )

        ControllerButtonLayout(
          selectedButtons = setOf(selectedButton),
          onToggleKey = { selectedButton = it },
          disabledButton = disabledButton,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSelect(selectedButton) },
        enabled = selectedButton != disabledButton,
      ) {
        Text(stringResource(id = R.string.button_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(id = R.string.button_cancel))
      }
    },
  )
}

@Composable
private fun RemapActionDialog(
  action: JoystickAction,
  currentBinding: ActionBinding,
  onDismiss: () -> Unit,
  onSave: (ActionBinding) -> Unit,
) {
  var isShiftActive by remember {
    mutableStateOf(
      currentBinding.modifier == ShiftModifier.SHIFT ||
        currentBinding.modifier == ShiftModifier.ALTSHIFT
    )
  }
  var isAltActive by remember {
    mutableStateOf(
      currentBinding.modifier == ShiftModifier.ALT ||
        currentBinding.modifier == ShiftModifier.ALTSHIFT
    )
  }
  var selectedKeyCode by remember { mutableStateOf(currentBinding.keyCodes.firstOrNull()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        stringResource(
          id = R.string.config_dialog_remap_title,
          ActionConfig.getActionDisplayName(action),
        )
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(id = R.string.config_dialog_modifier_label),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FilterChip(
            selected = isShiftActive,
            onClick = { isShiftActive = !isShiftActive },
            label = { Text("Shift") },
          )
          FilterChip(
            selected = isAltActive,
            onClick = { isAltActive = !isAltActive },
            label = { Text("Alt") },
          )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = stringResource(id = R.string.config_dialog_buttons_label),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )

        ControllerButtonLayout(
          selectedButtons = if (selectedKeyCode != null) setOf(selectedKeyCode!!) else emptySet(),
          onToggleKey = { keyCode ->
            selectedKeyCode = if (selectedKeyCode == keyCode) null else keyCode
          },
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val modifier =
            when {
              isShiftActive && isAltActive -> ShiftModifier.ALTSHIFT
              isShiftActive -> ShiftModifier.SHIFT
              isAltActive -> ShiftModifier.ALT
              else -> ShiftModifier.NONE
            }
          onSave(
            ActionBinding(
              modifier = modifier,
              keyCodes = if (selectedKeyCode != null) setOf(selectedKeyCode!!) else emptySet(),
              isChord = false,
            )
          )
        }
      ) {
        Text(stringResource(id = R.string.button_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(id = R.string.button_cancel))
      }
    },
  )
}

@Composable
private fun EditToggleChordDialog(
  currentChord: Set<Int>,
  onDismiss: () -> Unit,
  onSave: (Set<Int>) -> Unit,
) {
  var selectedButtons by remember { mutableStateOf(currentChord) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(id = R.string.config_dialog_toggle_chord_title)) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(id = R.string.config_dialog_toggle_chord_help),
          style = MaterialTheme.typography.bodyMedium,
        )

        ControllerButtonLayout(
          selectedButtons = selectedButtons,
          onToggleKey = { keyCode ->
            selectedButtons =
              if (selectedButtons.contains(keyCode)) {
                if (selectedButtons.size > 1) selectedButtons - keyCode else selectedButtons
              } else {
                selectedButtons + keyCode
              }
          },
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(selectedButtons) },
        enabled = selectedButtons.isNotEmpty(),
      ) {
        Text(stringResource(id = R.string.button_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(id = R.string.button_cancel))
      }
    },
  )
}
