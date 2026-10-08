-- Dump compact column metadata for the tables used in the thesis DB-design chapter.
SELECT c.table_name, c.ordinal_position, c.column_name,
       c.data_type,
       COALESCE(c.character_maximum_length::text, '') AS len,
       CASE WHEN c.is_nullable = 'NO' THEN 'N' ELSE 'Y' END AS nullable,
       COALESCE(c.column_default, '') AS def,
       CASE WHEN pk.column_name IS NOT NULL THEN 'Y' ELSE 'N' END AS is_pk
FROM information_schema.columns c
LEFT JOIN (
  SELECT kcu.table_name, kcu.column_name
  FROM information_schema.table_constraints tc
  JOIN information_schema.key_column_usage kcu
    ON kcu.constraint_name = tc.constraint_name AND kcu.table_schema = tc.table_schema
  WHERE tc.constraint_type = 'PRIMARY KEY' AND tc.table_schema = 'public'
) pk ON pk.table_name = c.table_name AND pk.column_name = c.column_name
WHERE c.table_schema = 'public'
  AND c.table_name IN (
    'user_account','role','permission','user_role','role_permission',
    'ethnic_group','ethnic_custom','ethnic_location','festival','art','food',
    'person_profile','autonomous_area','traditional_sport','topic','topic_entry',
    'favorite','like_record','search_document','interest_tag','user_interest',
    'user_behavior','workflow_instance','workflow_opinion','discussion_topic',
    'discussion_post','translate_glossary','image_credit','content_source',
    'content_source_link','form_config','content_review','notification'
  )
ORDER BY c.table_name, c.ordinal_position;
