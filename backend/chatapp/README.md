# ChatApp

ChatApp provides authenticated customer-care conversations for Grocers.

## Workflow

1. A signed-in customer opens **Support** and starts a chat.
2. ChatApp creates a `QUEUED` session.
3. Employees who mark themselves `AVAILABLE` receive an in-app assignment alert.
4. The first employee to accept owns the chat; their status becomes `BUSY`.
5. Both participants exchange persisted messages. The UI refreshes every 1.8 seconds for near-real-time delivery.
6. Either participant can end the chat. The employee becomes `AVAILABLE` again.

## Start order

Start EmployeeApp, GatewayApp, then ChatApp. ChatApp runs on port `8094` and is exposed through GatewayApp at `/grocers/api/chats/**`.

The schema is created automatically through JPA. It adds `support_chat_sessions`, `support_chat_messages`, and `employee_chat_availability`.
