-- Keep the mobile app's Supabase schema authoritative and usable.

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'public.chat_room_members'::regclass
      AND contype IN ('p','u')
      AND conkey = ARRAY[
        (SELECT attnum FROM pg_attribute WHERE attrelid='public.chat_room_members'::regclass AND attname='room_id'),
        (SELECT attnum FROM pg_attribute WHERE attrelid='public.chat_room_members'::regclass AND attname='user_id')
      ]
  ) THEN
    ALTER TABLE public.chat_room_members
      ADD CONSTRAINT chat_room_members_room_user_key UNIQUE (room_id, user_id);
  END IF;
END $$;

ALTER TABLE public.chat_room_members ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS chat_room_members_select_own ON public.chat_room_members;
CREATE POLICY chat_room_members_select_own
ON public.chat_room_members
FOR SELECT TO authenticated
USING (user_id = auth.uid());

DROP POLICY IF EXISTS chat_room_members_insert_own ON public.chat_room_members;
CREATE POLICY chat_room_members_insert_own
ON public.chat_room_members
FOR INSERT TO authenticated
WITH CHECK (user_id = auth.uid());

GRANT EXECUTE ON FUNCTION public.join_kfcc_community() TO authenticated;

INSERT INTO public.chat_rooms (type, title)
SELECT 'community', 'Community Chat'
WHERE NOT EXISTS (
  SELECT 1 FROM public.chat_rooms WHERE type = 'community'
);

ALTER TABLE public.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_message_length_check;
ALTER TABLE public.chat_messages
  ADD CONSTRAINT chat_messages_message_length_check
  CHECK (char_length(trim(message)) BETWEEN 1 AND 2000);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime'
      AND schemaname = 'public'
      AND tablename = 'chat_messages'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.chat_messages;
  END IF;
END $$;
