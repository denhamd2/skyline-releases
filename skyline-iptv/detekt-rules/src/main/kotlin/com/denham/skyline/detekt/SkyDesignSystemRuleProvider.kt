package com.denham.skyline.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/**
 * Registers the Sky design-system rules with detekt.
 *
 * Currently only [SkyPaletteUsageRule]. The other four rules that once sat
 * beside it (spacing, typography, animation duration, component reuse) were
 * written against a detekt API that does not exist — inventing visitor
 * methods such as `visitNamedArgument` and PSI types such as
 * `KtNamedArgument` — and had never been compiled, because nothing
 * referenced them. They were removed rather than kept as dead files, and are
 * recoverable from git history when reimplemented against the real API.
 */
class SkyDesignSystemRuleProvider : RuleSetProvider {

    override val ruleSetId = "sky-design-system"

    override fun instance(config: Config) = RuleSet(
        ruleSetId,
        listOf(
            SkyPaletteUsageRule(config),
        )
    )
}
