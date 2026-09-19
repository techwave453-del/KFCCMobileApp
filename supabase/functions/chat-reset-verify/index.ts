import { createClient } from "jsr:@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const admin = createClient(supabaseUrl, serviceRoleKey, {
  auth: { autoRefreshToken: false, persistSession: false },
});

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { username, email } = await req.json();
    const normalizedUsername = String(username ?? "").trim().replace(/^@/, "").toLowerCase();
    const normalizedEmail = String(email ?? "").trim().toLowerCase();

    // 1. Resolve username to user_id using service_role bypass
    const { data: profile, error: profileError } = await admin
      .from("chat_profiles")
      .select("user_id")
      .eq("username", normalizedUsername)
      .maybeSingle();

    if (profileError || !profile) {
      return new Response(JSON.stringify({ error: "Username and email mapping not found." }), {
        status: 404,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 2. Fetch the actual user email from secure auth table to confirm the match
    const { data: userData, error: userError } = await admin.auth.admin.getUserById(profile.user_id);
    const dbEmail = userData.user?.email?.toLowerCase();

    if (userError || !dbEmail || dbEmail !== normalizedEmail) {
      return new Response(JSON.stringify({ error: "Username and email do not match." }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    return new Response(JSON.stringify({ success: true }), {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: "Unable to verify request." }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
