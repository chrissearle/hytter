-- Ownership moves from the display name to the Keycloak `sub` claim. A name is
-- neither unique nor stable: two people sharing one would share edit rights,
-- and renaming yourself in Keycloak would orphan your bookings.
--
-- created_by is kept - admins need to see who asked for a booking - but it is
-- no longer what authorization is decided on. Rows written before this
-- migration have a null subject and fall back to the name comparison; see
-- BookingAccess.kt.
ALTER TABLE bookings ADD COLUMN created_by_subject TEXT;

CREATE INDEX bookings_created_by_subject_idx ON bookings (created_by_subject);
