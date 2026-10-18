package com.programmer.highalarm

import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Currency
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSubscriptionScreen(
    onBack: () -> Unit,
    isFromLockedChallenge: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val billingManager = remember { BillingManager.getInstance(context, scope) }
    val productDetails by billingManager.productDetails.collectAsState()
    val isPremium by billingManager.isPremium.collectAsState()
    
    val isReducedMotionEnabled = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    var selectedPlanId by remember { mutableStateOf("annual_subscription") } // "annual_subscription", "monthly_subscription", "lifetime_purchase"

    // Dynamic pricing lookup from Google Play Billing (Product ID "premium" for SUBS, "lifetime" for INAPP)
    val premiumDetails = productDetails.find { it.productId == "premium" } ?: productDetails.find { it.productId == "annual_subscription" } ?: productDetails.find { it.productId == "monthly_subscription" }
    val lifetimeDetails = productDetails.find { it.productId == "lifetime" } ?: productDetails.find { it.productId == "lifetime_purchase" }

    val yearlyOffer = premiumDetails?.subscriptionOfferDetails?.find { it.basePlanId == "yearly" } 
        ?: premiumDetails?.subscriptionOfferDetails?.firstOrNull()
    val monthlyOffer = premiumDetails?.subscriptionOfferDetails?.find { it.basePlanId == "monthly" } 
        ?: premiumDetails?.subscriptionOfferDetails?.drop(1)?.firstOrNull() 
        ?: premiumDetails?.subscriptionOfferDetails?.firstOrNull()

    val yearlyPhaseList = yearlyOffer?.pricingPhases?.pricingPhaseList ?: emptyList()
    val yearlyPricingPhase = yearlyPhaseList.lastOrNull()
    val yearlyIntroPhase = yearlyPhaseList.firstOrNull { it.priceAmountMicros == 0L || it.billingCycleCount > 0 }

    val monthlyPhaseList = monthlyOffer?.pricingPhases?.pricingPhaseList ?: emptyList()
    val monthlyPricingPhase = monthlyPhaseList.lastOrNull()

    val lifetimeOffer = lifetimeDetails?.oneTimePurchaseOfferDetails

    val isLoading = productDetails.isEmpty() || (yearlyPricingPhase == null && monthlyPricingPhase == null && lifetimeOffer == null)

    val yearlyPriceMicros = yearlyPricingPhase?.priceAmountMicros ?: 0L
    val yearlyCurrencyCode = yearlyPricingPhase?.priceCurrencyCode ?: monthlyPricingPhase?.priceCurrencyCode ?: "USD"
    val yearlyFormattedAnnual = (yearlyPricingPhase?.formattedPrice ?: "").replace(Regex("([A-Za-z]+)([0-9])"), "$1 $2")

    val monthlyEquivalentFormatted = if (yearlyPriceMicros > 0L) {
        try {
            val currency = Currency.getInstance(yearlyCurrencyCode)
            val format = NumberFormat.getCurrencyInstance().apply { this.currency = currency }
            format.format((yearlyPriceMicros / 12) / 1000000.0).replace(Regex("([A-Za-z]+)([0-9])"), "$1 $2")
        } catch (e: Exception) {
            yearlyFormattedAnnual
        }
    } else {
        yearlyFormattedAnnual
    }

    val monthlyFormatted = (monthlyPricingPhase?.formattedPrice ?: "").replace(Regex("([A-Za-z]+)([0-9])"), "$1 $2")
    val lifetimeFormatted = (lifetimeOffer?.formattedPrice ?: "").replace(Regex("([A-Za-z]+)([0-9])"), "$1 $2")

    val yearlyDisplayPrice = if (isLoading) "Loading..." else if (monthlyEquivalentFormatted.isNotEmpty()) "$monthlyEquivalentFormatted / mo" else "Unavailable"
    val yearlyOriginalMonthlyPrice = if (isLoading) null else if (monthlyFormatted.isNotEmpty()) "$monthlyFormatted / month" else null
    val yearlySupportingText = if (isLoading) "Loading prices..." else buildString {
        if (monthlyEquivalentFormatted.isNotEmpty() && yearlyFormattedAnnual.isNotEmpty()) {
            append("≈ $monthlyEquivalentFormatted/month • Billed annually at $yearlyFormattedAnnual")
        } else if (yearlyFormattedAnnual.isNotEmpty()) {
            append("Billed annually at $yearlyFormattedAnnual")
        }
    }.ifEmpty { "Billed annually" }

    val monthlyDisplayPrice = if (isLoading) "Loading..." else if (monthlyFormatted.isNotEmpty()) "$monthlyFormatted / month" else "Unavailable"
    val lifetimeDisplayPrice = if (isLoading) "Loading..." else lifetimeFormatted.ifEmpty { "Unavailable" }

    // Staggered Animation State
    var startStaggeredAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        billingManager.queryPurchases()
        delay(100.milliseconds)
        startStaggeredAnimation = true
    }

    LaunchedEffect(isPremium) {
        if (isPremium) onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlue)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Premium Header with Morning Sunrise ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    // Living 3D sunrise scene (animated, reacts to phone tilt)
                    PremiumSunriseBackground(
                        modifier = Modifier.matchParentSize(),
                        reducedMotion = isReducedMotionEnabled,
                        fadeTo = MidnightBlue
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        // Navigation Bar
                        StaggeredItem(index = 0, start = startStaggeredAnimation, isReducedMotion = isReducedMotionEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(34.dp).ha3DCoin(HaTone.Gold, elevation = 6.dp), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.WorkspacePremium, null, tint = haToneInk(HaTone.Gold), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("High Alarm Pro", color = HaIvory, fontWeight = FontWeight.Black, fontSize = 19.sp)
                                }
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(36.dp).ha3DOrb()
                                ) {
                                    Icon(Icons.Default.Close, null, tint = HaIvory, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // Hero Content - Dynamic Stack
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 12.dp, bottom = 30.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // Contextual message if from locked challenge
                            if (isFromLockedChallenge) {
                                StaggeredItem(index = 1, start = startStaggeredAnimation, isReducedMotion = isReducedMotionEnabled) {
                                    Surface(
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .padding(bottom = 12.dp)
                                            .ha3DPanelDeep(RoundedCornerShape(10.dp), elevation = 8.dp)
                                    ) {
                                        Text(
                                            "Unlock this challenge and more with Pro",
                                            color = SoftAmber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            StaggeredItem(index = 2, start = startStaggeredAnimation, isReducedMotion = isReducedMotionEnabled) {
                                Surface(color = Color.Transparent, shape = RoundedCornerShape(8.dp), modifier = Modifier.ha3DGoldFace(RoundedCornerShape(8.dp), elevation = 6.dp)) {
                                    Text("RISE • FOCUS • ACHIEVE", color = Color(0xFF1A140A), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), letterSpacing = 2.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            StaggeredItem(index = 3, start = startStaggeredAnimation, isReducedMotion = isReducedMotionEnabled) {
                                val shimmerTransition = rememberInfiniteTransition(label = "title_shimmer")
                                val shimmerOffset by shimmerTransition.animateFloat(initialValue = -800f, targetValue = 1800f, animationSpec = infiniteRepeatable(animation = tween(6000, easing = EaseInOutSine), repeatMode = RepeatMode.Restart), label = "shimmer_offset")
                                val titleBrush = if (isReducedMotionEnabled) Brush.linearGradient(listOf(SoftAmber, HaGoldPale)) else Brush.linearGradient(colors = listOf(HaGold, HaGoldPale, HaGold), start = Offset(shimmerOffset, 0f), end = Offset(shimmerOffset + 350f, 0f))
                                Text(
                                    text = "Never Oversleep\nAgain", 
                                    style = TextStyle(
                                        brush = titleBrush, 
                                        shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), offset = Offset(0f, 4f), blurRadius = 6f)
                                    ), 
                                    fontSize = 34.sp, 
                                    fontWeight = FontWeight.Black, 
                                    lineHeight = 38.sp, 
                                    letterSpacing = (-1).sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            StaggeredItem(index = 4, start = startStaggeredAnimation, isReducedMotion = isReducedMotionEnabled) {
                                Text(
                                    text = "Take control of your mornings.", 
                                    color = HaIvory.copy(alpha = 0.65f), 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            }

            // --- Why Go Pro ---
            item {
                StaggeredItem(index = 5, start = startStaggeredAnimation, isReducedMotion = isReducedMotionEnabled) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text("Why Go Pro?", color = HaIvory, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
                        Surface(modifier = Modifier.fillMaxWidth().ha3DPanel(RoundedCornerShape(24.dp), elevation = 12.dp), shape = RoundedCornerShape(24.dp), color = Color.Transparent) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Cancel, null, tint = HaCoral.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Without Pro", color = HaMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("No follow-up checks", color = HaIvory.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                }
                                Box(modifier = Modifier.width(1.dp).height(60.dp).background(HaIvory.copy(0.08f)))
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircle, null, tint = HaJade, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("With Pro", color = SoftAmber, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    Text("Verification + missions", color = HaIvory, fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }

            item {
                StaggeredItem(index = 6, start = startStaggeredAnimation, isReducedMotion = isReducedMotionEnabled) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BenefitRow(Icons.Default.Psychology, "17+ wake challenges", "Math, Maze, Shake, Steps, Scan & more")
                        BenefitRow(Icons.Default.Verified, "Wake-Up Verification", "A follow-up check ensures you are awake.")
                        BenefitRow(Icons.Default.Vibration, "Powerful vibration", "8 patterns designed for deep sleepers.")
                    }
                }
            }

            // --- Plan Selection ---
            item {
                StaggeredItem(index = 7, start = startStaggeredAnimation, isReducedMotion = isReducedMotionEnabled) {
                    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                        Text("Choose Your Plan", color = HaIvory, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))

                        PremiumPlanCard(title = "Annual", price = yearlyDisplayPrice, originalPrice = yearlyOriginalMonthlyPrice, badgeText = "70% OFF", supportingText = yearlySupportingText, isSelected = selectedPlanId == "annual_subscription", isRecommended = true, isReducedMotion = isReducedMotionEnabled, onClick = { selectedPlanId = "annual_subscription" })
                        PremiumPlanCard(title = "Monthly", price = monthlyDisplayPrice, supportingText = "Renews monthly afterwards", isSelected = selectedPlanId == "monthly_subscription", isReducedMotion = isReducedMotionEnabled, onClick = { selectedPlanId = "monthly_subscription" })
                        PremiumPlanCard(title = "Lifetime", price = lifetimeDisplayPrice, supportingText = "One-time payment", isSelected = selectedPlanId == "lifetime_purchase", isReducedMotion = isReducedMotionEnabled, onClick = { selectedPlanId = "lifetime_purchase" })
                    }
                }
            }

            item {
                StaggeredItem(index = 8, start = startStaggeredAnimation, isReducedMotion = isReducedMotionEnabled) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        UnlockProButton(
                            isLoading = isLoading,
                            isReducedMotion = isReducedMotionEnabled,
                            onClick = {
                                val activity = context.findActivity()
                                if (activity != null) {
                                    billingManager.launchPurchaseFlow(activity, selectedPlanId)
                                } else {
                                    Toast.makeText(context, "Unable to launch purchase screen.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Secure payment • Cancel anytime", color = HaIvory.copy(0.4f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(24.dp))
                        TextButton(onClick = { 
                            billingManager.restorePurchases { success, msg -> 
                                scope.launch {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (success) onBack()
                                }
                            }
                        }) {
                            Text("Restore Purchases", color = HaIvory.copy(0.35f), fontSize = 12.sp, textDecoration = TextDecoration.Underline)
                        }
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniFooterBadge(Icons.Default.CheckCircle, "Secure Checkout")
                    MiniFooterBadge(Icons.Default.CreditCard, "Multiple Methods")
                    MiniFooterBadge(Icons.Default.LockReset, "Cancel Anytime")
                    MiniFooterBadge(Icons.Default.GppGood, "No Hidden Fees")
                }
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun StaggeredItem(
    index: Int,
    start: Boolean,
    isReducedMotion: Boolean,
    content: @Composable () -> Unit
) {
    if (isReducedMotion) {
        Box { content() }
    } else {
        val animationDelay = index * 100
        val alpha by animateFloatAsState(
            targetValue = if (start) 1f else 0f,
            animationSpec = tween(durationMillis = 600, delayMillis = animationDelay, easing = EaseOutQuart),
            label = "alpha"
        )
        val offset by animateDpAsState(
            targetValue = if (start) 0.dp else 20.dp,
            animationSpec = tween(durationMillis = 600, delayMillis = animationDelay, easing = EaseOutQuart),
            label = "offset"
        )
        
        Box(modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = offset.toPx() }) {
            content()
        }
    }
}

@Composable
fun BenefitRow(icon: ImageVector, title: String, desc: String) {
    Surface(modifier = Modifier.fillMaxWidth().ha3DPanel(RoundedCornerShape(16.dp), elevation = 8.dp), shape = RoundedCornerShape(16.dp), color = Color.Transparent) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).ha3DGoldCoin(), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color(0xFF2A1F0E), modifier = Modifier.size(18.dp)) }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, color = HaIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(desc, color = HaMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun PremiumPlanCard(title: String, price: String, originalPrice: String? = null, badgeText: String? = null, supportingText: String? = null, isSelected: Boolean, isRecommended: Boolean = false, isReducedMotion: Boolean = false, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_motion")
    val glowAlpha by if (isRecommended || isSelected) {
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(animation = tween(2000, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
            label = "border_glow"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val finalGlowAlpha = if (isReducedMotion) 0.15f else glowAlpha
    val borderColor = when {
        isSelected -> SoftAmber
        isRecommended -> SoftAmber.copy(alpha = 0.45f)
        else -> HaIvory.copy(alpha = 0.12f)
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).graphicsLayer { shadowElevation = if (isSelected) 10.dp.toPx() else 0f; shape = RoundedCornerShape(18.dp); clip = true }
            .drawBehind {
                if (isRecommended || isSelected) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(SoftAmber.copy(alpha = finalGlowAlpha), Color.Transparent),
                            center = center,
                            radius = size.maxDimension / 1.5f
                        )
                    )
                }
                if (isSelected && !isReducedMotion) {
                    drawRect(brush = Brush.linearGradient(listOf(Color.Transparent, HaIvory.copy(0.1f), Color.Transparent)), style = Stroke(width = 2.dp.toPx()))
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) HaIvory.copy(0.06f) else Color.Transparent,
        border = BorderStroke(if (isSelected) 2.2.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(20.dp)
                        .border(1.5.dp, if (isSelected) SoftAmber else HaIvory.copy(0.3f), CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Box(modifier = Modifier.fillMaxSize().background(SoftAmber, CircleShape))
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                title,
                                color = HaIvory,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (badgeText != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (badgeText.contains("70%")) HaGoldLight.copy(0.2f) else SoftAmber.copy(0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, if (badgeText.contains("70%")) HaGoldLight.copy(0.5f) else SoftAmber.copy(0.4f))
                                ) {
                                    Text(
                                        badgeText,
                                        color = if (badgeText.contains("70%")) HaGoldLight else SoftAmber,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            val priceFontSize = when {
                                price.length > 14 -> 12.sp
                                price.length > 11 -> 13.sp
                                price.length > 9 -> 14.sp
                                else -> 17.sp
                            }
                            Text(
                                text = price,
                                color = HaIvory,
                                fontWeight = FontWeight.Black,
                                fontSize = priceFontSize,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible,
                                textAlign = TextAlign.End
                            )
                            if (originalPrice != null) {
                                Text(
                                    text = originalPrice,
                                    color = HaIvory.copy(0.4f),
                                    fontSize = 11.sp,
                                    textDecoration = TextDecoration.LineThrough,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    // BestValueBadge is placed inline next to 70% OFF badge

                    if (supportingText != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = supportingText,
                            color = HaIvory.copy(0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            softWrap = true
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun UnlockProButton(isLoading: Boolean, isReducedMotion: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "cta_motion")
    val floatY by if (isReducedMotion) remember { mutableFloatStateOf(0f) } else {
        infiniteTransition.animateFloat(initialValue = 0f, targetValue = -2f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse), label = "float")
    }
    val shimmerProgress by if (isReducedMotion) remember { mutableFloatStateOf(-1f) } else {
        infiniteTransition.animateFloat(
            initialValue = -1f, targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 5000
                    (-1f) at 0
                    (-1f) at 3000
                    2f at 4200
                    2f at 5000
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        )
    }
    val glowScale by if (isReducedMotion) remember { mutableFloatStateOf(1f) } else {
        infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.025f, animationSpec = infiniteRepeatable(animation = tween(2500, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse), label = "glow")
    }

    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(56.dp).offset(y = floatY.dp).graphicsLayer { scaleX = glowScale; scaleY = glowScale }
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(18.dp), ambientColor = SoftAmber.copy(0.45f), spotColor = SoftAmber.copy(0.45f)),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SoftAmber,
            contentColor = MidnightBlue,
            disabledContainerColor = SoftAmber.copy(alpha = 0.5f),
            disabledContentColor = MidnightBlue.copy(alpha = 0.5f)
        )
    ) {
        val shimmerBrush = if (isLoading || shimmerProgress < 0f || isReducedMotion) null else {
            Brush.linearGradient(colors = listOf(MidnightBlue, HaIvory.copy(0.45f), MidnightBlue), start = Offset(shimmerProgress * 600f, 0f), end = Offset(shimmerProgress * 600f + 120f, 0f))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isLoading) "LOADING..." else "UNLOCK PRO NOW", style = if (shimmerBrush != null) TextStyle(brush = shimmerBrush) else TextStyle.Default.copy(color = MidnightBlue), fontSize = 17.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Spacer(modifier = Modifier.width(10.dp))
            if (!isLoading) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun MiniFooterBadge(icon: ImageVector, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(84.dp)) {
        Icon(icon, null, tint = HaIvory.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text, color = HaIvory.copy(alpha = 0.35f), fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 11.sp)
    }
}
