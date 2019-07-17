package dev.benedikt.compression.sixpack.rework

data class Node(val value: Int, private val initialFrequency: Int = 0) {

    var parent: Node? = null
    var leftChild: Node? = null
    var rightChild: Node? = null

    private var _frequency = this.initialFrequency
    var frequency: Int
        get() {
            if (isLeaf()) return this._frequency
            return (this.leftChild?.frequency ?: 0) + (this.rightChild?.frequency ?: 0)
        }
        set(value) { this._frequency = value }

    fun clear() {
        this.parent = null
        this.leftChild = null
        this.rightChild = null
        this._frequency = this.initialFrequency
    }

    fun getSibling(): Node? {
        return if (this.isLeftChild()) {
            this.parent?.rightChild
        } else {
            this.parent?.leftChild
        }
    }

    fun hasSibling() = this.getSibling() != null

    fun isLeftChild() = this.parent?.leftChild == this
    fun isRightChild() = this.parent?.rightChild == this

    fun isLeaf() = this.leftChild == null && this.rightChild == null
    fun isRoot() = this.parent == null
}
