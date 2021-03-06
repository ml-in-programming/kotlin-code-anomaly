package io.github.ksmirenko.kotlin.featureCalc.metrics

import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression

class MethodNumTypeCastExpressionsMetric : Metric(
        csvName = "numTypecastExpr",
        description = "Число выражений преобразования типов"
) {

    override val visitor: Visitor by lazy { Visitor() }

    inner class Visitor : JavaRecursiveElementVisitor() {
        private var methodNestingDepth = 0
        private var typecastExprCount = 0

        override fun visitElement(element: PsiElement?) {
            when (element) {
                is KtOperationReferenceExpression -> {
                    if (element.text == "as" || element.text == "is") {
                        typecastExprCount += 1
                    }
                    super.visitElement(element)
                }

                is KtNamedFunction -> visitKtFunction(element)
                else -> super.visitElement(element)
            }
        }

        private fun visitKtFunction(function: KtNamedFunction) {
            if (methodNestingDepth == 0) {
                typecastExprCount = 0
            }

            methodNestingDepth++
            super.visitElement(function)
            methodNestingDepth--

            if (methodNestingDepth == 0) {
                val funName = function.fqName.toString()
                appendRecord(funName, typecastExprCount)
            }
        }
    }
}
