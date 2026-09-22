export type Role = "CUSTOMER" | "EMPLOYEE" | "ADMIN";
export interface Address { id: string; label: string; address: string; city: string; state: string; zip: string; isDefault: boolean; lat?: number; lng?: number; }
export interface SessionUser { id: string; firstName: string; lastName: string; name: string; email: string;address?: string; employeeId?: string; phone?: string; role: Role; locked?: boolean; mustChangePassword?: boolean; addresses?: Address[]; }
export type User = SessionUser;
export interface Product { id: string; name: string; description: string; price: number; originalPrice?: number; image?: string; category: string; unit: string; stock: number; discount?: number; rating?: number; reviewCount?: number; demandCount?: number; createdAt?: string; }
export interface CartItem { product: Product; quantity: number; }
export interface Cart { items: CartItem[]; subtotal: number; }
export interface WishlistItem { id: string; product: Product; }
export interface Wallet { availableBalance: number; accountNumber?: string; }
export interface WalletTransaction { id: string; amount: number; type: "CREDIT" | "DEBIT" | "REFUND"; createdAt: string; note?: string; }
export interface OrderItem { product: string; name: string; image?: string; price: number; quantity: number; unit: string; }
export interface Order { id: string; user: string | Pick<SessionUser, "id" | "name" | "email">; items: OrderItem[]; shippingAddress: Omit<Address, "id" | "isDefault">; subtotal: number; deliveryFee?: number; tax?: number; total: number; status: "PLACED" | "SHIPPED" | "OUT_FOR_DELIVERY" | "DELIVERED" | "CANCELLED"; cancellationReason?: string; refundedAmount?: number; statusHistory: { status: string; timestamp: string; note?: string }[]; createdAt: string; }
export interface Ticket { id: string; user: Pick<SessionUser, "id" | "name" | "email">; reason: string; status: "OPEN" | "RESOLVED"; createdAt: string; }
export interface Employee { id: string; employeeId: string; firstName: string; lastName: string; name: string; email: string; active: boolean; }
export interface InventoryRequest { id: string; productId?: string; productName: string; requestType: "ADD" | "UPDATE" | "DELETE" | "INCREASE_QUANTITY"; quantity?: number; note: string; status: "PENDING" | "APPROVED" | "REJECTED" | "RESOLVED"; createdAt: string; }
export interface ReportResult { summary: Record<string, number>; rows: Array<Record<string, string | number>>; }
