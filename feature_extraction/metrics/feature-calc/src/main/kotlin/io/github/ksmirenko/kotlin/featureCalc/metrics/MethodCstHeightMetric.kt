package io.github.ksmirenko.kotlin.featureCalc.metrics

import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNamedFunction

class MethodCstHeightMetric : Metric(
        csvName = "cstHeight",
        description = "Высота CST"
) {
    override val visitor: Visitor by lazy { Visitor() }

    inner class Visitor : JavaRecursiveElementVisitor() {
        private var methodNestingDepth = 0
        private var curAstHeight = 1
        private var maxAstHeight = 1

        override fun visitElement(element: PsiElement?) {
            if (curAstHeight > maxAstHeight) {
                maxAstHeight = curAstHeight
            }

            curAstHeight += 1
            when (element) {
                is KtNamedFunction -> visitKtFunction(element)
                else -> super.visitElement(element)
            }
            curAstHeight -= 1
        }

        private fun visitKtFunction(function: KtNamedFunction) {
            if (methodNestingDepth == 0) {
                curAstHeight = 1
                maxAstHeight = 1
            }

            methodNestingDepth++
            super.visitElement(function)
            methodNestingDepth--

            if (methodNestingDepth == 0) {
                val funName = function.fqName.toString()
                appendRecord(funName, maxAstHeight)
            }
        }
    }
}
