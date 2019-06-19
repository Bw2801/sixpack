package dev.benedikt.compression.sixpack

import java.util.*

data class SixpackConfig(
    val maxFrequency: Int = 2000,

    val minCopy: Int = 3,
    val maxCopy: Int = 64,

    val copyRanges: Int = 6,
    val copyBits: IntArray = intArrayOf(4, 6, 8, 10, 12, 14),
    val codesPerRange: Int = maxCopy - minCopy + 1,

    val terminationCode: Int = 256,
    val firstCode: Int = 257,

    val maxChar: Int = firstCode + copyRanges * codesPerRange - 1,
    val successMax: Int = maxChar + 1,
    val twiceMax: Int = 2 * maxChar + 1,

    val root: Int = 1
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SixpackConfig

    if (maxFrequency != other.maxFrequency) return false
    if (minCopy != other.minCopy) return false
    if (maxCopy != other.maxCopy) return false
    if (copyRanges != other.copyRanges) return false
    if (!Arrays.equals(copyBits, other.copyBits)) return false
    if (codesPerRange != other.codesPerRange) return false
    if (terminationCode != other.terminationCode) return false
    if (firstCode != other.firstCode) return false
    if (maxChar != other.maxChar) return false
    if (successMax != other.successMax) return false
    if (twiceMax != other.twiceMax) return false
    if (root != other.root) return false

    return true
  }

  override fun hashCode(): Int {
    var result = maxFrequency
    result = 31 * result + minCopy
    result = 31 * result + maxCopy
    result = 31 * result + copyRanges
    result = 31 * result + Arrays.hashCode(copyBits)
    result = 31 * result + codesPerRange
    result = 31 * result + terminationCode
    result = 31 * result + firstCode
    result = 31 * result + maxChar
    result = 31 * result + successMax
    result = 31 * result + twiceMax
    result = 31 * result + root
    return result
  }
}
