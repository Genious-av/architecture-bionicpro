
import logging
from datetime import datetime

import clickhouse_connect
import psycopg2

from airflow import DAG
from airflow.operators.python import PythonOperator


# ============================================================
# CONFIGURATION
# ============================================================

BATCH_SIZE = 1000

POSTGRES_CONFIG = {
    "host": "postgres",
    "port": 5432,
    "database": "airflow",
    "user": "airflow",
    "password": "airflow"
}

CLICKHOUSE_CONFIG = {
    "host": "clickhouse",
    "port": 8123,
    "username": "airflow",
    "password": "airflow",
    "database": "airflow"
}


# ============================================================
# SQL QUERIES
# ============================================================

CREATE_SOURCE_TABLE_SQL = """
                          CREATE TABLE IF NOT EXISTS airflow.client_device_operations
                          (
                              client_id UInt64,
                              device_number String,
                              connection_date DateTime,
                              device_type Int16,

                              operation_id UInt64,
                              action String,
                              operation_date DateTime,
                              action_params String
                          )
                              ENGINE = MergeTree()
                              ORDER BY (client_id, operation_date, operation_id) \
                          """


SELECT_POSTGRES_SQL = """
                      SELECT
                          c.id AS client_id,
                          c.device_number,
                          c.connection_date,
                          c.device_type,

                          o.id AS operation_id,
                          o.action,
                          o.operation_date,
                          o.action_params::text AS action_params

                      FROM crm_db.clients c
                               JOIN device.device_operations o
                                    ON c.id = o.device_id

                      ORDER BY o.id \
                      """


CREATE_MART_TABLE_SQL = """
                        CREATE TABLE IF NOT EXISTS airflow.client_daily_operations
                        (
                            client_id UInt64,
                            device_number String,
                            operation_date Date,
                            operations_count UInt64
                        )
                            ENGINE = MergeTree()
                            ORDER BY (operation_date, client_id) \
                        """


BUILD_MART_SQL = """
                 INSERT INTO airflow.client_daily_operations
                 (
                     client_id,
                     device_number,
                     operation_date,
                     operations_count
                 )
                 SELECT
                     client_id,
                     device_number,
                     operation_day,
                     count() AS operations_count
                 FROM
                     (
                         SELECT
                             client_id,
                             device_number,
                             toDate(operation_date) AS operation_day
                         FROM airflow.client_device_operations
                     )
                 GROUP BY
                     client_id,
                     device_number,
                     operation_day
                 ORDER BY
                     operation_day,
                     client_id \
                 """


# ============================================================
# CONNECTIONS
# ============================================================

def get_postgres_connection():
    return psycopg2.connect(**POSTGRES_CONFIG)


def get_clickhouse_client():
    return clickhouse_connect.get_client(**CLICKHOUSE_CONFIG)


# ============================================================
# TASK 1: CREATE CLICKHOUSE SOURCE TABLE
# ============================================================

def create_clickhouse_table():

    client = get_clickhouse_client()

    try:
        client.command(CREATE_SOURCE_TABLE_SQL)

        logging.info(
            "ClickHouse source table created or already exists"
        )

    finally:
        client.close()


# ============================================================
# TASK 2: TRANSFER DATA POSTGRES -> CLICKHOUSE
# ============================================================

def transfer_data():

    pg_conn = get_postgres_connection()
    ch_client = get_clickhouse_client()

    try:
        # Full reload of source table
        ch_client.command(
            "TRUNCATE TABLE airflow.client_device_operations"
        )

        total_inserted = 0

        with pg_conn.cursor(
                name="postgres_to_clickhouse_cursor"
        ) as cursor:

            cursor.itersize = BATCH_SIZE
            cursor.execute(SELECT_POSTGRES_SQL)

            while True:

                rows = cursor.fetchmany(BATCH_SIZE)

                if not rows:
                    break

                ch_client.insert(
                    "client_device_operations",
                    rows,
                    column_names=[
                        "client_id",
                        "device_number",
                        "connection_date",
                        "device_type",
                        "operation_id",
                        "action",
                        "operation_date",
                        "action_params"
                    ]
                )

                total_inserted += len(rows)

                logging.info(
                    "Inserted %s rows into ClickHouse",
                    total_inserted
                )

        logging.info(
            "Transfer completed. Total rows: %s",
            total_inserted
        )

    finally:
        pg_conn.close()
        ch_client.close()


# ============================================================
# TASK 3: BUILD DAILY OPERATIONS MART
# ============================================================

def build_daily_operations_mart():

    ch_client = get_clickhouse_client()

    try:
        # Create mart table
        ch_client.command(CREATE_MART_TABLE_SQL)

        # Full recalculation of mart
        ch_client.command(
            "TRUNCATE TABLE airflow.client_daily_operations"
        )

        # Aggregate operations by client and day
        ch_client.command(BUILD_MART_SQL)

        logging.info(
            "Daily operations mart successfully rebuilt"
        )

    finally:
        ch_client.close()


# ============================================================
# DAG CONFIGURATION
# ============================================================

default_args = {
    "owner": "airflow",
    "depends_on_past": False,
    "retries": 1,
}


with DAG(
        dag_id="postgres_to_clickhouse_dag",
        default_args=default_args,
        start_date=datetime(2026, 11, 1),
        schedule="@daily",
        catchup=False,
        max_active_runs=1,
        tags=["postgres", "clickhouse", "etl"]
) as dag:

    create_table = PythonOperator(
        task_id="create_clickhouse_table",
        python_callable=create_clickhouse_table
    )

    transfer = PythonOperator(
        task_id="transfer_data",
        python_callable=transfer_data
    )

    build_mart = PythonOperator(
        task_id="build_daily_operations_mart",
        python_callable=build_daily_operations_mart
    )

    # Execution order
    create_table >> transfer >> build_mart
