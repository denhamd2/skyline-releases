package com.denham.skyline.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

/**
 * Detects hardcoded Color(0x...) values and suggests using SkyPalette tokens.
 * See docs/SKY_DESIGN_SYSTEM.md#color-system
 */
class SkyPaletteUsageRule(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        id = "SkyPaletteUsage",
        severity = Severity.WARNING,
        description = "Hardcoded Color(0x...) values should use SkyPalette tokens",
        debt = Debt.FIVE_MINS,
    )

    private val colorConstructorPattern = Regex("""Color\s*\(\s*0x[0-9A-Fa-f]+\s*\)""")

    override fun visitKtCallExpression(expression: KtCallExpression) {
        super.visitKtCallExpression(expression)

        val callName = expression.calleeExpression?.text
        if (callName == "Color") {
            val args = expression.valueArguments
            if (args.isNotEmpty()) {
                val argText = args[0].text
                if (argText.matches(Regex("""0x[0-9A-Fa-f]+"""))) {
                    report(
                        CodeSmell(
                            issue = issue,
                            entity = Entity.from(expression),
                            message = "Color(0x${argText.substring(2)}) should use SkyPalette token. " +
                                "See docs/SKY_DESIGN_SYSTEM.md#color-system for available tokens.",
                        )
                    )
                }
            }
        }
    }
}
