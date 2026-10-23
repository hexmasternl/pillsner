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

# Every language the app supports, in comment order: (code, Play locale, comment heading).
# The Play locale names the distribution/whatsnew/whatsnew-<locale> file release.yml uploads.
LANGUAGES = [
    ("en", "en-US", "What's new (English)"),
    ("nl", "nl-NL", "Wat is er nieuw (Nederlands)"),
    ("de", "de-DE", "Was ist neu (Deutsch)"),
    ("fr", "fr-FR", "Quoi de neuf (Français)"),
    ("es", "es-ES", "Novedades (Español)"),
    ("pt", "pt-PT", "Novidades (Português)"),
]


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


def whatsnew_file_name(play_locale: str) -> str:
    return f"whatsnew-{play_locale}"


def read_styles(whatsnew_dir: str) -> dict[str, str]:
    """The current whatsnew text per Play locale, for every app language that has a file."""
    styles = {}
    for _, play_locale, _ in LANGUAGES:
        path = os.path.join(whatsnew_dir, whatsnew_file_name(play_locale))
        if os.path.isfile(path):
            with open(path, encoding="utf-8") as f:
                styles[play_locale] = f.read().strip()
    return styles


def build_prompt(context: dict, styles: dict[str, str]) -> str:
    lines = []
    for play_locale, style in styles.items():
        lines.append(f"Style reference -- the current Play Store \"what's new\" text ({play_locale}):")
        lines.append(style)
        lines.append("")
    lines.append("Changes shipped in this release:")

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
    return "\n".join(lines)


def cmd_format_prompt(args: argparse.Namespace) -> None:
    with open(args.context, encoding="utf-8") as f:
        context = json.load(f)

    prompt = build_prompt(context, read_styles(args.whatsnew_dir))
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(prompt)
    else:
        print(prompt)


def find_marker_comment_id(pr_number: str) -> int | None:
    comments = [
        comment
        for page in gh_json(["api", f"repos/:owner/:repo/issues/{pr_number}/comments", "--paginate", "--slurp"])
        for comment in page
    ]
    for comment in comments:
        if COMMENT_MARKER in (comment.get("body") or ""):
            return comment["id"]
    return None


def build_comment_body(nothing_new: bool, drafts: dict[str, str]) -> str:
    """One section per app language, in LANGUAGES order; drafts are keyed by language code."""
    if nothing_new:
        body = "Nothing new to summarize for this release.\n"
    else:
        sections = []
        for code, play_locale, heading in LANGUAGES:
            target = f"distribution/whatsnew/{whatsnew_file_name(play_locale)}"
            sections.append(f"## {heading}\n\n_For `{target}`_\n\n{drafts.get(code, '').strip()}\n\n")
        body = "".join(sections) + (
            "_Draft only -- copy the text you want into each language's file under "
            "`distribution/whatsnew/` before merging. Play allows 500 characters per file._\n"
        )
    return f"{body}\n{COMMENT_MARKER}\n"


def parse_draft_args(values: list[str]) -> dict[str, str]:
    """Maps each `<code>=<file>` argument to its language code."""
    files = {}
    for value in values:
        code, separator, path = value.partition("=")
        if not separator or not path:
            raise ValueError(f"--draft expects <code>=<file>, got {value!r}")
        files[code] = path
    return files


def cmd_comment(args: argparse.Namespace) -> None:
    drafts = {}
    if not args.nothing_new:
        for code, path in parse_draft_args(args.draft).items():
            with open(path, encoding="utf-8") as f:
                drafts[code] = f.read()

    body = build_comment_body(args.nothing_new, drafts)
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
    format_parser.add_argument("--whatsnew-dir", required=True)
    format_parser.add_argument("--out")
    format_parser.set_defaults(func=cmd_format_prompt)

    comment_parser = subparsers.add_parser("comment")
    comment_parser.add_argument("--pr", required=True)
    comment_parser.add_argument("--nothing-new", action="store_true")
    comment_parser.add_argument("--draft", action="append", default=[], metavar="CODE=FILE")
    comment_parser.set_defaults(func=cmd_comment)

    args = parser.parse_args()
    if args.command == "comment" and not args.nothing_new:
        expected = {code for code, _, _ in LANGUAGES}
        try:
            given = set(parse_draft_args(args.draft))
        except ValueError as error:
            parser.error(str(error))
        if given != expected:
            parser.error(
                "comment requires --nothing-new or one --draft <code>=<file> for each of: "
                + ", ".join(code for code, _, _ in LANGUAGES)
            )

    try:
        args.func(args)
    except subprocess.CalledProcessError as error:
        print(error.stderr or error.stdout, file=sys.stderr)
        sys.exit(error.returncode)


if __name__ == "__main__":
    main()
