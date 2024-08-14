CREATE type Access_Type as ENUM
(
   'api',
   'sub',
   'async',
   'file'
);

ALTER TABLE s3_upload_url ADD COLUMN isaudited boolean;
ALTER TABLE auditing_rs ADD COLUMN access_type Access_Type;