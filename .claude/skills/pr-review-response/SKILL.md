---
name: pr-review-response
description: Work through the review comments on a Pillsner pull request - Copilot's automatic review and human reviewers' - by fixing what's right, replying with a reason to what isn't, resolving Copilot threads once handled, and leaving human threads for their author. Use while monitoring a feature pull request (git-workflow lifecycle step 6), when a PR has unresolved review threads, when asked to "address the review" or "handle Copilot's comments", and when Copilot keeps repeating the same wrong kind of comment.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

The `protect-development` ruleset requests a Copilot code review on every push to a pull request into
`development`. Its comments are guided by `.github/copilot-instructions.md`, but it doesn't know the
specs, so some comments are right and some aren't. Each one gets an answer: a fix, or a reason.

Repository: `hexmasternl/pillsner`. Work in the change's worktree.

## 1. Collect the open threads

```bash
gh api graphql -F owner=hexmasternl -F repo=pillsner -F number=<pr> -f query='
query($owner:String!,$repo:String!,$number:Int!){
  repository(owner:$owner,name:$repo){ pullRequest(number:$number){
    reviewThreads(first:100){ nodes{
      id isResolved isOutdated path line
      comments(first:20){ nodes{ databaseId author{login} body url } }
    }}
  }}
}' --jq '.data.repository.pullRequest.reviewThreads.nodes[] | select(.isResolved|not)'
```

Also read top-level review bodies: `gh pr view <pr> --json reviews`.

## 2. Decide each thread

Read the comment, the code it points at (at the current head, not the commented commit — an
`isOutdated` thread may already be fixed), and the spec or design it concerns.

| Verdict | When | Action |
| --- | --- | --- |
| **Fix** | It's a real bug, a missing test, a privacy or accessibility problem, a design-system violation | Fix it, with a test where it's a bug |
| **Decline** | It contradicts a spec, the design, `CLAUDE.md`, or `.github/copilot-instructions.md`'s "Do not report" list | Reply with the reason, pointing at the rule |
| **Ask** | It's a product decision, or a real trade-off the design didn't settle | Leave it open and ask the user |

Group all fixes into as few commits as makes sense (`Address review: <what>`), run the checks the
fix needs (`verify-change` scope for the touched files), and push. Never force-push.

## 3. Reply and resolve

Reply on the thread (`databaseId` of its first comment):

```bash
gh api repos/hexmasternl/pillsner/pulls/<pr>/comments/<databaseId>/replies -f body='<reply>'
```

- **Fixed**: "Fixed in <short sha>: <one line on what changed>."
- **Declined**: one or two sentences with the reason and the rule — "The `reminder-scheduling` spec
  requires a skipped dose to stay skipped; recording it as missed would contradict it." Plain and
  polite; no arguing.

Then, **for Copilot threads only**, resolve:

```bash
gh api graphql -F id=<threadId> -f query='mutation($id:ID!){ resolveReviewThread(input:{threadId:$id}){ thread{ isResolved } } }'
```

**Human reviewers' threads** get the reply but stay open: the reviewer resolves their own thread when
satisfied.

## 4. Teach Copilot when it repeats itself

When Copilot raises the same wrong kind of comment on two or more pull requests (check recent PRs
with `gh pr list --state merged --limit 10` and their threads), propose a line for
`.github/copilot-instructions.md`'s "Do not report" list. Code review reads only about the first
4,000 characters of that file (it's about 1,300 now), so keep additions to a line each. Changing it
is a change of its own on a feature branch, not part of the feature being reviewed.

## Guardrails

- Never resolve a thread without a reply that says what was done.
- Never resolve a human reviewer's thread, dismiss a review, or re-request review unasked.
- Never merge the pull request, even when every thread is resolved.
- Never decline a comment about data loss, a missing migration, privacy or accessibility without
  asking the user first — those are the categories `copilot-instructions.md` asks Copilot to report.
