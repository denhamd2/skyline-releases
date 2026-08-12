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
 * Detects animation durations that don't match design spec.
 * Valid durations:
 * - Enter: 340ms (with LinearOutSlowInEasing)
 * - Exit: 200ms (with FastOutLinearInEasing)
 * - TV focus: 140ms (spring tween)
 * - Screen transition: 200ms (crossfade)
 * See docs/SKY_DESIGN_SYSTEM.md#motion
 */
class AnimationDurationComplianceRule(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        id = "AnimationDurationCompliance",
        severity = Severity.WARNING,
        description = "Animation durations should match design spec",
        debt = Debt.FIVE_MINS,
    )

    private val durationPatterns = mapOf(
        "tween" to setOf(140, 200, 220, 340),  // Valid tween durations
        "spring" to setOf(400, 500, 600),      // Valid spring durations
        "fadeIn" to setOf(200, 340),           // Enter fades
        "fadeOut" to setOf(200),               // Exit fades
    )

    override fun visitNamedArgument(argument: KtNamedArgument) {
        super.visitNamedArgument(argument)

        val name = argument.getArgumentName()?.asName?.asString()
        if (name == "durationMillis" || name == "duration") {
            val value = argument.getArgumentExpression()?.text
            if (value != null) {
                val duration = value.toIntOrNull()
                if (duration != null && !isValidDuration(duration)) {
                    report(
                        CodeSmell(
                            issue = issue,
                            entity = Entity.from(argument),
                            message = "Animation duration $duration ms may not match design spec. " +
                                "Expected: 140ms (TV focus), 200ms (exit/screen), 340ms (enter reveal). " +
                                "See docs/SKY_DESIGN_SYSTEM.md#motion-interaction",
                        )
                    )
                }
            }
        }
    }

    private fun isValidDuration(duration: Int): Boolean {
        val validDurations = setOf(
            140,   // TV focus scale
            200,   // Exit fade, screen transition
            220,   // Tab switch
            340,   // Enter reveal (staggered)
            400,   // Spring animation
            500,   // Spring animation
            600,   // Spring animation
        )
        return duration in validDurations
    }
}
