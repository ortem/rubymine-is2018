package com.jetbrains.python.inspection

enum class SimpleOperator {
    PLUS, MINUS, MULT, DIV
}

interface SimpleExpression {
    operator fun plus(other: SimpleExpression): SimpleExpression
    operator fun minus(other: SimpleExpression): SimpleExpression
    operator fun times(other: SimpleExpression): SimpleExpression
    operator fun div(other: SimpleExpression): SimpleExpression
}

class SimpleVariableExpression(val variable: String) : SimpleExpression {
    override fun plus(other: SimpleExpression): SimpleExpression {
        return when (other) {
            is SimpleValueExpression -> SimpleBinaryExpression(variable, SimpleOperator.PLUS, other.value)
            else -> throw UnsupportedOperationException()
        }
    }

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
}

class SimpleValueExpression(val value: Long) : SimpleExpression {
    override fun plus(other: SimpleExpression): SimpleExpression {
        return when (other) {
            is SimpleValueExpression -> SimpleValueExpression(this.value + other.value)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun minus(other: SimpleExpression): SimpleExpression {
        return when (other) {
            is SimpleValueExpression -> SimpleValueExpression(this.value - other.value)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun times(other: SimpleExpression): SimpleExpression {
        return when (other) {
            is SimpleValueExpression -> SimpleValueExpression(this.value * other.value)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun div(other: SimpleExpression): SimpleExpression {
        return when (other) {
            is SimpleValueExpression -> SimpleValueExpression(this.value / other.value)
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

}
