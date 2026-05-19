# AI Collaboration Workspace

This directory stores handoff documents for the local CC and Codex workflow.

Recommended flow:

1. Codex records marked issues in `.ai/issues/current.md`.
2. Claude Code reads `.ai/issues/current.md`, implements fixes, and writes `.ai/reports/current.md`.
3. Codex reviews `.ai/issues/current.md`, `.ai/reports/current.md`, and the current code diff, then writes optional review notes to `.ai/reviews/codex-review.md`.

Reusable prompts are stored in `.ai/prompts/`.

Runtime handoff files such as `current.md` may be overwritten during each task. Archive important records before starting a new task.
