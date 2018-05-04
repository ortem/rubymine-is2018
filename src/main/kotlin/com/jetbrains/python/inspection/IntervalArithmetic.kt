package com.jetbrains.python.inspection

enum class PointType {
    INCLUDE, EXCLUDE
}

interface PointSet {
    fun intersect(other: PointSet): PointSet
    fun union(other: PointSet): PointSet
    fun minus(other: PointSet): PointSet
    fun complement(): PointSet = Universe.minus(this)
}

class IntervalUnion(val intervals: List<Interval>) : PointSet {
    override fun intersect(other: PointSet): PointSet {
        return when (other) {
            is Interval -> {
                val overlapping = intervals.filter { it.isIntersectsWith(other) }
                IntervalUnion(overlapping.map { it.intersect(other) as Interval })
            }
            else -> throw UnsupportedOperationException()
        }
    }

    override fun union(other: PointSet): PointSet {
        when (other) {
            is Interval -> {
                val newIntervals = mutableListOf<Interval>()
                newIntervals.addAll(intervals.takeWhile { !it.isIntersectsWith(other) })

                val firstOverlapping = intervals.first { it.isIntersectsWith(other) }
                val lastOverlapping = intervals.last { it.isIntersectsWith(other) }

                newIntervals.add(Interval(firstOverlapping.start, lastOverlapping.end))
                newIntervals.addAll(intervals.takeLastWhile { !it.isIntersectsWith(other) })
                return IntervalUnion(newIntervals)
            }
            else -> throw UnsupportedOperationException()
        }
    }

    override fun minus(other: PointSet): PointSet {
        throw UnsupportedOperationException()
    }
}

class EndPoint(val value: Long, val type: PointType = PointType.INCLUDE) {
    fun toTheLeft(other: EndPoint): Boolean {
        if (this.value < other.value)
            return true
        else if (this.value == other.value && this.type == PointType.EXCLUDE && other.type == PointType.INCLUDE)
            return true
        return false
    }

    companion object {
        fun max(first: EndPoint, second: EndPoint): EndPoint {
            if (first.toTheLeft(second)) return second
            return first
        }

        fun min(first: EndPoint, second: EndPoint): EndPoint {
            if (first.toTheLeft(second)) return first
            return second
        }
    }
}

open class Interval(val start: EndPoint, val end: EndPoint) : PointSet {
    val isSinglePoint = (start.value == end.value)

    val isEmpty = (start.value == end.value && (start.type == PointType.EXCLUDE || end.type == PointType.EXCLUDE))

    fun isIntersectsWith(other: Interval) = (this.start.toTheLeft(other.end) && other.start.toTheLeft(this.end))

    // Examples:
    // (1, 3] is adjacent with (3, 4)
    // (1, 3) is adjacent with [3, 4)
    // (1, 3) is adjacent with (3, 4)
    fun isAdjacentWith(other: Interval): Boolean {
        if (this.isIntersectsWith(other)) return false
        if (this.end.toTheLeft(other.end))
            return this.end.value == other.start.value
        else
            return this.start.value == other.end.value
    }

    // (1, 3) is strongly adjacent with [3, 4)
    fun isStronglyAdjacentWith(other: Interval): Boolean {
        return when {
            this.isIntersectsWith(other) -> false
            this.end.toTheLeft(other.end) -> this.end.value == other.start.value &&
                    (this.end.type == PointType.INCLUDE || other.start.type == PointType.INCLUDE)
            else -> (this.start.value == other.end.value) &&
                    (this.start.type == PointType.INCLUDE || other.end.type == PointType.INCLUDE)
        }
    }

    override fun intersect(other: PointSet): PointSet {
        when (other) {
            is Interval -> {
                if (this.isIntersectsWith(other)) {
                    val intersection = Interval(EndPoint.max(this.start, other.start), EndPoint.min(this.end, other.end))
                    if (intersection.isEmpty)
                        return Empty
                    return intersection
                } else return Empty
            }
            else -> return Empty
        }
    }

    override fun union(other: PointSet): PointSet {
        when (other) {
            is Interval -> {
                return if (this.isIntersectsWith(other) || this.isStronglyAdjacentWith(other)) {
                    Interval(EndPoint.min(this.start, other.start), EndPoint.max(this.end, other.end))
                } else {
                    IntervalUnion(listOf(this, other).sortedBy { it.start.value })
                }
            }
            else -> return Empty
        }
    }

    override fun minus(other: PointSet): PointSet {
        throw UnsupportedOperationException()
    }
}

class Point(val value: Long) : Interval(EndPoint(value), EndPoint(value))

object Universe : Interval(EndPoint(Long.MIN_VALUE, PointType.INCLUDE), EndPoint(Long.MAX_VALUE, PointType.INCLUDE))

object Empty : Interval(EndPoint(0, PointType.EXCLUDE), EndPoint(0, PointType.EXCLUDE))

fun Boolean.toPointSet(): PointSet {
    return when (this) {
        true -> Universe
        false -> Empty
    }
}