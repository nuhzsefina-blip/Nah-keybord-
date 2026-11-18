package com.example

import com.example.ime.AmharicTransliterator
import org.junit.Assert.assertEquals
import org.junit.Test

class AmharicTransliteratorTest {

  private fun typeWord(input: String): String {
    val out = StringBuilder()
    var buffer = ""
    for (ch in input) {
      val res = AmharicTransliterator.processKeystroke(buffer, ch)
      out.append(res.committedPrefix)
      buffer = res.newBuffer
    }
    out.append(AmharicTransliterator.flushBuffer(buffer))
    return out.toString()
  }

  @Test
  fun testBasicAmharicPhoneticWords() {
    assertEquals("ኑህ", typeWord("nuh"))
    assertEquals("ሰላም", typeWord("selam"))
    assertEquals("ኢትዮጵያ", typeWord("ityoPya"))
    assertEquals("አዲስ", typeWord("adis"))
    assertEquals("አበባ", typeWord("abeba"))
  }

  @Test
  fun testDigraphsAndEjectives() {
    assertEquals("ሸ", typeWord("she"))
    assertEquals("ቸ", typeWord("che"))
    assertEquals("ጨ", typeWord("Ce"))
    assertEquals("ጠ", typeWord("Te"))
    assertEquals("ኘ", typeWord("gne"))
    assertEquals("ጸ", typeWord("tse"))
    assertEquals("ኳ", typeWord("kwa"))
  }

  @Test
  fun testEthiopicPunctuation() {
    assertEquals("ሰላም።", typeWord("selam::"))
    assertEquals("ሰላም፡", typeWord("selam:"))
    assertEquals("ሰላም፣", typeWord("selam,"))
    assertEquals("ምን፧", typeWord("mn?"))
  }

  @Test
  fun testInPlaceFidelMorphingWorkflow() {
    val buffer = StringBuilder()
    val target =
      object : com.example.ime.KeyboardOutputTarget {
        override fun setComposingText(text: String) {}
        override fun commitText(text: String) {
          buffer.append(text)
        }
        override fun finishComposing() {}
        override fun deleteBackward(count: Int) {
          repeat(count) { if (buffer.isNotEmpty()) buffer.deleteCharAt(buffer.length - 1) }
        }
        override fun replaceLastCharacter(replacement: String) {
          if (buffer.isNotEmpty()) buffer.deleteCharAt(buffer.length - 1)
          buffer.append(replacement)
        }
        override fun performEnter() {}
        override fun moveCursor(delta: Int) {}
        override fun getTextBeforeCursor(maxChars: Int): String = buffer.takeLast(maxChars).toString()
      }

    val controller = com.example.ime.KeyboardSessionController(target)
    val famById = com.example.ime.FidelTable.families.associateBy { it.id }

    // Tap ወ -> types ወ and activates inPlaceActiveFamily = w
    controller.onInPlaceConsonantPressed(famById["w"]!!)
    assertEquals("ወ", buffer.toString())
    assertEquals("ው", controller.uiState.value.inPlaceActiveFamily?.orders?.get(5))

    // Tap E-slot (index 5 = 6th order) -> replaces ወ with ው
    controller.onInPlaceVowelPressed(5)
    assertEquals("ው", buffer.toString())
    assertEquals(null, controller.uiState.value.inPlaceActiveFamily)

    // Type ኑህ: ነ -> u(ኑ) -> ሀ -> e(ህ)
    buffer.clear()
    controller.onInPlaceConsonantPressed(famById["n"]!!)
    controller.onInPlaceVowelPressed(1) // u -> ኑ
    controller.onInPlaceConsonantPressed(famById["h1"]!!)
    controller.onInPlaceVowelPressed(5) // e -> ህ
    assertEquals("ኑህ", buffer.toString())
  }
}
