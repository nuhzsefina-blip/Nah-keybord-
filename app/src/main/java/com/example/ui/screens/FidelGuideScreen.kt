package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.firebase.DictionaryWordEntry
import com.example.firebase.UiState
import com.example.ime.FidelTable
import com.example.ui.theme.EthiopicFontFamily
import com.example.ui.theme.MonoFontFamily

@Composable
fun FidelGuideScreen(
  dictionaryState: UiState<List<DictionaryWordEntry>>,
  onAddDictionaryWord: (word: String, note: String?) -> Unit,
  onDeleteDictionaryWord: (entryId: String) -> Unit,
  onCopyGlyph: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var newWordInput by remember { mutableStateOf("") }
  var newNoteInput by remember { mutableStateOf("") }

  val filteredFamilies =
    remember(searchQuery) {
      if (searchQuery.isBlank()) FidelTable.families
      else {
        val q = searchQuery.trim()
        FidelTable.families.filter { fam ->
          fam.latinKey.contains(q, ignoreCase = true) ||
            fam.phoneticHint.contains(q, ignoreCase = true) ||
            fam.allVariants.any { it.contains(q) }
        }
      }
    }

  val orderHints = listOf("e", "u", "i", "a", "ie", "ə", "o")

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = stringResource(R.string.fidel_guide_title),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "Tap any Fidel syllable to copy it, or manage your Cloud Amharic dictionary for auto-correction.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // Personal Cloud Amharic Dictionary Card
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Book,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Column {
              Text(
                text = "Personal Amharic Dictionary",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Words saved here boost auto-correction & next-fidel hints across your devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = newWordInput,
              onValueChange = { newWordInput = it },
              modifier = Modifier.weight(1f).testTag("dict_word_input"),
              placeholder = { Text("Add Amharic word (e.g. ሰላም)") },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
            )
            Button(
              onClick = {
                val word = newWordInput.trim()
                if (word.isNotEmpty()) {
                  onAddDictionaryWord(word, newNoteInput.trim().ifEmpty { null })
                  newWordInput = ""
                  newNoteInput = ""
                }
              },
              enabled = newWordInput.isNotBlank(),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("btn_add_dict_word"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = "Add word", modifier = Modifier.size(18.dp))
              Text("Save")
            }
          }

          OutlinedTextField(
            value = newNoteInput,
            onValueChange = { newNoteInput = it },
            modifier = Modifier.fillMaxWidth().testTag("dict_note_input"),
            placeholder = { Text("Optional translation or note (e.g. Peace)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
          )

          when (dictionaryState) {
            UiState.Loading -> {
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
              ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              }
            }
            is UiState.Error -> {
              Text(
                text = "Dictionary sync error: ${dictionaryState.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
              )
            }
            is UiState.Success -> {
              val entries = dictionaryState.data
              if (entries.isEmpty()) {
                Text(
                  text = "No custom words saved yet. Add names, slang, or regional Amharic terms above.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              } else {
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  entries.forEach { entry ->
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = MaterialTheme.colorScheme.surfaceContainerHigh,
                      border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    ) {
                      Row(
                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Column {
                          Text(
                            text = entry.word,
                            fontFamily = EthiopicFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                          )
                          val meta =
                            if (!entry.note.isNullOrBlank()) "${entry.note} · ×${entry.frequency}"
                            else "×${entry.frequency}"
                          Text(
                            text = meta,
                            fontFamily = MonoFontFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                        IconButton(
                          onClick = { onDeleteDictionaryWord(entry.id) },
                          modifier = Modifier.size(28.dp),
                        ) {
                          Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Remove ${entry.word}",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier.fillMaxWidth().testTag("fidel_search_input"),
        placeholder = { Text("Search phonetic key (sh, q, gn, T) or ፊደል...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Filled.Clear, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
      )
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Phonetic Speed Shortcuts",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          val shortcuts =
            listOf(
              "sh → ሸ",
              "ch → ቸ",
              "C → ጨ",
              "T → ጠ",
              "P / ph → ጰ",
              "gn / ny → ኘ",
              "ts → ጸ",
              "zh → ዠ",
              "kwa → ኳ",
              ": → ፡",
              ":: → ።",
              ", → ፣",
            )
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            shortcuts.forEach { item ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
              ) {
                Text(
                  text = item,
                  fontFamily = EthiopicFontFamily,
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
            }
          }
        }
      }
    }

    items(filteredFamilies, key = { it.id }) { family ->
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = family.phoneticHint,
              style = MaterialTheme.typography.titleSmall,
              fontFamily = EthiopicFontFamily,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Key: ${family.latinKey}",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = MonoFontFamily,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            family.allVariants.forEachIndexed { idx, glyph ->
              val vowelHint = orderHints.getOrElse(idx) { "wa" }
              Surface(
                onClick = { onCopyGlyph(glyph) },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border =
                  BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = glyph,
                    fontFamily = EthiopicFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = vowelHint,
                    fontFamily = MonoFontFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
