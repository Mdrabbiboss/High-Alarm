package com.programmer.highalarm

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class CircuitTile(
    val id: Int,
    var baseConnections: BooleanArray, // [Top, Right, Bottom, Left] at winning rotation (rotation = 0)
    var winningRotation: Int = 0,
    var rotation: Int = 0 // current rotation (0, 1, 2, 3)
) {
    val currentConnections: BooleanArray
        get() {
            val res = BooleanArray(4)
            for (i in 0 until 4) {
                res[i] = baseConnections[(i - rotation + 4) % 4]
            }
            return res
        }
}

data class CircuitPuzzle(
    val cols: Int,
    val rows: Int,
    val tiles: List<MutableList<CircuitTile>>,
    val initialRotations: List<List<Int>>
)

// Tile templates: [Top, Right, Bottom, Left]
private val tStraightV = booleanArrayOf(true, false, true, false)
private val tStraightH = booleanArrayOf(false, true, false, true)
private val tCornerTR = booleanArrayOf(true, true, false, false)
private val tCornerRB = booleanArrayOf(false, true, true, false)
private val tCornerBL = booleanArrayOf(false, false, true, true)
private val tCornerLT = booleanArrayOf(true, false, false, true)
private val tTee = booleanArrayOf(true, true, true, false)

fun generateCircuitPuzzle(difficulty: Int): CircuitPuzzle {
    val (cols, rows) = when (difficulty) {
        0 -> Pair(3, 3)
        1 -> Pair(4, 4)
        2 -> Pair(5, 5)
        3 -> Pair(6, 6)
        else -> Pair(3, 3)
    }

    val allTemplates = listOf(
        tStraightV, tStraightH, tCornerTR, tCornerRB, tCornerBL, tCornerLT, tTee
    )

    var puzzle: CircuitPuzzle? = null
    var attempts = 0
    do {
        var idCounter = 0
        val tiles = List(rows) { _ ->
            MutableList(cols) { _ ->
                val template = allTemplates.random()
                CircuitTile(id = idCounter++, baseConnections = template.copyOf(), winningRotation = 0, rotation = 0)
            }
        }

        // Build a guaranteed solution path from (0,0) to (rows-1, cols-1)
        var cr = 0
        var cc = 0
        while ((cr < rows - 1) || (cc < cols - 1)) {
            if (cr < rows - 1 && (cc == cols - 1 || Random.nextBoolean())) {
                tiles[cr][cc].baseConnections = tCornerRB.copyOf()
                cr++
                tiles[cr][cc].baseConnections = tStraightV.copyOf()
            } else {
                tiles[cr][cc].baseConnections = tCornerTR.copyOf()
                cc++
                tiles[cr][cc].baseConnections = tStraightH.copyOf()
            }
        }

        // Ensure power source at (0,0) connects Right (1), and lock at (rows-1, cols-1) connects Left (3)
        tiles[0][0].baseConnections[1] = true
        tiles[rows - 1][cols - 1].baseConnections[3] = true

        // Verify that rotation = 0 solves the puzzle
        val (winSolved, _) = checkCircuitSolved(CircuitPuzzle(cols, rows, tiles, emptyList()))
        if (!winSolved) {
            attempts++
            continue
        }

        // Generate initial scrambled rotations
        val initRots = List(rows) { _ ->
            MutableList(cols) { _ ->
                Random.nextInt(1, 4)
            }
        }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                tiles[r][c].rotation = initRots[r][c]
            }
        }

        val candidate = CircuitPuzzle(cols, rows, tiles, initRots)
        attempts++
        val (solved, _) = checkCircuitSolved(candidate)
        // Must be guaranteed solvable at win rotation (0), but currently unsolved at initial rotation
        if (!solved) {
            puzzle = candidate
            break
        }
    } while (attempts < 200)

    return puzzle ?: run {
        // Fallback simple puzzle
        val tiles = List(3) { r ->
            MutableList(3) { c ->
                CircuitTile(id = r * 3 + c, baseConnections = tStraightH.copyOf(), winningRotation = 0, rotation = 1)
            }
        }
        tiles[0][0].baseConnections[1] = true
        tiles[2][2].baseConnections[3] = true
        CircuitPuzzle(3, 3, tiles, List(3) { List(3) { 1 } })
    }
}

fun checkCircuitSolved(puzzle: CircuitPuzzle): Pair<Boolean, Set<Pair<Int, Int>>> {
    val rows = puzzle.rows
    val cols = puzzle.cols
    val tiles = puzzle.tiles
    val visited = mutableSetOf<Pair<Int, Int>>()
    val queue = ArrayDeque<Pair<Int, Int>>()

    val start = Pair(0, 0)
    queue.add(start)
    visited.add(start)

    val powered = mutableSetOf<Pair<Int, Int>>()
    powered.add(start)

    while (queue.isNotEmpty()) {
        val (r, c) = queue.removeFirst()
        val currentConn = tiles[r][c].currentConnections

        val dr = arrayOf(-1, 0, 1, 0)
        val dc = arrayOf(0, 1, 0, -1)
        val opp = arrayOf(2, 3, 0, 1)

        for (i in 0 until 4) {
            if (currentConn[i]) {
                val nr = r + dr[i]
                val nc = c + dc[i]
                if ((nr in 0 until rows) && (nc in 0 until cols)) {
                    val neighborConn = tiles[nr][nc].currentConnections
                    // Connection is valid ONLY when Current has connector toward neighbor AND neighbor has connector back toward current
                    if (neighborConn[opp[i]]) {
                        val nextCell = Pair(nr, nc)
                        if (!visited.contains(nextCell)) {
                            visited.add(nextCell)
                            powered.add(nextCell)
                            queue.add(nextCell)
                        }
                    }
                }
            }
        }
    }

    val target = Pair(rows - 1, cols - 1)
    val isSolved = visited.contains(target)
    return Pair(isSolved, powered)
}

