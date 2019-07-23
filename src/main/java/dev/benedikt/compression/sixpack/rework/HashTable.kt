package dev.benedikt.compression.sixpack.rework

class HashTable {

    val head = mutableMapOf<Int, Int>()
    val tail = mutableMapOf<Int, Int>()

    val next = mutableMapOf<Int, Int>()
    val prev = mutableMapOf<Int, Int>()

    fun add(index: Int, key: Int) {
        val head = this.head[key]

        if (head == null) {
            this.tail[key] = index
            this.next.remove(index)
        } else {
            this.next[index] = head
            this.prev[head] = index
        }

        this.head[key] = index
        this.prev.remove(index)
    }

//    fun remove(index: Int, key: Int) {
//        val tail = this.tail[key]
//        val head = this.head[key]
//
//        if (tail == head) {
//            this.head.remove(key)
//            return
//        }
//
//        val prevTail = this.prev[tail]!!
//
//        this.next.remove(prevTail)
//        this.tail[key] = prevTail
//    }
}
