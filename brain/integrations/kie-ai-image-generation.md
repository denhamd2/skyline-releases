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
   Poll with a short backoff, reading `data.state` on each response. Values:
   `waiting` | `queuing` | `generating` | `success` | `fail`. Keep polling
   through the first three; stop on `success` or `fail`.

3. **On `success`**: the image URL(s) are *not* a top-level field — parse
   `data.resultJson`, which is itself a **JSON string** (parse it a second
   time), to get `{ "resultUrls": ["https://..."] }`. Download the first
   URL immediately with `curl -o` into `design/<feature>/mockups/` —
   **these URLs expire (~24h)**, so never store the raw URL in a design
   brief as if it were durable; only the downloaded file path.

4. **On `fail`**: read `data.failCode` / `data.failMsg` and surface them
   plainly in the brief/handoff rather than silently skipping the mockup.

`callBackUrl` is an optional field on `createTask` for a webhook-based flow
— not used here, since we poll `recordInfo` directly instead.

Field names above (`state`, `resultJson`, `resultUrls`, `failCode`,
`failMsg`) are confirmed via kie.ai's own docs and third-party integration
notes; `docs.kie.ai` itself may be unreachable from a sandboxed session
(egress-blocked), in which case treat this doc as ground truth rather than
re-fetching.

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
