package com.jetbrains.python.inspection

enum class SimpleOperator {
    PLUS, MINUS, MULT, DIV
}

fun SimpleOperator.revert(): SimpleOperator {
    return when (this) {
        SimpleOperator.PLUS -> SimpleOperator.MINUS
        SimpleOperator.MINUS -> SimpleOperator.PLUS
        SimpleOperator.MULT -> SimpleOperator.DIV
        SimpleOperator.DIV -> SimpleOperator.MULT
    }
}

fun SimpleOperator.apply(left: Long, right: Long): Long {
    return when (this) {
        SimpleOperator.PLUS -> left + right
        SimpleOperator.MINUS -> left - right
        SimpleOperator.MULT -> left * right
        SimpleOperator.DIV -> left / right
    }
}


interface SimpleExpression {
    operator fun plus(other: SimpleExpression): SimpleExpression
    operator fun minus(other: SimpleExpression): SimpleExpression
    operator fun times(other: SimpleExpression): SimpleExpression
    operator fun div(other: SimpleExpression): SimpleExpression
    fun equals(other: SimpleExpression): PointSet
    fun notEquals(other: SimpleExpression): PointSet
    fun less(other: SimpleExpression): PointSet
    fun greater(other: SimpleExpression): PointSet
    fun lessOrEquals(other: SimpleExpression): PointSet
    fun greaterOrEquals(other: SimpleExpression): PointSet
}

class SimpleVariableExpression(val variable: String) : SimpleExpression {
    // x + 5
    override fun plus(other: SimpleExpression): SimpleExpression {
        return when (other) {
            is SimpleValueExpression -> SimpleBinaryExpression(variable, SimpleOperator.PLUS, other.value)
            else -> throw UnsupportedOperationException()
        }
    }

