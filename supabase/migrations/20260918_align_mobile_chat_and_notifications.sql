-- Align the mobile chat implementation with the live Supabase schema.

ALTER TABLE public.chat_rooms
  DROP CONSTRAINT IF EXISTS chat_rooms_type_check;

ALTER TABLE public.chat_rooms
  ADD CONSTRAINT chat_rooms_type_check
  CHECK (type = ANY (ARRAY['community'::text, 'group'::text, 'admin'::text, 'prayer'::text]));

ALTER TABLE public.chat_messages
  ADD COLUMN IF NOT EXISTS reply_to_id uuid NULL;

ALTER TABLE public.chat_messages
  DROP CONSTRAINT IF EXISTS chat_messages_reply_to_id_fkey;

ALTER TABLE public.chat_messages
  ADD CONSTRAINT chat_messages_reply_to_id_fkey
  FOREIGN KEY (reply_to_id) REFERENCES public.chat_messages(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS chat_messages_reply_to_id_idx
  ON public.chat_messages(reply_to_id);

DROP POLICY IF EXISTS "Chat rooms creator read" ON public.chat_rooms;
CREATE POLICY "Chat rooms creator read"
ON public.chat_rooms
FOR SELECT TO authenticated
USING (created_by = auth.uid());

ALTER TABLE public.app_notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_reads ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime'
      AND schemaname = 'public'
      AND tablename = 'app_notifications'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.app_notifications;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime'
      AND schemaname = 'public'
      AND tablename = 'notification_reads'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.notification_reads;
  END IF;
END $$;
