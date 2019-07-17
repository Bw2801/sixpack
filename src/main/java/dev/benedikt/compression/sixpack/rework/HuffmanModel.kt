package dev.benedikt.compression.sixpack.rework

import java.lang.IllegalArgumentException

open class HuffmanModel(val maxChar: Int, val maxFrequency: Int) {

    protected val successMax = this.maxChar + 1
    protected val twiceMax = 2 * this.maxChar + 1

    val rootNode = Node(1)
    protected val nodes = mutableMapOf<Int, Node>()

    init {
        // Initializes the huffman model

        this.rootNode.clear()
        this.nodes.clear()
        this.nodes[1] = rootNode

        (2 until this.twiceMax).forEach {
            val node = Node(it)

            if (it > 1) {
                val parent = this.nodes[it / 2]!!
                node.parent = parent

                if (it % 2 == 0) {
                    parent.leftChild = node
                } else {
                    parent.rightChild = node
                }
            }

            this.nodes[it] = node
        }
    }

    /**
     * Updates the huffman model by increasing the frequency of the given value and updating the order of the nodes
     * accordingly from leaf to root.
     *
     * @param code the value to update the model with
     */
    fun update(code: Int) {
        val node = this.nodes[code + successMax] ?: throw IllegalArgumentException("The given value is not part of the huffman tree: $code")
        this.increaseFrequency(node)

        var currentNode = node
        var parent: Node? = currentNode.parent ?: return

        // Sort the huffman tree by frequency from the given leaf (value) to the root.
        while (parent?.isRoot() == false) {
            val grandparent = parent.parent!!
            val uncle = parent.getSibling()!!

            // Determine whether the nodes have to be switched.
            if (currentNode.frequency > uncle.frequency) {
                // The "uncle" (sibling of parent) node is higher up in the tree and has a lower frequency than the
                // current node, which is not allowed. They have to be switched in order to maintain the structure.

                if (parent.isLeftChild()) {
                    grandparent.rightChild = currentNode
                } else {
                    grandparent.leftChild = currentNode
                }

                if (currentNode.isLeftChild()) {
                    parent.leftChild = uncle
                } else {
                    parent.rightChild = uncle
                }

                // Update parent references.
                uncle.parent = parent
                currentNode.parent = grandparent
                currentNode = parent
            } else {
                currentNode = currentNode.parent!!
            }

            parent = currentNode.parent
        }
    }

    /**
     * Increases the node's frequency. Parent frequencies will be adjusted accordingly.
     *
     * @param node the node to increase the frequency for.
     */
    protected fun increaseFrequency(node: Node) {
        // Increment the nodes frequency by one.
        node.frequency += 1

        // Scale frequencies down by half to prevent overflow. This also provides some local adaption and better
        // compression.
        if (node.frequency >= this.maxFrequency) {
            node.frequency = node.frequency shr 1
        }
    }
}
