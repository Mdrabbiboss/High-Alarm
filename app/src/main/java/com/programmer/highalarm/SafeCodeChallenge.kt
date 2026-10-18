package com.programmer.highalarm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

data class SafeCodePuzzle(
    val code: String,
    val clues: List<String>,
)

fun generateSafeCodePuzzle(difficulty: Int): SafeCodePuzzle {
    val d1 = Random.nextInt(1, 10)
    val d2 = Random.nextInt(0, 10)
    val d3 = Random.nextInt(0, 10)
    val d4 = Random.nextInt(0, 10)
    val code = "$d1$d2$d3$d4"

    val clues = mutableListOf<String>()
    when (difficulty) {
        0 -> { // Easy
            clues.add("1. The first digit is $d1.")
            clues.add("2. The second digit is $d2.")
            clues.add("3. The third digit is $d3.")
            clues.add("4. The fourth digit is $d4.")
        }
        1 -> { // Medium
            clues.add("• The first digit is $d1.")
            clues.add("• The sum of the first two digits is ${d1 + d2}.")
            clues.add("• The third digit is $d3.")
            clues.add("• The fourth digit is ${if ((d4 % 2) == 0) "even" else "odd"} and equals $d4.")
        }
        2 -> { // Hard
            val sum = d1 + d2 + d3 + d4
            clues.add("• The sum of all four digits is $sum.")
            clues.add("• The first digit is $d1.")
            clues.add("• The second digit is $d2.")
            clues.add("• The last digit is $d4.")
        }
        else -> { // Extreme
            val sum = d1 + d2 + d3 + d4
            val prod = (d2 * d3).coerceAtMost(99)
            clues.add("• The sum of all digits is $sum.")
            clues.add("• The first digit is $d1.")
            clues.add("• The product of digits 2 & 3 is $prod.")
            clues.add("• The final digit is $d4.")
        }
    }
    return SafeCodePuzzle(code, clues)
}

@Composable
fun SafeCodeChallengeView(
    rounds: Int,
    difficulty: Int = 0,
    onComplete: () -> Unit
) {
    val totalRounds = if (rounds <= 0) 1 else rounds
    var currentRound by remember { mutableIntStateOf(1) }
    var puzzle by remember(currentRound, difficulty) { mutableStateOf(generateSafeCodePuzzle(difficulty)) }
    var inputCode by remember(currentRound) { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(value = false) }

    val keypadButtons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("⌫", "0", "✓")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HaObsidian)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SAFE CODE (Round $currentRound/$totalRounds)",
                color = SoftAmber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Crack the digital lock",
            color = HaIvory,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Code Slots Display
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = HaSurfaceLow,
            border = BorderStroke(1.5.dp, if (errorMessage != null) HaCoral.copy(alpha = 0.6f) else SoftAmber.copy(alpha = 0.4f)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val char = if (i < inputCode.length) inputCode[i].toString() else "_"
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = HaInk,
                        border = BorderStroke(1.dp, if (char != "_") SoftAmber else HaIvory.copy(alpha = 0.15f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = char,
                                color = if (char != "_") SoftAmber else HaMuted,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = HaCoral,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Clues Card
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(18.dp),
            color = HaSurfaceLow,
            border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "SECURITY CLUES:",
                    color = HaMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                puzzle.clues.forEach { clue ->
                    Text(
                        text = clue,
                        color = HaIvory,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Keypad
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            keypadButtons.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { label ->
                        Surface(
                            onClick = {
                                errorMessage = null
                                when (label) {
                                    "⌫" -> {
                                        if (inputCode.isNotEmpty()) {
                                            inputCode = inputCode.dropLast(1)
                                        }
                                    }
                                    "✓" -> {
                                            if (inputCode.length == 4) {
                                            if (inputCode == puzzle.code) {
                                                isSuccess = true
                                                if (currentRound < totalRounds) {
                                                    currentRound++
                                                    inputCode = ""
                                                    isSuccess = false
                                                } else {
                                                    onComplete()
                                                }
                                            } else {
                                                errorMessage = "Incorrect Code. Try Again."
                                            }
                                        } else {
                                            errorMessage = "Enter all 4 digits."
                                        }
                                    }
                                    else -> {
                                        if (inputCode.length < 4) {
                                            inputCode += label
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(72.dp, 56.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = if (label == "✓") SoftAmber else HaSurfaceLow,
                            border = BorderStroke(1.dp, if (label == "✓") SoftAmber else HaIvory.copy(alpha = 0.12f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = if (label == "✓") Color.Black else HaIvory,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
