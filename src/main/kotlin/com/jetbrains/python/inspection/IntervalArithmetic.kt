package com.jetbrains.python.inspection

enum class PointType {
    INCLUDE, EXCLUDE
}

fun PointType.revert(): PointType {
    return when (this) {
        PointType.INCLUDE -> PointType.EXCLUDE
        PointType.EXCLUDE -> PointType.INCLUDE
    }
}

interface PointSet {
    fun intersect(other: PointSet): PointSet
    fun union(other: PointSet): PointSet
    fun complement(): PointSet
    fun minus(other: PointSet): PointSet = this.intersect(other.complement())

    fun isEmpty(): Boolean
    fun isUniverse(): Boolean
}

class IntervalUnion(intervals: List<Interval>) : PointSet {
    val intervals = intervals.filter { !it.isEmpty() }.sortedBy { it.start.value }

    override fun isEmpty() = intervals.isEmpty()

    override fun isUniverse(): Boolean {
        if (intervals.isEmpty()) return false
        if (intervals.first().start.value != Long.MIN_VALUE) return false
        if (intervals.last().end.value != Long.MAX_VALUE) return false

        for (i in 0 until intervals.size - 1) {
            if (!intervals[i].isIntersectsWith(intervals[i + 1]) && !intervals[i].isStronglyAdjacentWith(intervals[i + 1]))
                return false
        }

        return true
    }

    override fun intersect(other: PointSet): PointSet {
        when (other) {
            is Interval -> {
                val overlapping = intervals.filter { it.isIntersectsWith(other) }
                if (overlapping.isEmpty())
                    return Empty
                return IntervalUnion(overlapping.map { it.intersect(other) as Interval })
            }
            else -> throw UnsupportedOperationException()
        }
    }

    override fun union(other: PointSet): PointSet {
        when (other) {
            is Interval -> {
                if (intervals.all { !it.isIntersectsWith(other) }) {
                    return IntervalUnion(intervals + other)
                }

                val newIntervals = mutableListOf<Interval>()
                newIntervals.addAll(intervals.takeWhile { !it.isIntersectsWith(other) })

                val firstOverlapping = intervals.first { it.isIntersectsWith(other) }
                val lastOverlapping = intervals.last { it.isIntersectsWith(other) }

                newIntervals.add(Interval.create(firstOverlapping.start, lastOverlapping.end))
                newIntervals.addAll(intervals.takeLastWhile { !it.isIntersectsWith(other) })
                return IntervalUnion(newIntervals)
            }
            else -> throw UnsupportedOperationException()
        }
    }

    override fun complement(): PointSet {
        if (this.isEmpty()) return Universe
        if (this.isUniverse()) return Empty

        val newIntervals = mutableListOf<Interval>()
        val firstStart = intervals.first().start
        newIntervals += Interval.create(EndPoint(Long.MIN_VALUE), EndPoint(firstStart.value, firstStart.type.revert()))

        for (i in 0 until intervals.size - 1) {
            val start = intervals[i].end
            val end = intervals[i + 1].start
            newIntervals += Interval.create(EndPoint(start.value, start.type.revert()), EndPoint(end.value, end.type.revert()))
        }

        val lastEnd = intervals.last().end
        newIntervals += Interval.create(EndPoint(lastEnd.value, lastEnd.type.revert()), EndPoint(Long.MAX_VALUE))

        return IntervalUnion(newIntervals)
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

open class Interval protected constructor(val start: EndPoint, val end: EndPoint) : PointSet {
    val isPoint = (start.value == end.value)

    override fun isEmpty() =
            (start.value == end.value && (start.type == PointType.EXCLUDE || end.type == PointType.EXCLUDE))

    override fun isUniverse() =
            (start.value == Long.MIN_VALUE && end.value == Long.MAX_VALUE)

    companion object {
        fun create(start: EndPoint, end: EndPoint): Interval {
            val interval = Interval(start, end)
            return when {
                interval.isEmpty() -> Empty
                interval.isUniverse() -> Universe
                else -> interval
            }
        }
    }

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
                when {
                    this.isIntersectsWith(other) -> {
                        return Interval.create(EndPoint.max(this.start, other.start), EndPoint.min(this.end, other.end))
                    }
                    this.isStronglyAdjacentWith(other) -> {
                        return if (this.end.toTheLeft(other.end))
                            Point(this.end.value)
                        else
                            Point(this.start.value)
                    }
                    else -> return Empty
                }
            }

            is IntervalUnion -> return other.intersect(this)

            else -> throw UnsupportedOperationException()
        }
    }

    override fun union(other: PointSet): PointSet {
        when (other) {
            is Interval -> {
                return if (this.isIntersectsWith(other) || this.isStronglyAdjacentWith(other)) {
                    Interval.create(EndPoint.min(this.start, other.start), EndPoint.max(this.end, other.end))
                } else {
                    IntervalUnion(listOf(this, other))
                }
            }

            is IntervalUnion -> return other.union(this)

            else -> throw UnsupportedOperationException()
        }
    }


    override fun complement(): PointSet {
        if (this.isEmpty()) return Universe
        if (this.isUniverse()) return Empty

        val left = Interval.create(EndPoint(Long.MIN_VALUE), EndPoint(this.start.value, this.start.type.revert()))
        val right = Interval.create(EndPoint(this.end.value, this.end.type.revert()), EndPoint(Long.MAX_VALUE))
        return IntervalUnion(listOf(left, right))
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