package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.SubscriptionTier
import com.example.ui.theme.DarkInk
import com.example.ui.theme.GoldBone
import com.example.ui.theme.LightCardBg
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.ProViolet
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSoft

@Composable
fun LightPageTopBar(
    title: String,
    showMenuButton: Boolean,
    userTier: SubscriptionTier,
    onOpenMenu: () -> Unit,
    onDownloadProjectZip: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPro = userTier == SubscriptionTier.PREMIUM

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(LightCardBg)
            .border(1.dp, LightCardBorder)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showMenuButton) {
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("top_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Abrir menu lateral",
                        tint = DarkInk
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = DarkInk
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onDownloadProjectZip,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("topbar_download_zip_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderZip,
                    contentDescription = "Baixar projeto inteiro (.ZIP)",
                    tint = TealPrimary
                )
            }

            Surface(
                onClick = onOpenPlans,
                color = if (isPro) ProViolet.copy(alpha = 0.14f) else TealSoft,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .border(
                        1.dp,
                        if (isPro) GoldBone else TealPrimary.copy(alpha = 0.35f),
                        RoundedCornerShape(18.dp)
                    )
                    .testTag("topbar_plan_pill")
            ) {
                Text(
                    text = if (isPro) "PRO ✦" else "BÁSICO",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isPro) ProViolet else TealPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(TealSoft)
                    .border(1.dp, TealPrimary.copy(alpha = 0.3f), CircleShape)
                    .clickable { onOpenProfile() }
                    .testTag("topbar_profile_avatar")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "Perfil e configurações",
                    tint = TealPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
