package com.example.expensetracker.ui.addexpense

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.repository.CategoryRepository
import com.example.expensetracker.ui.theme.Divider
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.util.CategoryIcons
import com.example.expensetracker.util.DateUtils

@Composable
fun CategoryDrillDown(
    categoryRepository: CategoryRepository,
    onCategorySelected: (Category) -> Unit,
    onBack: () -> Unit
) {
    // Navigation stack: null = root level, otherwise parent ID
    val navigationStack = remember { mutableStateListOf<Int?>(null) }
    var navigationDirection by remember { mutableIntStateOf(1) } // 1=forward, -1=back

    val currentParentId = navigationStack.last()
    val sinceDate = remember { DateUtils.monthsAgo(6) }

    // Use usage-sorted queries (B-021)
    val categories by if (currentParentId == null) {
        categoryRepository.getRootCategoriesByUsage(sinceDate)
            .collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        categoryRepository.getChildrenByUsage(currentParentId, sinceDate)
            .collectAsStateWithLifecycle(initialValue = emptyList())
    }

    // Track child counts for current level (B-019)
    val childCounts = remember { mutableStateMapOf<Int, Int>() }
    LaunchedEffect(categories) {
        for (cat in categories) {
            if (cat.id !in childCounts) {
                childCounts[cat.id] = categoryRepository.getChildCount(cat.id)
            }
        }
    }

    BackHandler {
        if (navigationStack.size > 1) {
            navigationDirection = -1
            navigationStack.removeLast()
        } else {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (navigationStack.size > 1) {
                    navigationDirection = -1
                    navigationStack.removeLast()
                } else {
                    onBack()
                }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnSurface
                )
            }
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface
            )
        }

        // Category list with animated transitions
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = currentParentId,
                transitionSpec = {
                    val direction = navigationDirection
                    slideInHorizontally { width -> width * direction } togetherWith
                        slideOutHorizontally { width -> -width * direction } using
                        SizeTransform(clip = false)
                },
                label = "drill_down"
            ) { _ ->
                LazyColumn {
                    items(categories) { category ->
                        val hasChildren = (childCounts[category.id] ?: 0) > 0
                        CategoryDrillDownItem(
                            category = category,
                            hasChildren = hasChildren,
                            onClick = {
                                if (hasChildren) {
                                    // B-019: Root/branch with children → drill down
                                    navigationDirection = 1
                                    navigationStack.add(category.id)
                                } else {
                                    // B-020: Leaf → select immediately
                                    onCategorySelected(category)
                                }
                            },
                            onDrillDown = {
                                navigationDirection = 1
                                navigationStack.add(category.id)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            thickness = 1.dp,
                            color = Divider
                        )
                    }
                }
            }
        }
        // B-020: Select button removed — tap to pick
    }
}

@Composable
private fun CategoryDrillDownItem(
    category: Category,
    hasChildren: Boolean,
    onClick: () -> Unit,
    onDrillDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = CategoryIcons.get(category.icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurface,
            modifier = Modifier.weight(1f)
        )
        if (hasChildren) {
            IconButton(onClick = onDrillDown) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Explore subcategories",
                    tint = OnSurfaceTertiary
                )
            }
        }
    }
}
