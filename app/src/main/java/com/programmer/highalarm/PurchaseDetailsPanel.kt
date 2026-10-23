package com.programmer.highalarm

import android.accounts.AccountManager
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date

/**
 * "Purchase details" inside the PRO card: the Gmail the user saved for this purchase,
 * the Google Play order ID, purchase date and renewal state.
 *
 * Google Play never tells apps which Gmail paid, so the Gmail is one the user picks from
 * the phone's Google accounts. The order ID is the reliable link: Google emailed the
 * receipt for it to exactly the Gmail that paid.
 */
@Composable
fun PurchaseDetailsPanel() {
    val context = LocalContext.current
    remember { PurchaseRecord.load(context); true }
    val info by PurchaseRecord.info.collectAsState()
    val email by PurchaseRecord.savedEmail.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val name = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (result.resultCode == Activity.RESULT_OK && !name.isNullOrBlank()) {
            PurchaseRecord.saveEmail(context, name)
        }
    }
    val pickGmail: () -> Unit = {
        try {
            picker.launch(
                AccountManager.newChooseAccountIntent(
                    null, null, arrayOf("com.google"),
                    "Choose the Gmail you used to buy High Alarm PRO",
                    null, null, null
                )
            )
        } catch (_: Exception) {
            Toast.makeText(context, "Couldn't open your Google accounts on this phone.", Toast.LENGTH_SHORT).show()
        }
    }

    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(HaBlackRaised.copy(alpha = 0.92f), HaPanelDeep.copy(alpha = 0.92f)))
            )
            .border(1.dp, HaGold.copy(alpha = 0.16f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            "PURCHASE DETAILS",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
            color = HaGold.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(12.dp))

        // Gmail used to buy (saved by the user)
        Row(verticalAlignment = Alignment.CenterVertically) {
            DetailIcon(Icons.Default.AccountCircle)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gmail used to buy", fontSize = 11.sp, color = HaMuted, modifier = Modifier.weight(1f))
                    GoldChip(if (email == null) "Save" else "Change", onClick = pickGmail)
                }
                Spacer(Modifier.height(2.dp))
                val shown = email ?: "Not saved yet"
                Text(
                    shown,
                    // long addresses step down a size so the whole Gmail stays readable on one line
                    fontSize = when {
                        shown.length > 30 -> 12.sp
                        shown.length > 24 -> 13.sp
                        else -> 14.sp
                    },
                    fontWeight = FontWeight.SemiBold,
                    color = if (email != null) HaIvory else HaIvory.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val i = info
        if (i != null) {
            DetailHairline()
            val order = i.orderId
            if (order != null) {
                DetailRow(
                    icon = Icons.Default.Receipt,
                    label = "Google Play order ID",
                    value = order,
                    trailing = {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy order ID",
                            tint = HaGold.copy(alpha = 0.8f),
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    onClick = { copyToClipboard(context, "Order ID", order) }
                )
            }
            if (i.purchaseTime > 0L) {
                DetailRow(
                    icon = Icons.Default.Event,
                    label = "Purchased on",
                    value = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(i.purchaseTime))
                )
            }
            when {
                i.isLifetime -> DetailRow(Icons.Default.AllInclusive, "Status", "Lifetime · never expires")
                i.isAutoRenewing -> DetailRow(Icons.Default.Autorenew, "Status", "Active · renews automatically")
                else -> DetailRow(Icons.Default.EventBusy, "Status", "Cancelled · active until the period ends")
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                "Order details will appear here once Google Play connects.",
                fontSize = 11.5.sp,
                color = HaMuted,
                lineHeight = 16.sp
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            if (i?.orderId != null)
                "Google keeps the paying Gmail private. Its receipt for this order ID was emailed to that Gmail, so searching your inbox for the ID shows which one it is."
            else
                "Google keeps the paying Gmail private. Save it here so you always know which account has PRO.",
            fontSize = 11.sp,
            color = HaMuted.copy(alpha = 0.9f),
            lineHeight = 15.5.sp
        )
    }
}

@Composable
private fun DetailIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(HaGold.copy(alpha = 0.10f))
            .border(1.dp, HaGold.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = HaGold, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DetailIcon(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = HaMuted)
            Spacer(Modifier.height(1.dp))
            Text(
                value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = HaIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
private fun DetailHairline() {
    Spacer(Modifier.height(10.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, HaGold.copy(alpha = 0.22f), Color.Transparent)
                )
            )
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun GoldChip(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(HaGoldSelectedFill)
            .border(1.dp, HaGold.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = HaGoldLight, letterSpacing = 0.4.sp)
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    // Android 13+ shows its own "copied" confirmation
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }
}
