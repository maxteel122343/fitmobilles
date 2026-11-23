package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.fitness.ExerciseCatalog
import com.example.domain.model.SubscriptionTier
import com.example.domain.model.UserFitnessProfile
import com.example.ui.theme.DarkInk
import com.example.ui.theme.GoldBone
import com.example.ui.theme.LightCanvasBg
import com.example.ui.theme.LightCardBg
import com.example.ui.theme.LightCardBorder
import com.example.ui.theme.MutedInk
import com.example.ui.theme.ProViolet
import com.example.ui.theme.SidebarNavy
import com.example.ui.theme.TealMint
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSoft

@Composable
fun PlansScreen(
    profile: UserFitnessProfile,
    onSelectTier: (SubscriptionTier) -> Unit,
    modifier: Modifier = Modifier
) {
    var annualBilling by remember { mutableStateOf(true) }
    var showCheckoutConfirm by remember { mutableStateOf(false) }
    val isPro = profile.tier == SubscriptionTier.PREMIUM

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LightCanvasBg),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Planos & Assinatura",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = DarkInk
                )
                Text(
                    text = "Compare o Plano Básico gratuito com o FitVerso Pro e escolha como deseja treinar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk
                )
            }
        }

        // Current Active Status Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightCardBg)
                    .border(
                        1.dp,
                        if (isPro) GoldBone else LightCardBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("SEU PLANO ATUAL", style = MaterialTheme.typography.labelSmall, color = MutedInk)
                    Text(
                        text = profile.tier.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isPro) ProViolet else TealPrimary
                    )
                    Text(
                        text = "${profile.totalLifetimeReps} reps · ${profile.workoutsCompleted} treinos concluídos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk
                    )
                }
                Surface(
                    color = if (isPro) ProViolet.copy(alpha = 0.14f) else TealSoft,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isPro) "PRO ATIVO" else "GRATUITO",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isPro) ProViolet else TealPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Billing Cycle Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !annualBilling,
                    onClick = { annualBilling = false },
                    label = { Text("Mensal") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = LightCardBg,
                        labelColor = DarkInk
                    )
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = annualBilling,
                    onClick = { annualBilling = true },
                    label = { Text("Anual · Economize 33%") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SidebarNavy,
                        selectedLabelColor = GoldBone,
                        containerColor = LightCardBg,
                        labelColor = DarkInk
                    )
                )
            }
        }

        // FitVerso Pro Plan Card (Distinctive Navy + Gold Hero Card)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(SidebarNavy, Color(0xFF1F2E3E))
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(listOf(TealPrimary, GoldBone)),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.WorkspacePremium, null, tint = GoldBone, modifier = Modifier.size(22.dp))
                            Text("FitVerso Pro", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                        Surface(color = GoldBone, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = "MAIS POPULAR",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SidebarNavy,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (annualBilling) "R$ 19,90" else "R$ 29,90",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoldBone
                        )
                        Text(
                            text = if (annualBilling) "/mês (R$ 238,80/ano)" else "/mês · 7 dias grátis",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    PlanFeatureRow("Biblioteca completa com ${ExerciseCatalog.ALL_EXERCISES.size}+ exercícios e IA que ensina a forma", TealMint, Color.White)
                    PlanFeatureRow("Coach por Voz + APIs Gemini, DeepSeek, ChatGPT, ElevenLabs e Imagem", TealMint, Color.White)
                    PlanFeatureRow("Exportação ilimitada de snapshots JSON, CSV e malha 3D .obj", TealMint, Color.White)
                    PlanFeatureRow("Avatar 3D completo com InstantHMR Medium + Modo Web (Celular e PC)", TealMint, Color.White)

                    if (isPro) {
                        Surface(
                            color = TealMint.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ Assinatura FitVerso Pro ativa neste dispositivo",
                                style = MaterialTheme.typography.labelMedium,
                                color = TealMint,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = { showCheckoutConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldBone, contentColor = SidebarNavy),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("plans_subscribe_pro_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Assinar FitVerso Pro", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // Basic Free Plan Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(LightCardBg)
                    .border(1.dp, if (!isPro) TealPrimary else LightCardBorder, RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Plano Básico", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = DarkInk)
                    Text("Grátis", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TealPrimary)
                }
                Text(
                    text = "Ideal para treinar os movimentos principais com feedback em tempo real.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk
                )

                PlanFeatureRow("${ExerciseCatalog.BASIC_INCLUDED_IDS.size} exercícios essenciais gratuitos + Modo Auto", TealPrimary, DarkInk)
                PlanFeatureRow("Câmera ao vivo, fotos e vídeos com contador de reps", TealPrimary, DarkInk)
                PlanFeatureRow("Feedback visual de forma (Verde / Amarelo / Vermelho)", TealPrimary, DarkInk)
                PlanFeatureRow("Avatar 3D interativo + Modo Web no navegador do celular e PC", TealPrimary, DarkInk)

                if (!isPro) {
                    Surface(
                        color = TealSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✓ Seu plano atual (Grátis)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TealPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { onSelectTier(SubscriptionTier.BASIC) },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plans_switch_basic_button")
                    ) {
                        Text("Mudar para Plano Básico (Grátis)", color = MutedInk)
                    }
                }
            }
        }
    }

    if (showCheckoutConfirm) {
        AlertDialog(
            onDismissRequest = { showCheckoutConfirm = false },
            containerColor = LightCardBg,
            title = {
                Text("Confirmar Assinatura FitVerso Pro", color = DarkInk, fontWeight = FontWeight.Bold)
            },
            text = {
                val priceDesc = if (annualBilling) "Plano Anual · R$ 19,90/mês" else "Plano Mensal · R$ 29,90/mês"
                Text(
                    text = "$priceDesc com 7 dias de teste gratuito.\n\nAo confirmar, todos os ${ExerciseCatalog.ALL_EXERCISES.size}+ exercícios, Coach por voz e exportação JSON/CSV serão desbloqueados imediatamente neste app.",
                    color = MutedInk
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCheckoutConfirm = false
                        onSelectTier(SubscriptionTier.PREMIUM)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_pro_checkout_button")
                ) {
                    Text("Ativar FitVerso Pro")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutConfirm = false }) {
                    Text("Cancelar", color = MutedInk)
                }
            }
        )
    }
}

@Composable
private fun PlanFeatureRow(
    text: String,
    iconTint: Color,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.CheckCircle, null, tint = iconTint, modifier = Modifier.size(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = textColor)
    }
}
