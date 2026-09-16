#!/usr/bin/env python3
"""Collects what shipped in a development-to-main release pull request, formats it into a
drafting prompt, and upserts the drafted release notes as a single PR comment.

Three subcommands, used in order by .github/workflows/release-notes.yml:
  collect       -- diff the OpenSpec archive between base and head, plus fallback merged PRs.
  format-prompt -- turn the collected JSON into the text prompt sent to GitHub Models.
  comment       -- upsert the drafted (or "nothing new") comment on the release PR.

Stdlib only, no third-party dependencies -- this runs on ubuntu-latest with no extra setup step.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys

COMMENT_MARKER = "<!-- release-notes-drafting -->"
ARCHIVE_DIR = "openspec/changes/archive/"


def run(args: list[str], input: str | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(
        args,
        check=True,
        text=True,
        capture_output=True,
        input=input,
        encoding="utf-8",
    )


def extract_section(text: str, heading: str) -> str:
    pattern = rf"^##\s+{re.escape(heading)}\s*$(.*?)(?=^##\s+|\Z)"
    match = re.search(pattern, text, re.MULTILINE | re.DOTALL)
    if not match:
        return ""
    section = match.group(1)
    section = re.sub(r"^\*\*GitHub Issue:\*\*.*$\n*", "", section, flags=re.MULTILINE)
    section = re.sub(r"^\*\*Pull Request:\*\*.*$\n*", "", section, flags=re.MULTILINE)
    return section.strip()


def extract_issue(text: str) -> dict | None:
    match = re.search(r"\*\*GitHub Issue:\*\*\s*#(\d+)\s*\(([^)]+)\)", text)
    if not match:
        return None
    return {"number": int(match.group(1)), "url": match.group(2)}


def newly_archived_proposal_paths(base: str, head: str) -> list[str]:
    diff = run(
        [
            "git",
            "diff",
            "--no-renames",
            "--diff-filter=A",
            "--name-only",
            base,
            head,
            "--",
            ARCHIVE_DIR,
        ]
    )
    return sorted(
        line
        for line in diff.stdout.splitlines()
        if line.strip().endswith("proposal.md")
    )


def gh_json(args: list[str]):
    return json.loads(run(["gh", *args]).stdout)


def fallback_prs(base: str, head: str, covered_issue_numbers: set[int]) -> list[dict]:
    merges = run(["git", "log", "--merges", "--pretty=%s", f"{base}..{head}"]).stdout
    prs = []
    seen = set()
    for subject in merges.splitlines():
        match = re.match(r"Merge pull request #(\d+)", subject)
        if not match:
            continue
        number = int(match.group(1))
        if number in seen:
            continue
        seen.add(number)

        try:
            details = gh_json(["pr", "view", str(number), "--json", "title,body"])
        except subprocess.CalledProcessError:
            prs.append({"number": number, "title": subject})
            continue

        closes = re.search(r"Closes #(\d+)", details.get("body") or "")
        if closes and int(closes.group(1)) in covered_issue_numbers:
            continue
        prs.append({"number": number, "title": details.get("title") or subject})
    return prs


def cmd_collect(args: argparse.Namespace) -> None:
    changes = []
    covered_issue_numbers: set[int] = set()
    for path in newly_archived_proposal_paths(args.base, args.head):
        content = run(["git", "show", f"{args.head}:{path}"]).stdout
        issue = extract_issue(content)
        if issue:
            covered_issue_numbers.add(issue["number"])
        changes.append(
            {
                "path": path,
                "why": extract_section(content, "Why"),
                "what_changes": extract_section(content, "What Changes"),
                "issue": issue,
            }
        )

    result = {
        "changes": changes,
        "fallback_prs": fallback_prs(args.base, args.head, covered_issue_numbers),
    }
    result["has_content"] = bool(result["changes"] or result["fallback_prs"])

    output = json.dumps(result, indent=2)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(output)
    else:
        print(output)

    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output:
        with open(github_output, "a", encoding="utf-8") as f:
            f.write(f"has_content={'true' if result['has_content'] else 'false'}\n")


def cmd_format_prompt(args: argparse.Namespace) -> None:
    with open(args.context, encoding="utf-8") as f:
        context = json.load(f)

    with open(args.style_en, encoding="utf-8") as f:
        style_en = f.read().strip()
    with open(args.style_nl, encoding="utf-8") as f:
        style_nl = f.read().strip()

    lines = [
        "Style reference -- the current Play Store \"what's new\" text (English):",
        style_en,
        "",
        "Style reference -- the current Play Store \"what's new\" text (Dutch):",
        style_nl,
        "",
        "Changes shipped in this release:",
    ]

    if context["changes"]:
        for change in context["changes"]:
            issue = change.get("issue")
            issue_note = f" (issue #{issue['number']})" if issue else ""
            lines.append(f"\n- Why: {change['why']}")
            lines.append(f"  What changed{issue_note}: {change['what_changes']}")
    else:
        lines.append("(none)")

    if context["fallback_prs"]:
        lines.append("\nAdditional merged changes with no OpenSpec change behind them:")
        for pr in context["fallback_prs"]:
            lines.append(f"- {pr['title']} (PR #{pr['number']})")

    lines.append(
        "\nWrite a short, human, non-technical \"what's new\" draft for this release, "
        "in the same tone, length and bullet style as the style reference above. "
        "Do not invent features that are not described above."
    )

    prompt = "\n".join(lines)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(prompt)
    else:
        print(prompt)


def find_marker_comment_id(pr_number: str) -> int | None:
    comments = gh_json(["api", f"repos/:owner/:repo/issues/{pr_number}/comments"])
    for comment in comments:
        if COMMENT_MARKER in (comment.get("body") or ""):
            return comment["id"]
    return None


def build_comment_body(nothing_new: bool, english: str, dutch: str) -> str:
    if nothing_new:
        body = "Nothing new to summarize for this release.\n"
    else:
        body = (
            "## What's new (English)\n\n"
            f"{english.strip()}\n\n"
            "## Wat is er nieuw (Nederlands)\n\n"
            f"{dutch.strip()}\n\n"
            "_Draft only -- copy the text you want into `distribution/whatsnew/whatsnew-en-US` "
            "and `distribution/whatsnew/whatsnew-nl-NL` before merging._\n"
        )
    return f"{body}\n{COMMENT_MARKER}\n"


def cmd_comment(args: argparse.Namespace) -> None:
    english = ""
    dutch = ""
    if not args.nothing_new:
        with open(args.english_file, encoding="utf-8") as f:
            english = f.read()
        with open(args.dutch_file, encoding="utf-8") as f:
            dutch = f.read()

    body = build_comment_body(args.nothing_new, english, dutch)
    existing_id = find_marker_comment_id(args.pr)
    if existing_id:
        run(
            [
                "gh",
                "api",
                f"repos/:owner/:repo/issues/comments/{existing_id}",
                "-X",
                "PATCH",
                "--input",
                "-",
            ],
            input=json.dumps({"body": body}),
        )
    else:
        run(["gh", "pr", "comment", args.pr, "--body-file", "-"], input=body)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    collect_parser = subparsers.add_parser("collect")
    collect_parser.add_argument("--base", required=True)
    collect_parser.add_argument("--head", required=True)
    collect_parser.add_argument("--out")
    collect_parser.set_defaults(func=cmd_collect)

    format_parser = subparsers.add_parser("format-prompt")
    format_parser.add_argument("--context", required=True)
    format_parser.add_argument("--style-en", required=True)
    format_parser.add_argument("--style-nl", required=True)
    format_parser.add_argument("--out")
    format_parser.set_defaults(func=cmd_format_prompt)

    comment_parser = subparsers.add_parser("comment")
    comment_parser.add_argument("--pr", required=True)
    comment_parser.add_argument("--nothing-new", action="store_true")
    comment_parser.add_argument("--english-file")
    comment_parser.add_argument("--dutch-file")
    comment_parser.set_defaults(func=cmd_comment)

    args = parser.parse_args()
    if args.command == "comment" and not args.nothing_new and not (
        args.english_file and args.dutch_file
    ):
        parser.error("comment requires --nothing-new or both --english-file and --dutch-file")

    try:
        args.func(args)
    except subprocess.CalledProcessError as error:
        print(error.stderr or error.stdout, file=sys.stderr)
        sys.exit(error.returncode)


if __name__ == "__main__":
    main()
