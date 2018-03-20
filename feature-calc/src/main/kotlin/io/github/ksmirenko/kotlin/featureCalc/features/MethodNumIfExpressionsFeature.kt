package io.github.ksmirenko.kotlin.featureCalc.features

import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiElement
import io.github.ksmirenko.kotlin.featureCalc.records.FeatureRecord
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class MethodNumIfExpressionsFeature : Feature(
        id = FeatureRecord.Type.MethodNumIfExpressions,
        csvName = "numIfExprs",
        description = "Number of if expressions"
) {
    override val visitor: Visitor by lazy { Visitor() }

    inner class Visitor : JavaRecursiveElementVisitor() {
        private var isInsideFunction = false
        private var ifExprCount = 0

        override fun visitElement(element: PsiElement?) {
            when (element) {
                is KtIfExpression -> {
                    ifExprCount += 1
                    super.visitElement(element)
                }

                is KtNamedFunction -> visitOuterFunction(element)
                is KtClassOrObject -> {
                }  // skip nested classes
                else -> super.visitElement(element)
            }
        }

        private fun visitOuterFunction(function: KtNamedFunction) {
            if (isInsideFunction) {
                // skip nested functions
                return
            }
            isInsideFunction = true

            ifExprCount = 0
            super.visitElement(function)

            val funName = function.fqName.toString()
            appendRecord(funName, ifExprCount)
            isInsideFunction = false
        }
    }
}