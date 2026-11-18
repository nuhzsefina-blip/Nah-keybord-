package com.example

import com.example.data.AmharicInputMode
import com.example.ime.AmharicDictionary
import com.example.ime.FidelTable
import com.example.ime.KeyboardOutputTarget
import com.example.ime.KeyboardSessionController
import com.example.ime.NextFidelCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmharicDictionaryTest {

  private class FakeOutputTarget : KeyboardOutputTarget {
    val buffer = StringBuilder()

    override fun setComposingText(text: String) {}

    override fun commitText(text: String) {
      buffer.append(text)
    }

    override fun finishComposing() {}

    override fun deleteBackward(count: Int) {
      val deleteCount = count.coerceAtMost(buffer.length)
      if (deleteCount > 0) {
        buffer.delete(buffer.length - deleteCount, buffer.length)
      }
    }

    override fun replaceLastCharacter(replacement: String) {
      if (buffer.isNotEmpty()) {
        buffer.deleteCharAt(buffer.length - 1)
      }
      buffer.append(replacement)
    }

    override fun performEnter() {
      buffer.append('\n')
    }

    override fun moveCursor(delta: Int) {}

    override fun getTextBeforeCursor(maxChars: Int): String =
      if (buffer.length <= maxChars) buffer.toString()
      else buffer.substring(buffer.length - maxChars)
  }

  @Test
  fun `suggests next correct fidel while typing prefix`() {
    val result = AmharicDictionary.suggest("ሰላ")
    assertTrue(
      "Expected next fidel 'ም' to complete ሰላም",
      result.nextFidelHints.any { it.fidel == "ም" && !it.replacesActiveConsonant },
    )
    assertTrue(result.wordCompletions.contains("ሰላም"))
  }

  @Test
  fun `suggests active consonant vowel morph and next fidel`() {
    val famWe = FidelTable.families.first { it.id == "w" }
    val resultForWe = AmharicDictionary.suggest("ወ", activeConsonantFamily = famWe)
    assertTrue(
      "Expected APPEND suggestion 'ደ' for ወደ",
      resultForWe.nextFidelHints.any { it.fidel == "ደ" && !it.replacesActiveConsonant },
    )
    assertTrue(
      "Expected vowel morph suggestion 'ው' for ውሃ/ውስጥ",
      resultForWe.nextFidelHints.any { it.fidel == "ው" && it.replacesActiveConsonant },
    )
  }

  @Test
  fun `auto corrects common fidel vowel order and edit typos`() {
    assertEquals("ሰላም", AmharicDictionary.suggest("ሰለም").autoCorrection)
    assertEquals("እንዴት", AmharicDictionary.suggest("እንደት").autoCorrection)
    assertEquals("ደህና", AmharicDictionary.suggest("ደህነ").autoCorrection)
    assertEquals("መልካም", AmharicDictionary.suggest("መልከም").autoCorrection)
    assertEquals("ኢትዮጵያ", AmharicDictionary.suggest("ኢትዮፕያ").autoCorrection)
    assertNull(AmharicDictionary.suggest("ሰላም").autoCorrection)
    assertNull(AmharicDictionary.suggest("ወደ").autoCorrection)
  }

  @Test
  fun `controller auto corrects on space and reverts on immediate backspace`() {
    val target = FakeOutputTarget()
    val controller = KeyboardSessionController(target)

    val famSe = FidelTable.families.first { it.id == "s1" }
    val famLe = FidelTable.families.first { it.id == "l" }
    val famMe = FidelTable.families.first { it.id == "m" }

    controller.onInPlaceConsonantPressed(famSe)
    controller.onInPlaceConsonantPressed(famLe)
    controller.onInPlaceConsonantPressed(famMe)

    assertEquals("ሰለም", target.buffer.toString())
    assertEquals("ሰላም", controller.uiState.value.autoCorrection)

    // Space applies auto-correction
    controller.onSpacePressed(AmharicInputMode.FIDEL_INPLACE)
    assertEquals("ሰላም ", target.buffer.toString())

    // Immediate backspace reverts auto-correction back to typed word
    controller.onBackspacePressed(AmharicInputMode.FIDEL_INPLACE)
    assertEquals("ሰለም", target.buffer.toString())
  }

  @Test
  fun `controller respects disabled autoCorrect and applies next fidel clicks`() {
    val target = FakeOutputTarget()
    val controller = KeyboardSessionController(target)

    val famSe = FidelTable.families.first { it.id == "s1" }
    val famLe = FidelTable.families.first { it.id == "l" }

    controller.onInPlaceConsonantPressed(famSe)
    controller.onInPlaceConsonantPressed(famLe)
    controller.onInPlaceVowelPressed(3) // ለ -> ላ

    assertEquals("ሰላ", target.buffer.toString())
    val nextMe =
      controller.uiState.value.nextFidelHints.firstOrNull {
        it.fidel == "ም" && !it.replacesActiveConsonant
      }
    assertNotNull(nextMe)
    controller.onNextFidelClicked(nextMe!!, AmharicInputMode.FIDEL_INPLACE)
    assertEquals("ሰላም", target.buffer.toString())

    // Test vowel morph via next-fidel pill
    controller.onSpacePressed(AmharicInputMode.FIDEL_INPLACE)
    val famWe = FidelTable.families.first { it.id == "w" }
    controller.onInPlaceConsonantPressed(famWe)
    val morphWu =
      NextFidelCandidate(fidel = "ው", previewWord = "ውሃ", replacesActiveConsonant = true)
    controller.onNextFidelClicked(morphWu, AmharicInputMode.FIDEL_INPLACE)
    assertEquals("ሰላም ው", target.buffer.toString())

    // Test autoCorrect = false leaves misspelled word intact on space
    controller.updateDictionaryConfig(
      customDictionaryWords = emptySet(),
      autoCorrect = false,
      nextFidelHints = true,
      inputMode = AmharicInputMode.FIDEL_INPLACE,
    )
    controller.onSpacePressed(AmharicInputMode.FIDEL_INPLACE)
    val famMe = FidelTable.families.first { it.id == "m" }
    controller.onInPlaceConsonantPressed(famSe)
    controller.onInPlaceConsonantPressed(famLe)
    controller.onInPlaceConsonantPressed(famMe)
    controller.onSpacePressed(AmharicInputMode.FIDEL_INPLACE)
    assertTrue(target.buffer.toString().endsWith("ሰለም "))
  }
}
