package com.example.ime

import kotlin.math.abs

data class NextFidelCandidate(
  val fidel: String,
  val previewWord: String,
  val replacesActiveConsonant: Boolean = false,
)

data class AmharicSuggestionResult(
  val nextFidelHints: List<NextFidelCandidate> = emptyList(),
  val autoCorrection: String? = null,
  val wordCompletions: List<String> = emptyList(),
)

private data class WeightedWord(val word: String, val weight: Int)

object AmharicDictionary {

  private val baseEntries: List<WeightedWord> by lazy {
    FidelTable.commonAmharicWords
      .distinct()
      .mapIndexed { idx, word ->
        val priorityBoost = if (idx < 60) 120 - idx else (60 - (idx - 60) / 4).coerceAtLeast(10)
        WeightedWord(word = word, weight = priorityBoost)
      }
  }

  private val homophoneGroupByChar: Map<Char, String> by lazy {
    buildMap {
      for (family in FidelTable.families) {
        val canonicalGroup =
          when (family.id) {
            "h1", "h2", "h3" -> "h_group"
            "s1", "s2" -> "s_group"
            "a1", "a2" -> "a_group"
            "ts", "tz" -> "ts_group"
            else -> family.id
          }
        for (glyph in family.allVariants) {
          val ch = glyph.firstOrNull() ?: continue
          put(ch, canonicalGroup)
        }
      }
    }
  }

  fun isKnownWord(word: String, customWords: Collection<String> = emptyList()): Boolean {
    if (word.isBlank()) return false
    return customWords.any { it == word } || baseEntries.any { it.word == word }
  }

  fun suggest(
    prefix: String,
    activeConsonantFamily: FidelFamily? = null,
    customWords: Collection<String> = emptyList(),
    autoCorrectEnabled: Boolean = true,
    nextFidelHintsEnabled: Boolean = true,
    wordLimit: Int = 8,
    fidelLimit: Int = 5,
  ): AmharicSuggestionResult {
    val lexicon = buildLexicon(customWords)

    if (prefix.isBlank()) {
      val defaultWords =
        (customWords.toList() + listOf("ሰላም", "እንዴት", "እሺ", "አመሰግናለሁ", "መልካም", "ነህ", "ወደ", "ውስጥ"))
          .distinct()
          .take(wordLimit)
      val starterFidels =
        if (nextFidelHintsEnabled) {
          if (activeConsonantFamily != null) {
            // Suggest top vowel orders for this active consonant from dictionary starters
            lexicon
              .filter { entry ->
                val first = entry.word.firstOrNull()?.toString()
                first != null && activeConsonantFamily.allVariants.contains(first)
              }
              .groupBy { it.word.first().toString() }
              .entries
              .sortedByDescending { (_, list) -> list.maxOf { it.weight } }
              .take(fidelLimit)
              .map { (fidel, list) ->
                NextFidelCandidate(
                  fidel = fidel,
                  previewWord = list.maxBy { it.weight }.word,
                  replacesActiveConsonant = true,
                )
              }
          } else {
            emptyList()
          }
        } else {
          emptyList()
        }
      return AmharicSuggestionResult(
        nextFidelHints = starterFidels,
        autoCorrection = null,
        wordCompletions = defaultWords,
      )
    }

    val exactCompletions =
      lexicon
        .filter { it.word.startsWith(prefix) && it.word != prefix }
        .sortedByDescending { it.weight }
        .map { it.word }

    val exactHit = lexicon.any { it.word == prefix }

    // Family-aware matching: e.g., user typed "ሰለ" (1st order ለ instead of 4th order ላ) -> matches "ሰላም"
    // or typed "ወድ" (1st order ወ instead of 6th order ው) -> matches "ውድ"!
    val familyMatches =
      lexicon
        .filter { entry ->
          entry.word != prefix &&
            entry.word.length >= prefix.length &&
            matchesByFamilyPrefix(prefix, entry.word)
        }
        .sortedWith(
          compareByDescending<WeightedWord> { it.word.length == prefix.length }
            .thenByDescending { it.weight }
        )

    // Auto-correction candidate if prefix is NOT already an exact dictionary word
    val autoCorrection: String? =
      if (!autoCorrectEnabled || exactHit || prefix.length < 2) {
        null
      } else {
        // 1. Prefer exact-length same-family correction (e.g., ሰለም -> ሰላም, ወድ -> ውድ, በጠም -> በጣም)
        val sameLengthFamily = familyMatches.firstOrNull { it.word.length == prefix.length }?.word
        if (sameLengthFamily != null) {
          sameLengthFamily
        } else {
          // 2. Otherwise check 1-fidel edit distance on words of similar length
          findClosestEditMatch(prefix, lexicon)
        }
      }

    // Next-fidel suggestions while typing:
    // Priority A: If activeConsonantFamily is currently morphing the LAST char of prefix (e.g., user just tapped ወ at end of prefix),
    // also predict the right vowel form of that last char if a dictionary word uses another order in that family!
    val nextFidels =
      if (!nextFidelHintsEnabled) {
        emptyList()
      } else {
        buildNextFidelCandidates(
          prefix = prefix,
          activeConsonantFamily = activeConsonantFamily,
          lexicon = lexicon,
          familyMatches = familyMatches,
          limit = fidelLimit,
        )
      }

    val combinedWords =
      buildList {
        autoCorrection?.let { add(it) }
        addAll(exactCompletions)
        familyMatches.forEach { entry ->
          if (!contains(entry.word)) add(entry.word)
        }
        if (size < 3) {
          lexicon
            .filter { it.word.contains(prefix) && it.word != prefix && !contains(it.word) }
            .take(wordLimit - size)
            .forEach { add(it.word) }
        }
      }.take(wordLimit)

    return AmharicSuggestionResult(
      nextFidelHints = nextFidels,
      autoCorrection = autoCorrection,
      wordCompletions = combinedWords,
    )
  }

