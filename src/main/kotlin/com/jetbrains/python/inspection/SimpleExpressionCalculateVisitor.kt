package com.jetbrains.python.inspection

import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.*
import java.util.*

class SimpleExpressionCalculateVisitor : PyElementVisitor() {
    private val simpleExpressionStack: Stack<SimpleExpression> = Stack()

    var isValid = true
        private set

    fun getValue(): SimpleExpression? {
        return if (isValid && !simpleExpressionStack.empty())
            simpleExpressionStack.peek()
        else
            null
    }

    private fun calculate(expr: PyExpression): SimpleExpression? {
        val stackSize = simpleExpressionStack.size
        expr.accept(this)
        if (simpleExpressionStack.size > stackSize)
            return simpleExpressionStack.pop()
        return null
    }

    override fun visitPyNumericLiteralExpression(node: PyNumericLiteralExpression) {
        isValid = false
        val value = node.longValue ?: return
        simpleExpressionStack.push(SimpleValueExpression(value))
        isValid = true
    }

    override fun visitPyBoolLiteralExpression(node: PyBoolLiteralExpression) {
        if (node.value)
            simpleExpressionStack.push(SimpleValueExpression(1))
        else
            simpleExpressionStack.push(SimpleValueExpression(0))
    }

    override fun visitPyReferenceExpression(node: PyReferenceExpression) {
        isValid = false
        val name = node.name ?: return
        simpleExpressionStack.push(SimpleVariableExpression(name))
        isValid = true
    }

    override fun visitPyBinaryExpression(node: PyBinaryExpression) {
        isValid = false

        val left = node.leftExpression ?: return
        val op = node.operator ?: return
        val right = node.rightExpression ?: return

        val leftCalculated = calculate(left)
        val rightCalculated = calculate(right)

        val result: SimpleExpression? =
                if (leftCalculated != null && rightCalculated != null) {
                    when (op) {
                        PyTokenTypes.PLUS -> leftCalculated + rightCalculated

                        PyTokenTypes.MINUS -> leftCalculated - rightCalculated

                        PyTokenTypes.MULT -> leftCalculated * rightCalculated

                        PyTokenTypes.DIV -> leftCalculated / rightCalculated

                        else -> null
                    }
                } else null

        if (result != null) {
            isValid = true
            simpleExpressionStack.push(result)
        }
    }

    override fun visitPyParenthesizedExpression(node: PyParenthesizedExpression) {
        node.containedExpression?.accept(this)
    }
}