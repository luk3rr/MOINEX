# Filename: test_scripts_isolated.py
# Created on: November 6, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

"""
The app runs each script from a temporary directory that holds only the script and yf_resilience.py. These
tests copy the scripts there and run them as a subprocess, so an import that only works from the repository
is caught. No network access is used.
"""

import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.dont_write_bytecode = True

SCRIPTS = Path(__file__).resolve().parents[2] / "main" / "resources" / "scripts"
ALL_SCRIPTS = [
    "get_stock_price.py",
    "get_stock_price_history.py",
    "get_fundamental_data.py",
    "get_market_indicator_history.py",
    "get_brazilian_market_indicators.py",
    "get_stock_logo.py",
]


class IsolatedScriptsTest(unittest.TestCase):
    def run_isolated(self, script, args, with_module=True):
        with tempfile.TemporaryDirectory() as directory:
            shutil.copy(SCRIPTS / script, directory)
            if with_module:
                shutil.copy(SCRIPTS / "yf_resilience.py", directory)
            return subprocess.run(
                [sys.executable, str(Path(directory) / script)] + args,
                cwd=tempfile.gettempdir(),
                capture_output=True,
                text=True,
                timeout=120,
            )

    def test_scripts_start_from_an_isolated_directory(self):
        usage = {
            "get_stock_price.py": [],
            "get_stock_price_history.py": [],
            "get_fundamental_data.py": ["--help"],
            "get_market_indicator_history.py": [],
            "get_brazilian_market_indicators.py": ["--help"],
            "get_stock_logo.py": [],
        }
        for script in ALL_SCRIPTS:
            with self.subTest(script=script):
                result = self.run_isolated(script, usage[script])
                self.assertNotIn("ModuleNotFoundError", result.stderr)
                self.assertNotIn("ImportError", result.stderr)

    def test_without_the_module_the_import_fails(self):
        result = self.run_isolated("get_stock_price.py", [], with_module=False)
        self.assertIn("yf_resilience", result.stderr)


if __name__ == "__main__":
    unittest.main()
