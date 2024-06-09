package com.c6h5no2.probfilter.util

import com.google.common.base.Strings


object ClassEx {
  implicit final class Clazz(private val clazz: Class[_]) extends AnyVal {
    /** @return the (canonical) name without package name prefix */
    def getShortName: String = {
      var name = clazz.getCanonicalName
      if (Strings.isNullOrEmpty(name)) {
        name = clazz.getName
      }
      val packageName = clazz.getPackageName + "."
      if (name.startsWith(packageName)) {
        name = name.substring(packageName.length)
      }
      name
    }
  }

  /** @see [[ClassEx.Clazz.getShortName]] */
  def getShortName(clazz: Class[_]): String = clazz.getShortName
}
