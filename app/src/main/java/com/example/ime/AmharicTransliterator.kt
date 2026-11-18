package com.example.ime

/**
 * Stateful SERA/GeezIME-inspired phonetic transliteration engine for Amharic (Fidel).
 * Converts Latin keystrokes sequentially into Ethiopic syllables, supporting:
 * - Multi-character consonants (sh, ch, gn, ny, zh, ph, ts, tz, kh)
 * - Case-sensitive ejectives & pharyngeal contrasts (s/S, t/T, c/C, p/P, h/H, a/A, k/K)
 * - All 7 orders (e/ä, u, i, a, ie/E/ee, bare 6th, o) plus labialized w-forms (kwa, gwa, swa, etc.)
 * - Ethiopic punctuation (: -> ፡, :: -> ።, , -> ፣, ; -> ፤, ? -> ፧)
 */
object AmharicTransliterator {

  private val consonantDigraphs =
    mapOf(
      "sh" to "h_sh",
      "ch" to "ch",
      "gn" to "gn",
      "ny" to "gn",
      "zh" to "zh",
      "ph" to "P",
      "ts" to "ts",
      "tz" to "tz",
      "kh" to "kh",
      "dh" to "d",
    )

  private val singleConsonants =
    mapOf(
      'h' to "h1",
      'H' to "h2",
      'l' to "l",
      'L' to "l",
      'm' to "m",
      'M' to "m",
      'S' to "s2",
      'r' to "r",
      'R' to "r",
      's' to "s1",
      'q' to "q",
      'Q' to "q",
      'b' to "b",
      'B' to "b",
      'v' to "v",
      'V' to "v",
      't' to "t",
      'T' to "T",
      'c' to "ch",
      'C' to "C",
      'n' to "n",
      'N' to "ny",
      'k' to "k",
      'K' to "kx",
      'w' to "w",
      'W' to "w",
      'z' to "z",
      'Z' to "zh",
      'y' to "y",
      'Y' to "y",
      'd' to "d",
      'D' to "d",
      'j' to "j",
      'J' to "j",
      'g' to "g",
      'G' to "g",
      'P' to "P",
      'f' to "f",
      'F' to "f",
      'p' to "p",
      'x' to "sh",
      'X' to "kh",
    )

  private val familyMap: Map<String, FidelFamily> =
    FidelTable.families.associateBy { it.id } +
      mapOf(
        "h_sh" to FidelTable.families.first { it.id == "sh" },
        "ch" to FidelTable.families.first { it.id == "ch" },
        "gn" to FidelTable.families.first { it.id == "ny" },
        "zh" to FidelTable.families.first { it.id == "zh" },
        "P" to FidelTable.families.first { it.id == "P" },
        "ts" to FidelTable.families.first { it.id == "ts" },
        "tz" to FidelTable.families.first { it.id == "tz" },
        "kh" to FidelTable.families.first { it.id == "h3" },
      )

  data class SyllableResult(
    val committedPrefix: String,
    val composingSyllable: String,
    val newBuffer: String,
    val activeFamily: FidelFamily?,
  )

  fun processKeystroke(currentBuffer: String, char: Char): SyllableResult {
    // Handle Ethiopic punctuation shortcuts
    if (char == ':' && currentBuffer == ":") {
      return SyllableResult(
        committedPrefix = "።",
        composingSyllable = "",
        newBuffer = "",
        activeFamily = null,
      )
    }
    if (char == '-' && currentBuffer == ":") {
      return SyllableResult(
        committedPrefix = "፥",
        composingSyllable = "",
        newBuffer = "",
        activeFamily = null,
      )
    }
    if (char == ':') {
      val flushed = flushBuffer(currentBuffer)
      return SyllableResult(
        committedPrefix = flushed,
        composingSyllable = "፡",
        newBuffer = ":",
        activeFamily = null,
      )
    }

    if (currentBuffer == ":") {
      val punctuationCommit = "፡"
      val next = processKeystroke("", char)
      return next.copy(committedPrefix = punctuationCommit + next.committedPrefix)
    }

    when (char) {
      ',' -> return SyllableResult(flushBuffer(currentBuffer) + "፣", "", "", null)
      ';' -> return SyllableResult(flushBuffer(currentBuffer) + "፤", "", "", null)
      '?' -> return SyllableResult(flushBuffer(currentBuffer) + "፧", "", "", null)
    }

    if (!char.isLetter()) {
      return SyllableResult(flushBuffer(currentBuffer) + char, "", "", null)
    }

    val candidate = currentBuffer + char
    val resolved = resolveExactBuffer(candidate)
    if (resolved != null) {
      return SyllableResult(
        committedPrefix = "",
        composingSyllable = resolved.first,
        newBuffer = candidate,
        activeFamily = resolved.second,
      )
    }

    // Buffer didn't extend cleanly -> commit previous buffer and start fresh with char
    val flushed = flushBuffer(currentBuffer)
    val fresh = resolveExactBuffer(char.toString())
    return if (fresh != null) {
      SyllableResult(
        committedPrefix = flushed,
        composingSyllable = fresh.first,
        newBuffer = char.toString(),
        activeFamily = fresh.second,
      )
    } else {
      SyllableResult(
        committedPrefix = flushed + char,
        composingSyllable = "",
        newBuffer = "",
        activeFamily = null,
      )
    }
  }

