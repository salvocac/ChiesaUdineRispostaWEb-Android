package com.caccavo.chiesaudinerispostaweb.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun <T> SimpleDropdownSelector(
    label: (T) -> String,
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    
    val horizontalPadding = if (isCompact) 6.dp else 10.dp
    val verticalPadding = if (isCompact) 4.dp else 6.dp
    val textStyle = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
    val iconSize = if (isCompact) 16.dp else 20.dp
    val cornerRadius = if (isCompact) 8.dp else 10.dp

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(cornerRadius)
                )
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label(selected),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = textStyle,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(if (isCompact) 2.dp else 4.dp))
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()

            Box(modifier = Modifier.heightIn(max = 300.dp)) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .width(intrinsicSize = androidx.compose.foundation.layout.IntrinsicSize.Max)
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(label(option), style = textStyle) },
                            onClick = {
                                onSelected(option)
                                expanded = false
                            }
                        )
                    }
                }

                // Mostra una freccetta verso il basso se la lista ha altri elementi da scorrere
                if (scrollState.value < scrollState.maxValue) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                // Scorre in basso di 250 pixel
                                val target = (scrollState.value + 250).coerceAtMost(scrollState.maxValue)
                                scrollState.animateScrollTo(target)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Mostra altri",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
