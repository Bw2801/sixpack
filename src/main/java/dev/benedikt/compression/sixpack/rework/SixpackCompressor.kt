package dev.benedikt.compression.sixpack.rework

import java.nio.ByteBuffer

class SixpackCompressor(val maxFrequency: Int = 2000, val terminateCode: Int = 256, val firstCode: Int = 257,
                        val minCopy: Int = 3, val maxCopy: Int = 64, val copyRanges: Int = 6) {

    private val codesPerRange = this.maxCopy - this.minCopy + 1
    private val maxChar = this.firstCode + this.copyRanges * this.codesPerRange - 1

    private var model: HuffmanModel = HuffmanModel(this.maxChar, this.maxFrequency)
    private var inputBuffer: ByteBuffer = ByteBuffer.wrap(byteArrayOf())
    private var output = mutableListOf<Byte>()

    private var outputBitCount = 0
    private var outputBitBuffer = 0

    fun compress(inputBuffer: ByteBuffer): List<Byte> {
        this.model = HuffmanModel(this.maxChar, this.maxFrequency)
        this.inputBuffer = inputBuffer
        this.output = mutableListOf()

        while (true) {
            val code = this.readByte() ?: break
            this.compressCode(code)
        }

        this.compressCode(this.terminateCode)
        this.flushBitBuffer()

        return this.output
    }

    fun compressCode(code: Int) {
        val bits = mutableListOf<Boolean>()
        var node = this.model.getNode(code)

        do {
            bits.add(node.isRightChild())
            node = node.parent!!
        } while (!node.isRoot())

        bits.reversed().forEach { this.writeBit(it) }

        this.model.update(code)
    }

    private fun writeBit(bit: Boolean) {
        // Write 0 or 1 to buffer
        this.outputBitBuffer = this.outputBitBuffer shl 1
        if (bit) this.outputBitBuffer = this.outputBitBuffer or 1

        if (++this.outputBitCount >= 8) {
            // The buffer is full, write it to output and create a new one.
            this.output.add(this.outputBitBuffer.toByte())
            this.outputBitBuffer = 0
            this.outputBitCount = 0
        }
    }

    private fun flushBitBuffer() {
        while (this.outputBitCount > 0) {
            this.writeBit(false)
        }
    }

    private fun readByte(): Int? {
        if (!this.inputBuffer.hasRemaining()) return null
        // Convert the signed byte to an unsigned one for easier processing.
        return this.inputBuffer.get().toInt() and 0xff
    }
}