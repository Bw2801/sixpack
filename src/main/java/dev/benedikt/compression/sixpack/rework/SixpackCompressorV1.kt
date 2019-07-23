package dev.benedikt.compression.sixpack.rework

import java.nio.ByteBuffer

class SixpackCompressorV1(val maxFrequency: Int = 2000, val terminateCode: Int = 256, val firstCode: Int = 257,
                          val minCopy: Int = 3, val maxCopy: Int = 64, val shortRange: Int = 3,
                          val copyRanges: Int = 6, val copyBits: IntArray = intArrayOf(4, 6, 8, 10, 12, 14),
                          val hashSize: Int = 16384, val hashMask: Int = hashSize - 1,
                          val binSearch: Int = 200, val binNext: Int = 20,
                          val textSearch: Int = 1000, val textNext: Int = 50) {

    private val codesPerRange = this.maxCopy - this.minCopy + 1

    private val copyMin = IntArray(this.copyRanges)
    private val copyMax = IntArray(this.copyRanges)

    private val maxChar = this.firstCode + this.copyRanges * this.codesPerRange - 1

    private var model = HuffmanModel(this.maxChar, this.maxFrequency)
    private var hashTable = HashTable()

    private var inputBuffer = ByteBuffer.wrap(byteArrayOf())
    private var codeBuffer = mutableListOf<Int>()
    private var output = mutableListOf<Byte>()
    private var bytesRead = 0

    private var outputBitCount = 0
    private var outputBitBuffer = 0

    private var isBinary: Boolean = false

    private var hasDictionary: Boolean = false

    private fun init() {
        this.model = HuffmanModel(this.maxChar, this.maxFrequency)
        this.hashTable = HashTable()
        this.output = mutableListOf()
        this.codeBuffer = mutableListOf()
        this.bytesRead = 0

        this.isBinary = false
        this.hasDictionary = false

        var distance = 0
        (0 until this.copyRanges).forEach {
            this.copyMin[it] = distance
            distance += 1 shl this.copyBits[it]
            this.copyMax[it] = distance - 1
        }
    }

    fun compress(inputBuffer: ByteBuffer): List<Byte> {
        this.init()

        this.inputBuffer = inputBuffer

        for (i in 0 until this.minCopy) {
            val code = this.readNextByte()

            if (code == null) {
                this.compressCode(this.terminateCode)
                this.flushBitBuffer()
                return this.output
            }

            this.codeBuffer.add(code)
            this.compressCode(code)
        }

        for (i in 0 until this.maxCopy) {
            val code = this.readNextByte() ?: break

            this.codeBuffer.add(code)

            if (code > 127) {
                this.isBinary = true
            }
        }

        this.hasDictionary = this.hasDictionary()

        var copying = false
        var index = this.minCopy
        var position = 0
        var length = 0

        while (index != this.codeBuffer.size) {
            // TODO: can this be moved to hasDictionary() ?
            if (this.hasDictionary && this.bytesRead % this.maxCopy == 0 && this.bytesRead / this.output.size < 2) {
                this.hasDictionary = false
            }

            this.hashTable.add(position, this.getBufferHashKey(position))

            if (copying) {
                length--
                if (length <= 1) {
                    copying = false
                }
            } else {
                val nextLength = this.findMatch(index + 1, if (this.isBinary) this.binNext else this.textNext).first

                val result = this.findMatch(index, if (this.isBinary) this.binSearch else this.textSearch)
                length = result.first
                val distance = result.second

                if (length < this.minCopy || length < nextLength) {
                    this.compressCode(this.getBufferedCode(index))

                    index++
                    position++

                    val code = this.readNextByte() ?: continue
                    this.codeBuffer.add(code)

                    continue
                }

                copying = true

                for (i in 0 until this.copyRanges) {
                    if (distance > this.copyMax[i]) continue

                    this.compressCode(this.firstCode - this.minCopy + length + i * this.codesPerRange)
                    this.outputCode(distance - this.copyMin[i], this.copyBits[i])
                    break
                }
            }

            index++
            position++

            val code = this.readNextByte() ?: continue
            this.codeBuffer.add(code)
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

    fun outputCode(code: Int, bits: Int) {
        var value = code
        for (i in 0 until bits) {
            this.writeBit(value and 1 == 1)
            value = value shr 1
        }
    }

    fun findMatch(pos: Int, maxDepth: Int): Pair<Int, Int> {
        var maxLength = 0
        var maxDistance = 0

        val position = if (pos == this.output.size) 0 else pos
        val key = this.getBufferHashKey(position)

        var depth = 0
        var index = this.hashTable.head[key]
        while (index != null) {
            if (++depth > maxDepth) break

            if (this.getBufferedCode(position + maxLength) != this.getBufferedCode(index + maxLength)) {
                index = this.hashTable.next[index]
                continue
            }

            var length = 0
            var from = index
            var to = position

            while (this.getBufferedCode(to) == this.getBufferedCode(from) && length < this.maxCopy && from != position && to != this.codeBuffer.size) {
                length++
                from++
                to++
            }

            val distance = position - index - length
            if (this.hasDictionary && distance > this.copyMax[0]) break
            if (length <= maxLength || distance > maxDistance) continue
            if (length <= this.minCopy || distance > this.copyMax[this.shortRange + (if (this.isBinary) 1 else 0)]) continue

            maxLength = length
            maxDistance = distance

            index = this.hashTable.next[index]
        }

        return Pair(maxLength, maxDistance)
    }

    fun getBufferHashKey(index: Int): Int {
        var key = this.getBufferedCode(index)
        for (i in 1 until this.minCopy) {
            if (index + i > 4200) {
                var foo = "bar"
            }
            key = key xor (this.getBufferedCode(index + i) shl (4 * i))
        }
        return key and this.hashMask
    }

    fun hasDictionary(): Boolean {
        var count = 0
        var fromIndex = 0

        for (i in 1 until this.minCopy + this.maxCopy) {
            if (this.getBufferedCode(i - 1) != 10) continue // TODO: why 10?

            var toIndex = i
            while (this.getBufferedCode(fromIndex) == this.getBufferedCode(toIndex)) {
                fromIndex++
                toIndex++
                count++
            }

            fromIndex = i
        }

        return count > (this.minCopy + this.maxCopy * 0.25)
    }

    private fun getBufferedCode(index: Int): Int {
        if (index >= this.codeBuffer.size) return 0
        return this.codeBuffer[index]
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

    /**
     * Read the next byte from the input stream.
     *
     * @return the read byte
     */
    private fun readNextByte(): Int? {
        if (!this.inputBuffer.hasRemaining()) return null
        this.bytesRead++
        // Convert the signed byte to an unsigned one for easier processing.
        return this.inputBuffer.get().toInt() and 0xff
    }
}
