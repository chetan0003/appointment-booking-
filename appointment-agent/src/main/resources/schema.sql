-- ===================================================================
-- AI Appointment Booking Agent - Phase 1 Schema
-- Hardened version of the design doc: adds constraints that make
-- double-booking and duplicate-session bugs impossible at the DB layer.
-- ===================================================================

CREATE TABLE clinic (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    whatsapp_number VARCHAR(20),
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE doctor (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL REFERENCES clinic(id),
    name VARCHAR(255) NOT NULL,
    specialization VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE service (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL REFERENCES clinic(id),
    name VARCHAR(255) NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    price NUMERIC(10,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE doctor_service (
    doctor_id BIGINT NOT NULL REFERENCES doctor(id),
    service_id BIGINT NOT NULL REFERENCES service(id),
    PRIMARY KEY (doctor_id, service_id)
);

CREATE TABLE patient (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL REFERENCES clinic(id),
    name VARCHAR(255),
    whatsapp_number VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_patient_clinic_phone UNIQUE (clinic_id, whatsapp_number)
);

CREATE TABLE clinic_working_hours (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL REFERENCES clinic(id),
    day_of_week VARCHAR(20) NOT NULL CHECK (day_of_week IN
        ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_start_time TIME,
    break_end_time TIME,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_working_hours_clinic_day UNIQUE (clinic_id, day_of_week),
    CONSTRAINT chk_working_hours_order CHECK (start_time < end_time)
);

CREATE TABLE appointment (
    id BIGSERIAL PRIMARY KEY,
    appointment_code VARCHAR(20) NOT NULL UNIQUE,  -- e.g. APT-1001, human-facing id
    clinic_id BIGINT NOT NULL REFERENCES clinic(id),
    doctor_id BIGINT NOT NULL REFERENCES doctor(id),
    service_id BIGINT NOT NULL REFERENCES service(id),
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED'
        CHECK (status IN ('CONFIRMED','COMPLETED','CANCELLED','NO_SHOW')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- THE core anti-double-booking guarantee: no two ACTIVE appointments
-- for the same doctor can overlap the same start_time on the same date.
-- (Partial unique index -> cancelled/completed appointments don't block reuse of a slot.)
CREATE UNIQUE INDEX uq_doctor_slot_active
    ON appointment (doctor_id, appointment_date, start_time)
    WHERE status = 'CONFIRMED';

CREATE INDEX idx_appointment_doctor_date ON appointment (doctor_id, appointment_date);

CREATE TABLE conversation_session (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL REFERENCES clinic(id),
    whatsapp_number VARCHAR(20) NOT NULL,
    intent VARCHAR(50),
    service_id BIGINT REFERENCES service(id),
    doctor_id BIGINT REFERENCES doctor(id),
    appointment_date DATE,
    selected_start_time TIME,
    patient_name VARCHAR(255),
    state VARCHAR(50) NOT NULL DEFAULT 'STARTED'
        CHECK (state IN ('STARTED','COLLECTING_SERVICE','COLLECTING_DOCTOR',
                          'COLLECTING_DATE','SHOWING_SLOTS','WAITING_FOR_SLOT',
                          'COLLECTING_NAME','CONFIRMING','BOOKED','ABANDONED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Only one *active* (non-terminal) session per patient per clinic at a time.
CREATE UNIQUE INDEX uq_session_active_per_patient
    ON conversation_session (clinic_id, whatsapp_number)
    WHERE state NOT IN ('BOOKED','ABANDONED');

-- Generic updated_at trigger, reused across tables
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_patient_updated_at BEFORE UPDATE ON patient
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_appointment_updated_at BEFORE UPDATE ON appointment
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_session_updated_at BEFORE UPDATE ON conversation_session
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ===================================================================
-- Seed data for local dev / Postman testing (mirrors the doc's example)
-- ===================================================================
INSERT INTO clinic (name, whatsapp_number, timezone) VALUES
    ('ABC Dental Clinic', '+919876543210', 'Asia/Kolkata');

INSERT INTO doctor (clinic_id, name, specialization) VALUES
    (1, 'Dr Sharma', 'Dentist'),
    (1, 'Dr Patel', 'Dentist');

INSERT INTO service (clinic_id, name, duration_minutes, price) VALUES
    (1, 'Dental Consultation', 30, 500.00),
    (1, 'Teeth Cleaning', 30, 800.00);

INSERT INTO doctor_service (doctor_id, service_id) VALUES
    (1, 1), (1, 2), (2, 1);

INSERT INTO clinic_working_hours (clinic_id, day_of_week, start_time, end_time, break_start_time, break_end_time) VALUES
    (1, 'MONDAY', '09:00', '19:00', '13:00', '14:00'),
    (1, 'TUESDAY', '09:00', '19:00', '13:00', '14:00'),
    (1, 'WEDNESDAY', '09:00', '19:00', '13:00', '14:00'),
    (1, 'THURSDAY', '09:00', '19:00', '13:00', '14:00'),
    (1, 'FRIDAY', '09:00', '19:00', '13:00', '14:00'),
    (1, 'SATURDAY', '09:00', '19:00', '13:00', '14:00');


CREATE TABLE role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO role (name)
VALUES
    ('SUPER_ADMIN'),
    ('CLINIC_ADMIN'),
    ('STAFF'),
    ('DOCTOR');


CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,

    username VARCHAR(100) NOT NULL UNIQUE,

    email VARCHAR(255) UNIQUE,

    password VARCHAR(255) NOT NULL,

    first_name VARCHAR(100),

    last_name VARCHAR(100),

    phone VARCHAR(30),

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id)
        REFERENCES role(id)
        ON DELETE CASCADE
);

CREATE TABLE clinic_user (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    clinic_id BIGINT NOT NULL,

    doctor_id BIGINT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_clinic_user_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_clinic_user_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES clinic(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_clinic_user_doctor
        FOREIGN KEY (doctor_id)
        REFERENCES doctor(id)
        ON DELETE SET NULL,

    CONSTRAINT uq_clinic_user
        UNIQUE (user_id, clinic_id)
);



INSERT INTO app_user (
    username,
    email,
    password,
    first_name,
    last_name,
    enabled
)
VALUES (
    'chetan03',
    'chetan@clinicflow.com',
    '$2a$12$MZMsSvilIEesR9285ViCmu0pfzpy67Zwd9y7DJEyRD1v/mneyYhKu',
    'Chetan',
    'Dahule',
    true
);

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN role r
    ON r.name = 'SUPER_ADMIN'
WHERE u.username = 'chetan03';


CREATE TABLE doctor_availability (
    id BIGSERIAL PRIMARY KEY,

    doctor_id BIGINT NOT NULL,

    day_of_week VARCHAR(20) NOT NULL,

    start_time TIME NOT NULL,

    end_time TIME NOT NULL,

    break_start_time TIME,

    break_end_time TIME,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_doctor_availability_doctor
        FOREIGN KEY (doctor_id)
        REFERENCES doctor(id),

    CONSTRAINT chk_doctor_availability_day
        CHECK (
            day_of_week IN (
                'MONDAY',
                'TUESDAY',
                'WEDNESDAY',
                'THURSDAY',
                'FRIDAY',
                'SATURDAY',
                'SUNDAY'
            )
        ),

    CONSTRAINT chk_doctor_availability_time
        CHECK (start_time < end_time),

    CONSTRAINT chk_doctor_availability_break
        CHECK (
            break_start_time IS NULL
            OR break_end_time IS NULL
            OR break_start_time < break_end_time
        )
);

CREATE INDEX idx_doctor_availability_doctor_day
ON doctor_availability (
    doctor_id,
    day_of_week,
    active
);

CREATE UNIQUE INDEX uk_doctor_availability_doctor_day
ON doctor_availability (
    doctor_id,
    day_of_week
)
WHERE active = TRUE;


CREATE TABLE doctor_leaves (
    id BIGSERIAL PRIMARY KEY,

    doctor_id BIGINT NOT NULL,

    leave_date DATE NOT NULL,

    start_time TIME,

    end_time TIME,

    reason VARCHAR(500),

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_doctor_leaves_doctor
        FOREIGN KEY (doctor_id)
        REFERENCES doctor(id),

    CONSTRAINT chk_doctor_leave_time
        CHECK (
            start_time IS NULL
            OR end_time IS NULL
            OR start_time < end_time
        )
);

CREATE INDEX idx_doctor_leaves_doctor_date
ON doctor_leaves (
    doctor_id,
    leave_date,
    active
);


CREATE TABLE clinic_holidays (
    id BIGSERIAL PRIMARY KEY,

    clinic_id BIGINT NOT NULL,

    holiday_date DATE NOT NULL,

    name VARCHAR(255) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_clinic_holidays_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES clinic(id),

    CONSTRAINT uk_clinic_holiday
        UNIQUE (
            clinic_id,
            holiday_date
        )
);

CREATE INDEX idx_clinic_holidays_clinic_date
ON clinic_holidays (
    clinic_id,
    holiday_date,
    active
);

--doc service table modify
ALTER TABLE doctor_service
DROP CONSTRAINT doctor_service_pkey;

ALTER TABLE doctor_service
ADD COLUMN id BIGSERIAL;

ALTER TABLE doctor_service
ADD CONSTRAINT pk_doctor_service
PRIMARY KEY (id);


CREATE TABLE notification (

    id BIGSERIAL PRIMARY KEY,

    appointment_id BIGINT NOT NULL,

    type VARCHAR(30) NOT NULL,

    channel VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP',

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    scheduled_at TIMESTAMP NOT NULL,

    sent_at TIMESTAMP,

    error_message VARCHAR(500),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointment(id),

    CONSTRAINT chk_notification_type
        CHECK (
            type IN (
                'BOOKING_CONFIRMATION',
                'REMINDER_24H',
                'RESCHEDULED',
                'FOLLOW_UP_SUGGESTED'
            )
        ),

    CONSTRAINT chk_notification_channel
        CHECK (
            channel IN (
                'WHATSAPP',
                'SMS',
                'EMAIL',
                'PUSH'
            )
        ),

    CONSTRAINT chk_notification_status
        CHECK (
            status IN (
                'PENDING',
                'SENT',
                'FAILED'
            )
        )
);

CREATE UNIQUE INDEX uq_notification_appointment_type_channel
    ON notification (
        appointment_id,
        type,
        channel
    );

    ALTER TABLE appointment
        ADD COLUMN follow_up_of_appointment_id BIGINT,
        ADD COLUMN suggested_follow_up_date DATE;

    ALTER TABLE appointment
        ADD CONSTRAINT fk_appointment_follow_up_of
            FOREIGN KEY (follow_up_of_appointment_id)
            REFERENCES appointment(id);

    CREATE INDEX idx_appointment_follow_up_of
        ON appointment(follow_up_of_appointment_id);

    CREATE INDEX idx_appointment_suggested_follow_up_date
        ON appointment(suggested_follow_up_date);


    ALTER TABLE patient
    ADD COLUMN email VARCHAR(150);

    ALTER TABLE patient
    ADD COLUMN date_of_birth DATE;