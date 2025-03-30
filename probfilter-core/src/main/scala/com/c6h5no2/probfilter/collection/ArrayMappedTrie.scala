package com.c6h5no2.probfilter.collection

import scala.annotation.tailrec
import scala.collection.AbstractIterator
import scala.jdk.CollectionConverters.IterableHasAsJava

/**
 * @note This variant of Trie uses `0` for `null`.
 */
final class ArrayMappedTrie private(private val height: Int, private val data: AnyRef) {

  import ArrayMappedTrie._

  def this() = this(-1, null)

  def get(index: Int): Int = {
    if (0 <= index && index < getCapacityByHeight(height)) {
      getLeafPath(index, height, data)
    } else {
      defaultValue
    }
  }

  @throws[IndexOutOfBoundsException]
  def set(index: Int, value: Int): ArrayMappedTrie = {
    if (0 <= index && index < getCapacityByHeight(maxHeight)) {
      val (newHeight, rootPath) = setRootPath(index, height, data)
      val newData = setLeafPath(index, value, newHeight, rootPath)
      new ArrayMappedTrie(newHeight, newData)
    } else {
      throw new IndexOutOfBoundsException()
    }
  }

  def iterator: Iterator[Int] = new ArrayMappedTrieIterator(this)

  def toProtoRaw: ArrayMappedTrieMessage = {
    ArrayMappedTrieMessage
      .newBuilder()
      .setHeight(height)
      .addAllData(iterator.map(java.lang.Integer.valueOf).to(Iterable).asJava)
      .build()
  }

  def toProtoHashEncoded: ArrayMappedTrieMessage = {
    val builder =
      ArrayMappedTrieMessage
        .newBuilder()
        .setHeight(height)
    var indexOfHash = 0
    iterator.zipWithIndex.foreach { case (value, index) =>
      val offset = index & mask
      if (offset == 0) {
        indexOfHash = builder.getDataCount
        builder.addData(0)
      }
      if (value != defaultValue) {
        builder.addData(value)
        val hash = builder.getData(indexOfHash)
        builder.setData(indexOfHash, hash | (1 << offset))
      }
    }
    builder.build()
  }
}

object ArrayMappedTrie {
  private type Leaf = Array[Int]
  private type Branch = Array[AnyRef] // An array of Leaf or Branch.

  private final val defaultValue: Int = 0
  private final val logWidth: Int = 5
  private final val mask: Int = 31
  private final val maxHeight: Int = 5

  private def newLeaf: Leaf =
    java.util.Arrays.copyOf(Array.emptyIntArray, 1 << logWidth)

  private def newBranch: Branch =
    java.util.Arrays.copyOf(Array.emptyObjectArray, 1 << logWidth)

  private def getCapacityByHeight(height: Int): Int = {
    height match {
      case -1 => 0
      case _ if 0 <= height && height <= maxHeight => 1 << (logWidth * (height + 1))
      case _ => throw new IllegalArgumentException()
    }
  }

  @tailrec
  private def getLeaf(index: Int, height: Int, data: AnyRef): Leaf = {
    if (data == null) {
      null
    } else {
      height match {
        case 0 =>
          data.asInstanceOf[Leaf]

        case _ => // 1 to maxHeight
          val offset = (index >>> (logWidth * height)) & mask
          val next = data.asInstanceOf[Branch].apply(offset)
          getLeaf(index, height - 1, next)
      }
    }
  }

  private def getLeafPath(index: Int, height: Int, data: AnyRef): Int = {
    val leaf = getLeaf(index, height, data)
    if (leaf == null) {
      defaultValue
    } else {
      val offset = index & mask
      leaf.apply(offset)
    }
  }

  @tailrec
  private def setRootPath(index: Int, height: Int, data: AnyRef): (Int, AnyRef) = {
    if (index < getCapacityByHeight(height)) {
      (height, data)
    } else {
      val newData = height match {
        case -1 =>
          newLeaf

        case _ =>
          val branch = newBranch
          branch.update(0, data)
          branch
      }
      setRootPath(index, height + 1, newData)
    }
  }

  private def setLeafPath(index: Int, value: Int, height: Int, data: AnyRef): AnyRef = {
    height match {
      case 0 =>
        val offset = index & mask
        val newData = if (data == null) {
          newLeaf
        } else {
          data.asInstanceOf[Leaf].clone()
        }
        newData.update(offset, value)
        newData

      case _ => // 1 to maxHeight
        val offset = (index >>> (logWidth * height)) & mask
        val next = if (data == null) {
          null
        } else {
          data.asInstanceOf[Branch].apply(offset)
        }
        val leafPath = setLeafPath(index, value, height - 1, next)
        val newData = if (data == null) {
          newBranch
        } else {
          data.asInstanceOf[Branch].clone()
        }
        newData.update(offset, leafPath)
        newData
    }
  }

  private class ArrayMappedTrieIterator(trie: ArrayMappedTrie) extends AbstractIterator[Int] {
    private[this] var index = 0
    private[this] val capacity = getCapacityByHeight(trie.height)
    private[this] var leaf: Leaf = _

    override def hasNext: Boolean = index < capacity

    override def next(): Int = {
      val offset = index & mask
      if (offset == 0) {
        leaf = getLeaf(index, trie.height, trie.data)
      }
      index += 1
      if (leaf == null) {
        defaultValue
      } else {
        leaf.apply(offset)
      }
    }
  }
}
