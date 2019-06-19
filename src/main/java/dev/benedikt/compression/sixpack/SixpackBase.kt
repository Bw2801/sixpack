package dev.benedikt.compression.sixpack

open class SixpackBase(protected val config: SixpackConfig) {

  protected var up = IntArray(this.config.twiceMax + 1)

  protected var leftCode = IntArray(this.config.maxChar + 1)
  protected var rightCode = IntArray(this.config.maxChar + 1)

  protected val frequencies = IntArray(this.config.twiceMax + 1)

  protected val copyMin = IntArray(this.config.copyRanges)
  protected val copyMax = IntArray(this.config.copyRanges)

  protected var maxDistance: Int = 0
  protected var maxSize: Int = 0

  /**
   * Initializes the data used for compression and decompression.
   */
  protected open fun init() {
    // Initialize the huffman frequency tree.
    this.up = IntArray(this.up.size) { it / 2 }
    this.leftCode = IntArray(this.leftCode.size) { 2 * it }
    this.rightCode = IntArray(this.rightCode.size) { 2 * it + 1 }
    this.frequencies.fill(1)

    // Initialize copy distance ranges.
    var distance = 0
    for (i in 0 until this.config.copyRanges) {
      this.copyMin[i] = distance
      distance += 1 shl this.config.copyBits[i]
      this.copyMax[i] = distance - 1
    }

    this.maxDistance = distance - 1
    this.maxSize = this.maxDistance + this.config.maxCopy
  }

  /**
   * Update the huffman model for each code.
   */
  protected fun updateHuffmanModel(code: Int) {
    var a = code + this.config.successMax

    this.frequencies[a] += 1

    if (this.up[a] == this.config.root) return

    var ua = this.up[a]
    this.updateFrequency(a, if (this.leftCode[ua] == a) this.rightCode[ua] else this.leftCode[ua])

    do {
      val uua = this.up[ua]
      val b = if (this.leftCode[uua] == ua) this.rightCode[uua] else this.leftCode[uua]

      if (this.frequencies[a] <= this.frequencies[b]) {
        a = this.up[a]
        ua = this.up[a]
        continue
      }

      if (this.leftCode[uua] == ua) {
        this.rightCode[uua] = a
      } else {
        this.leftCode[uua] = a
      }

      val c: Int
      if (this.leftCode[ua] == a) {
        this.leftCode[ua] = b
        c = this.rightCode[ua]
      } else {
        this.rightCode[ua] = b
        c = this.leftCode[ua]
      }

      this.up[b] = ua
      this.up[a] = uua

      this.updateFrequency(b, c)

      a = this.up[b]
      ua = this.up[a]
    } while (ua != this.config.root)
  }

  /**
   * Update frequency counts from leaf to root.
   */
  private fun updateFrequency(initialA: Int, initialB: Int) {
    var a = initialA
    var b = initialB

    do {
      this.frequencies[this.up[a]] = this.frequencies[a] + this.frequencies[b]

      a = this.up[a]
      if (a == this.config.root) break

      b = if (this.leftCode[this.up[a]] == a) this.rightCode[this.up[a]] else this.leftCode[this.up[a]]
    } while (a != this.config.root)

    if (this.frequencies[this.config.root] == this.config.maxFrequency) {
      (1..this.config.twiceMax).forEach {
        this.frequencies[a] = this.frequencies[a] shr 1
      }
    }
  }
}
