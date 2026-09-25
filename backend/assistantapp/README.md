# Grocery Assistant App

This service turns a customer's food request into a catalogue-backed grocery proposal. Gemini creates only a generic recipe ingredient plan; it never receives or selects a product ID. Assistant App then strictly and deterministically matches those requirements against the live Products App catalogue and calculates the minimum number of packs from `unitValue` and `unitType`.

## Run locally

Start Products App first on port `8083`. For local development, create `backend/.env` once and add this line (replace the placeholder with the real key):

```powershell
GEMINI_API_KEY=paste-your-new-key-here
```

Then start Assistant App from its own directory so it loads the parent `backend/.env` file:

```powershell
cd "C:\Users\akash\Documents\Project-grocers\grocers-project\backend\assistantapp"
mvn spring-boot:run
```

Do not paste the key into `application.properties` or commit `backend/.env`. Assistant App runs on port `8092`. Start Gateway on port `8091` too, then use the protected endpoint below with a normal customer JWT:

```http
POST http://localhost:8091/grocers/api/assistant/recommendations
Authorization: Bearer <USER_JWT>
Content-Type: application/json

{
  "message": "Paneer butter masala for 3 people",
  "servings": 3
}
```

`budget` is optional. When omitted, `budget` and `withinBudget` in the response are `null`.

Each item in `recommendedProducts` has either `IN_STOCK` or `OUT_OF_STOCK` status. Out-of-stock items include `availabilityReason`, one of `NO_CATALOG_MATCH`, `INCOMPATIBLE_UNIT`, `INACTIVE_PRODUCT`, or `INSUFFICIENT_STOCK`; unavailable optional ingredients are omitted. `quantity` is the number of product packs required: for example, 1 kg required with a 400 g pack produces a quantity of 3, while 500 g required with a 1 kg pack produces a quantity of 1. The endpoint is intentionally for role `USER` only and does not add anything to a cart.
