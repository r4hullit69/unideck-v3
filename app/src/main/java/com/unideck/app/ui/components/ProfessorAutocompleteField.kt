package com.unideck.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Pure helper for offline professor autocomplete:
 * - Deduplicates case-insensitively while preserving original display casing.
 * - Hides suggestions when [query] exactly matches an existing professor (case-insensitive).
 * - Ranks prefix matches first, then partial substring matches (alphabetical within each group).
 */
fun filterProfessorSuggestions(
    query: String,
    savedProfessors: List<String>,
    maxResults: Int = 8
): List<String> {
    val deduped = LinkedHashMap<String, String>()
    for (raw in savedProfessors) {
        val cleaned = raw.trim()
        if (cleaned.isNotEmpty()) {
            val key = cleaned.lowercase()
            if (!deduped.containsKey(key)) {
                deduped[key] = cleaned
            }
        }
    }
    if (deduped.isEmpty()) return emptyList()

    val trimmedQuery = query.trim()
    val lowerQuery = trimmedQuery.lowercase()

    if (lowerQuery.isEmpty()) {
        return deduped.values
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .take(maxResults)
    }

    // Hide dropdown once an exact match is already entered
    if (deduped.containsKey(lowerQuery)) {
        return emptyList()
    }

    val prefixMatches = mutableListOf<String>()
    val substringMatches = mutableListOf<String>()

    for ((lowerName, displayName) in deduped) {
        when {
            lowerName.startsWith(lowerQuery) -> prefixMatches.add(displayName)
            lowerName.contains(lowerQuery) -> substringMatches.add(displayName)
        }
    }

    prefixMatches.sortWith(String.CASE_INSENSITIVE_ORDER)
    substringMatches.sortWith(String.CASE_INSENSITIVE_ORDER)

    return (prefixMatches + substringMatches).take(maxResults)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    savedProfessors: List<String>,
    modifier: Modifier = Modifier,
    label: String = "Professor (Optional)",
    enabled: Boolean = true,
    testTagName: String = "professor_autocomplete_input"
) {
    var isFocused by remember { mutableStateOf(false) }
    var dismissedForValue by remember { mutableStateOf<String?>(null) }

    val suggestions = remember(value, savedProfessors) {
        filterProfessorSuggestions(query = value, savedProfessors = savedProfessors)
    }

    val expanded = enabled && isFocused && suggestions.isNotEmpty() && dismissedForValue != value

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { shouldExpand ->
            if (!shouldExpand) {
                dismissedForValue = value
            } else {
                dismissedForValue = null
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { updated ->
                dismissedForValue = null
                onValueChange(updated)
            },
            label = { Text(label) },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = enabled)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (!focusState.isFocused) {
                        dismissedForValue = null
                    }
                }
                .testTag(testTagName)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { dismissedForValue = value },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.heightIn(max = 180.dp)
        ) {
            suggestions.forEach { profName ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = profName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onValueChange(profName)
                        dismissedForValue = profName
                    },
                    modifier = Modifier.testTag("professor_suggestion_$profName")
                )
            }
        }
    }
}
