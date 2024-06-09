package com.c6h5no2.probfilter.util

import scala.reflect.ClassTag


object ArrayOpsEx {
  @inline def zeros[T: ClassTag](length: Int): Array[T] = Array.copyOf(Array.empty[T], length)

  @inline def boxedZero[T](array: Array[T]): java.lang.Number = array match {
    case _: Array[Byte] => java.lang.Byte.valueOf(0.asInstanceOf[Byte])
    case _: Array[Short] => java.lang.Short.valueOf(0.asInstanceOf[Short])
    case _: Array[Int] => java.lang.Integer.valueOf(0)
    case _: Array[Long] => java.lang.Long.valueOf(0L)
  }

  /** [[scala.collection.ArrayOps.appended]] without [[scala.reflect.ClassTag]] */
  @inline def appended[T](array: Array[T], elem: T): Array[T] = {
    val dest = Array.copyOf(array, array.length + 1)
    dest.update(array.length, elem)
    dest
  }

  /** [[scala.collection.ArrayOps.concat]] without [[scala.reflect.ClassTag]] */
  @inline def concated[T](array: Array[T], suffix: Array[T]): Array[T] = {
    val dest = Array.copyOf(array, array.length + suffix.length)
    System.arraycopy(suffix, 0, dest, array.length, suffix.length)
    dest
  }

  /** [[scala.collection.ArrayOps.updated]] without [[scala.reflect.ClassTag]] */
  @inline def updated[T](array: Array[T], index: Int, elem: T): Array[T] = {
    val dest = array.clone()
    dest.update(index, elem)
    dest
  }

  @inline def removedAt[T](array: Array[T], index: Int): Array[T] = {
    val dest = Array.copyOf(array, array.length - 1)
    System.arraycopy(array, index + 1, dest, index, array.length - index - 1)
    dest
  }

  /** @note `dest` is mutated in-place. */
  @inline def copyTo[T](src: Array[T], srcPos: Int, dest: Array[T], destPos: Int, length: Int): Unit = {
    System.arraycopy(src, srcPos, dest, destPos, length)
  }
}
