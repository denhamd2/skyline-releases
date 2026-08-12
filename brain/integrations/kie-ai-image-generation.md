# kie.ai — GPT Image 2 mockup generation

How `ux-design` generates visual mockups for substantially new features via
kie.ai's GPT Image 2 API.

## Auth

Bearer token in an `Authorization: Bearer <key>` header. The key comes from
the `KIE_AI_API_KEY` environment variable — **never hardcode it, never write
it into a design brief, prompt, curl command echoed to output, or any
committed file.** If the variable isn't set, say so explicitly and skip
image generation rather than asking the user to paste a key into chat.

## Workflow (async, task-based)

1. **Create the task**:
   ```
   POST https://api.kie.ai/api/v1/jobs/createTask
   Authorization: Bearer $KIE_AI_API_KEY
   Content-Type: application/json

   {
     "model": "gpt-image-2-text-to-image",
     "input": {
       "prompt": "<description grounded in SkyPalette tokens, spacing, and the screen's purpose>",
       "aspect_ratio": "<e.g. 9:16 for phone, 16:9 for TV/desktop>"
     }
   }
   ```
   Returns a task id.

2. **Poll for completion**:
   ```
   GET https://api.kie.ai/api/v1/jobs/recordInfo?taskId=<id>
   Authorization: Bearer $KIE_AI_API_KEY
   ```
   Poll with a short backoff until the task status is complete; the response
   carries the resulting image URL(s).

3. **Download** the image URL with `curl -o` into
   `design/<feature>/mockups/`.

**Confirm the exact field names against the live docs before relying on
this** — `https://docs.kie.ai/market/gpt/gpt-image-2-text-to-image` (and
`.../gpt-image-2-image-to-image` for image-to-image) — this summary was
assembled from search snippets, not a fetched page, so treat it as a
starting point, not ground truth.

## Prompting

Ground every prompt in what's already known, so output actually looks like
Skyline rather than generic Material:

- Reference `SkyPalette` colours by description (near-black canvas, navy
  cards, action-blue accents, red LIVE badge) since the model won't resolve
  Kotlin token names.
- State phone vs. TV explicitly — 10-foot D-pad layouts need different
  framing than one-handed phone screens.
- Reference the relevant baseline in `brain/reference-designs/` or
  `brain/component-screenshots/` by description if one exists, and note any
  known divergence (nav/section changes since the baseline was made).

## Scope

Only for a **substantially new feature** — a new screen or a materially new
flow — not for reviews, restyles, or incremental changes to something that
already exists. Don't spend API calls (kie.ai bills per generation) on
routine work.
