package com.jetbrains.python.inspection

import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.*
import java.util.*

class BoolCalculateVisitor : PyElementVisitor() {
    private val pointSetStack: Stack<PointSet> = Stack()

    var isValid = true
        private set

    fun getValue(): Boolean? {
        return if (isValid && !pointSetStack.empty()) {
            val result = pointSetStack.peek()
            when {
                result.isUniverse() -> true
                result.isEmpty() -> false
                else -> null
            }
        }
        else
            null
    }

    private fun calculateSimpleExpression(expr: PyExpression): SimpleExpression? {
        val simpleExpressionCalculateVisitor = SimpleExpressionCalculateVisitor()
        expr.accept(simpleExpressionCalculateVisitor)
        return simpleExpressionCalculateVisitor.getValue()
    }

    private fun calculateBool(expr: PyExpression): PointSet? {
        val stackSize = pointSetStack.size
        expr.accept(this)
        if (pointSetStack.size > stackSize)
            return pointSetStack.pop()
        return null
    }

    override fun visitPyBoolLiteralExpression(node: PyBoolLiteralExpression) {
        pointSetStack.push(node.value.toPointSet())
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

        val result: PointSet?

        if (isLongToBoolOperator(op)) {
            val leftCalculated = calculateSimpleExpression(left)
            val rightCalculated = calculateSimpleExpression(right)
            result =
                    if (leftCalculated != null && rightCalculated != null) {
                        when (op) {
                            PyTokenTypes.EQEQ -> leftCalculated.equals(rightCalculated)
                            PyTokenTypes.NE -> leftCalculated.notEquals(rightCalculated)
                            PyTokenTypes.LT -> leftCalculated.less(rightCalculated)
                            PyTokenTypes.GT -> leftCalculated.greater(rightCalculated)
                            PyTokenTypes.LE -> leftCalculated.lessOrEquals(rightCalculated)
                            PyTokenTypes.GE -> leftCalculated.greaterOrEquals(rightCalculated)

                            else -> null
                        }
                    } else null
        } else {
            val leftCalculated = calculateBool(left)
            val rightCalculated = calculateBool(right)
            result =
                    if (leftCalculated != null && rightCalculated != null) {
                        when (op) {
                            PyTokenTypes.AND_KEYWORD -> leftCalculated.intersect(rightCalculated)
                            PyTokenTypes.OR_KEYWORD -> leftCalculated.union(rightCalculated)
                            else -> null
                        }
                    } else null
        }

        if (result != null) {
            isValid = true
            pointSetStack.push(result)
        }
    }

    override fun visitPyPrefixExpression(node: PyPrefixExpression) {
        isValid = false

        val op = node.operator ?: return
        val operand = node.operand ?: return

        operand.accept(this)

        val operandCalculated = calculateBool(operand)
        val result: PointSet? = operandCalculated?.let {
            when (op) {
                PyTokenTypes.NOT_KEYWORD -> operandCalculated.complement()
                else -> null
            }
        }


        if (result != null) {
            isValid = true
            pointSetStack.push(result)
        }
    }

    override fun visitPyParenthesizedExpression(node: PyParenthesizedExpression) {
        node.containedExpression?.accept(this)
    }
}