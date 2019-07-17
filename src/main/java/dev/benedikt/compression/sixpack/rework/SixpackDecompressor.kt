package dev.benedikt.compression.sixpack.rework

import java.lang.RuntimeException
import java.nio.ByteBuffer

class SixpackDecompressor(val maxFrequency: Int = 2000, val terminateCode: Int = 256, val firstCode: Int = 257,
                          val minCopy: Int = 3, val maxCopy: Int = 64,
                          val copyRanges: Int = 6, val copyBits: IntArray = intArrayOf(4, 6, 8, 10, 12, 14)) {

    private var maxSize = 0
    private val codesPerRange = this.maxCopy - this.minCopy + 1

    private val copyMin = IntArray(this.copyRanges)
    private val copyMax = IntArray(this.copyRanges)

    private val maxChar = this.firstCode + this.copyRanges * this.codesPerRange - 1
    private val successMax = this.maxChar + 1

    private var model = HuffmanModel(this.maxChar, this.maxFrequency)

    private var inputBuffer = ByteBuffer.wrap(byteArrayOf())
    private var inputBitCount = 0
    private var inputBitBuffer = 0

    private fun init() {
        this.model = HuffmanModel(this.maxChar, this.maxFrequency)

        this.inputBitCount = 0
        this.inputBitBuffer = 0

        var distance = 0
        (0 until this.copyRanges).forEach {
            this.copyMin[it] = distance
            distance += 1 shl this.copyBits[it]
            this.copyMax[it] = distance - 1
        }

        this.maxSize = distance - 1 + this.maxCopy
    }

    fun decompress(inputBuffer: ByteBuffer): ByteArray {
        this.init()

        this.inputBuffer = inputBuffer
        val output = mutableListOf<Byte>()

        do {
            val code = this.decompressCode()!!
            if (code == this.terminateCode) break

            // The first 255 codes are single literal characters and do not need to be processed any further.
            if (code < 256) {
                output.add(code.toByte())
                continue
            }

            // Every other code determines a range to copy codes from.
            val index = (code - this.firstCode) / this.codesPerRange
            val length = code - this.firstCode + this.minCopy - index * this.codesPerRange
            val distance = this.getInputCode(this.copyBits[index]) + length + this.copyMin[index]
            val copyFrom = output.size - distance

            // Copy the bytes from to the end of the output.
            (0 until length).forEach { output.add(output[copyFrom + it]) }
        } while (true)

        return output.toByteArray()
    }

    /**
     * Decompresses the next character value from the input stream.
     *
     * @return the decompressed value
     */
    private fun decompressCode(): Int? {
        var node: Node? = this.model.rootNode

        do {
            val bit = this.readNextBit() ?: throw RuntimeException("Unexpected end of stream.")
            node = if (bit) node?.rightChild else node?.leftChild
            if (node == null) throw RuntimeException("The compressed data does not match with the huffman model.")
        } while(node!!.value <= this.maxChar)

        val code = node.value - this.successMax
        this.model.update(code)
        return code
    }

    /**
     * Reads a multi bit input code from the input stream.
     *
     * @return the input code
     */
    private fun getInputCode(bits: Int): Int {
        var mask = 1
        var code = 0

        for (i in 0 until bits) {
            val bit = this.readNextBit() ?: throw RuntimeException("Unexpected end of stream.")
            if (bit) {
                code = code or mask
            }
            mask = code shl 1
        }

        return code
    }

    /**
     * Read the next bit from the input stream.
     *
     * @return the bit value. `null` if there is none
     */
    private fun readNextBit(): Boolean? {
        if (this.inputBitCount-- <= 0) {
            // The current bit buffer is depleted. Get the next one.
            this.inputBitBuffer = this.readNextByte() ?: return null
            this.inputBitCount = 7
        }

        // Retrieve the first bit and shift the byte to the left by one (removing the first bit).
        val masked = this.inputBitBuffer and 128
        val bit = masked != 0
        this.inputBitBuffer = this.inputBitBuffer shl 1 and 255
        println(bit)
        return bit
    }

    /**
     * Read the next byte from the input stream.
     *
     * @return the read byte
     */
    private fun readNextByte(): Int? {
        if (!this.inputBuffer.hasRemaining()) return null
        // Convert the signed byte to an unsigned one for easier processing.
        return this.inputBuffer.get().toInt() and 0xff
    }
}
