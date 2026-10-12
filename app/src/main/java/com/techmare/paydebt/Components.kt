package com.techmare.paydebt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.techmare.paydebt.core.Fmt

/** A money figure: big number, small "dh" next to it, on one line. */
@Composable
fun Money(
    value: Long,
    color: Color,
    modifier: Modifier = Modifier,
    large: Boolean = true,
) {
    val numberStyle = if (large) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displayMedium
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            Fmt.number(value),
            style = numberStyle,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "dh",
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/** What a single week looked like. */
enum class Wk { MET, MISSED, NOW, AHEAD }

private const val PER_ROW = 13 // one quarter per row

/** One rounded square per week of the plan, 13 to a row. */
@Composable
fun WeekGrid(weeks: List<Wk>, modifier: Modifier = Modifier) {
    val met = MaterialTheme.colorScheme.primary
    val missed = MaterialTheme.colorScheme.error
    val nowFill = MaterialTheme.colorScheme.primaryContainer
    val ahead = MaterialTheme.colorScheme.surfaceContainerHighest
    val gap = 4.dp
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cell: Dp = (maxWidth - gap * (PER_ROW - 1)) / PER_ROW
        val rows = (weeks.size + PER_ROW - 1) / PER_ROW
        val gridHeight = if (rows == 0) 0.dp else cell * rows + gap * (rows - 1)
        Canvas(Modifier.fillMaxWidth().height(gridHeight)) {
            val c = cell.toPx()
            val g = gap.toPx()
            val radius = CornerRadius(c * 0.3f, c * 0.3f)
            weeks.forEachIndexed { i, w ->
                val x = (i % PER_ROW) * (c + g)
                val y = (i / PER_ROW) * (c + g)
                val topLeft = Offset(x, y)
                val size = Size(c, c)
                when (w) {
                    Wk.MET -> drawRoundRect(met, topLeft, size, radius)
                    Wk.AHEAD -> drawRoundRect(ahead, topLeft, size, radius)
                    Wk.MISSED -> {
                        val s = 1.5.dp.toPx()
                        drawRoundRect(
                            missed, Offset(x + s / 2, y + s / 2), Size(c - s, c - s), radius,
                            style = Stroke(width = s),
                        )
                    }
                    Wk.NOW -> {
                        drawRoundRect(nowFill, topLeft, size, radius)
                        val s = 2.dp.toPx()
                        drawRoundRect(
                            met, Offset(x + s / 2, y + s / 2), Size(c - s, c - s), radius,
                            style = Stroke(width = s),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeekLegend(showMissed: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem("Paid", MaterialTheme.colorScheme.primary)
        if (showMissed) LegendItem("Missed", MaterialTheme.colorScheme.error)
        LegendItem("This week", MaterialTheme.colorScheme.primaryContainer)
        LegendItem("To go", MaterialTheme.colorScheme.surfaceContainerHighest)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Whole-number money field with a "dh" suffix. Only digits can be typed. */
@Composable
fun AmountField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    focusRequester: FocusRequester? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() }.take(9)) },
        label = { Text(label) },
        suffix = { Text("dh") },
        singleLine = true,
        isError = errorText != null,
        supportingText = if (errorText != null) { { Text(errorText) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = (if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier).fillMaxWidth(),
    )
}

/** Plain text field used for names and notes. */
@Composable
fun TextField1(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    focusRequester: FocusRequester? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        isError = errorText != null,
        supportingText = if (errorText != null) { { Text(errorText) } } else null,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = (if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier).fillMaxWidth(),
    )
}
