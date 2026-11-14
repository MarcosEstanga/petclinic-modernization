-- Seed data identical to the legacy db/h2/data.sql (BR-SYS-03).
-- In the original, pets 7 (Samantha) and 8 (Max) belong to owner 6 (Jean Coleman).
INSERT INTO visits VALUES (default, 6, 7, '2013-01-01', 'rabies shot');
INSERT INTO visits VALUES (default, 6, 8, '2013-01-02', 'rabies shot');
INSERT INTO visits VALUES (default, 6, 8, '2013-01-03', 'neutered');
INSERT INTO visits VALUES (default, 6, 7, '2013-01-04', 'spayed');
