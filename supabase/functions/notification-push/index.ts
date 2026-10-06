import { createClient } from "npm:@supabase/supabase-js@2";

type WebhookPayload = {
  type: "INSERT" | "UPDATE" | "DELETE";
  table: string;
  schema: string;
  record: {
    id?: string;
    user_id?: string | null;
    title?: string;
    message?: string;
    type?: string;
    image_url?: string | null;
    is_enabled?: boolean;
  } | null;
};

const supabase = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
);

function base64Url(value: Uint8Array | string): string {
  const bytes = typeof value === "string"
    ? new TextEncoder().encode(value)
    : value;
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

async function createGoogleAccessToken(serviceAccount: {
  client_email: string;
  private_key: string;
}): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payload = base64Url(JSON.stringify({
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const unsigned = header + "." + payload;

  const pem = serviceAccount.private_key
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");
  const keyBytes = Uint8Array.from(atob(pem), c => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8",
    keyBytes.buffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = new Uint8Array(
    await crypto.subtle.sign(
      "RSASSA-PKCS1-v1_5",
      key,
      new TextEncoder().encode(unsigned),
    ),
  );
  const assertion = unsigned + "." + base64Url(signature);

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });

  if (!response.ok) {
    throw new Error("Unable to obtain the Firebase access token.");
  }

  const token = await response.json();
  return token.access_token;
}

Deno.serve(async (request) => {
  if (request.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  const expectedSecret = Deno.env.get("KFCC_NOTIFICATION_WEBHOOK_SECRET");
  if (!expectedSecret || request.headers.get("x-kfcc-webhook-secret") !== expectedSecret) {
    return new Response("Unauthorized", { status: 401 });
  }

  const payload = await request.json() as WebhookPayload;
  if (payload.type !== "INSERT" || payload.table !== "app_notifications" || !payload.record) {
    return Response.json({ ignored: true });
  }

  const notification = payload.record;
  if (
    !notification.user_id ||
    notification.is_enabled === false ||
    notification.type === "daily_scripture"
  ) {
    return Response.json({ ignored: true });
  }

  const { data: tokenRows, error } = await supabase
    .from("device_tokens")
    .select("token")
    .eq("user_id", notification.user_id);

  if (error) throw error;

  const tokens = [...new Set(
    (tokenRows ?? []).map((row) => row.token).filter(Boolean),
  )];
  if (tokens.length === 0) {
    return Response.json({ sent: 0 });
  }

  const serviceAccount = JSON.parse(
    Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")!,
  );
  const projectId = Deno.env.get("FIREBASE_PROJECT_ID") || serviceAccount.project_id;
  const accessToken = await createGoogleAccessToken(serviceAccount);

  let sent = 0;
  for (const token of tokens) {
    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
      {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token,
            notification: {
              title: notification.title || "Church Notification",
              body: notification.message || "",
              ...(notification.image_url ? { image: notification.image_url } : {}),
            },
            data: {
              notification_id: notification.id || "",
              title: notification.title || "Church Notification",
              message: notification.message || "",
              type: notification.type || "general",
              ...(notification.image_url ? { image_url: notification.image_url } : {}),
            },
            android: {
              notification: {
                channel_id: "kfcc_church_notifications",
                ...(notification.image_url ? { image: notification.image_url } : {}),
              },
            },
          },
        }),
      },
    );

    if (response.ok) sent += 1;
  }

  return Response.json({ sent, total: tokens.length });
});
