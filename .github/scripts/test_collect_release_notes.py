"""Tests for collect_release_notes.py and the Play "what's new" files it relies on.

Run from the repository root: python3 -m unittest discover -s .github/scripts
"""

import tempfile
import unittest
from pathlib import Path

import collect_release_notes as notes

REPO_ROOT = Path(__file__).resolve().parents[2]
WHATSNEW_DIR = REPO_ROOT / "distribution" / "whatsnew"
PLAY_LIMIT = 500

# The Play locale of every language the app supports. Listing languages the app does not
# support get no file.
APP_PLAY_LOCALES = ["en-US", "nl-NL", "de-DE", "fr-FR", "es-ES", "pt-PT"]


class WhatsNewFilesTest(unittest.TestCase):
    def test_every_app_language_has_release_notes(self):
        missing = [
            locale for locale in APP_PLAY_LOCALES if not (WHATSNEW_DIR / f"whatsnew-{locale}").is_file()
        ]
        self.assertEqual(missing, [], f"no whatsnew file for: {', '.join(missing)}")

    def test_release_notes_are_not_empty(self):
        for locale in APP_PLAY_LOCALES:
            path = WHATSNEW_DIR / f"whatsnew-{locale}"
            if path.is_file():
                with self.subTest(locale=locale):
                    self.assertTrue(path.read_text(encoding="utf-8").strip(), f"{path.name} is empty")

    def test_release_notes_fit_play_limit(self):
        for locale in APP_PLAY_LOCALES:
            path = WHATSNEW_DIR / f"whatsnew-{locale}"
            if path.is_file():
                length = len(path.read_text(encoding="utf-8").strip())
                with self.subTest(locale=locale):
                    self.assertLessEqual(
                        length, PLAY_LIMIT, f"{path.name} has {length} characters, Play allows {PLAY_LIMIT}"
                    )

    def test_script_drafts_every_app_language(self):
        self.assertEqual([play_locale for _, play_locale, _ in notes.LANGUAGES], APP_PLAY_LOCALES)


class CommentBodyTest(unittest.TestCase):
    def test_sections_follow_language_order_and_name_their_file(self):
        drafts = {code: f"draft {code}" for code, _, _ in notes.LANGUAGES}
        body = notes.build_comment_body(False, drafts)

        positions = [body.index(f"## {heading}") for _, _, heading in notes.LANGUAGES]
        self.assertEqual(positions, sorted(positions))
        for code, play_locale, _ in notes.LANGUAGES:
            self.assertIn(f"`distribution/whatsnew/whatsnew-{play_locale}`", body)
            self.assertIn(f"draft {code}", body)
        self.assertTrue(body.rstrip().endswith(notes.COMMENT_MARKER))

    def test_nothing_new_body_is_unchanged(self):
        body = notes.build_comment_body(True, {})
        self.assertEqual(body, f"Nothing new to summarize for this release.\n\n{notes.COMMENT_MARKER}\n")

    def test_draft_arguments_map_codes_to_files(self):
        self.assertEqual(notes.parse_draft_args(["de=a.txt", "pt=b=c.txt"]), {"de": "a.txt", "pt": "b=c.txt"})
        with self.assertRaises(ValueError):
            notes.parse_draft_args(["de"])


class PromptTest(unittest.TestCase):
    def test_every_whatsnew_file_is_a_style_reference(self):
        with tempfile.TemporaryDirectory() as whatsnew_dir:
            for _, play_locale, _ in notes.LANGUAGES:
                Path(whatsnew_dir, f"whatsnew-{play_locale}").write_text(f"style {play_locale}\n", encoding="utf-8")
            styles = notes.read_styles(whatsnew_dir)

        prompt = notes.build_prompt({"changes": [], "fallback_prs": []}, styles)
        for _, play_locale, _ in notes.LANGUAGES:
            self.assertIn(f"style {play_locale}", prompt)

    def test_shipped_changes_are_in_the_prompt(self):
        context = {
            "changes": [{"why": "a reason", "what_changes": "a change", "issue": {"number": 7}}],
            "fallback_prs": [{"title": "a fix", "number": 8}],
        }
        prompt = notes.build_prompt(context, {"en-US": "style"})
        self.assertIn("a reason", prompt)
        self.assertIn("(issue #7)", prompt)
        self.assertIn("a fix (PR #8)", prompt)


if __name__ == "__main__":
    unittest.main()
