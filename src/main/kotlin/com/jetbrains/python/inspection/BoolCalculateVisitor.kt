package com.jetbrains.python.inspection

import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.*
import java.util.*

class BoolCalculateVisitor : PyElementVisitor() {
    private val booleanStack: Stack<Boolean> = Stack()

    var isValid = true
        private set

    fun getValue(): Boolean? {
        if (isValid && !booleanStack.empty())
            return booleanStack.peek()
        else
            return null
    }

    private fun calculateLong(expr: PyExpression): Long? {
        val simpleExpressionCalculateVisitor = SimpleExpressionCalculateVisitor()
        expr.accept(simpleExpressionCalculateVisitor)
        val result = simpleExpressionCalculateVisitor.getValue()

        return when (result) {
            is SimpleValueExpression -> result.value
            else -> null
        }
    }

    private fun calculateBool(expr: PyExpression): Boolean? {
        val stackSize = booleanStack.size
        expr.accept(this)
        if (booleanStack.size > stackSize)
            return booleanStack.pop()
        return null
    }

    override fun visitPyBoolLiteralExpression(node: PyBoolLiteralExpression) {
        booleanStack.push(node.value)
    }

    private fun isLongToBoolOperator(op: PyElementType): Boolean {
        return when (op) {
            PyTokenTypes.EQEQ,
            PyTokenTypes.NE,
            PyTokenTypes.LT,
            PyTokenTypes.GT,
            PyTokenTypes.LE,
            PyTokenTypes.GE -> true

            else -> false
        }
    }

    override fun visitPyBinaryExpression(node: PyBinaryExpression) {
        isValid = false

        val left = node.leftExpression ?: return
        val op = node.operator ?: return
        val right = node.rightExpression ?: return

        val result: Boolean?

        if (isLongToBoolOperator(op)) {
            val leftCalculated = calculateLong(left)
            val rightCalculated = calculateLong(right)
            result =
                    if (leftCalculated != null && rightCalculated != null) {
                        when (op) {
                            PyTokenTypes.EQEQ -> leftCalculated == rightCalculated

                            PyTokenTypes.NE -> leftCalculated != rightCalculated

                            PyTokenTypes.LT -> leftCalculated < rightCalculated

                            PyTokenTypes.GT -> leftCalculated > rightCalculated

                            PyTokenTypes.LE -> leftCalculated <= rightCalculated

                            PyTokenTypes.GE -> leftCalculated >= rightCalculated

                            else -> null
                        }
                    } else null
        } else {
            val leftCalculated = calculateBool(left)
            val rightCalculated = calculateBool(right)
            result =
                    if (leftCalculated != null && rightCalculated != null) {
                        when (op) {
                            PyTokenTypes.AND_KEYWORD -> leftCalculated && rightCalculated

                            PyTokenTypes.OR_KEYWORD -> leftCalculated || rightCalculated

                            else -> null
                        }
                    } else null
        }

        if (result != null) {
            isValid = true
            booleanStack.push(result)
        }
    }

    override fun visitPyPrefixExpression(node: PyPrefixExpression) {
        isValid = false

        val op = node.operator ?: return
        val operand = node.operand ?: return

        operand.accept(this)

        val operandCalculated = calculateBool(operand)
        val result: Boolean? =
                if (operandCalculated != null) {
                    when (op) {
                        PyTokenTypes.NOT_KEYWORD -> !operandCalculated

                        else -> null
                    }
                } else null


        if (result != null) {
            isValid = true
            booleanStack.push(result)
        }
    }

    override fun visitPyParenthesizedExpression(node: PyParenthesizedExpression) {
        node.containedExpression?.accept(this)
    }
}