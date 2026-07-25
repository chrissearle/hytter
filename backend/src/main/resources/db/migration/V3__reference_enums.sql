-- Huts and booking name types move from lookup table / free string to enums
-- owned by the backend (domain/Hut.kt, domain/BookingNameType.kt). The enum
-- name is the stored value; the Bokmål label lives only in Kotlin.

ALTER TABLE bookings ADD COLUMN hut VARCHAR(32);

UPDATE bookings b
SET hut = CASE h.name
    WHEN 'Huldrebakken' THEN 'HULDREBAKKEN'
    WHEN 'Trollhaugen' THEN 'TROLLHAUGEN'
    WHEN 'Tent/hammock' THEN 'TENT_HAMMOCK'
END
FROM huts h
WHERE h.id = b.hut_id;

ALTER TABLE bookings ALTER COLUMN hut SET NOT NULL;

ALTER TABLE bookings ADD CONSTRAINT bookings_hut_check
    CHECK (hut IN ('HULDREBAKKEN', 'TROLLHAUGEN', 'TENT_HAMMOCK'));

ALTER TABLE bookings DROP COLUMN hut_id;

DROP TABLE huts;

-- Previously the name alone had to be reverse-matched against the preset list
-- to work out which dropdown entry produced it, which cannot distinguish a
-- personal booking from a free-text one. Store the type explicitly.
ALTER TABLE bookings ADD COLUMN name_type VARCHAR(32);

UPDATE bookings
SET name_type = CASE name
    WHEN 'Opphavet' THEN 'OPPHAVET'
    WHEN 'Sørkisrampen' THEN 'SORKISRAMPEN'
    WHEN 'HA12' THEN 'HA12'
    ELSE 'OTHER'
END;

ALTER TABLE bookings ALTER COLUMN name_type SET NOT NULL;

ALTER TABLE bookings ADD CONSTRAINT bookings_name_type_check
    CHECK (name_type IN ('OPPHAVET', 'SORKISRAMPEN', 'HA12', 'PERSONAL', 'OTHER'));

-- The only predicate any query filters on (see findInRange).
CREATE INDEX bookings_dates_idx ON bookings (arrival_date, departure_date);
