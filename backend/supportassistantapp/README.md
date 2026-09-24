# Grocers Help Assistant

This service powers the role-aware Grocers platform assistant. It is intentionally separate from `assistantapp`, which continues to power the Recipe Planner.

## What it does

- Answers questions about using the Grocers platform from the local help knowledge base.
- Retrieves read-only, live information allowed for the signed-in user, employee, or admin.
- Returns structured product, cart, order, request, report, and account cards for the frontend chat panel.
- Sends recipe questions to the separate Recipe Planner instead of answering them here.
- Uses a deterministic help response if Gemini is not configured or is temporarily unavailable.

The assistant never performs mutations and never receives passwords, security answers, access tokens, or payment credentials.

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