  private fun buildNextFidelCandidates(
    prefix: String,
    activeConsonantFamily: FidelFamily?,
    lexicon: List<WeightedWord>,
    familyMatches: List<WeightedWord>,
    limit: Int,
  ): List<NextFidelCandidate> {
    val candidates = mutableListOf<NextFidelCandidate>()
    val seenFidels = mutableSetOf<String>()

    // 1. Next fidel continuing exact prefix matches (at index = prefix.length)
    val exactNext =
      lexicon
        .filter { it.word.startsWith(prefix) && it.word.length > prefix.length }
        .groupBy { it.word[prefix.length].toString() }
        .entries
        .sortedByDescending { (_, group) -> group.maxOf { it.weight } }

    for ((fidel, group) in exactNext) {
      if (candidates.size >= limit) break
      if (seenFidels.add("next:$fidel")) {
        candidates.add(
          NextFidelCandidate(
            fidel = fidel,
            previewWord = group.maxBy { it.weight }.word,
            replacesActiveConsonant = false,
          )
        )
      }
    }

    // 2. If a consonant was just tapped (activeConsonantFamily != null) OR no exact continuations found,
    // suggest the likely vowel morph of the LAST character (e.g. typed "ወ" -> "ው" from "ውሃ"/"ውስጥ", or "ሰላ" -> "ላ" already matched)
    if (activeConsonantFamily != null && prefix.isNotEmpty()) {
      val lastIdx = prefix.length - 1
      val morphCandidates =
        familyMatches
          .filter { it.word.length > lastIdx }
          .map { entry -> entry.word[lastIdx].toString() to entry }
          .filter { (fidel, _) ->
            fidel != prefix[lastIdx].toString() && activeConsonantFamily.allVariants.contains(fidel)
          }
          .groupBy { it.first }
          .entries
          .sortedByDescending { (_, pairs) -> pairs.maxOf { it.second.weight } }

      for ((morphedFidel, pairs) in morphCandidates) {
        if (candidates.size >= limit + 2) break
        if (seenFidels.add("morph:$morphedFidel")) {
          candidates.add(
            0, // Place active-consonant morph right at the front when morphing!
            NextFidelCandidate(
              fidel = morphedFidel,
              previewWord = pairs.maxBy { it.second.weight }.second.word,
              replacesActiveConsonant = true,
            ),
          )
        }
      }
    } else if (candidates.size < limit) {
      // Fall back to next fidel from family-level matches (in case user mistyped an earlier order)
      val familyNext =
        familyMatches
          .filter { it.word.length > prefix.length }
          .groupBy { it.word[prefix.length].toString() }
          .entries
          .sortedByDescending { (_, group) -> group.maxOf { it.weight } }
      for ((fidel, group) in familyNext) {
        if (candidates.size >= limit) break
        if (seenFidels.add("next:$fidel")) {
          candidates.add(
            NextFidelCandidate(
              fidel = fidel,
              previewWord = group.maxBy { it.weight }.word,
              replacesActiveConsonant = false,
            )
          )
        }
      }
    }

    return candidates.take(limit)
  }

  private fun buildLexicon(customWords: Collection<String>): List<WeightedWord> {
    if (customWords.isEmpty()) return baseEntries
    val customWeighted =
      customWords
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .mapIndexed { idx, w -> WeightedWord(word = w, weight = 250 - idx.coerceAtMost(80)) }
    val seen = HashSet<String>(customWeighted.size + baseEntries.size)
    return buildList {
      for (entry in customWeighted) {
        if (seen.add(entry.word)) add(entry)
      }
      for (entry in baseEntries) {
        if (seen.add(entry.word)) add(entry)
      }
    }
  }

  private fun matchesByFamilyPrefix(prefix: String, candidate: String): Boolean {
    if (candidate.length < prefix.length) return false
    for (i in prefix.indices) {
      val a = prefix[i]
      val b = candidate[i]
      if (a == b) continue
      val groupA = homophoneGroupByChar[a] ?: return false
      val groupB = homophoneGroupByChar[b] ?: return false
      if (groupA != groupB) return false
    }
    return true
  }

  private fun findClosestEditMatch(input: String, lexicon: List<WeightedWord>): String? {
    var bestWord: String? = null
    var bestScore = Int.MIN_VALUE
    for (entry in lexicon) {
      val w = entry.word
      if (abs(w.length - input.length) > 1) continue
      val dist = levenshteinWithinOne(input, w)
      if (dist == 1) {
        val sameFirstGroup =
          homophoneGroupByChar[input.first()] == homophoneGroupByChar[w.first()]
        val score = entry.weight + (if (sameFirstGroup) 40 else 0)
        if (score > bestScore) {
          bestScore = score
          bestWord = w
        }
      }
    }
    return bestWord
  }

  private fun levenshteinWithinOne(a: String, b: String): Int {
    val lenA = a.length
    val lenB = b.length
    if (abs(lenA - lenB) > 1) return 2
    if (a == b) return 0

    if (lenA == lenB) {
      var diffs = 0
      for (i in 0 until lenA) {
        if (a[i] != b[i]) {
          diffs++
          if (diffs > 1) return 2
        }
      }
      return diffs
    }

    val shorter = if (lenA < lenB) a else b
    val longer = if (lenA < lenB) b else a
    var i = 0
    var j = 0
    var skipped = false
    while (i < shorter.length && j < longer.length) {
      if (shorter[i] != longer[j]) {
        if (skipped) return 2
        skipped = true
        j++
      } else {
        i++
        j++
      }
    }
    return 1
  }
}
