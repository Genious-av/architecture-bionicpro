CREATE Schema crm_db;
GRANT ALL PRIVILEGES ON DATABASE airflow TO airflow;
CREATE TABLE IF NOT EXISTS crm_db.clients
(
    id BIGINT PRIMARY KEY,
    device_number VARCHAR(50) NOT NULL,
    connection_date TIMESTAMP NOT NULL,
    device_type SMALLINT NOT NULL
    CHECK (device_type BETWEEN 1 AND 4)
    );
INSERT INTO crm_db.clients
(id, device_number, connection_date, device_type)
VALUES
    (1, 'DEV-100001', '2026-01-10 09:15:00', 1),
    (2, 'DEV-100002', '2026-01-12 11:30:00', 2),
    (3, 'DEV-100003', '2026-01-15 14:20:00', 1),
    (4, 'DEV-100004', '2026-02-01 08:45:00', 3),
    (5, 'DEV-100005', '2026-02-10 16:10:00', 4),
    (6, 'DEV-100006', '2026-02-15 10:00:00', 1),
    (7, 'DEV-100007', '2026-03-05 12:25:00', 2),
    (8, 'DEV-100008', '2026-03-12 17:40:00', 3),
    (9, 'DEV-100009', '2026-04-01 13:15:00', 1),
    (10, 'DEV-100010', '2026-04-20 19:30:00', 4);

CREATE Schema device;

CREATE TABLE IF NOT EXISTS device.device_operations
(
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    operation_date TIMESTAMP NOT NULL,
    action_params JSONB NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_device_operations_device_date
    ON device.device_operations (device_id, operation_date);

INSERT INTO device.device_operations
(device_id, action, operation_date, action_params)
SELECT
    c.id AS device_id,

    CASE (g.n % 5)
        WHEN 0 THEN 'CONNECT'
        WHEN 1 THEN 'DISCONNECT'
        WHEN 2 THEN 'MOVE'
        WHEN 3 THEN 'ACTIVATE'
        WHEN 4 THEN 'DEACTIVATE'
        END AS action,

    TIMESTAMP '2026-01-01 00:00:00'
        + (g.n * INTERVAL '1 hour') AS operation_date,

    jsonb_build_object(
        'coordinates',
        jsonb_build_object(
            'x', round((random() * 100)::numeric, 2),
            'y', round((random() * 100)::numeric, 2),
            'z', round((random() * 100)::numeric, 2)
        )
    ) AS action_params

FROM generate_series(1, 100) AS g(n)
    JOIN crm_db.clients c
ON c.id = ((g.n - 1) % 10) + 1;