package com.eschedule.app.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eschedule.app.theme.AppTheme
import com.eschedule.app.theme.Spacing
import com.eschedule.app.ui.components.EmptyAction
import com.eschedule.app.ui.components.FigmaTextMuted
import com.eschedule.app.ui.components.ScreenMargin
import com.eschedule.app.widget.WidgetPinner

// ─────────────────────────────────────────────────────────────────────────────
// The read-mostly Settings pages: About us, Privacy policy and Widgets.
// ─────────────────────────────────────────────────────────────────────────────

// ── Shared bits ───────────────────────────────────────────────────────────────

@Composable
private fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 19.sp,
        color = AppTheme.colors.text75,
        modifier = modifier,
    )
}

/** A circle with a letter or two in it, in the app's accent colour. */
@Composable
private fun Monogram(text: String, size: Int = 40) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(AppTheme.colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = (size * 0.36f).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

// ── About us ──────────────────────────────────────────────────────────────────

private data class Developer(val name: String, val role: String) {
    /** First letter of the first and last name ("Jules Ian C. Tomacas" → "JT"). */
    val initials: String
        get() = name.split(" ").filter { it.isNotBlank() }.let { "${it.first().first()}${it.last().first()}" }
}

private val Developers = listOf(
    Developer("Jules Ian C. Tomacas", "Lead UI/UX & Developer"),
    Developer("Jovan P. Atencio", "Reminder, Testing & Integration"),
    Developer("Keniel Drew D. De Asis", "UI/UX & Frontend Developer"),
    Developer("Kent Jeanne S. De Leon", "Project Manager, Documentation & Requirements"),
)

private val AboutParagraphs = listOf(
    "eSchedule turns your class schedule into a clear, organized week. Snap a photo of a printed " +
        "or on-screen timetable and the app reads it for you, filling in every class with its " +
        "time, days and room so you never have to type a schedule out by hand.",
    "Add reminders for tasks and deadlines, get a heads-up before each class begins, and keep " +
        "your day in view from the home screen with a widget. Everything is stored on your " +
        "device, so your schedule stays private and works without an account.",
    "eSchedule is designed and built by a small team that believes staying organized should " +
        "take seconds, not setup.",
)

/** Space left under a page so its last line can scroll above the floating nav bar. */
private val BottomClearance = 140.dp

@Composable
fun AboutContent() {
    Column(modifier = Modifier.padding(horizontal = ScreenMargin)) {
        Text(
            text = "eSchedule",
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.title,
        )
        Spacer(Modifier.height(Spacing.sm))
        AboutParagraphs.forEachIndexed { index, paragraph ->
            if (index > 0) Spacer(Modifier.height(Spacing.md))
            BodyText(paragraph)
        }

        Spacer(Modifier.height(Spacing.lg))
        GroupLabel("Meet the devs")
        SettingsCard {
            Developers.forEachIndexed { index, dev ->
                if (index > 0) HairlineDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Monogram(dev.initials)
                    Spacer(Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dev.name,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.text,
                        )
                        Text(
                            text = dev.role,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = FigmaTextMuted,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(BottomClearance))
    }
}

@Composable
private fun HairlineDivider() {
    Box(
        Modifier
            .padding(horizontal = Spacing.md)
            .fillMaxWidth()
            .height(1.dp)
            .background(AppTheme.colors.hairline),
    )
}

// ── Privacy policy ────────────────────────────────────────────────────────────

private data class PolicySection(val heading: String, val body: String)

private val PolicySections = listOf(
    PolicySection(
        "Your data stays on your phone",
        "Your classes, reminders and settings are saved on this device only. eSchedule has no " +
            "accounts and no servers of its own, so we never receive a copy of your schedule.",
    ),
    PolicySection(
        "Photos you scan",
        "When you scan a schedule, the photo is shrunk and sent over the internet to Google's " +
            "Gemini service so the text can be read. That is the only time a photo leaves your " +
            "phone. eSchedule doesn't keep the photo after reading it, and the service's own " +
            "handling of it is covered by Google's terms.",
    ),
    PolicySection(
        "Your API keys",
        "Keys you add under API keys are kept in the app's private storage on this device and " +
            "are only sent to Google when a scan runs. You can delete them any time, and Reset " +
            "app removes them along with everything else.",
    ),
    PolicySection(
        "Permissions we ask for",
        "Camera: to take a picture of your schedule. Notifications and alarms: to remind you " +
            "before a class or reminder is due. Internet: to read a scan. Nothing else.",
    ),
    PolicySection(
        "No tracking",
        "No ads, no analytics, no profiling and no selling or sharing of data.",
    ),
    PolicySection(
        "Deleting your data",
        "Reset app wipes your classes, reminders, settings and keys. Uninstalling eSchedule " +
            "removes whatever is left.",
    ),
    PolicySection(
        "Changes",
        "If this policy ever changes, the new version will be in this screen after you update.",
    ),
)

@Composable
fun PrivacyContent() {
    Column(modifier = Modifier.padding(horizontal = ScreenMargin)) {
        BodyText("The short version: eSchedule keeps your schedule to itself.")
        PolicySections.forEach { section ->
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = section.heading,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.text,
            )
            Spacer(Modifier.height(Spacing.xs))
            BodyText(section.body)
        }
        Spacer(Modifier.height(BottomClearance))
    }
}

// ── Widgets ───────────────────────────────────────────────────────────────────

@Composable
fun WidgetsContent() {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(horizontal = ScreenMargin)) {
        BodyText("Keep your day on your home screen. The widget updates by itself as your classes start and end.")
        Spacer(Modifier.height(Spacing.md))

        SettingsCard {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = "Today's schedule",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.text,
                )
                Spacer(Modifier.height(Spacing.xs))
                BodyText("Every class you have left today in order, with the one in progress highlighted.")
                Spacer(Modifier.height(Spacing.md))
                EmptyAction(
                    text = "Add to home screen",
                    onClick = { pinWidget(context) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Can't see the button work? Press and hold an empty spot on your home screen, " +
                "tap Widgets and look for eSchedule. The widget follows your phone's light or dark " +
                "setting.",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = FigmaTextMuted,
        )
        Spacer(Modifier.height(BottomClearance))
    }
}

private fun pinWidget(context: Context) {
    if (WidgetPinner.pinToday(context) == WidgetPinner.Result.Unsupported) {
        Toast.makeText(
            context,
            "Your launcher can't add widgets from here. Press and hold your home screen and pick eSchedule under Widgets.",
            Toast.LENGTH_LONG,
        ).show()
    }
}
