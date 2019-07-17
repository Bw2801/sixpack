package dev.benedikt.compression.sixpack.rework

class SixpackCompressor(val maxChar: Int = 628, val maxFrequency: Int = 2000) {

    private var model = HuffmanModel(this.maxChar, this.maxFrequency)

    fun compress() {


        this.reset()
    }

    private fun reset() {
        this.model = HuffmanModel(this.maxChar, this.maxFrequency)
    }
}
