package work.bearbrains.joymouse.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import work.bearbrains.joymouse.R
import work.bearbrains.joymouse.model.ControllerInfo

@Composable
fun ControllerSelectionDialog(
  controllers: List<ControllerInfo>,
  onControllerSelected: (Int) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.5f))
        .clickable(onClick = onDismiss),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier =
        Modifier
          .fillMaxWidth(0.9f)
          .padding(16.dp)
          .clickable(enabled = false) {}, // Prevent clicks inside card from dismissing
      shape = RoundedCornerShape(20.dp),
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface,
        ),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text(
          text = stringResource(id = R.string.controller_selection_dialog_title),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )

        Text(
          text = stringResource(id = R.string.controller_selection_dialog_description),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(controllers, key = { it.id }) { controller ->
            ControllerItem(
              controller = controller,
              onClick = { onControllerSelected(controller.id) },
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          OutlinedButton(onClick = onDismiss) {
            Text(stringResource(id = R.string.button_cancel))
          }
        }
      }
    }
  }
}

@Composable
private fun ControllerItem(
  controller: ControllerInfo,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color =
      if (controller.isPrimary) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceVariant
      },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        RadioButton(
          selected = controller.isPrimary,
          onClick = onClick,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = controller.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (controller.isPrimary) FontWeight.Bold else FontWeight.Normal,
          )
        }
      }

      if (controller.isPrimary) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primary,
        ) {
          Text(
            text = stringResource(id = R.string.controller_selection_active_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
