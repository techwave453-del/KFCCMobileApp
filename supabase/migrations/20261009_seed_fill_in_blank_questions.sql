insert into public.bible_game_questions
    (game_type, category, question, options, correct_answer_index, explanation, reference, is_published, sort_order)
select seed.game_type, seed.category, seed.question, seed.options, seed.correct_answer_index,
       seed.explanation, seed.reference, seed.is_published, seed.sort_order
from (values
    ('fill_blank','FAITH_AND_LIFE','The Lord is my shepherd; I shall not ____.','["want","fear","sleep","wander"]'::jsonb,0,'Psalm 23 describes God as a shepherd who provides for His people.','Psalm 23:1',true,201),
    ('fill_blank','FAITH_AND_LIFE','Trust in the Lord with all your ____.','["strength","heart","wisdom","wealth"]'::jsonb,1,'Proverbs teaches wholehearted trust in the Lord.','Proverbs 3:5',true,202),
    ('fill_blank','FAITH_AND_LIFE','Be strong and of good ____.','["fortune","cheer","courage","health"]'::jsonb,2,'God encouraged Joshua to be strong and courageous.','Joshua 1:9',true,203),
    ('fill_blank','NEW_TESTAMENT','For God so loved the world that He gave His only ____.','["prophet","Son","angel","servant"]'::jsonb,1,'John 3:16 describes God’s love and the gift of His Son.','John 3:16',true,204),
    ('fill_blank','NEW_TESTAMENT','I can do all things through Christ who ____.','["calls me","strengthens me","guides others","created me"]'::jsonb,1,'Paul describes receiving strength through Christ.','Philippians 4:13',true,205),
    ('fill_blank','FAITH_AND_LIFE','The fruit of the Spirit includes love, joy, peace, and ____.','["patience","pride","envy","fear"]'::jsonb,0,'Patience is included in the fruit of the Spirit.','Galatians 5:22–23',true,206),
    ('fill_blank','NEW_TESTAMENT','Jesus said, “I am the way, the truth, and the ____.”','["light","life","door","bread"]'::jsonb,1,'Jesus identifies Himself as the way, the truth, and the life.','John 14:6',true,207),
    ('fill_blank','OLD_TESTAMENT','Your word is a lamp to my feet and a light to my ____.','["path","house","heart","eyes"]'::jsonb,0,'The psalmist describes God’s word as guidance for life.','Psalm 119:105',true,208)
) as seed(game_type, category, question, options, correct_answer_index, explanation, reference, is_published, sort_order)
where not exists (
    select 1 from public.bible_game_questions existing
    where existing.game_type = seed.game_type and existing.question = seed.question
);