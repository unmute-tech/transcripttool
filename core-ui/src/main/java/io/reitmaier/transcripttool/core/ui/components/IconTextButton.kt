package io.reitmaier.transcripttool.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun IconTextButton(
  imageVector: ImageVector,
  text: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
  buttonColors: ButtonColors = ButtonDefaults.buttonColors(),
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  Button(
    enabled = enabled,
    modifier = modifier,
    colors = buttonColors,
    onClick = { onClick() },
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(ButtonDefaults.IconSize)
      )
//      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      Text(text)
    }
  }
}

@Preview
@Composable
private fun IconTextButtonPreview() {
  IconTextButton(
    imageVector = Icons.Filled.Search,
    text = "Search",
    contentDescription = "Search",
  ) {}
}
