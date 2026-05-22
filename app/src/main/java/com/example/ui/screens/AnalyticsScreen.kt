package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.TransactionCategory
import com.example.ui.utils.L10n
import com.example.ui.viewmodel.FintechViewModel
import java.util.*

@Composable
fun AnalyticsScreen(
    viewModel: FintechViewModel,
    langCode: String,
    modifier: Modifier = Modifier
) {
    val txList by viewModel.transactions.collectAsState()
    val budgetList by viewModel.budgets.collectAsState()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetTargetCategory by remember { mutableStateOf<TransactionCategory?>(null) }

    // Precalculate aggregates
    val spentTx = txList.filter { it.transactionType == "debit" || it.transactionType == "spend" }
    val totalSpent = spentTx.sumOf { it.amount }

    val categorySum = spentTx.groupBy { it.category }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
        ) {
            // Screen Header title
            item {
                Text(
                    text = L10n.get("analytics", langCode),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Donut Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                        .testTag("donut_chart_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = L10n.get("monthly_donut_title", langCode),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom Donut Canvas drawing
                        Box(
                            modifier = Modifier.size(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (totalSpent <= 0) {
                                    // Empty state: simple gray circle outline
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.1f),
                                        radius = size.minDimension / 2 - 16.dp.toPx(),
                                        style = Stroke(width = 16.dp.toPx())
                                    )
                                } else {
                                    var startAngle = -90f
                                    categorySum.forEach { (categoryName, amt) ->
                                        val cat = TransactionCategory.fromString(categoryName)
                                        val sweep = (amt / totalSpent * 360f).toFloat()

                                        drawArc(
                                            color = cat.color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            size = Size(
                                                size.width - 32.dp.toPx(),
                                                size.height - 32.dp.toPx()
                                            ),
                                            topLeft = Offset(16.dp.toPx(), 16.dp.toPx()),
                                            style = Stroke(width = 16.dp.toPx())
                                        )
                                        startAngle += sweep
                                    }
                                }
                            }

                            // Center overlay total spent metrics
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "₹${String.format("%.0f", totalSpent)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Mini Color Legends
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeCategories = categorySum.keys.take(4).map { TransactionCategory.fromString(it) }
                            activeCategories.forEachIndexed { index, cat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(cat.color)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = cat.displayName.split(" ")[0],
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Custom Spends Bar Chart (Mon - Sun)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                        .testTag("bar_chart_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = L10n.get("weekly_overview_title", langCode),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Compute day-wise spends (1=Sun, 2=Mon... 7=Sat)
                        val daySpends = DoubleArray(7) { 0.0 }
                        spentTx.forEach { tx ->
                            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                            val dayIdx = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun to 6=Sat
                            daySpends[dayIdx] += tx.amount
                        }
                        val maxDaySpent = daySpends.maxOrNull() ?: 1.0

                        // Render beautiful Bars
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            daySpends.forEachIndexed { idx, amt ->
                                val barRatio = (amt / maxDaySpent).coerceIn(0.01, 1.0).toFloat()

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (amt > 0) "₹${String.format("%.0f", amt)}" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.42f)
                                            .fillMaxHeight(barRatio)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                    )
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = dayLabels[idx],
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Core 3-Month wave line trend using custom Bezier drawing
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = L10n.get("monthly_trends_title", langCode),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Wave Canvas drawing
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        ) {
                            val strokeColor = TealAccentPrimary
                            val fillGradient = Brush.verticalGradient(
                                listOf(TealAccentPrimary.copy(alpha = 0.35f), Color.Transparent)
                            )

                            // 5 nodes defining waveform wave
                            val points = listOf(
                                Offset(0f, size.height * 0.75f),
                                Offset(size.width * 0.25f, size.height * 0.40f),
                                Offset(size.width * 0.50f, size.height * 0.85f),
                                Offset(size.width * 0.75f, size.height * 0.15f),
                                Offset(size.width, size.height * 0.30f)
                            )

                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    val prev = points[i - 1]
                                    val curr = points[i]
                                    val contX = (prev.x + curr.x) / 2
                                    quadraticTo(prev.x, prev.y, contX, (prev.y + curr.y) / 2)
                                }
                                lineTo(points.last().x, points.last().y)
                            }

                            // Fill
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }

                            drawPath(path = fillPath, brush = fillGradient)
                            drawPath(path = path, color = strokeColor, style = Stroke(width = 3.dp.toPx()))

                            // Draw glowing points
                            points.forEach { pt ->
                                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
                                drawCircle(color = strokeColor, radius = 8.dp.toPx(), center = pt, style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                    }
                }
            }

            // Budget Capping Subsection
            item {
                Text(
                    text = L10n.get("budget_usage", langCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Category wise Budgets listing
            val categoriesToShow = TransactionCategory.entries
            items(categoriesToShow) { category ->
                val spent = categorySum[category.name] ?: 0.0
                val budget = budgetList.find { it.category == category.name }
                val capLimit = budget?.limitAmount ?: 0.0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable {
                            budgetTargetCategory = category
                            showBudgetDialog = true
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(category.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.displayName,
                                tint = category.color,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "₹${spent.toInt()} / " + if (capLimit > 0) "₹${capLimit.toInt()}" else "₹0 (Uncapped)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Progression indicator bar
                            if (capLimit > 0) {
                                val ratio = (spent / capLimit).coerceIn(0.0, 1.0).toFloat()
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    color = if (ratio >= 0.9f) Color(0xFFFF5252) else category.color,
                                    trackColor = Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                )
                            } else {
                                LinearProgressIndicator(
                                    progress = { 0f },
                                    color = category.color,
                                    trackColor = Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit cap",
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Sleek Budget revision dialog
        if (showBudgetDialog && budgetTargetCategory != null) {
            val category = budgetTargetCategory!!
            val initialCap = budgetList.find { it.category == category.name }?.limitAmount ?: 0.0

            Dialog(onDismissRequest = { showBudgetDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    var limitStr by remember { mutableStateOf(if (initialCap > 0) initialCap.toInt().toString() else "") }
                    var hasError by remember { mutableStateOf(false) }

                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = L10n.get("set_budget", langCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Modify spend thresholds for ${category.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = limitStr,
                            onValueChange = {
                                limitStr = it
                                hasError = false
                            },
                            label = { Text(L10n.get("budget_limit", langCode), color = Color.White.copy(alpha = 0.5f)) },
                            isError = hasError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("budget_limit_input")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { showBudgetDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text(L10n.get("cancel", langCode), color = Color.White)
                            }
                            Button(
                                onClick = {
                                    val amt = limitStr.toDoubleOrNull()
                                    if (amt == null || amt < 0) {
                                        hasError = true
                                    } else {
                                        viewModel.setBudgetLimit(category.name, amt)
                                        showBudgetDialog = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                                    .testTag("budget_save_btn")
                            ) {
                                Text(L10n.get("save", langCode), color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Color variable
val TealAccentPrimary = Color(0xFF00BFA5)
