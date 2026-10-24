-- Clean up any existing Kamba translation record to avoid unique constraint conflicts
delete from public.bible_translations where id = 'kam' or abbreviation = 'KAM';

-- Add Kamba Bible (Mbivilia) translation record
insert into public.bible_translations (id, abbreviation, name, language_code, language_name, license_name, source_name, is_enabled, sort_order)
values ('kam', 'KAM', 'Kamba Bible (Mbivilia)', 'kam', 'Kamba', 'God''s word open for all ©Bible Society of Kenya, 1956', 'Bible Society of Kenya', true, 25);