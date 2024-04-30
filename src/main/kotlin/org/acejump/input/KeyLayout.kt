package org.acejump.input

import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlin.math.abs
import kotlin.math.max

/**
 * Defines common keyboard layouts. Each layout has a key priority order,
 * based on each key's distance from the home row and how ergonomically
 * difficult they are to press.
 */
@Suppress("unused")
enum class KeyLayout(private val rows: Array<String>) {
  QWERTY(arrayOf("qwertyuiop", "asdfghjkl;", "zxcvbnm,./")),
  HYROLL(arrayOf("pclmvkuoy`", "nsrtd.aeih", "fg'wqx,zjb"));

  private val rowDistancesToHome = arrayOf(1, 0, -1)
  private val fingerIndexes = arrayOf(4, 3, 2, 1, 1, 1, 1, 2, 3, 4)
  private val handIndexes = arrayOf(1, 1, 1, 1, 1, 2, 2, 2, 2, 2)

  private val columnWeights = arrayOf(70, 80, 100, 95, 40, 40, 95, 100, 80, 70)
  private val rowDistanceWeights = arrayOf(100, 80, 60, 10, 1, 1, 1, 1) // TODO Why the fuck the index number was out of range? With rows [2,-1] the bigram row index should never be more than 4
  enum class BigramKind(internal val weight: Int) {
    Alternate(80),
    SameFinger(10),
    InRoll(100),
    OutRoll(60);
  }

  internal val allChars = rows.joinToString("").toCharArray().apply(CharArray::sort).joinToString("")

  inner class Key(rowIndex: Int, columnIndex: Int) {
    val distanceToHomeRow by lazy { rowDistancesToHome[rowIndex] }
    val fingerIndex by lazy { fingerIndexes[columnIndex] }
    val handIndex by lazy { handIndexes[columnIndex] }
    val columnWeight by lazy { columnWeights[columnIndex] }
  }

  inner class Bigram(private val key1: Key, private val key2: Key)
  {
    val weight by lazy { getWeight() }

    private fun getWeight() : Int {
      val bigramKindWeight = getBigramKind().weight
      val key1ColumnWeight = key1.columnWeight
      val key2ColumnWeight = key2.columnWeight
      val rowDistanceWeight = rowDistanceWeights[getDistanceToHomeRow()]
      return bigramKindWeight * key1ColumnWeight * key2ColumnWeight// * rowDistanceWeight
    }

    private fun getBigramKind(): BigramKind {
      if (key1.handIndex != key2.handIndex)
        return BigramKind.Alternate
      if (key1.fingerIndex == key2.fingerIndex)
        return BigramKind.SameFinger
      if (key1.fingerIndex < key2.fingerIndex)
        return BigramKind.InRoll
      return BigramKind.OutRoll
    }

    private fun getDistanceToHomeRow() : Int {
      var distanceHomeToKey1 = abs(key1.distanceToHomeRow)
      var distanceHomeToKey2 = abs(key2.distanceToHomeRow)
      var distanceKey1ToKey2 = abs(key2.distanceToHomeRow - key1.distanceToHomeRow)
      if (key1.handIndex == key2.handIndex)
        return distanceHomeToKey1 + distanceKey1ToKey2
      return max(distanceHomeToKey1, distanceHomeToKey2)
    }
  }

  private val allBigrams: Map<Char, Object2ObjectMap<Char, Bigram>> by lazy {
    val allBigrams = mutableMapOf<Char, Object2ObjectMap<Char, Bigram>>()
    val keysData = mutableMapOf<Char, Key>()
    for ((rowIndex, rowChars) in rows.withIndex()) {
      for ((columnIndex, char) in rowChars.withIndex()) {
        keysData[char] = Key(rowIndex, columnIndex)
      }
    }
    for (char1 in allChars) {
      val bigramsChar1 = Object2ObjectOpenHashMap<Char,Bigram>()
      val key1 = keysData.getValue(char1)
      for (char2 in allChars) {
        val key2 = keysData.getValue(char2)
        bigramsChar1[char2] = Bigram(key1, key2)
      }
      allBigrams[char1] = bigramsChar1
    }
    allBigrams
  }

  internal fun bigramWeight(char1: Char, char2: Char): Int {
    return allBigrams.getValue(char1).getValue(char2).hashCode()
  }
}
