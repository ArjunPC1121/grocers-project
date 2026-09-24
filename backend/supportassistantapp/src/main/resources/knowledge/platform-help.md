# Grocers platform help

## Products and search
Customers can browse all products from Products or search by product name, brand, category, subcategory, or description. Product pages show the current price, discount, pack size, availability, and stock-backed purchase controls. Live product information must come from Products App. The Deals page shows discounted products. Product path: /products. Deals path: /deals.

## Recipe Planner
Recipe Planner is a separate Grocers feature. Customers open Plan a Recipe at /recipe-assistant, enter a dish, servings, and an optional budget, then receive catalogue-backed ingredient recommendations. The Support Assistant must not generate a recipe plan itself; it should send the customer to Recipe Planner.

## Cart and checkout
Signed-in customers can add available products to their cart, adjust quantities, remove items, and proceed to Checkout. Checkout confirms the delivery address and payment flow before placing an order. Cart changes use current product stock and may fail if inventory changed. Checkout path: /checkout.

## Orders and delivery
Customers view their own orders at /orders and open an individual order to see status and tracking information. Normal progress is PLACED, SHIPPED, OUT FOR DELIVERY, then DELIVERED. Cancelled orders show CANCELLED. An order can only be cancelled when the backend allows it; the assistant must not promise cancellation before the server confirms it.

## Account, profile, and password
Customers manage personal information at /profile. Password changes require the current password and a valid new password. The assistant never asks for, stores, or displays passwords or security answers. Employee profile is at /employee/profile. Admin profile is at /admin/profile.

## Locked account recovery
A customer locked after failed sign-in attempts can use secure account recovery. The customer answers the security question at /recover-account. If self-service recovery is unavailable, they can submit an employee-reviewed unlock request at /unlock-account. The assistant can explain and navigate to recovery but must never collect the secret answer in chat.

## Funds
Customers view available funds at /funds. Funds are private and may only be shown for the authenticated customer. Adding funds follows the Funds page workflow. The assistant never displays another customer's balance and never claims that money moved unless the backend confirms it.

## Wishlist
Signed-in customers manage saved products at /wishlist. They can add products from catalogue screens and remove them from Wishlist. Wishlist data is private to the authenticated customer.

## Customer support
The Support page is available at /support. When the assistant cannot solve a problem, it should offer this page or the appropriate locked-account recovery flow. It must clearly say when a live service is unavailable instead of inventing data.

## Employee workspace
Employees use /employee for their operations overview. Product Requests lets an employee submit and track catalogue create, update, and delete requests. Order Operations lets an employee progress assigned orders. Account Tickets contains customer unlock requests. Employees may only access employee-authorized information and their own request history.

## Admin workspace
Admins use /admin. They can manage products, customers, employees, requests, orders, reports, and their own profile. The Requests screen reviews employee product requests. Reports provides live period and filter based results. Normal admins cannot manage other admin accounts.

## Super admin access
Only the Super Admin can view and manage Admin accounts. The backend determines Super Admin status; the assistant must never infer it from a message. Removing or changing an admin remains a dedicated Admin Accounts screen operation.
