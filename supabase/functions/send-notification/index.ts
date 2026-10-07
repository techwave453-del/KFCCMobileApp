import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { SignJWT, importPKCS8 } from "npm:jose@6";

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });

const required = (name: string) => {
  const value = Deno.env.get(name)?.trim();
  if (!value) throw new Error(`Missing secret: ${name}`);
  return value;
};

async function getFcmAccessToken() {
  const projectId = required("FIREBASE_PROJECT_ID");
  const clientEmail = required("FIREBASE_CLIENT_EMAIL");
  const privateKey = required("FIREBASE_PRIVATE_KEY").replace(/\\n/g, "\n");

  const key = await importPKCS8(privateKey, "RS256");
  const assertion = await new SignJWT({
    scope: "https://www.googleapis.com/auth/firebase.messaging",
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(clientEmail)
    .setSubject(clientEmail)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt()
    .setExpirationTime("1h")
    .sign(key);

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });

  const body = await response.json();
  if (!response.ok || !body.access_token) {
    throw new Error(`Google OAuth token request failed: ${JSON.stringify(body)}`);
  }

  return { projectId, accessToken: body.access_token as string };
}

async function supabaseRequest(path: string, init: RequestInit = {}) {
  const baseUrl = required("SUPABASE_URL");
  const serviceKey = required("SUPABASE_SERVICE_ROLE_KEY");

  return fetch(`${baseUrl}/rest/v1/${path}`, {
    ...init,
    headers: {
      apikey: serviceKey,
      Authorization: `Bearer ${serviceKey}`,
      "Content-Type": "application/json",
      ...(init.headers ?? {}),
    },
  });
}

async function sendToToken(
  projectId: string,
  accessToken: string,
  token: string,
  title: string,
  message: string,
  notificationId: string,
  type: string,
  imageUrl: string | null = null,
) {
  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token,
          notification: {
            title,
            body: message,
            ...(imageUrl ? { image: imageUrl } : {}),
          },
          data: {
            notification_id: notificationId,
            title,
            message,
            type,
            ...(imageUrl ? { image_url: imageUrl } : {}),
          },
          android: {
            priority: "HIGH",
            notification: {
              channel_id: "kfcc_church_notifications",
              ...(imageUrl ? { image: imageUrl } : {}),
            },
          },
        },
      }),
    },
  );

  const body = await response.json().catch(() => ({}));
  return { ok: response.ok, status: response.status, body };
}

async function getTokensForUserIds(userIds: string[]) {
  const uniqueUserIds = [...new Set(userIds.filter(Boolean))];
  if (uniqueUserIds.length === 0) return [];

  const encodedIds = uniqueUserIds.map((id) => encodeURIComponent(id)).join(",");
  const response = await supabaseRequest(
    `device_tokens?select=token&user_id=in.(${encodedIds})`,
  );
  const rows = await response.json();

  if (!response.ok) {
    throw new Error(`Failed to load device tokens: ${JSON.stringify(rows)}`);
  }

  return [
    ...new Set(
      (rows ?? [])
        .map((row: { token?: string }) => row.token?.trim())
        .filter((token: string | undefined): token is string => Boolean(token)),
    ),
  ];
}

async function getNotificationTargets(notification: any) {
  const query = notification.user_id
    ? `device_tokens?select=token,user_id&user_id=eq.${encodeURIComponent(notification.user_id)}`
    : "device_tokens?select=token,user_id";
  const response = await supabaseRequest(query);
  const rows = await response.json();
  if (!response.ok) {
    throw new Error(`Failed to load device tokens: ${JSON.stringify(rows)}`);
  }

  const seen = new Set<string>();
  return (rows ?? [])
    .map((row: { token?: string; user_id?: string }) => ({
      token: row.token?.trim() || "",
      userId: row.user_id || "",
    }))
    .filter((target: { token: string }) => {
      if (!target.token || seen.has(target.token)) return false;
      seen.add(target.token);
      return true;
    });
}

async function getUsernames(userIds: string[]) {
  const ids = [...new Set(userIds.filter(Boolean))];
  if (ids.length === 0) return new Map<string, string>();
  const encoded = ids.map(encodeURIComponent).join(",");
  const response = await supabaseRequest(
    `chat_profiles?select=user_id,username,display_name&user_id=in.(${encoded})`,
  );
  const rows = await response.json();
  if (!response.ok) {
    throw new Error(`Failed to load notification profiles: ${JSON.stringify(rows)}`);
  }

  return new Map(
    (rows ?? []).map((row: { user_id?: string; username?: string; display_name?: string }) => [
      row.user_id || "",
      row.display_name?.trim() || row.username?.trim() || "",
    ]),
  );
}

function personalize(value: string, username: string) {
  if (!username) return value;
  return value
    .replace(/\{\{username\}\}/gi, username)
    .replace(/\{username\}/gi, username);
}

