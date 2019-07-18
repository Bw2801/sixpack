package dev.benedikt.compression.sixpack.rework

data class Node(val value: Int, var frequency: Int = 1) {

    var parent: Node? = null
    var leftChild: Node? = null
    var rightChild: Node? = null

    fun clear() {
        this.parent = null
        this.leftChild = null
        this.rightChild = null
    }

    fun updateFrequency() {
        if (!this.isLeaf()) {
            // Leaf node frequencies are set manually. Nodes with children combine each children's frequencies
            // to determine their own.
            this.frequency = (this.leftChild?.frequency ?: 0) + (this.rightChild?.frequency ?: 0)
        }

        // Cascade the frequency update to the parent.
        this.parent?.updateFrequency()
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
