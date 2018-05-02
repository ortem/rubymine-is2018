package com.jetbrains.python.inspection

import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.*
import java.util.*

class LongCalculateVisitor : PyElementVisitor() {
    private val valueStack: Stack<Long> = Stack()

    var isValid = true
        private set

    fun getValue(): Long? {
        if (isValid && !valueStack.empty())
            return valueStack.peek()
        else
            return null
    }

    private fun calculate(expr: PyExpression): Long? {
        val stackSize = valueStack.size
        expr.accept(this)
        if (valueStack.size > stackSize)
            return valueStack.pop()
        return null
    }

    override fun visitPyNumericLiteralExpression(node: PyNumericLiteralExpression) {
        valueStack.push(node.longValue)
    }

    override fun visitPyBoolLiteralExpression(node: PyBoolLiteralExpression) {
        if (node.value)
            valueStack.push(1)
        else
            valueStack.push(0)
    }

    override fun visitPyBinaryExpression(node: PyBinaryExpression) {
        val left = node.leftExpression ?: return
        val op = node.operator ?: return
        val right = node.rightExpression ?: return

        val leftCalculated = calculate(left)
        val rightCalculated = calculate(right)

        val result: Long? =
                if (leftCalculated != null && rightCalculated != null) {
                    when (op) {
                        PyTokenTypes.PLUS -> leftCalculated + rightCalculated

                        PyTokenTypes.MINUS -> leftCalculated - rightCalculated

                        PyTokenTypes.MULT -> leftCalculated * rightCalculated

                        PyTokenTypes.DIV -> leftCalculated / rightCalculated

                        else -> null
                    }
                } else null

        if (result == null) {
            isValid = false
        } else {
            valueStack.push(result)
        }
    }

    override fun visitPyParenthesizedExpression(node: PyParenthesizedExpression) {
        node.containedExpression?.accept(this)
    }
}