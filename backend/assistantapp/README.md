# Grocery Assistant App

This service turns a customer's food request into a catalogue-backed grocery proposal. Gemini only returns generic ingredients. Assistant App fetches the live Products App catalogue and returns only products that actually exist and have enough stock; it calculates the discounted price itself.

## Run locally

Start Products App first on port `8083`, then set the Gemini key in the PowerShell window that will start Assistant App:

```powershell
$env:GEMINI_API_KEY = "paste-your-new-key-here"
cd "C:\Users\Pranav Patil\Desktop\Project\grocers-project\backend\assistantapp"
mvn spring-boot:run
```

Do not paste the key into `application.properties` or commit it. Assistant App runs on port `8092`. Start Gateway on port `8091` too, then use the protected endpoint below with a normal customer JWT:

```http
POST http://localhost:8091/grocers/api/assistant/recommendations
Authorization: Bearer <USER_JWT>
Content-Type: application/json

{
  "message": "Paneer butter masala for 3 people",
  "budget": 700,
  "servings": 3
}
```

The endpoint is intentionally for role `USER` only. It does not add anything to a cart; the frontend should show the returned products and allow the customer to choose what to add.