    // x - 5
    override fun minus(other: SimpleExpression): SimpleExpression {
        return when (other) {
            is SimpleValueExpression -> SimpleBinaryExpression(variable, SimpleOperator.MINUS, other.value)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun times(other: SimpleExpression): SimpleExpression {
        throw UnsupportedOperationException()
    }

    override fun div(other: SimpleExpression): SimpleExpression {
        throw UnsupportedOperationException()
    }

    // x == 5
    override fun equals(other: SimpleExpression): PointSet {
        return when (other) {
            is SimpleValueExpression -> Point(other.value)
            else -> throw UnsupportedOperationException()
        }
    }

    // x != 5
    override fun notEquals(other: SimpleExpression): PointSet {
        return when (other) {
            is SimpleValueExpression -> Point(other.value).complement()
            else -> throw UnsupportedOperationException()
        }
    }

    // x < 5
    override fun less(other: SimpleExpression): PointSet {
        return when (other) {
            is SimpleValueExpression -> Interval.create(EndPoint(Long.MIN_VALUE), EndPoint(other.value, PointType.EXCLUDE))
            else -> throw UnsupportedOperationException()
        }
    }

    // x > 5
    override fun greater(other: SimpleExpression): PointSet {
        return when (other) {
            is SimpleValueExpression -> Interval.create(EndPoint(other.value, PointType.EXCLUDE), EndPoint(Long.MAX_VALUE))
            else -> throw UnsupportedOperationException()
        }
    }

    // x <= 5
    override fun lessOrEquals(other: SimpleExpression): PointSet {
        return when (other) {
            is SimpleValueExpression -> Interval.create(EndPoint(Long.MIN_VALUE), EndPoint(other.value))
            else -> throw UnsupportedOperationException()
        }
    }

    // x >= 5
    override fun greaterOrEquals(other: SimpleExpression): PointSet {
        return when (other) {
            is SimpleValueExpression -> Interval.create(EndPoint(other.value), EndPoint(Long.MAX_VALUE))
            else -> throw UnsupportedOperationException()
        }
    }
}

class SimpleValueExpression(val value: Long) : SimpleExpression {
    // 5 + 7
    override fun plus(other: SimpleExpression): SimpleExpression {
        return when (other) {
            is SimpleValueExpression -> SimpleValueExpression(this.value + other.value)
            else -> throw UnsupportedOperationException()
        }
    }

    // 5 - 7
    override fun minus(other: SimpleExpression): SimpleExpression {
        return when (other) {
            is SimpleValueExpression -> SimpleValueExpression(this.value - other.value)
            else -> throw UnsupportedOperationException()
        }
    }

    // 5 * 7
    override fun times(other: SimpleExpression): SimpleExpression {
        return when (other) {
            is SimpleValueExpression -> SimpleValueExpression(this.value * other.value)
            else -> throw UnsupportedOperationException()
        }
    }

    // 10 / 2
    override fun div(other: SimpleExpression): SimpleExpression {
        return when (other) {
            is SimpleValueExpression -> SimpleValueExpression(this.value / other.value)
            else -> throw UnsupportedOperationException()
        }
    }

    // 5 == 5
    // 5 == x
    override fun equals(other: SimpleExpression): PointSet {
        return when (other) {
            is SimpleValueExpression -> if (this.value == other.value) Universe else Empty
            else -> throw UnsupportedOperationException()
        }
    }

    // x != 5
    override fun notEquals(other: SimpleExpression): PointSet {
        return when (other) {
            is SimpleValueExpression -> if (this.value != other.value) Universe else Empty
            else -> throw UnsupportedOperationException()
        }
    }

    // 5 < 7
    // 5 < x
    // 5 < x + 2
    override fun less(other: SimpleExpression): PointSet {
        return when (other) {
            is SimpleValueExpression -> if (this.value < other.value) Universe else Empty
            is SimpleVariableExpression -> other.greater(this)
            is SimpleBinaryExpression -> other.greater(this)
            else -> throw UnsupportedOperationException()
        }
    }

    // 7 > 5
    // 7 > x
    // 7 > x + 5
    override fun greater(other: SimpleExpression): PointSet {
        return when (other) {
            is SimpleValueExpression -> if (this.value > other.value) Universe else Empty
            is SimpleVariableExpression -> other.less(this)
            is SimpleBinaryExpression -> other.less(this)
            else -> throw UnsupportedOperationException()
        }
    }

    // 5 <= 7
    // 5 <= x
    // 5 <= x + 7
    override fun lessOrEquals(other: SimpleExpression): PointSet {
        return when (other) {
            is SimpleValueExpression -> if (this.value <= other.value) Universe else Empty
            is SimpleVariableExpression -> other.greaterOrEquals(this)
            is SimpleBinaryExpression -> other.greaterOrEquals(this)
            else -> throw UnsupportedOperationException()
        }
    }

    // 7 >= 5
    // 7 >= x
    // 7 >= x + 5
    override fun greaterOrEquals(other: SimpleExpression): PointSet {
        return when (other) {
            is SimpleValueExpression -> if (this.value >= other.value) Universe else Empty
            is SimpleVariableExpression -> other.lessOrEquals(this)
            is SimpleBinaryExpression -> other.lessOrEquals(this)
            else -> throw UnsupportedOperationException()
        }
    }
}

class SimpleBinaryExpression(val variable: String, val op: SimpleOperator, val value: Long) : SimpleExpression {
    override fun plus(other: SimpleExpression): SimpleExpression {
        throw UnsupportedOperationException()
    }

    override fun minus(other: SimpleExpression): SimpleExpression {
        throw UnsupportedOperationException()
    }

    override fun times(other: SimpleExpression): SimpleExpression {
        throw UnsupportedOperationException()
    }

    override fun div(other: SimpleExpression): SimpleExpression {
        throw UnsupportedOperationException()
    }

    // x + 5 == 5
    // 5 == x
    override fun equals(other: SimpleExpression): PointSet {
        when (other) {
            is SimpleValueExpression -> {
                val variableExpression = SimpleVariableExpression(this.variable)
                val value = op.revert().apply(other.value, this.value)
                val valueExpression = SimpleValueExpression(value)
                return variableExpression.equals(valueExpression)
            }
            else -> throw UnsupportedOperationException()
        }
    }

    // x != 5
    override fun notEquals(other: SimpleExpression): PointSet {
        when (other) {
            is SimpleValueExpression -> {
                val variableExpression = SimpleVariableExpression(this.variable)
                val value = op.revert().apply(other.value, this.value)
                val valueExpression = SimpleValueExpression(value)
                return variableExpression.notEquals(valueExpression)
            }
            else -> throw UnsupportedOperationException()
        }
    }

    // x + 5 < 10
    override fun less(other: SimpleExpression): PointSet {
        when (other) {
            is SimpleValueExpression -> {
                val variableExpression = SimpleVariableExpression(this.variable)
                val value = op.revert().apply(other.value, this.value)
                val valueExpression = SimpleValueExpression(value)
                return variableExpression.less(valueExpression)
            }
            else -> throw UnsupportedOperationException()
        }
    }

    // x + 5 > 10
    override fun greater(other: SimpleExpression): PointSet {
        when (other) {
            is SimpleValueExpression -> {
                val variableExpression = SimpleVariableExpression(this.variable)
                val value = op.revert().apply(other.value, this.value)
                val valueExpression = SimpleValueExpression(value)
                return variableExpression.greater(valueExpression)
            }
            else -> throw UnsupportedOperationException()
        }
    }

    // x + 5 <= 7
    override fun lessOrEquals(other: SimpleExpression): PointSet {
        when (other) {
            is SimpleValueExpression -> {
                val variableExpression = SimpleVariableExpression(this.variable)
                val value = op.revert().apply(other.value, this.value)
                val valueExpression = SimpleValueExpression(value)
                return variableExpression.lessOrEquals(valueExpression)
            }
            else -> throw UnsupportedOperationException()
        }
    }

    // x + 5 >= 7
    override fun greaterOrEquals(other: SimpleExpression): PointSet {
        when (other) {
            is SimpleValueExpression -> {
                val variableExpression = SimpleVariableExpression(this.variable)
                val value = op.revert().apply(other.value, this.value)
                val valueExpression = SimpleValueExpression(value)
                return variableExpression.greaterOrEquals(valueExpression)
            }
            else -> throw UnsupportedOperationException()
        }
    }
}
