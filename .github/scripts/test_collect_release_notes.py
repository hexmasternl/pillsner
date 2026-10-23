"""Tests for collect_release_notes.py and the Play "what's new" files it relies on.

Run from the repository root: python3 -m unittest discover -s .github/scripts
"""

import unittest
from pathlib import Path

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


if __name__ == "__main__":
    unittest.main()