@Composable
fun CircuitBreakerChallengeView(
    rounds: Int,
    difficulty: Int = 0,
    onComplete: () -> Unit
) {
    val totalRounds = if (rounds <= 0) 1 else rounds
    var currentRound by remember { mutableIntStateOf(1) }
    var puzzle by remember(currentRound, difficulty) { mutableStateOf(generateCircuitPuzzle(difficulty)) }
    var hasCompleted by remember { mutableStateOf(value = false) }

    val (isSolved, poweredCells) = remember(puzzle) { checkCircuitSolved(puzzle) }

    LaunchedEffect(isSolved) {
        if (isSolved && !hasCompleted) {
            hasCompleted = true
            delay(600L)
            if (currentRound < totalRounds) {
                currentRound++
                puzzle = generateCircuitPuzzle(difficulty)
                hasCompleted = false
            } else {
                onComplete()
            }
        }
    }

    val diffName = when (difficulty) {
        0 -> "Easy"
        1 -> "Medium"
        2 -> "Hard"
        3 -> "Extreme"
        else -> "Easy"
    }

    val totalCells = puzzle.rows * puzzle.cols
    val progressPercent = if (isSolved) 100 else ((poweredCells.size.toFloat() / totalCells) * 100).toInt().coerceIn(0, 99)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HaObsidian)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Memory, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "CIRCUIT BREAKER ($diffName)",
                color = SoftAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Round $currentRound / $totalRounds",
            color = HaIvory,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "CONNECT POWER TO LOCK",
            color = HaMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.wrapContentSize(),
            shape = RoundedCornerShape(22.dp),
            color = HaSurfaceLow,
            border = BorderStroke(1.5.dp, SoftAmber.copy(alpha = 0.4f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = puzzle.rows
                val cols = puzzle.cols

                for (r in 0 until rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in 0 until cols) {
                            val tile = puzzle.tiles[r][c]
                            val isPowered = poweredCells.contains(Pair(r, c))
                            val isPowerSource = (r == 0 && c == 0)
                            val isLockDestination = (r == rows - 1 && c == cols - 1)

                            val cellSize = when {
                                cols <= 4 -> 58.dp
                                cols == 5 -> 50.dp
                                cols == 6 -> 44.dp
                                else -> 38.dp
                            }

                            CircuitTileCell(
                                tile = tile,
                                isPowered = isPowered,
                                isPowerSource = isPowerSource,
                                isLockDestination = isLockDestination,
                                cellSize = cellSize,
                                onClick = {
                                    if (!isSolved && !hasCompleted) {
                                        tile.rotation = (tile.rotation + 1) % 4
                                        puzzle = puzzle.copy(tiles = puzzle.tiles.map { it.toMutableList() })
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSolved) "🔒 LOCK UNLOCKED" else "Power Path: $progressPercent%",
                color = if (isSolved) HaJade else HaMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Reset Button
                OutlinedButton(
                    onClick = {
                        if (!isSolved && !hasCompleted) {
                            puzzle.tiles.forEachIndexed { r, row ->
                                row.forEachIndexed { c, tile ->
                                    tile.rotation = puzzle.initialRotations[r][c]
                                }
                            }
                            puzzle = puzzle.copy(tiles = puzzle.tiles.map { it.toMutableList() })
                        }
                    },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HaIvory)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Hint Button
                Button(
                    onClick = {
                        if (!isSolved && !hasCompleted) {
                            outer@ for (r in 0 until puzzle.rows) {
                                for (c in 0 until puzzle.cols) {
                                    val t = puzzle.tiles[r][c]
                                    if (t.rotation != t.winningRotation) {
                                        t.rotation = t.winningRotation
                                        puzzle = puzzle.copy(tiles = puzzle.tiles.map { it.toMutableList() })
                                        break@outer
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftAmber, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hint", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun CircuitTileCell(
    tile: CircuitTile,
    isPowered: Boolean,
    isPowerSource: Boolean,
    isLockDestination: Boolean,
    cellSize: Dp,
    onClick: () -> Unit
) {
    val conn = tile.currentConnections
    val activeColor = if (isPowered) HaJade else SoftAmber

    var clicked by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (clicked) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { clicked = false },
        label = "tile_scale"
    )

    Surface(
        onClick = {
            clicked = true
            onClick()
        },
        modifier = Modifier
            .size(cellSize)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        color = HaInk,
        border = BorderStroke(1.dp, if (isPowered) HaJade.copy(alpha = 0.7f) else HaIvory.copy(alpha = 0.12f)),
        shadowElevation = if (isPowered) 4.dp else 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val strokeWidth = 5.dp.toPx()

                drawCircle(
                    color = activeColor.copy(alpha = 0.9f),
                    radius = strokeWidth * 1.2f,
                    center = Offset(cx, cy)
                )

                if (conn[0]) {
                    drawLine(
                        color = activeColor,
                        start = Offset(cx, cy),
                        end = Offset(cx, 0f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
                if (conn[1]) {
                    drawLine(
                        color = activeColor,
                        start = Offset(cx, cy),
                        end = Offset(w, cy),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
                if (conn[2]) {
                    drawLine(
                        color = activeColor,
                        start = Offset(cx, cy),
                        end = Offset(cx, h),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
                if (conn[3]) {
                    drawLine(
                        color = activeColor,
                        start = Offset(cx, cy),
                        end = Offset(0f, cy),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            if (isPowerSource) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(activeColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡", fontSize = (cellSize.value * 0.38f).sp)
                }
            } else if (isLockDestination) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(activeColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔒", fontSize = (cellSize.value * 0.34f).sp)
                }
            }
        }
    }
}
