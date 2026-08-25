package work.bearbrains.joymouse.ui

import android.graphics.PorterDuff
import android.graphics.drawable.VectorDrawable
import android.view.KeyEvent
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import kotlin.math.roundToInt
import work.bearbrains.joymouse.R
import work.bearbrains.joymouse.input.ActionBinding
import work.bearbrains.joymouse.input.ActionConfig
import work.bearbrains.joymouse.input.CursorColors
import work.bearbrains.joymouse.input.CursorConfig
import work.bearbrains.joymouse.input.CursorPalette
import work.bearbrains.joymouse.input.JoystickAction
import work.bearbrains.joymouse.input.MouseStick
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
  var isEditingMouseStick by remember { mutableStateOf(false) }
  var isEditingToggleChord by remember { mutableStateOf(false) }
  var isEditingCursorConfig by remember { mutableStateOf(false) }
  var showModifiersHelp by remember { mutableStateOf(false) }
  var showActionsHelp by remember { mutableStateOf(false) }
  var showCursorHelp by remember { mutableStateOf(false) }
  var showResetConfirmDialog by remember { mutableStateOf(false) }

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

      // Modifiers Section
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

      // Actions Section (renamed from Special actions)
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(id = R.string.config_section_actions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          IconButton(onClick = { showActionsHelp = true }) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription =
                stringResource(id = R.string.config_info_actions_content_description),
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

      // Cursor Section
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(id = R.string.config_section_cursor),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          IconButton(onClick = { showCursorHelp = true }) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription =
                stringResource(id = R.string.config_info_cursor_content_description),
            )
          }
        }
      }

      item {
        CursorAppearanceCard(
          cursorConfig = actionConfig.cursorConfig,
          onClick = { isEditingCursorConfig = true },
        )
      }

      // Special Section (at the very bottom of the list)
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = stringResource(id = R.string.config_section_special),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }

      item {
        ModifierButtonCard(
          title = stringResource(id = R.string.config_mouse_stick_title),
          buttonName = ActionConfig.getMouseControlDisplayName(actionConfig),
          onClick = { isEditingMouseStick = true },
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
        Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
          horizontalArrangement = Arrangement.Center,
        ) {
          OutlinedButton(onClick = { showResetConfirmDialog = true }) {
            Text(stringResource(id = R.string.config_button_reset_defaults))
          }
        }
      }
    }
  }

  if (showResetConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showResetConfirmDialog = false },
      title = { Text(stringResource(id = R.string.config_dialog_reset_confirm_title)) },
      text = { Text(stringResource(id = R.string.config_dialog_reset_confirm_message)) },
      confirmButton = {
        Button(
          onClick = {
            onResetDefaults()
            showResetConfirmDialog = false
          }
        ) {
          Text(stringResource(id = R.string.button_reset))
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirmDialog = false }) {
          Text(stringResource(id = R.string.button_cancel))
        }
      },
    )
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

  if (showActionsHelp) {
    AlertDialog(
      onDismissRequest = { showActionsHelp = false },
      title = { Text(stringResource(id = R.string.config_actions_info_title)) },
      text = { Text(stringResource(id = R.string.config_actions_info_description)) },
      confirmButton = {
        TextButton(onClick = { showActionsHelp = false }) {
          Text(stringResource(id = R.string.button_ok))
        }
      },
    )
  }

  if (showCursorHelp) {
    AlertDialog(
      onDismissRequest = { showCursorHelp = false },
      title = { Text(stringResource(id = R.string.config_cursor_info_title)) },
      text = { Text(stringResource(id = R.string.config_cursor_info_description)) },
      confirmButton = {
        TextButton(onClick = { showCursorHelp = false }) {
          Text(stringResource(id = R.string.button_ok))
        }
      },
    )
  }

  if (isEditingCursorConfig) {
    CursorConfigDialog(
      cursorConfig = actionConfig.cursorConfig,
      onDismiss = { isEditingCursorConfig = false },
      onSave = { newCursorConfig ->
        onSaveConfig(actionConfig.copy(cursorConfig = newCursorConfig))
        isEditingCursorConfig = false
      },
    )
  }

  if (isEditingMouseStick) {
    SelectMouseStickDialog(
      currentStick = actionConfig.mouseStick,
      currentInvertX = actionConfig.invertX,
      currentInvertY = actionConfig.invertY,
      onDismiss = { isEditingMouseStick = false },
      onSave = { newStick, newInvertX, newInvertY ->
        onSaveConfig(
          actionConfig.copy(
            mouseStick = newStick,
            invertX = newInvertX,
            invertY = newInvertY,
          )
        )
        isEditingMouseStick = false
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
private fun SelectMouseStickDialog(
  currentStick: MouseStick,
  currentInvertX: Boolean,
  currentInvertY: Boolean,
  onDismiss: () -> Unit,
  onSave: (MouseStick, Boolean, Boolean) -> Unit,
) {
  var selectedStick by remember { mutableStateOf(currentStick) }
  var invertX by remember { mutableStateOf(currentInvertX) }
  var invertY by remember { mutableStateOf(currentInvertY) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(id = R.string.config_dialog_select_mouse_stick_title)) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(id = R.string.config_dialog_select_mouse_stick_help),
          style = MaterialTheme.typography.bodyMedium,
        )

        // Left thumbstick on the left, Right thumbstick on the right
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedStick == MouseStick.LEFT_STICK,
            onClick = { selectedStick = MouseStick.LEFT_STICK },
            label = { Text(MouseStick.LEFT_STICK.getDisplayName()) },
            modifier = Modifier.weight(1f),
          )
          FilterChip(
            selected = selectedStick == MouseStick.RIGHT_STICK,
            onClick = { selectedStick = MouseStick.RIGHT_STICK },
            label = { Text(MouseStick.RIGHT_STICK.getDisplayName()) },
            modifier = Modifier.weight(1f),
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().clickable { invertX = !invertX },
        ) {
          Checkbox(checked = invertX, onCheckedChange = { invertX = it })
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(id = R.string.config_dialog_invert_x),
            style = MaterialTheme.typography.bodyMedium,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().clickable { invertY = !invertY },
        ) {
          Checkbox(checked = invertY, onCheckedChange = { invertY = it })
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(id = R.string.config_dialog_invert_y),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    },
    confirmButton = {
      Button(onClick = { onSave(selectedStick, invertX, invertY) }) {
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

@Composable
private fun CursorAppearanceCard(
  cursorConfig: CursorConfig,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = cursorConfig.getColors()
  Card(
    modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Column {
        Text(
          text = stringResource(id = R.string.config_cursor_appearance_title),
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
        )
        Text(
          text =
            "${cursorConfig.palette.getDisplayName()} • ${
              if (cursorConfig.changeShapeForMode) "Mode shapes: On" else "Mode shapes: Off"
            }",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CursorPreviewItem(
          label = "Pointer",
          drawableRes = R.drawable.mouse_cursor,
          colorInt = colors.released,
        )
        CursorPreviewItem(
          label = "Tap",
          drawableRes = R.drawable.mouse_cursor,
          colorInt = colors.tap,
        )
        CursorPreviewItem(
          label = "Long",
          drawableRes =
            if (cursorConfig.changeShapeForMode) R.drawable.mouse_cursor_target
            else R.drawable.mouse_cursor,
          colorInt = colors.longTouch,
        )
        CursorPreviewItem(
          label = "Drag",
          drawableRes =
            if (cursorConfig.changeShapeForMode) R.drawable.mouse_cursor_drag
            else R.drawable.mouse_cursor,
          colorInt = colors.drag,
        )
        CursorPreviewItem(
          label = "Fling",
          drawableRes =
            if (cursorConfig.changeShapeForMode) R.drawable.mouse_cursor_fling
            else R.drawable.mouse_cursor,
          colorInt = colors.fling,
        )
      }
    }
  }
}

@Composable
private fun CursorPreview(
  @DrawableRes drawableRes: Int,
  @ColorInt colorInt: Int,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  Canvas(
    modifier = modifier.size(width = 32.dp, height = 28.dp)
  ) {
    val drawable =
      ContextCompat.getDrawable(context, drawableRes)?.mutate() as? VectorDrawable ?: return@Canvas
    DrawableCompat.setTint(drawable, colorInt)
    DrawableCompat.setTintMode(drawable, PorterDuff.Mode.MULTIPLY)
    drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
    drawIntoCanvas { canvas ->
      drawable.draw(canvas.nativeCanvas)
    }
  }
}

@Composable
private fun CursorPreviewItem(
  label: String,
  @DrawableRes drawableRes: Int,
  @ColorInt colorInt: Int,
  modifier: Modifier = Modifier,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier,
  ) {
    CursorPreview(
      drawableRes = drawableRes,
      colorInt = colorInt,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun CursorConfigDialog(
  cursorConfig: CursorConfig,
  onDismiss: () -> Unit,
  onSave: (CursorConfig) -> Unit,
) {
  var selectedPalette by remember { mutableStateOf(cursorConfig.palette) }
  var changeShapeForMode by remember { mutableStateOf(cursorConfig.changeShapeForMode) }
  var customColors by remember { mutableStateOf(cursorConfig.customColors) }
  var colorPickingState by remember { mutableStateOf<String?>(null) }

  val activeColors =
    when (selectedPalette) {
      CursorPalette.DEFAULT -> CursorColors.DEFAULT
      CursorPalette.COLORBLIND_FRIENDLY -> CursorColors.COLORBLIND_FRIENDLY
      CursorPalette.CUSTOM -> customColors
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(id = R.string.config_cursor_dialog_title)) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text(
          text = stringResource(id = R.string.config_cursor_palette_title),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          CursorPalette.values().forEach { palette ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { selectedPalette = palette }
                  .padding(vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = selectedPalette == palette,
                onClick = { selectedPalette = palette },
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = palette.getDisplayName(),
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }
        }

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clickable { changeShapeForMode = !changeShapeForMode }
              .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(
            checked = changeShapeForMode,
            onCheckedChange = { changeShapeForMode = it },
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = stringResource(id = R.string.config_cursor_shape_mode_title),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = stringResource(id = R.string.config_cursor_shape_mode_summary),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Text(
          text =
            if (selectedPalette == CursorPalette.CUSTOM) "State cursors (tap to customize color)"
            else "State cursors preview",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )

        ColorStateRow(
          label = stringResource(id = R.string.config_cursor_state_released),
          drawableRes = R.drawable.mouse_cursor,
          colorInt = activeColors.released,
          isClickable = selectedPalette == CursorPalette.CUSTOM,
          onClick = { colorPickingState = "released" },
        )
        ColorStateRow(
          label = stringResource(id = R.string.config_cursor_state_tap),
          drawableRes = R.drawable.mouse_cursor,
          colorInt = activeColors.tap,
          isClickable = selectedPalette == CursorPalette.CUSTOM,
          onClick = { colorPickingState = "tap" },
        )
        ColorStateRow(
          label = stringResource(id = R.string.config_cursor_state_long_touch),
          drawableRes =
            if (changeShapeForMode) R.drawable.mouse_cursor_target else R.drawable.mouse_cursor,
          colorInt = activeColors.longTouch,
          isClickable = selectedPalette == CursorPalette.CUSTOM,
          onClick = { colorPickingState = "longTouch" },
        )
        ColorStateRow(
          label = stringResource(id = R.string.config_cursor_state_drag),
          drawableRes =
            if (changeShapeForMode) R.drawable.mouse_cursor_drag else R.drawable.mouse_cursor,
          colorInt = activeColors.drag,
          isClickable = selectedPalette == CursorPalette.CUSTOM,
          onClick = { colorPickingState = "drag" },
        )
        ColorStateRow(
          label = stringResource(id = R.string.config_cursor_state_fling),
          drawableRes =
            if (changeShapeForMode) R.drawable.mouse_cursor_fling else R.drawable.mouse_cursor,
          colorInt = activeColors.fling,
          isClickable = selectedPalette == CursorPalette.CUSTOM,
          onClick = { colorPickingState = "fling" },
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            CursorConfig(
              palette = selectedPalette,
              customColors = customColors,
              changeShapeForMode = changeShapeForMode,
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

  if (colorPickingState != null) {
    val stateKey = colorPickingState!!
    val (stateTitle, currentDrawableRes, currentColorInt) =
      when (stateKey) {
        "released" ->
          Triple(
            stringResource(id = R.string.config_cursor_state_released),
            R.drawable.mouse_cursor,
            activeColors.released,
          )
        "tap" ->
          Triple(
            stringResource(id = R.string.config_cursor_state_tap),
            R.drawable.mouse_cursor,
            activeColors.tap,
          )
        "longTouch" ->
          Triple(
            stringResource(id = R.string.config_cursor_state_long_touch),
            if (changeShapeForMode) R.drawable.mouse_cursor_target else R.drawable.mouse_cursor,
            activeColors.longTouch,
          )
        "drag" ->
          Triple(
            stringResource(id = R.string.config_cursor_state_drag),
            if (changeShapeForMode) R.drawable.mouse_cursor_drag else R.drawable.mouse_cursor,
            activeColors.drag,
          )
        "fling" ->
          Triple(
            stringResource(id = R.string.config_cursor_state_fling),
            if (changeShapeForMode) R.drawable.mouse_cursor_fling else R.drawable.mouse_cursor,
            activeColors.fling,
          )
        else -> Triple(stateKey, R.drawable.mouse_cursor, 0)
      }

    ColorPickerDialog(
      title = stringResource(id = R.string.config_cursor_pick_color_title, stateTitle),
      drawableRes = currentDrawableRes,
      currentColorInt = currentColorInt,
      onDismiss = { colorPickingState = null },
      onColorSelected = { selectedColorInt ->
        customColors =
          when (stateKey) {
            "released" -> customColors.copy(released = selectedColorInt)
            "tap" -> customColors.copy(tap = selectedColorInt)
            "longTouch" -> customColors.copy(longTouch = selectedColorInt)
            "drag" -> customColors.copy(drag = selectedColorInt)
            "fling" -> customColors.copy(fling = selectedColorInt)
            else -> customColors
          }
        colorPickingState = null
      },
    )
  }
}

@Composable
private fun ColorStateRow(
  label: String,
  @DrawableRes drawableRes: Int,
  @ColorInt colorInt: Int,
  isClickable: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .then(if (isClickable) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      CursorPreview(
        drawableRes = drawableRes,
        colorInt = colorInt,
      )
      if (isClickable) {
        Text(
          text = "Change",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

private val PRESET_COLORS =
  listOf(
    Color(0xFFFFFFFF.toInt()), // White
    Color(0xFFF44336.toInt()), // Red
    Color(0xFFE91E63.toInt()), // Pink
    Color(0xFFFF4081.toInt()), // Vivid Magenta / Accent Pink
    Color(0xFF9C27B0.toInt()), // Purple
    Color(0xFF673AB7.toInt()), // Deep Purple
    Color(0xFF3F51B5.toInt()), // Indigo
    Color(0xFF2196F3.toInt()), // Blue
    Color(0xFF03A9F4.toInt()), // Light Blue
    Color(0xFF00BCD4.toInt()), // Cyan
    Color(0xFF009688.toInt()), // Teal
    Color(0xFF4CAF50.toInt()), // Green
    Color(0xFF8BC34A.toInt()), // Light Green
    Color(0xFFCDDC39.toInt()), // Lime
    Color(0xFFFFEB3B.toInt()), // Yellow
    Color(0xFFFFC107.toInt()), // Amber
    Color(0xFFFF9800.toInt()), // Orange
    Color(0xFFFF5722.toInt()), // Deep Orange
    Color(0xFF795548.toInt()), // Brown
    Color(0xFF9E9E9E.toInt()), // Grey
  )

@Composable
private fun ColorPickerDialog(
  title: String,
  @DrawableRes drawableRes: Int,
  currentColorInt: Int,
  onDismiss: () -> Unit,
  onColorSelected: (Int) -> Unit,
) {
  var red by remember { mutableStateOf((currentColorInt shr 16) and 0xFF) }
  var green by remember { mutableStateOf((currentColorInt shr 8) and 0xFF) }
  var blue by remember { mutableStateOf(currentColorInt and 0xFF) }
  var hexText by remember { mutableStateOf(String.format("%02X%02X%02X", red, green, blue)) }

  fun updateFromRgb(r: Int, g: Int, b: Int) {
    red = r.coerceIn(0, 255)
    green = g.coerceIn(0, 255)
    blue = b.coerceIn(0, 255)
    hexText = String.format("%02X%02X%02X", red, green, blue)
  }

  fun updateFromHex(hex: String) {
    val clean = hex.removePrefix("#").trim()
    hexText = clean
    if (clean.length == 6) {
      try {
        val parsed = clean.toLong(16).toInt()
        red = (parsed shr 16) and 0xFF
        green = (parsed shr 8) and 0xFF
        blue = parsed and 0xFF
      } catch (_: Exception) {}
    }
  }

  val selectedColorInt = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Cursor preview with selected shape and live color
        CursorPreview(
          drawableRes = drawableRes,
          colorInt = selectedColorInt,
          modifier = Modifier.size(width = 56.dp, height = 48.dp),
        )

        // Hex input
        OutlinedTextField(
          value = hexText,
          onValueChange = { updateFromHex(it) },
          label = { Text(stringResource(id = R.string.config_cursor_hex_label)) },
          prefix = { Text("#") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        // RGB Sliders
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = stringResource(id = R.string.config_cursor_red_slider, red),
            style = MaterialTheme.typography.bodySmall,
          )
          Slider(
            value = red.toFloat(),
            onValueChange = { updateFromRgb(it.roundToInt(), green, blue) },
            valueRange = 0f..255f,
          )

          Text(
            text = stringResource(id = R.string.config_cursor_green_slider, green),
            style = MaterialTheme.typography.bodySmall,
          )
          Slider(
            value = green.toFloat(),
            onValueChange = { updateFromRgb(red, it.roundToInt(), blue) },
            valueRange = 0f..255f,
          )

          Text(
            text = stringResource(id = R.string.config_cursor_blue_slider, blue),
            style = MaterialTheme.typography.bodySmall,
          )
          Slider(
            value = blue.toFloat(),
            onValueChange = { updateFromRgb(red, green, it.roundToInt()) },
            valueRange = 0f..255f,
          )
        }

        // Preset Swatches for quick selection
        Text(
          text = stringResource(id = R.string.config_cursor_presets_label),
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.align(Alignment.Start),
        )
        val rows = PRESET_COLORS.chunked(5)
        for (rowColors in rows) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            for (color in rowColors) {
              val isSelected = color.toArgb() == selectedColorInt
              Box(
                modifier =
                  Modifier.size(36.dp)
                    .background(color, RoundedCornerShape(6.dp))
                    .border(
                      width = if (isSelected) 3.dp else 1.dp,
                      color =
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                      shape = RoundedCornerShape(6.dp),
                    )
                    .clickable {
                      val c = color.toArgb()
                      updateFromRgb((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)
                    }
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = { onColorSelected(selectedColorInt) }) {
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
