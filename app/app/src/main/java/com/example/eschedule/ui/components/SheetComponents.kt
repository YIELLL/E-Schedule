package com.example.eschedule.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eschedule.R
import com.example.eschedule.theme.Spacing
import com.example.eschedule.theme.UepBlue

// ─────────────────────────────────────────────────────────────────────────────
// Bottom-sheet building blocks shared by the Class and Reminder sheets.
//
//   Sheet   : #F7F7F7, 28dp top radius, 24dp side inset, drag pill on top
//             (swipe down or tap the dimmed area outside to dismiss)
//   Header  : Poppins Bold 20 UEP-blue title + 64 × 36 "Save" pill (or two 36dp circles)
//   Rhythm  : 16dp after the header, 24dp between groups, 8dp label → card
//   Cards   : white, radius 12, rows ≥ 48dp, 16dp inner padding
// ─────────────────────────────────────────────────────────────────────────────

val SheetInset = Spacing.lg
private val SheetScrim = Color(0xE0000000)            // #1E1E1E over #F7F7F7
private val SaveFill = Color(0xBF0088FF)              // rgba(0,136,255,0.75)

val SheetTextValue = Color(0xBF000000)                // black @ 75% — entered values
val SheetTextPlaceholder = Color(0x40000000)          // black @ 25% — placeholders

/** Subtle "you can swipe me away" pill shown at the top of every sheet. */
@Composable
fun SheetDragPill() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = Spacing.sm)
            .size(width = 36.dp, height = 4.dp)
            .background(Color(0x33000000), RoundedCornerShape(2.dp)),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FigmaSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xFFF7F7F7),
        scrimColor = SheetScrim,
        tonalElevation = 0.dp,
        dragHandle = { SheetDragPill() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = SheetInset, end = SheetInset, top = Spacing.xs, bottom = Spacing.xl),
            content = content,
        )
    }
}

/** Title row (40dp): title on the left, [trailing] actions on the right. */
@Composable
fun SheetHeader(
    title: String,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            color = UepBlue,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
fun SheetSaveButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 36.dp)
            .background(SaveFill, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Save",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

/** 36dp round action (Delete / Edit): black @ 20% disc with a 20dp white glyph. */
@Composable
fun SheetCircleAction(
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color(0x33000000), CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = description,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Group label: Poppins SemiBold 12 / 18, black @ 50%. */
@Composable
fun SheetLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = FigmaTextMuted,
    )
}

/** Vertical rhythm helper so screens read as a sequence of groups. */
@Composable fun SheetGap(height: Dp) = Spacer(Modifier.height(height))

/** White card with the shared shadow. */
@Composable
fun SheetCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(CardRadius),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .appShadow(cornerRadius = CardRadius)
            .background(Color.White, shape),
        content = content,
    )
}

/**
 * Title field: 4dp accent bar + white card (≥ 56dp, grows with wrapped text),
 * Poppins Medium 20 / 30.
 */
@Composable
fun SheetTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Title",
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(UepBlue, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(12.dp))
        SheetCard(modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Medium,
                    color = SheetTextValue,
                ),
                cursorBrush = SolidColor(UepBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = 13.dp),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 20.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Medium,
                                color = SheetTextPlaceholder,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

/**
 * Icon + text card (Location / Description / Notes): ≥ 48dp tall, 20dp icon,
 * 12dp icon-to-text gap. [bottomRadius] is larger on the last field of a sheet.
 */
@Composable
fun SheetIconField(
    iconRes: Int,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    bottomRadius: Dp = CardRadius,
) {
    SheetCard(
        modifier = Modifier.heightIn(min = Spacing.touchTarget),
        shape = RoundedCornerShape(CardRadius, CardRadius, bottomRadius, bottomRadius),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.touchTarget)
                .padding(horizontal = Spacing.md, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(painterResource(iconRes), null, Modifier.size(20.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    color = SheetTextValue,
                ),
                cursorBrush = SolidColor(UepBlue),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                color = SheetTextPlaceholder,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

/** Read-only variant of [SheetIconField] used by the View sheets. */
@Composable
fun SheetIconText(
    iconRes: Int,
    text: String,
    textColor: Color,
    bottomRadius: Dp = CardRadius,
) {
    SheetCard(
        modifier = Modifier.heightIn(min = Spacing.touchTarget),
        shape = RoundedCornerShape(CardRadius, CardRadius, bottomRadius, bottomRadius),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.touchTarget)
                .padding(horizontal = Spacing.md, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(painterResource(iconRes), null, Modifier.size(20.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = textColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 1dp hairline, black @ 10%. */
@Composable
fun SheetHairline(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(Color(0x1A000000)))
}

/** Hairline inset 16dp from both card edges, used between option rows. */
@Composable
fun OptionDivider() {
    SheetHairline(Modifier.padding(horizontal = Spacing.md))
}

/** One option row (48dp): label on the left, value + 15dp chevron on the right. */
@Composable
fun OptionRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touchTarget)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
            color = FigmaTextMuted,
        )
        Spacer(Modifier.width(Spacing.sm))
        Image(painterResource(R.drawable.ic_sheet_expand15), null, Modifier.size(16.dp))
    }
}