  fun flushBuffer(buffer: String): String {
    if (buffer.isEmpty()) return ""
    if (buffer == ":") return "፡"
    return resolveExactBuffer(buffer)?.first ?: buffer
  }

  fun backspaceBuffer(currentBuffer: String): SyllableResult {
    if (currentBuffer.length <= 1) {
      return SyllableResult("", "", "", null)
    }
    val trimmed = currentBuffer.dropLast(1)
    if (trimmed == ":") {
      return SyllableResult("", "፡", ":", null)
    }
    val resolved = resolveExactBuffer(trimmed)
    return if (resolved != null) {
      SyllableResult("", resolved.first, trimmed, resolved.second)
    } else {
      SyllableResult("", "", "", null)
    }
  }

  private fun resolveExactBuffer(buffer: String): Pair<String, FidelFamily>? {
    if (buffer.isEmpty()) return null

    // Check standalone vowels (a1 = አ..ኦ, a2 = ዐ..ዖ)
    standaloneVowel(buffer)?.let { return it }

    // Split into consonant part + vowel/labialized suffix
    val lower = buffer.lowercase()
    var consonantLen = 0
    var family: FidelFamily? = null

    if (buffer.length >= 2) {
      val two = lower.substring(0, 2)
      val mapped = consonantDigraphs[two]
      if (mapped != null) {
        family = familyMap[mapped]
        consonantLen = 2
      }
    }
    if (family == null) {
      val firstChar = buffer[0]
      val mapped = singleConsonants[firstChar] ?: return null
      family = familyMap[mapped] ?: return null
      consonantLen = 1
    }

    val tail = buffer.substring(consonantLen)
    if (tail.isEmpty()) {
      // Bare consonant -> 6th order (ሳድስ)
      return family.sixthOrder to family
    }

    // Labialized check: e.g., kwa, gwa, qwa, swa, lwa, etc.
    if (tail.startsWith("w", ignoreCase = true) || tail.startsWith("u", ignoreCase = true)) {
      if (tail.equals("w", ignoreCase = true)) {
        // Pending labialized: if family has 5-form labialized (q, h3, k, g), use 5th labialized; else 2nd order + wait
        val lab = family.labialized
        return when (lab.size) {
          5 -> lab[4] to family // e.g., ኵ, ቍ, ጕ, ኍ
          1 -> family.orders[1] to family
          else -> return null
        }
      }
      if (tail.length == 2 && tail[0].lowercaseChar() == 'w') {
        val v = tail[1]
        val lab = family.labialized
        if (lab.size == 5) {
          val idx =
            when (v) {
              'e' -> 0
              'i' -> 1
              'a' -> 2
              'E' -> 3
              else -> -1
            }
          if (idx >= 0) return lab[idx] to family
        } else if (lab.size == 1 && (v == 'a' || v == 'A')) {
          return lab[0] to family
        }
      }
      if (tail.equals("wa", ignoreCase = true) || tail.equals("ua", ignoreCase = true)) {
        val lab = family.labialized
        if (lab.size == 1) return lab[0] to family
        if (lab.size == 5) return lab[2] to family
      }
      if (tail.equals("wie", ignoreCase = true) && family.labialized.size == 5) {
        return family.labialized[3] to family
      }
    }

    val orderIndex =
      when (tail) {
        "e" -> 0
        "u" -> 1
        "i" -> 2
        "a" -> 3
        "ie", "ee", "E" -> 4
        "I" -> 5
        "o" -> 6
        else -> -1
      }
    if (orderIndex in 0..6) {
      return family.orders[orderIndex] to family
    }
    return null
  }

  private fun standaloneVowel(buffer: String): Pair<String, FidelFamily>? {
    val a1 = familyMap["a1"] ?: return null
    val a2 = familyMap["a2"] ?: return null
    return when (buffer) {
      "e", "a" -> a1.orders[0] to a1 // አ
      "u" -> a1.orders[1] to a1 // ኡ
      "i" -> a1.orders[2] to a1 // ኢ
      "aa" -> a1.orders[3] to a1 // ኣ
      "ie", "ee", "E" -> a1.orders[4] to a1 // ኤ
      "I" -> a1.orders[5] to a1 // እ
      "o" -> a1.orders[6] to a1 // ኦ
      "A" -> a2.orders[0] to a2 // ዐ
      "Au" -> a2.orders[1] to a2 // ዑ
      "Ai" -> a2.orders[2] to a2 // ዒ
      "Aa" -> a2.orders[3] to a2 // ዓ
      "Ae", "Aie", "AE" -> a2.orders[4] to a2 // ዔ
      "AI" -> a2.orders[5] to a2 // ዕ
      "Ao" -> a2.orders[6] to a2 // ዖ
      else -> null
    }
  }
}
