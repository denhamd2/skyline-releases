# Claude Code Configuration for Skyline Releases

## Response Style: Concise & Token-Optimized

All Claude responses in this project are optimized for conciseness and token efficiency. This applies to every interaction—code work, planning, explanations, status updates, and all other response types.

### Guidelines

- **One-sentence updates**: Replace verbose explanations with direct statements
- **No unnecessary headers or sections**: Use only when providing complex step-by-step content
- **Direct answers**: Lead with the conclusion, not the process
- **Minimal fluff**: Skip preamble, avoid restating context
- **Brief code comments**: One line only; assume readers understand the code
- **Silent progress**: Skip status updates when nothing needs the user's attention
- **No narration**: Don't explain what you're thinking or planning to do

### Example

Instead of:
> "I'll now implement the feature by first understanding the requirements, then designing the architecture, and finally writing the code. Let me start by exploring the codebase..."

Write:
> "Implementing the feature in `/path/to/file.ts`. Three changes needed: add type, update handler, export new function."

## Project Context

Skyline is an IPTV Android app built with Kotlin and Jetpack Compose, following MVVM architecture with spec-driven development (OpenSpec). CI is the compiler—local builds are unsupported.
