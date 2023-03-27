package io.reitmaier.transcripttool.core.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun IconButton(
  imageVector: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
  colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  IconButton(
    enabled = enabled,
    onClick = onClick,
    modifier = modifier,
    colors = colors
  ) {
    Icon(
      imageVector = imageVector,
      contentDescription = contentDescription
    )
  }
}
