package com.jyotirmoy.musicly.presentation.components.subcomps

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AutoSizingTextToFill(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    minFontSize: TextUnit = 8.sp,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    maxFontSizeLimit: TextUnit = 100.sp, // Practical upper limit for the search
    lineHeightRatio: Float = 1.2f // Factor for line spacing (e.g., 1.2f for 20% more space)
) {
    // TextMeasurer is used to measure text efficiently.
    val textMeasurer = rememberTextMeasurer()
    // Density is needed to convert dp to px.
    val density = LocalDensity.current

    // State for the determined font size.
    var currentFontSize by remember { mutableStateOf(minFontSize) }
    // State to know if the calculation is ready and we can draw.
    var readyToDraw by remember { mutableStateOf(false) }

    // BoxWithConstraints gives us the available maxWidth and maxHeight.
    BoxWithConstraints(modifier = modifier) {
        // We convert dp constraints to pixels once.
        val maxWidthPx = with(density) { maxWidth.toPx() }.toInt()
        val maxHeightPx = with(density) { maxHeight.toPx() }.toInt()

        // LaunchedEffect to recalculate when text, style, font limits,
        // line height ratio, or container size change.
        LaunchedEffect(text, style, minFontSize, maxFontSizeLimit, lineHeightRatio, maxWidthPx, maxHeightPx) {
            readyToDraw = false // Indicate that we need to recalculate.
            var bestFitFontSize = minFontSize // We start by assuming the minimum.

            // Ensure search limits are valid.
            var lowerBoundSp = minFontSize.value
            var upperBoundSp = maxFontSizeLimit.value.coerceAtLeast(minFontSize.value)

            // If the search range is invalid (e.g., min > max limit), we use minFontSize.
            if (lowerBoundSp > upperBoundSp + 0.01f) {
                currentFontSize = minFontSize
                readyToDraw = true
                return@LaunchedEffect
            }

            // 1. Check if the text with minFontSize (and its corresponding lineHeight) already overflows.
            val minFontEffectiveLineHeight = minFontSize * lineHeightRatio
            val minFontEffectiveStyle = style.copy(
                fontSize = minFontSize,
                lineHeight = minFontEffectiveLineHeight
            )
            val minFontLayoutResult = textMeasurer.measure(
                text = AnnotatedString(text),
                style = minFontEffectiveStyle,
                overflow = TextOverflow.Clip, // We use Clip for precise measurement.
                softWrap = true,
                maxLines = Int.MAX_VALUE, // Allow all necessary lines.
                constraints = Constraints(
                    maxWidth = maxWidthPx.coerceAtLeast(0), // Ensure it is not negative.
                    maxHeight = maxHeightPx.coerceAtLeast(0) // Ensure it is not negative.
                )
            )

            if (minFontLayoutResult.hasVisualOverflow) {
                // Even with minFontSize, the text overflows. We will use minFontSize and it will be truncated.
                currentFontSize = minFontSize
                readyToDraw = true
                return@LaunchedEffect
            } else {
                // minFontSize fits, so it's our initial "best fit".
                bestFitFontSize = minFontSize
            }

            // 2. Binary search to find the largest font size that fits.
            // We iterate a fixed number of times to ensure convergence.
            repeat(15) { // 15 iterations are usually enough for sp precision.
                // If the difference between the limits is very small, we have converged.
                if (upperBoundSp - lowerBoundSp < 0.1f) {
                    currentFontSize = bestFitFontSize
                    readyToDraw = true
                    return@LaunchedEffect // We exit the LaunchedEffect.
                }

                val midSp = (lowerBoundSp + upperBoundSp) / 2f
                val candidateFontSize = midSp.sp

                // Avoid measuring sizes smaller than our known best fit, if we already passed them.
                if (candidateFontSize.value < bestFitFontSize.value && candidateFontSize.value < midSp) {
                    lowerBoundSp = midSp + 0.01f // Continue search in the upper half.
                    return@repeat // Skip this repeat iteration.
                }

                // We calculate the lineHeight dynamically based on the candidateFontSize.
                val currentEffectiveLineHeight = candidateFontSize * lineHeightRatio
                val candidateStyle = style.copy(
                    fontSize = candidateFontSize,
                    lineHeight = currentEffectiveLineHeight
                )

                val layoutResult = textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = candidateStyle,
                    overflow = TextOverflow.Clip,
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                    constraints = Constraints(
                        maxWidth = maxWidthPx.coerceAtLeast(0),
                        maxHeight = maxHeightPx.coerceAtLeast(0)
                    )
                )

                if (layoutResult.hasVisualOverflow) {
                    // The candidate size is too large (it overflows in height or width).
                    upperBoundSp = midSp - 0.01f
                } else {
                    // The candidate size fits. It is our new "best fit".
                    // We will try to find an even larger one.
                    bestFitFontSize = candidateFontSize
                    lowerBoundSp = midSp + 0.01f
                }
            }

            currentFontSize = bestFitFontSize
            readyToDraw = true
        }

        // We only draw the Text once we have determined the font size.
        if (readyToDraw) {
            // We apply the calculated fontSize and lineHeight to the final Text.
            val finalEffectiveLineHeight = currentFontSize * lineHeightRatio
            Text(
                text = text,
                modifier = Modifier, // The Text modifier doesn't need fillMaxSize here.
                style = style.copy(
                    fontSize = currentFontSize,
                    lineHeight = finalEffectiveLineHeight
                ),
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                overflow = TextOverflow.Ellipsis, // Truncates if, despite everything, it still overflows.
                softWrap = true,
                // The font size was chosen so that all lines fit in height.
                maxLines = Int.MAX_VALUE
            )
        }
    }
}