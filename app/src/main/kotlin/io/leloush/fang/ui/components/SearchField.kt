package io.leloush.fang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.leloush.fang.R
import io.leloush.fang.ui.theme.FangTokens
import io.leloush.fang.ui.theme.fangColors

/** A sheet's single-line search field: a search icon and [placeholder] on a rounded surface2 fill. */
@Composable
fun SearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
) {
  val colors = fangColors
  BasicTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = modifier.fillMaxWidth().padding(
      horizontal = FangTokens.Spacing.s16,
      vertical = FangTokens.Spacing.s4,
    ),
    textStyle = TextStyle(color = colors.foreground, fontSize = FangTokens.FontSize.base),
    cursorBrush = SolidColor(colors.accent),
    keyboardOptions = keyboardOptions,
    singleLine = true,
    decorationBox = { field ->
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .background(colors.surface2, RoundedCornerShape(FangTokens.Radius.lg))
            .padding(horizontal = FangTokens.Spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FangTokens.Spacing.s8),
      ) {
        LucideIcon(icon = R.drawable.ic_lucide_search, contentDescription = null, tint = colors.foregroundMuted)
        Box(modifier = Modifier.weight(1f)) {
          if (query.isEmpty()) {
            Text(text = placeholder, color = colors.foregroundMuted, fontSize = FangTokens.FontSize.base)
          }
          field()
        }
      }
    },
  )
}
