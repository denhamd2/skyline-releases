package com.denham.skyline.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedArgument

/**
 * Detects padding/margin values that don't align to the 8-point grid.
 * Valid values: 4, 8, 12, 16, 24, 32, 40, 48, ...
 * See docs/SKY_DESIGN_SYSTEM.md#spacing
 */
class SpacingGridComplianceRule(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        id = "SpacingGridCompliance",
        severity = Severity.WARNING,
        description = "Padding/margin values should align to 8-point grid",
        debt = Debt.TEN_MINS,
    )

    private val spacingProperties = setOf(
        "padding", "margin", "verticalPadding", "horizontalPadding",
        "paddingStart", "paddingEnd", "paddingTop", "paddingBottom",
        "marginStart", "marginEnd", "marginTop", "marginBottom"
    )

    private val validSpacingValues = setOf(4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 56, 64, 72, 80, 88, 96)

    override fun visitNamedArgument(argument: KtNamedArgument) {
        super.visitNamedArgument(argument)

        val name = argument.getArgumentName()?.asName?.asString()
        if (name in spacingProperties) {
            val value = argument.getArgumentExpression()?.text
            if (value != null) {
                val dpValue = extractDpValue(value)
                if (dpValue != null && dpValue !in validSpacingValues) {
                    report(
                        CodeSmell(
                            issue = issue,
                            entity = Entity.from(argument),
                            message = "$name = ${dpValue}.dp breaks 8-point grid. " +
                                "Use one of: 4, 8, 12, 16, 20, 24, 28, 32, 36, 40... " +
                                "See docs/SKY_DESIGN_SYSTEM.md#spacing",
                        )
                    )
                }
            }
        }
    }

    override fun visitKtCallExpression(expression: KtCallExpression) {
        super.visitKtCallExpression(expression)

        val callName = expression.calleeExpression?.text
        if (callName in spacingProperties) {
            val args = expression.valueArguments
            if (args.isNotEmpty()) {
                val value = args[0].text
                val dpValue = extractDpValue(value)
                if (dpValue != null && dpValue !in validSpacingValues) {
                    report(
                        CodeSmell(
                            issue = issue,
                            entity = Entity.from(expression),
                            message = "$callName($dpValue.dp) breaks 8-point grid. " +
                                "Use one of: 4, 8, 12, 16, 20, 24, 28, 32, 36, 40... " +
                                "See docs/SKY_DESIGN_SYSTEM.md#spacing",
                        )
                    )
                }
            }
        }
    }

    private fun extractDpValue(text: String): Int? {
        val cleanText = text.replace("\\s".toRegex(), "")
        val match = Regex("""(\d+)\.?dp""").find(cleanText)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }
}
