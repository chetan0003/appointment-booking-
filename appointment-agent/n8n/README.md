# n8n Workflow — Phase 1 (Meta WhatsApp Cloud API)

## Import
n8n → Workflows → Import from File → `appointment-booking-phase1.json`

## Meta setup (do this first, outside n8n)

1. **Meta App** — [developers.facebook.com](https://developers.facebook.com) →
   create a Business-type app → add the "WhatsApp" product.
2. **Test number** — Meta gives you a free test phone number and a `phone_number_id`
   automatically; you can develop against this before buying a real number.
3. **Access token** — start with the 24-hour temporary token shown in the app
   dashboard to get moving today. Before anything long-running: Business Settings
   → System Users → create a system user → assign your WhatsApp Business Account
   → generate a **permanent** token with `whatsapp_business_messaging` scope.
4. **Webhook subscription** — App Dashboard → WhatsApp → Configuration → set the
   Callback URL to your n8n webhook's production URL (`.../webhook/whatsapp-inbound`)
   and a Verify Token of your choosing. Meta will immediately GET that URL with
   `hub.mode`, `hub.verify_token`, `hub.challenge` — the workflow's **"Meta Verify
   Handshake"** node handles this and must be reachable (publicly, not localhost —
   use a tunnel like ngrok while developing) before Meta will accept the subscription.
5. Subscribe to the **`messages`** field specifically (not the whole account) so you
   only get inbound message events, not every status callback.

## n8n environment variables to set

| Variable | Value |
|---|---|
| `SPRING_BOOT_BASE_URL` | e.g. `http://localhost:8080` (or wherever it's deployed) |
| `ANTHROPIC_API_KEY` | your Claude API key |
| `WHATSAPP_ACCESS_TOKEN` | the system user permanent token from step 3 |
| `WHATSAPP_PHONE_NUMBER_ID` | from the Meta app dashboard (not the phone number itself) |

`CLINIC_ID` is hardcoded to `1` (ABC Dental Clinic, matches the seed data) in the
"Extract Message" node — change it there if you seeded a different clinic.

## Why there are two webhook nodes

Meta hits the same URL with two different HTTP methods for two different purposes:
**GET** once, to verify you own the endpoint (must echo back `hub.challenge`), and
**POST** on every actual event after that. n8n binds one method per Webhook node, so
the workflow has "Meta Verify Handshake" (GET) alongside "WhatsApp Inbound" (POST),
both on the same path. The POST path also immediately fires "Ack Meta Immediately" —
Meta requires a 200 response within 5 seconds, and the AI call can occasionally take
longer than that, so acknowledging first and continuing processing async avoids Meta
marking your webhook unhealthy and retrying (which would create duplicate inbound
events — the `waMessageId`-based idempotency key on booking is your backstop against
that turning into a duplicate appointment).

## Non-message events

Meta's webhook also delivers delivery/read status callbacks on the same URL. The
"Extract Message" node returns an empty array when there's no `messages[]` in the
payload, which stops the branch cleanly without erroring.

## Flow shape (matches doc section 17)

```
Webhook (WhatsApp inbound)
  -> Extract Message (Code node: pull phone + text out of the provider payload)
  -> Get Active Session (HTTP GET /api/clinics/1/sessions/active)
  -> Get Services + Get Doctors (HTTP GET, parallel — context for the AI)
  -> AI Agent (LLM w/ system prompt enforcing doc section 19 rules)
       reads: session state + message text + services/doctors list
       outputs: {reply_to_patient, session_updates, action}
       action is one of: ASK_MORE_INFO | CHECK_AVAILABILITY | CONFIRM_BOOKING | NONE
  -> Update Session (HTTP PATCH /api/sessions/{id}, always runs)
  -> Switch on `action`:
       CHECK_AVAILABILITY -> HTTP GET /availability -> AI formats slot list -> reply
       CONFIRM_BOOKING    -> HTTP POST /api/appointments
                               success -> AI formats confirmation -> reply
                               409 (SlotUnavailableException) -> HTTP GET availability
                                 again -> AI offers fresh alternatives -> reply
       ASK_MORE_INFO / NONE -> reply is already in AI Agent's output -> send as-is
  -> WhatsApp Send (reply to patient)
```

## Why the AI node runs *before* Update Session, and Update Session always runs

The AI decides what changed in the conversation (e.g. "doctor = Dr Sharma") but
never writes to the DB itself — Update Session is the only place session state
changes, keeping n8n's role limited to orchestration per doc section 5.2 ("n8n
should NOT contain the core appointment business rules"). Availability and
booking still go through Spring Boot's authoritative checks regardless of what
the AI believes is available.
