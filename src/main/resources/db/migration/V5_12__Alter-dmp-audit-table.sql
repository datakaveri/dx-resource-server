ALTER TABLE auditing_dmp DROP CONSTRAINT auditing_dmp_pkey CASCADE;
ALTER TABLE auditing_dmp ALTER COLUMN _id DROP DEFAULT;
ALTER TABLE auditing_dmp ALTER COLUMN _id TYPE varchar,
ADD PRIMARY KEY (_id);
