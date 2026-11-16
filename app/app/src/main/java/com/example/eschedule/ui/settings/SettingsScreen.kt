package com.example.eschedule.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eschedule.R
import com.example.eschedule.theme.Spacing
import com.example.eschedule.ui.components.CardRadius
import com.example.eschedule.ui.components.PageTitle
import com.example.eschedule.ui.components.ScreenMargin
import com.example.eschedule.ui.components.appShadow

// Settings: group label (Poppins Bold 12, #BBB) → 8dp → white card of 52dp rows.
// Each row = 16dp UEP-blue icon (Figma exports, scaled up from ~12dp), 16dp gap, Poppins 12 label.
private val GroupLabelColor = Color(0xFFBBBBBB)

private data class SettingsItem(
    val label: String,
    val iconRes: Int,
    val route: String,
)

@Composable
fun SettingsScreen(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val generalItems = remember {
        listOf(
            SettingsItem("Account Sync",            R.drawable.ic_settings_account_sync,  "account_sync"),
            SettingsItem("Widgets",                 R.drawable.ic_settings_widgets,       "widgets"),
            SettingsItem("Notification & Reminder", R.drawable.ic_settings_notifications, "notifications"),
            SettingsItem("Theme",                   R.drawable.ic_settings_theme,         "theme"),
        )
    }
    val aboutItems = remember {
        listOf(
            SettingsItem("Language",       R.drawable.ic_settings_language, "language"),
            SettingsItem("Feedback",       R.drawable.ic_settings_feedback, "feedback"),
            SettingsItem("Share App",      R.drawable.ic_settings_share,    "share"),
            SettingsItem("Follow Us",      R.drawable.ic_settings_follow,   "follow"),
            SettingsItem("Privacy Policy", R.drawable.ic_settings_privacy,  "privacy"),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        PageTitle(title = "Settings")

        SettingsGroup(label = "General", items = generalItems, onItemClick = onItemClick)
        Spacer(Modifier.height(Spacing.lg))
        SettingsGroup(label = "About", items = aboutItems, onItemClick = onItemClick)

        // Clear the floating nav bar
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun SettingsGroup(
    label: String,
    items: List<SettingsItem>,
    onItemClick: (String) -> Unit,
) {
    Text(
        text = label,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
        color = GroupLabelColor,
        modifier = Modifier.padding(horizontal = ScreenMargin),
    )
    Spacer(Modifier.height(Spacing.sm))
    Column(
        modifier = Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .appShadow(cornerRadius = CardRadius)
            .background(Color.White, RoundedCornerShape(CardRadius))
            .padding(vertical = Spacing.xs),
    ) {
        items.forEach { item ->
            SettingsRow(item = item, onClick = { onItemClick(item.route) })
        }
    }
}

@Composable
private fun SettingsRow(
    item: SettingsItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = item.label,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            maxLines = 1,
        )
    }
}
