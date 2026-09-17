Verify before UI wiring:

- Confirm AdminRepository.login return type and error mapping.
- Keep administrator session in AdminRepository; do not merge it into Supabase auth.
- Keep member session in ChatAuthRepository.
- Replace all raw exception text with safe user-facing messages.
- Add a server-side username resolver if members must sign in without email.
- Run compileDebugKotlin before merging.
