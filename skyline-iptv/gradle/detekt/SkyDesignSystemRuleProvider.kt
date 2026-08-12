package com.denham.skyline.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/**
 * Provides custom Detekt rules for Sky Design System compliance.
 */
class SkyDesignSystemRuleProvider : RuleSetProvider {
    override val ruleSetId = "sky-design-system"

    override fun instance(config: Config) = RuleSet(
        ruleSetId,
        listOf(
            SkyPaletteUsageRule(config),
            SpacingGridComplianceRule(config),
            TypographyComplianceRule(config),
            AnimationDurationComplianceRule(config),
            ComponentReuseRule(config),
        )
    )
}
