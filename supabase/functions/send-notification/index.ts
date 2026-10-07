import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { SignJWT, importPKCS8 } from "npm:jose@6";

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

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
  const assertion = await new SignJWT({ scope: "https://www.googleapis.com/auth/firebase.messaging" })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(clientEmail).setSubject(clientEmail)
    .setAudience("https://oauth2.googleapis.com/token").setIssuedAt()
    .setExpirationTime("1h").sign(key);
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  const body = await response.json();
  if (!response.ok || !body.access_token) throw new Error(`Google OAuth token request failed: ${JSON.stringify(body)}`);
  return { projectId, accessToken: body.access_token as string };
}

async function supabaseRequest(path: string, init: RequestInit = {}) {
  const baseUrl = required("SUPABASE_URL");
  const serviceKey = required("SUPABASE_SERVICE_ROLE_KEY");
  return fetch(`${baseUrl}/rest/v1/${path}`, {
    ...init,
    headers: { apikey: serviceKey, Authorization: `Bearer ${serviceKey}`, "Content-Type": "application/json", ...(init.headers ?? {}) },
  });
}

async function sendToToken(projectId: string, accessToken: string, token: string, title: string, message: string, notificationId: string, type: string, imageUrl: string | null = null, senderAvatarUrl: string | null = null, roomId: string | null = null) {
  const response = await fetch(`https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`, {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      message: {
        token,
        // Data-only delivery is intentional. It guarantees Kanisa's service
        // creates the notification in both foreground and background, so the
        // avatar and exact-chat PendingIntent are always under app control.
        data: {
          notification_id: notificationId, title, message, type,
          ...(imageUrl ? { image_url: imageUrl } : {}),
          ...(senderAvatarUrl ? { sender_avatar_url: senderAvatarUrl } : {}),
          ...(roomId ? { room_id: roomId } : {}),
        },
        android: { priority: "HIGH" },
      },
    }),
  });
  const body = await response.json().catch(() => ({}));
  return { ok: response.ok, status: response.status, body };
}

async function getNotificationTargets(notification: any) {
  const query = notification.user_id
    ? `device_tokens?select=token,user_id&user_id=eq.${encodeURIComponent(notification.user_id)}`
    : "device_tokens?select=token,user_id";
  const response = await supabaseRequest(query);
  const rows = await response.json();
  if (!response.ok) throw new Error(`Failed to load device tokens: ${JSON.stringify(rows)}`);
  const seen = new Set<string>();
  return (rows ?? [])
    .map((row: { token?: string; user_id?: string }) => ({ token: row.token?.trim() || "", userId: row.user_id || "" }))
    .filter((target: { token: string }) => {
      if (!target.token || seen.has(target.token)) return false;
      seen.add(target.token); return true;
    });
}

async function getUsernames(userIds: string[]) {
  const ids = [...new Set(userIds.filter(Boolean))];
  if (ids.length === 0) return new Map<string, string>();
  const encoded = ids.map(encodeURIComponent).join(",");
  const response = await supabaseRequest(`chat_profiles?select=user_id,username,display_name&user_id=in.(${encoded})`);
  const rows = await response.json();
  if (!response.ok) throw new Error(`Failed to load notification profiles: ${JSON.stringify(rows)}`);
  return new Map((rows ?? []).map((row: { user_id?: string; username?: string; display_name?: string }) => [
    row.user_id || "", row.username?.trim() || row.display_name?.trim() || "",
  ]));
}

function formatCommunityUsername(username: string) {
  const value = username.trim();
  return value ? value.charAt(0).toUpperCase() + value.slice(1) : "";
}

function personalize(value: string, username: string) {
  if (!username) return value;
  return value.replace(/\{\{username\}\}/gi, username).replace(/\{username\}/gi, username);
}

async function getSenderAvatar(senderId: string | null | undefined) {
  if (!senderId) return null;
  const response = await supabaseRequest(`chat_profiles?select=avatar_url&user_id=eq.${encodeURIComponent(senderId)}&limit=1`);
  const rows = await response.json();
  return response.ok ? rows?.[0]?.avatar_url?.trim() || null : null;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);
  try {
    const webhookSecret = required("FCM_WEBHOOK_SECRET");
    if (req.headers.get("x-kfcc-webhook-secret") !== webhookSecret) return json({ error: "Unauthorized webhook" }, 401);
    const payload = await req.json();
    const { projectId, accessToken } = await getFcmAccessToken();

    let notificationId: string, title: string, message: string, type: string;
    let targets: { token: string; userId: string }[], imageUrl: string | null = null;
    let senderAvatarUrl: string | null = null, roomId: string | null = null;

    if (payload?.type === "INSERT" && payload?.schema === "public" && payload?.table === "app_notifications") {
      const notification = payload.record;
      if (!notification?.id || !notification?.title || !notification?.message) return json({ error: "Invalid notification payload" }, 400);
      notificationId = notification.id;
      title = notification.title;
      message = notification.message;
      type = notification.type ?? "general";
      imageUrl = notification.image_url ?? null;
      roomId = notification.room_id ?? null;
      targets = await getNotificationTargets(notification);
      senderAvatarUrl = await getSenderAvatar(notification.sender_id);
    } else {
      return json({ ok: true, ignored: true });
    }

    if (targets.length === 0) return json({ ok: true, sent: 0, targeted: 0, message: "No registered devices" });

    const usernames = await getUsernames(targets.map((target) => target.userId));
    let sent = 0;
    const invalidTokens: string[] = [];

    for (const target of targets) {
      const username = formatCommunityUsername(usernames.get(target.userId) || "");
      const result = await sendToToken(
        projectId, accessToken, target.token,
        personalize(title, username), personalize(message, username),
        notificationId, type, imageUrl, senderAvatarUrl, roomId,
      );
      if (result.ok) {
        sent++;
      } else {
        const errorText = JSON.stringify(result.body);
        if (result.status === 404 || errorText.includes("UNREGISTERED") || errorText.includes("registration-token-not-registered")) {
          invalidTokens.push(target.token);
        } else {
          console.error("FCM send failed", result.status, result.body);
        }
      }
    }

    for (const token of invalidTokens) {
      await supabaseRequest(`device_tokens?token=eq.${encodeURIComponent(token)}`, { method: "DELETE" });
    }

    return json({ ok: true, notification_id: notificationId, targeted: targets.length, sent, removed_invalid_tokens: invalidTokens.length });
  } catch (error) {
    console.error(error);
    return json({ error: error instanceof Error ? error.message : "FCM delivery failed" }, 500);
  }
});
