package com.jetbrains.python.inspection

enum class PointType {
    INCLUDE, EXCLUDE
}

open class PointSet(elements: List<SimplePointSet>)

interface SimplePointSet

class EndPoint(value: Long, type: PointType = PointType.INCLUDE) : SimplePointSet
open class Interval(val from: EndPoint, val to: EndPoint) : SimplePointSet

object Universe : Interval(EndPoint(Long.MIN_VALUE, PointType.INCLUDE), EndPoint(Long.MAX_VALUE, PointType.INCLUDE))
object Empty : SimplePointSet
