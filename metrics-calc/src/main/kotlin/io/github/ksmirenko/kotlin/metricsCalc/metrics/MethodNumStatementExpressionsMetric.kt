package io.github.ksmirenko.kotlin.metricsCalc.metrics

import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiElement
import io.github.ksmirenko.kotlin.metricsCalc.records.MetricRecord
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStatementExpression

class MethodNumStatementExpressionsMetric : Metric(
        id = MetricRecord.Type.MethodNumStatementExpressions,
        csvName = "numStatementExpressions",
        description = "Number of Kotlin StatementExpressions"
) {
    override val visitor: Visitor by lazy { Visitor() }

    inner class Visitor : JavaRecursiveElementVisitor() {
        private var isInsideFunction = false
        private var stmtCount = 0

        override fun visitElement(element: PsiElement?) {
            when (element) {
                is KtNamedFunction -> visitOuterFunction(element)
                is KtClassOrObject -> {
                }  // skip nested classes

                is KtStatementExpression -> {
                    if (element !is KtBlockExpression) {
                        stmtCount += 1
                    }
                    super.visitElement(element)
                }
                else -> super.visitElement(element)
            }
        }

        private fun visitOuterFunction(function: KtNamedFunction) {
            if (isInsideFunction) {
                // skip nested functions
                return
            }
            isInsideFunction = true

            stmtCount = 0
            super.visitElement(function)

            val funName = function.fqName.toString()
            appendRecord(funName, stmtCount)
            isInsideFunction = false
        }
    }
}
