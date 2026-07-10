CREATE TABLE huts (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

INSERT INTO huts (name)
VALUES ('Huldrebakken'), ('Trollhaugen'), ('Tent/hammock');

CREATE TABLE bookings (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    number_of_people INT NOT NULL CHECK (number_of_people > 0),
    hut_id INT NOT NULL REFERENCES huts (id),
    arrival_date DATE NOT NULL,
    departure_date DATE NOT NULL CHECK (departure_date >= arrival_date),
    admin_notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'APPROVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
