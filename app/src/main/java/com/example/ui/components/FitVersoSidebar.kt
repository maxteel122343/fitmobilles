package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.SubscriptionTier
import com.example.ui.theme.GoldBone
import com.example.ui.theme.LabBorder
import com.example.ui.theme.ProViolet
import com.example.ui.theme.SidebarItemActive
import com.example.ui.theme.SidebarNavy
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class FitVersoNavItem(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("Início", Icons.Rounded.Home, "sidebar_home"),
    LIVE("Treino ao vivo", Icons.Rounded.Sensors, "sidebar_live"),
    EXERCISES("Exercícios", Icons.Rounded.FitnessCenter, "sidebar_exercises"),
    HISTORY("Histórico", Icons.Rounded.History, "sidebar_history"),
    PROGRESS("Progresso", Icons.Rounded.AutoGraph, "sidebar_progress"),
    COACH("Coach IA & Voz", Icons.Rounded.RecordVoiceOver, "sidebar_coach"),
    PLANS("Planos & Pro", Icons.Rounded.WorkspacePremium, "sidebar_plans"),
    PROFILE("Perfil e configurações", Icons.Rounded.Person, "sidebar_profile")
}

@Composable
fun FitVersoSidebar(
    activeItem: FitVersoNavItem,
    userTier: SubscriptionTier,
    onSelect: (FitVersoNavItem) -> Unit,
    onDownloadProjectZip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPro = userTier == SubscriptionTier.PREMIUM

    Column(
        modifier = modifier
            .width(236.dp)
            .fillMaxHeight()
            .background(SidebarNavy)
            .padding(horizontal = 14.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Brand Header matching mockups
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(TealPrimary.copy(alpha = 0.25f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ViewInAr,
                        contentDescription = "FitVerso",
                        tint = SidebarItemActive,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "FitVerso",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(10.dp))

            FitVersoNavItem.entries.forEach { item ->
                val selected = item == activeItem
                val bg = if (selected) SidebarItemActive else Color.Transparent
                val fg = if (selected) Color.White else TextSecondary

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { onSelect(item) }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .testTag(item.testTag)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (item == FitVersoNavItem.PLANS && isPro && !selected) GoldBone else fg,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                onClick = onDownloadProjectZip,
                color = TealPrimary.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TealPrimary.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .testTag("sidebar_download_zip_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderZip,
                        contentDescription = "Baixar projeto inteiro (.ZIP)",
                        tint = SidebarItemActive,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Baixar Projeto (.ZIP)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Active Subscription Footer Card (keeps Basic & Premium clear in Sidebar)
            Surface(
                onClick = { onSelect(FitVersoNavItem.PLANS) },
                color = if (isPro) ProViolet.copy(alpha = 0.22f) else Color(0xFF202D3C),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isPro) GoldBone.copy(alpha = 0.55f) else LabBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .testTag("sidebar_plan_card")
            ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isPro) GoldBone else SidebarItemActive,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPro) "FitVerso Pro" else "Plano Básico",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isPro) GoldBone else TextPrimary
                    )
                    Text(
                        text = if (isPro) "Acesso total liberado" else "Fazer upgrade →",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
        }
    }
}
