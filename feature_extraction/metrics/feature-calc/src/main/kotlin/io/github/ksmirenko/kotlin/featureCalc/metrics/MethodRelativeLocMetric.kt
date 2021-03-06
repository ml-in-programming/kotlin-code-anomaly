package io.github.ksmirenko.kotlin.featureCalc.metrics

import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiElement
import com.sixrr.stockmetrics.utils.LineUtil
import org.jetbrains.kotlin.psi.KtNamedFunction

class MethodRelativeLocMetric : Metric(
        csvName = "relativeLoc",
        description = "Относительное число строк кода"
) {
    override val visitor: Visitor by lazy { Visitor() }

    inner class Visitor : JavaRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement?) {
            if (element !is KtNamedFunction) {
                return
            }

            val funLinesCount = LineUtil.countLines(element)
            // parent is assumed to be class, object or file
            val parentLinesCount = LineUtil.countLines(element.parent)
            val funName = element.fqName.toString()
            appendRecord(funName, 1.0 * funLinesCount / parentLinesCount)
        }
    }
}
