# Grocers Help Assistant

This service powers the role-aware Grocers platform assistant. It is intentionally separate from `assistantapp`, which continues to power the Recipe Planner.

## What it does

- Answers questions about using the Grocers platform from the local help knowledge base.
- Retrieves read-only, live information allowed for the signed-in user, employee, or admin.
- Returns structured product, cart, order, request, report, and account cards for the frontend chat panel.
- Sends recipe questions to the separate Recipe Planner instead of answering them here.
- Uses a deterministic help response if Gemini is not configured or is temporarily unavailable.
- Retains a short, per-user conversation history so references such as “add that product” can be resolved safely.
- Performs only narrowly scoped actions when one exact target is identified: add a named product to a user's cart or wishlist, progress one employee order, or approve/reject one admin product request.

The assistant never accepts passwords, security answers, access tokens, or payment credentials. It refuses ambiguous actions and returns a page link when more input is needed.

## Configuration

The service reads `GEMINI_API_KEY` from `backend/.env`, just like the existing recipe assistant. The optional `SUPPORT_GEMINI_MODEL` setting can select a different model for this assistant.

```env
GEMINI_API_KEY=your_key_here
SUPPORT_GEMINI_MODEL=gemini-3.5-flash-lite
```

If no Gemini key is present, platform retrieval, live cards, navigation, and deterministic help answers still work without an AI API call.

## Run

Start the dependent Grocers services, then run this service on port `8093`:

```powershell
cd backend\supportassistantapp
mvn spring-boot:run
```

The gateway exposes the authenticated endpoint at:

```text
POST http://localhost:8091/grocers/api/support-assistant/chat
```

Example body:

```json
{
  "message": "Where is my latest order?",
  "conversationId": "optional-existing-conversation-id"
}
```

The response contains a natural-language `reply`, optional structured `cards`, navigation or follow-up `actions`, and non-sensitive `sources`. The frontend continues the conversation by returning the same `conversationId`.

## Role-aware actions

| Signed-in role | Supported action | Safety rule |
| --- | --- | --- |
| User | Add one unambiguous product to cart or wishlist | The product name must resolve to exactly one catalogue item. |
| Employee | Progress one order to shipped, out for delivery, or delivered | The order number or customer must identify exactly one assigned/available order. |
| Admin | Approve or reject one pending product request | Product and request type must identify exactly one pending request; rejection needs a reason. |

Actions are sent through trusted service-to-service calls with the caller identity supplied by GatewayApp. If any dependent service is unavailable, no partial action is reported as successful.

## API and operating notes

`POST /grocers/api/support-assistant/chat` is authenticated for `USER`, `EMPLOYEE`, and `ADMIN` roles. The request body requires a nonblank `message` up to the DTO limit; `conversationId` is optional.

The service does not own a database table. Conversation memory is intentionally in-process and short-lived, so it resets on application restart. The local knowledge base is in `src/main/resources/knowledge/platform-help.md`.
