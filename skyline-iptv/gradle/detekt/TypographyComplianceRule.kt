package com.denham.skyline.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedArgument

/**
 * Detects hardcoded fontSize values and suggests using MaterialTheme.typography styles.
 * See docs/SKY_DESIGN_SYSTEM.md#typography
 */
class TypographyComplianceRule(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        id = "TypographyCompliance",
        severity = Severity.WARNING,
        description = "Hardcoded fontSize values should use MaterialTheme.typography",
        debt = Debt.FIVE_MINS,
    )

    override fun visitNamedArgument(argument: KtNamedArgument) {
        super.visitNamedArgument(argument)

        val name = argument.getArgumentName()?.asName?.asString()
        if (name == "fontSize") {
            val value = argument.getArgumentExpression()?.text
            if (value != null && value.matches(Regex("""(\d+)\.?sp"""))) {
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(argument),
                        message = "Hardcoded fontSize = $value should use MaterialTheme.typography style. " +
                            "See docs/SKY_DESIGN_SYSTEM.md#typography for available styles.",
                    )
                )
            }
        }
    }
}
