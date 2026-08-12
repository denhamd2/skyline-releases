package com.denham.skyline.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Flags hardcoded `Color(0x...)` literals, which should come from
 * `SkyPalette` instead. See docs/SKY_DESIGN_SYSTEM.md.
 *
 * The report names the specific token to use where the literal is a known Sky
 * colour, so the fix is stated rather than left to be looked up. A literal
 * that matches no token gets the generic instruction instead — the rule never
 * guesses a token it isn't certain of.
 *
 * The original version of this rule did not compile: it declared
 * `Severity.WARNING` (the constant is `Warning`) and overrode
 * `visitKtCallExpression`, which does not exist — the Kotlin PSI visitor
 * method is `visitCallExpression`. Nothing referenced the rule, so that was
 * never discovered.
 */
class SkyPaletteUsageRule(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        id = "SkyPaletteUsage",
        severity = Severity.Warning,
        description = "Hardcoded Color(0x...) values should use SkyPalette tokens",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (expression.calleeExpression?.text != "Color") return

        val firstArg = expression.valueArguments.firstOrNull()?.text ?: return
        if (!HEX_LITERAL.matches(firstArg)) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = fixFor(firstArg),
            )
        )
    }

    /** The proposed fix, as specific as the literal allows. */
    private fun fixFor(literal: String): String {
        val token = TOKENS[literal.uppercase().replace("0X", "0x")]
        return if (token != null) {
            "Color($literal) is SkyPalette.$token. Replace the literal with " +
                "the token: SkyPalette.$token."
        } else {
            "Color($literal) is not a Sky colour. Use the closest existing " +
                "SkyPalette token, or add one to SkyPalette in " +
                "ui/theme/Theme.kt and reference it — do not inline the hex. " +
                "See docs/SKY_DESIGN_SYSTEM.md."
        }
    }

    private companion object {
        val HEX_LITERAL = Regex("""0[xX][0-9A-Fa-f]+""")

        /**
         * Mirrors `SkyPalette` in ui/theme/Theme.kt. Kept here rather than
         * derived, because detekt runs without type resolution and cannot read
         * the app's constants. If a token is added there and not here the
         * message degrades to the generic form — it cannot become wrong.
         */
        val TOKENS = mapOf(
            "0xFF05070A" to "Canvas",
            "0xFF0E1520" to "Surface",
            "0xFF16233A" to "SurfaceElevated",
            "0xFF0B69F5" to "Accent",
            "0xFF2A9BE0" to "AccentBright",
            "0xFF000FF5" to "Brand",
            "0xFF0C1B87" to "Indigo",
            "0xFFE11D3F" to "LiveRed",
            "0xFFFFFFFF" to "TextPrimary",
            "0xFF8FA0B5" to "TextSecondary",
            "0xFF7C8899" to "TextMuted",
            "0xFFFF6B6B" to "Error",
        )
    }
}
