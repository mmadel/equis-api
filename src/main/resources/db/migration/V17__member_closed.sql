-- E11 · track where each person is (status)
-- Only "closed" is stored; waiting for assessment, no-show and rider are worked out from the bookings.
-- Closing hides the person from the active lists and keeps their history; reopening clears the date.
ALTER TABLE member ADD COLUMN closed_at timestamptz;