async function getChatNotification(payload: any) {
  const message = payload.record;
  if (!message?.id || !message?.room_id || !message?.sender_id || !message?.message) {
    throw new Error("Invalid chat message payload");
  }

  const membersResponse = await supabaseRequest(
    `chat_room_members?select=user_id&room_id=eq.${encodeURIComponent(message.room_id)}&user_id=neq.${encodeURIComponent(message.sender_id)}`,
  );
  const members = await membersResponse.json();
  if (!membersResponse.ok) {
    throw new Error(`Failed to load chat members: ${JSON.stringify(members)}`);
  }

  const senderResponse = await supabaseRequest(
    `chat_profiles?select=username,display_name&user_id=eq.${encodeURIComponent(message.sender_id)}&limit=1`,
  );
  const senderRows = await senderResponse.json();
  if (!senderResponse.ok) {
    throw new Error(`Failed to load sender profile: ${JSON.stringify(senderRows)}`);
  }

  const roomResponse = await supabaseRequest(
    `chat_rooms?select=title& id=eq.${encodeURIComponent(message.room_id)}&limit=1`.replace("title& id", "title&id"),
  );
  const roomRows = await roomResponse.json();
  if (!roomResponse.ok) {
    throw new Error(`Failed to load chat room: ${JSON.stringify(roomRows)}`);
  }

  const sender = senderRows?.[0];
  const room = roomRows?.[0];
  const senderName = sender?.display_name?.trim() || sender?.username?.trim() || "Member";
  const roomTitle = room?.title?.trim() || "Community Chat";
  const userIds = (members ?? []).map((row: { user_id?: string }) => row.user_id).filter(Boolean);

  return {
    id: message.id,
    title: `${senderName} · ${roomTitle}`,
    body: message.message,
    type: "chat",
    tokens: await getTokensForUserIds(userIds),
  };
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  try {
    const webhookSecret = required("FCM_WEBHOOK_SECRET");
    if (req.headers.get("x-kfcc-webhook-secret") !== webhookSecret) {
      return json({ error: "Unauthorized webhook" }, 401);
    }

    const payload = await req.json();

    const { projectId, accessToken } = await getFcmAccessToken();

    let notificationId: string;
    let title: string;
    let message: string;
    let type: string;
    let targets: { token: string; userId: string }[];
    let imageUrl: string | null = null;

    if (
      payload?.type === "INSERT" &&
      payload?.schema === "public" &&
      payload?.table === "app_notifications"
    ) {
      const notification = payload.record;
      if (!notification?.id || !notification?.title || !notification?.message) {
        return json({ error: "Invalid notification payload" }, 400);
      }

      notificationId = notification.id;
      title = notification.title;
      message = notification.message;
      type = notification.type ?? "general";
      imageUrl = notification.image_url ?? null;
      targets = await getNotificationTargets(notification);
    } else if (
      payload?.type === "INSERT" &&
      payload?.schema === "public" &&
      payload?.table === "chat_messages"
    ) {
      const chat = await getChatNotification(payload);
      notificationId = chat.id;
      title = chat.title;
      message = chat.body;
      type = chat.type;
      targets = chat.tokens.map((token) => ({ token, userId: "" }));
    } else {
      return json({ ok: true, ignored: true });
    }

    if (targets.length === 0) {
      return json({ ok: true, sent: 0, targeted: 0, message: "No registered devices" });
    }

    const usernames = await getUsernames(targets.map((target) => target.userId));
    let sent = 0;
    const invalidTokens: string[] = [];

    for (const target of targets) {
      const username = usernames.get(target.userId) || "";
      const personalizedTitle = personalize(title, username);
      const personalizedMessage = personalize(message, username);
      const result = await sendToToken(
        projectId,
        accessToken,
        target.token,
        personalizedTitle,
        personalizedMessage,
        notificationId,
        type,
        imageUrl,
      );

      if (result.ok) {
        sent++;
        continue;
      }

      const errorText = JSON.stringify(result.body);
      if (
        result.status === 404 ||
        errorText.includes("UNREGISTERED") ||
        errorText.includes("registration-token-not-registered")
      ) {
        invalidTokens.push(token);
      } else {
        console.error("FCM send failed", result.status, result.body);
      }
    }

    for (const token of invalidTokens) {
      await supabaseRequest(
        `device_tokens?token=eq.${encodeURIComponent(token)}`,
        { method: "DELETE" },
      );
    }

    return json({
      ok: true,
      notification_id: notificationId,
      targeted: targets.length,
      sent,
      removed_invalid_tokens: invalidTokens.length,
    });
  } catch (error) {
    console.error(error);
    return json(
      { error: error instanceof Error ? error.message : "FCM delivery failed" },
      500,
    );
  }
});