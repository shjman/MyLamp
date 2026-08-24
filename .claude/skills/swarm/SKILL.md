---
name: swarm
description: Runs an agent chain (researcher → executor → reviewer) for complex tasks. Use when a task requires changes to >50 lines of code or touches multiple files/modules.
argument-hint: "<task description>"
allowed-tools: ["Bash", "Agent", "Read", "Write"]
---

# Swarm

An agent chain for complex tasks: **researcher** (analysis + plan) → **[APPROVAL GATE]** → **executor** (implementation) → **reviewer** (check).

Runs only on an explicit request.

## 🚫 HARD RULE — no git mutations without explicit permission

**Neither the orchestrating process, nor researcher, nor executor, nor reviewer ever run
`git commit`, `git push`, `git branch`, `git checkout -b`, `git merge`, `git rebase`, `git reset`, or
any other mutating git command — not once, not at any step, even "to record an
intermediate result".** The user, and only the user, owns the git history. Only read-only
commands are allowed (`git status`, `git diff`, `git log`, `git show`, `git branch -a`) — for context, not for
changes.

This also applies to subagents: if executor (or any other agent in this chain) reports in its
response that it committed or pushed something — that's a violation, not normal behavior, even if
the changes were correct. Immediately inform the user about it, don't carry on as if nothing
happened.

Changes remain uncommitted in the working tree once the whole chain finishes — the user decides
whether to commit them (and with what message).

## 🚫 HARD RULE — executor only runs after the user's explicit APPROVE

**After any research/re-plan (Step 2) and before running executor (Step 4) — always pause at Step 3. No
exceptions.** Never skip this step — not when the plan looks simple/one-liner, not when
`Questions for User` is empty, not when a previous iteration was already approved (a new/revised plan
after `BLOCKED`/`FAIL` requires a **new** APPROVE, an earlier consent to a different version of the plan
does not carry over).

**Explicit APPROVE is the user's decision, not my assessment of the plan.** Even if the plan looks
completely correct, contains no questions, and seems trivial — that's not a reason to consider it approved
on my own. Approve must come as a separate, unambiguous message from the user specifically about this plan
("yes", "ok", "approve", "go", "do it" or an explicit synonym) — don't infer consent from the general tone
of the conversation, from an earlier permission for a different task, or from the user not objecting to
something else in the same message.

This rule applies on **every** iteration of the loop, including repeat passes after
`status: BLOCKED` (executor) or `status: FAIL, next_step: researcher` (reviewer) — a new `plan.md`
always goes through Step 3 again, even if a previous version was already approved.

---

## Step 1 — Prepare the context

```bash
mkdir -p .claude/context
```

Create `.claude/context/task.md`:

```
# Task

$ARGUMENTS

---
```

Clean up the starting context files (delete if they exist):
- `.claude/context/plan.md`
- `.claude/context/execution-report.md`
- `.claude/context/review-result.md`

Initialize the iteration counter: `iteration = 0`, `max_iterations = 3`.

---

## Step 2 — Research (run researcher)

Run the `researcher` agent from `.claude/agents/researcher.md`.

Pass: the path to `task.md`, a brief instruction "Analyze the task and draft a plan".

After it finishes, read `.claude/context/plan.md`.

### If `next_step: clarification`

1. Read the `## Questions for User` section from `plan.md`
2. **Show the questions to the user verbatim**
3. **Wait for the answers** (don't continue without them)
4. Add the answers to `task.md` under the `## Answers` heading
5. Go back to the start of Step 2

### If `next_step: plan`

→ Move to Step 3

---

## Step 3 — APPROVAL GATE (mandatory pause, no exceptions — see HARD RULE above)

1. Read `plan.md`
2. **Show the user:**
   - The `## Approach` section
   - The `## Steps` section
   - The `## Files to Create` and `## Files to Change` sections
3. **Wait for the user's explicit APPROVE** (the words "yes", "ok", "approve", "go", "do it" or an
   explicit synonym — specifically for this plan, not implied consent)
4. Without an explicit APPROVE — do not move to execution. The plan's simplicity, the lack of
   questions from researcher, or "it looks obvious" are not grounds to skip this step on your own.

→ After APPROVE, move to Step 4

---

## Step 4 — Execution (run executor)

Increment the counter: `iteration += 1`.

If `iteration > max_iterations`:
1. Read `execution-report.md` and `review-result.md`
2. **Show the user a summary:** what was tried, how many iterations, what failed at each one
3. **Wait for a decision** — don't call executor without explicit permission

Otherwise: run the `executor` agent from `.claude/agents/executor.md`.

Pass: the paths to `plan.md` and `task.md`.

Executor compiles the code and runs ktlint/detekt (the baseline grandfathers old code — only
new violations), fixes trivial ones itself, does not attempt to fix architectural ones — see
`execution-report.md → static_analysis_findings`.

After it finishes, read `.claude/context/execution-report.md`.

### If `status: BLOCKED`

1. **Show the user** the `## blockers` section from `execution-report.md`
2. Append to `task.md`:

```
## Previous Attempts

### Iteration N
- Approach: [from plan.md → Approach]
- Failed at: Execution
- Root cause: [from execution-report.md → blockers]

## Already Done

### Iteration N
- completed_steps: [from execution-report.md]
- files_changed: [from execution-report.md]
```

3. Go back to Step 2 (researcher re-plans)

### If `status: DONE`

→ Move to Step 5

---

## Step 5 — Validation (run reviewer)

Run the `reviewer` agent from `.claude/agents/reviewer.md`.

Pass: the paths to `plan.md` and `execution-report.md`.

Reviewer also runs `:app:detekt`/`:app:ktlintCheck` and cross-checks against
executor's `static_analysis_findings`.

After it finishes, read `.claude/context/review-result.md`.

### If `status: PASS`

→ Move to Step 6

### If `status: FAIL` + `next_step: executor`

Append to `task.md`:

```
## Previous Attempts

### Iteration N
- Failed at: Validation
- Issues: [from review-result.md → issues, brief]
```

→ Go back to Step 4

### If `status: FAIL` + `next_step: researcher`

Append to `task.md`:

```
## Previous Attempts

### Iteration N
- Approach: [from plan.md → Approach]
- Failed at: Validation
- Root cause: [from review-result.md → reason, in full, unabridged]
- Issues: [from review-result.md → issues, in full, unabridged]

## Already Done

### Iteration N
- completed_steps: [from execution-report.md]
- files_changed: [from execution-report.md]
```

→ Go back to Step 2

---

## Step 6 — Final

Run MemPalace (only on PASS):

```bash
/opt/homebrew/bin/python3.11 -m mempalace mine .claude/context --wing SmartHome
```

Show the user the final report:
- Which files were changed
- How many iterations it took
- Key deviations from the plan (if any)

---

## A reminder on when to use this

The chain is a good fit when:
- The change is likely >50 lines of code
- Several files or modules are affected
- An architectural decision is required

For simple local fixes (fix a bug, rename something, add a field) — the chain is overkill, work directly.
