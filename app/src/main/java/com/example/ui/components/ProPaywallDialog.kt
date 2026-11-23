package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.fitness.ExerciseCatalog
import com.example.domain.model.SubscriptionTier
import com.example.ui.theme.CyanBox
import com.example.ui.theme.GoldBone
import com.example.ui.theme.LabBorder
import com.example.ui.theme.LabSurface
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ProViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlanTierPill(
    tier: SubscriptionTier,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPro = tier == SubscriptionTier.PREMIUM
    Surface(
        onClick = onClick,
        color = if (isPro) ProViolet.copy(alpha = 0.22f) else LabSurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (isPro) GoldBone.copy(alpha = 0.65f) else LabBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("plan_tier_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = if (isPro) GoldBone else CyanBox,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isPro) "PRO" else "BÁSICO",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isPro) GoldBone else TextPrimary
            )
        }
    }
}

@Composable
fun ProPaywallDialog(
    featureTitle: String,
    onUpgradePro: () -> Unit,
    onContinueBasic: () -> Unit,
    onViewAllPlans: () -> Unit
) {
    Dialog(
        onDismissRequest = onContinueBasic,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(LabSurface)
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(ProViolet, GoldBone)),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ProViolet.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = GoldBone, modifier = Modifier.size(15.dp))
                        Text(
                            text = "RECURSO FITVERSO PRO",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoldBone
                        )
                    }
                }
                IconButton(
                    onClick = onContinueBasic,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("paywall_close_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = TextSecondary)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Desbloqueie \"$featureTitle\"",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Treine sem limites com inteligência biomecânica completa em tempo real no FitVerso Pro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LabSurfaceElevated)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaywallBenefitRow("Todos os ${ExerciseCatalog.ALL_EXERCISES.size}+ exercícios com guia IA e 3D")
                PaywallBenefitRow("Detecção automática (Auto) do exercício na câmera")
                PaywallBenefitRow("Conversação por voz com o Coach FitVerso")
                PaywallBenefitRow("Exportação ilimitada de snapshots JSON e CSV")
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(ProViolet.copy(alpha = 0.28f), CyanBox.copy(alpha = 0.16f))
                        )
                    )
                    .border(1.dp, ProViolet.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FitVerso Pro",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "7 dias grátis · Cancele quando quiser",
                            style = MaterialTheme.typography.labelSmall,
                            color = MintSuccess
                        )
                    }
                    Text(
                        text = "R$ 29,90/mês",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldBone
                    )
                }
            }

            Button(
                onClick = onUpgradePro,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldBone,
                    contentColor = ObsidianBg
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paywall_upgrade_button")
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Assinar FitVerso Pro Agora",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onContinueBasic,
                    modifier = Modifier.testTag("paywall_continue_basic_button")
                ) {
                    Text("Continuar no Básico", color = TextSecondary)
                }
                Text(
                    text = "Comparar planos",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanBox,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onViewAllPlans() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PaywallBenefitRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MintSuccess,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary
        )
    }
}
