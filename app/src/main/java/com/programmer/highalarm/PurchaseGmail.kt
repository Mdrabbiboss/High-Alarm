package com.programmer.highalarm

import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lives at the app root. When a purchase finishes (or the user taps the Gmail row) it asks
 * Android which Google account was used: with one account on the phone the answer comes
 * back instantly with no screen; with several, Android shows its list once.
 */
@Composable
fun PurchaseGmailDetector() {
    val context = LocalContext.current
    remember { PurchaseRecord.load(context); true }
    val askNow by PurchaseRecord.askNow.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val name = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        } else null
        PurchaseRecord.onPickerResult(context, name)
    }

    LaunchedEffect(askNow) {
        if (!askNow) return@LaunchedEffect
        try {
            picker.launch(
                AccountManager.newChooseAccountIntent(
                    null, null, arrayOf("com.google"),
                    "Which Gmail did you use to buy High Alarm PRO?",
                    null, null, null
                )
            )
        } catch (_: Exception) {
            PurchaseRecord.onPickerResult(context, null)
        }
    }
}

/** The single line in the PRO card: which Gmail owns PRO. */
@Composable
fun PurchaseGmailRow() {
    val context = LocalContext.current
    remember { PurchaseRecord.load(context); true }
    val email by PurchaseRecord.savedEmail.collectAsState()

    // PRO from before this update (or after a reinstall): detect once, automatically
    LaunchedEffect(Unit) { PurchaseRecord.requestGmail(context) }

    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(HaBlackRaised.copy(alpha = 0.92f), HaPanelDeep.copy(alpha = 0.92f))))
            .border(1.dp, HaGold.copy(alpha = 0.18f), shape)
            .clickable { PurchaseRecord.requestGmail(context, force = true) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(HaGold.copy(alpha = 0.10f))
                .border(1.dp, HaGold.copy(alpha = 0.24f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = HaGold, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Purchased with", fontSize = 11.sp, color = HaMuted)
            Spacer(Modifier.height(2.dp))
            val shown = email ?: "Tap to show your Gmail"
            Text(
                shown,
                // long addresses step down a size so the whole Gmail stays on one line
                fontSize = when {
                    shown.length > 30 -> 12.sp
                    shown.length > 24 -> 13.sp
                    else -> 14.5.sp
                },
                fontWeight = FontWeight.SemiBold,
                color = if (email != null) HaIvory else HaIvory.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
