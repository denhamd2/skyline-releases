package com.denham.skyline.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

/**
 * Detects potential duplicate component implementations.
 * Flags @Composable functions that look like they might duplicate existing components.
 * Examples: multiple Button wrappers, multiple Card styles, multiple LazyRow patterns.
 * See docs/COMPONENT_LIBRARY.md for approved component library.
 */
class ComponentReuseRule(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        id = "ComponentReuse",
        severity = Severity.INFO,
        description = "Consider reusing existing components from the component library",
        debt = Debt.TEN_MINS,
    )

    // Composable function names that might be duplicates of built-in components
    private val suspiciousNames = listOf(
        ".*Button.*",        // Duplicate button styles
        ".*Card.*",          // Duplicate card styles
        ".*Chip.*",          // Duplicate chip styles
        ".*Rail.*",          // Duplicate rail/carousel
        ".*Row.*",           // Duplicate LazyRow wrapper
        ".*Column.*",        // Duplicate LazyColumn wrapper
        ".*Grid.*",          // Duplicate grid layout
        ".*Hero.*",          // Duplicate hero section
        ".*Divider.*",       // Duplicate divider
        ".*Text.*",          // Duplicate text wrapper
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    override fun visitFunction(function: KtFunction) {
        super.visitFunction(function)

        // Check if function has @Composable annotation
        val isComposable = function.annotationEntries.any { annotation ->
            annotation.typeReference?.text?.contains("Composable") == true
        }

        if (isComposable) {
            val functionName = function.name ?: return
            val isFileInComponentsDir = function.containingFile.virtualFile?.path?.contains("/components/") == true
            val isFileInThemeDir = function.containingFile.virtualFile?.path?.contains("/theme/") == true

            // Skip approved component library files
            if (isFileInComponentsDir || isFileInThemeDir) {
                return
            }

            // Check if function name matches suspicious patterns
            for (pattern in suspiciousNames) {
                if (pattern.matches(functionName)) {
                    report(
                        CodeSmell(
                            issue = issue,
                            entity = Entity.from(function),
                            message = "@Composable '$functionName' looks like it might duplicate an existing component. " +
                                "Check docs/COMPONENT_LIBRARY.md to see if there's an approved component you can reuse instead.",
                        )
                    )
                    break
                }
            }
        }
    }
}
