import sqlite3
import sys
import os
import argparse

# =============================================================================
# SQLite Data Migration Script
#
# Author: Lucas Araújo
# Date: July 18, 2025
#
# Description:
# This script performs a robust data migration from one SQLite database (source)
# to another (destination). It is designed to be safe and interactive,
# ensuring data integrity throughout the process.
#
# Usage:
#   python3 transfer_data.py /path/to/old_database.db /path/to/new_database.db
#
# =============================================================================

def get_column_names(cursor, table_name):
    """Returns a list of column names for a given table."""
    cursor.execute(f"PRAGMA table_info('{table_name}')")
    return [row[1] for row in cursor.fetchall()]

def check_files(old_db_path, new_db_path):
    """Checks if the database files exist before proceeding."""
    if not os.path.exists(old_db_path):
        print(f"ERROR: Old database file not found at: {old_db_path}")
        sys.exit(1)
    if not os.path.exists(new_db_path):
        print(f"ERROR: New database file not found at: {new_db_path}")
        sys.exit(1)

def migrate_data(old_db_path, new_db_path):
    """
    Connects to both databases, migrates data, and verifies row counts.
    Prompts for confirmation if the destination database is not empty.
    """
    old_conn = None
    new_conn = None

    try:
        old_conn = sqlite3.connect(old_db_path)
        new_conn = sqlite3.connect(new_db_path)

        old_cursor = old_conn.cursor()
        new_cursor = new_conn.cursor()

        # 1. Get the list of all tables from the old database
        old_cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'flyway_%';")
        tables = [row[0] for row in old_cursor.fetchall()]

        print(f"Tables found for migration: {tables}\n")

        # 2. Check if the destination database has any data before proceeding
        total_rows_in_new_db = 0
        for table_name in tables:
            try:
                new_cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
                count = new_cursor.fetchone()[0]
                total_rows_in_new_db += count
            except sqlite3.OperationalError:
                # Table might not exist in the new DB, which is fine.
                pass

        if total_rows_in_new_db > 0:
            print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! WARNING !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            print(f"The destination database at '{new_db_path}' is NOT empty.")
            print("Continuing will DELETE ALL existing data in the target tables.")
            print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")

            confirm = input("Are you sure you want to proceed? (Type 'yes' to continue): ")
            if confirm.lower() != 'yes':
                print("Migration aborted by user.")
                sys.exit(0)
            print("\nUser confirmed. Proceeding with data deletion...")

        new_conn.execute('BEGIN TRANSACTION')

        # 3. Iterate over each table to migrate data
        for table_name in tables:
            print(f"Processing table: '{table_name}'...")

            old_columns = get_column_names(old_cursor, table_name)
            new_columns = get_column_names(new_cursor, table_name)

            # 4. Strict Schema Validation: Abort if columns do not match
            if set(old_columns) != set(new_columns):
                print(f"\n--- ERROR: Schema mismatch for table '{table_name}' ---")
                print(f"  [OLD DB] Columns: {sorted(old_columns)}")
                print(f"  [NEW DB] Columns: {sorted(new_columns)}")
                print("Aborting migration. Please ensure schemas are identical before running.")
                new_conn.rollback()
                sys.exit(1)

            # 5. Standard handling for all tables
            new_cursor.execute(f"DELETE FROM {table_name}")
            new_cursor.execute(f"DELETE FROM sqlite_sequence WHERE name='{table_name}'")
            print(f" -> Data from table '{table_name}' cleared.")

            old_cursor.execute(f"SELECT * FROM {table_name}")
            data = old_cursor.fetchall()

            if not data:
                print(f" -> Table '{table_name}' is empty. Skipping insertion.")
                continue

            placeholders = ', '.join(['?'] * len(old_columns))
            column_list_str = ', '.join(f'"{col}"' for col in old_columns)
            sql_insert = f"INSERT INTO {table_name} ({column_list_str}) VALUES ({placeholders})"

            new_cursor.executemany(sql_insert, data)
            print(f" -> {new_cursor.rowcount} rows migrated to '{table_name}'.")

        # 6. If everything went well, commit the transaction
        new_conn.commit()
        print("\nData migration phase complete. Starting verification...")

        # 7. VERIFICATION: Compare row counts post-migration
        verification_failed = False
        for table_name in tables:
            old_cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
            old_count = old_cursor.fetchone()[0]

            new_cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
            new_count = new_cursor.fetchone()[0]

            if old_count == new_count:
                print(f" -> Verification OK for '{table_name}' ({new_count}/{old_count} rows).")
            else:
                print(f" -> VERIFICATION ERROR for '{table_name}': Expected {old_count}, found {new_count}.")
                verification_failed = True

        if verification_failed:
            print("\n!!! Post-migration verification found inconsistencies.")
        else:
            print("\nVerification complete. All data appears to be migrated correctly!")


    except sqlite3.Error as e:
        print(f"\nERROR DURING MIGRATION: {e}")
        print("!!! No changes were saved to the new database (rollback executed).")
        if new_conn:
            new_conn.rollback()
        sys.exit(1)

    finally:
        if old_conn:
            old_conn.close()
        if new_conn:
            new_conn.close()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description="Migrates data from one SQLite database to another, handling schema differences and special cases."
    )
    parser.add_argument(
        "old_db_path",
        help="The path to the source (old) SQLite database file."
    )
    parser.add_argument(
        "new_db_path",
        help="The path to the destination (new) SQLite database file."
    )

    args = parser.parse_args()

    check_files(args.old_db_path, args.new_db_path)
    migrate_data(args.old_db_path, args.new_db_path)
