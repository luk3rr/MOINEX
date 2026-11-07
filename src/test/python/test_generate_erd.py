# Filename: test_generate_erd.py
# Created on: November 7, 2026
# Author: Lucas Araújo <araujolucas@dcc.ufmg.br>

"""
Tests for scripts/dev/generate_erd.py, which draws the database in Mermaid from the Flyway migrations.
No network access is used.
"""

import importlib.util
import os
import re
import sqlite3
import unittest

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
SCRIPT = os.path.join(ROOT, "scripts", "dev", "generate_erd.py")

spec = importlib.util.spec_from_file_location("generate_erd", SCRIPT)
generate_erd = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generate_erd)


def small_schema():
    database = sqlite3.connect(":memory:")
    database.executescript(
        """
        CREATE TABLE owner (id INTEGER PRIMARY KEY, name VARCHAR(50) NOT NULL);
        CREATE TABLE pet (
            id INTEGER PRIMARY KEY,
            owner_id INTEGER NOT NULL REFERENCES owner (id),
            vet_id INTEGER REFERENCES owner (id),
            passport_id INTEGER UNIQUE REFERENCES owner (id)
        );
        CREATE TABLE loose (id INTEGER PRIMARY KEY);
        """
    )
    return generate_erd.read_schema(database)


class RelationshipTest(unittest.TestCase):
    def test_required_foreign_key_is_one_to_many(self):
        schema = small_schema()
        fk = next(f for f in schema["pet"]["foreign_keys"] if f["column"] == "owner_id")
        self.assertEqual(generate_erd.relationship("pet", fk, schema), '  OWNER ||--o{ PET : "owner_id"')

    def test_nullable_foreign_key_makes_the_parent_optional(self):
        schema = small_schema()
        fk = next(f for f in schema["pet"]["foreign_keys"] if f["column"] == "vet_id")
        self.assertEqual(generate_erd.relationship("pet", fk, schema), '  OWNER |o--o{ PET : "vet_id"')

    def test_unique_foreign_key_is_one_to_one(self):
        schema = small_schema()
        fk = next(f for f in schema["pet"]["foreign_keys"] if f["column"] == "passport_id")
        self.assertEqual(generate_erd.relationship("pet", fk, schema), '  OWNER |o--o| PET : "passport_id"')


class EntityTest(unittest.TestCase):
    def test_entity_marks_keys_and_drops_the_length_of_the_type(self):
        block = generate_erd.entity_block("owner", small_schema())
        self.assertIn("integer id PK", block)
        self.assertIn("varchar name", block)
        self.assertNotIn("(", block)

    def test_foreign_key_column_is_marked(self):
        block = generate_erd.entity_block("pet", small_schema())
        self.assertIn("integer owner_id FK", block)


class RealSchemaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = generate_erd.read_schema(generate_erd.migrated_database())
        cls.document = generate_erd.render(cls.schema)

    def test_every_table_is_drawn_in_a_domain_diagram(self):
        for table in self.schema:
            self.assertIn(f"  {table.upper()} {{", self.document, table)

    def test_diagrams_are_balanced_mermaid_blocks(self):
        self.assertEqual(self.document.count("```mermaid"), self.document.count("```") // 2)

    def test_attribute_types_are_single_words(self):
        for line in re.findall(r"^    (\S.*)$", self.document, flags=re.MULTILINE):
            self.assertRegex(line, r"^[a-z_]+ [A-Za-z0-9_]+( PK| FK| PK,FK)?$")

    def test_chat_history_tables_are_in_the_chat_domain(self):
        section = self.document.split("## Conversas com o assistente de IA")[1]
        for table in ("AI_CHAT", "AI_CHAT_MESSAGE", "AI_CHAT_CHANGE_SET", "AI_CHAT_CHANGE"):
            self.assertIn(f"  {table} {{", section)


if __name__ == "__main__":
    unittest.main()
