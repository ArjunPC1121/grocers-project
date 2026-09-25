import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { CalendarIcon, ChevronDownIcon, PackageIcon } from "lucide-react";
import toast from "react-hot-toast";

import { useCart } from "../context/CartContext";
import Loading from "../components/Loading";
import api from "../config/api";
import { useAuth } from "../context/AuthContext";

type CustomerOrderSummary = {
    orderId: number;
    orderNumber: string;
    totalAmount: number;
    status: string;
    checkedOutAt: string;
};

type OrderItem = {
    id: number;
    productId: number;
    productName?: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
};

type BackendOrder = {
    id: number;
    orderNumber: string;
    totalAmount: number;
    status: string;
    orderedAt: string;
    deliveryAddress?: string;
    paymentMethod?: string;
    cancellationReason?: string;
    items?: OrderItem[];
};

const tabs = [
    { label: "All Orders", status: null },
    { label: "Placed", status: "PLACED" },
    { label: "Shipped", status: "SHIPPED" },
    { label: "Out for Delivery", status: "OUT_FOR_DELIVERY" },
    { label: "Delivered", status: "DELIVERED" },
];

const formatStatus = (status: string) => status.replaceAll("_", " ");

const MyOrders = () => {
    const currency = "₹";

    const [orders, setOrders] = useState<CustomerOrderSummary[]>([]);
    const [loading, setLoading] = useState(true);
    const [activeStatus, setActiveStatus] = useState<string | null>(null);
    const [expandedOrderId, setExpandedOrderId] = useState<number | null>(null);
    const [orderDetails, setOrderDetails] = useState<Record<number, BackendOrder>>({});
    const [detailsLoadingId, setDetailsLoadingId] = useState<number | null>(null);
    const [searchParams, setSearchParams] = useSearchParams();

    const { user } = useAuth();
    const { clearCart } = useCart();

    // Checkout may link here with this flag; clear the visual cart once and remove the flag from the URL.
    useEffect(() => {
        if (searchParams.get("clearCart")) {
            clearCart();
            setSearchParams({}, { replace: true });
        }
    }, [clearCart, searchParams, setSearchParams]);

    // The order list is the Kafka-backed summary stored by UserApp.
    useEffect(() => {
        const loadOrders = async () => {
            if (!user?.id) {
                setOrders([]);
                setOrderDetails({});
                setLoading(false);
                return;
            }

            setLoading(true);

            try {
                const { data } = await api.get<CustomerOrderSummary[]>(
                    `/users/${user.id}/orders`,
                );

                setOrders(
                    activeStatus
                        ? data.filter(
                            (order) => order.status === activeStatus,
                        )
                        : data,
                );
            } catch (error: any) {
                toast.error(
                    error.response?.data?.message ||
                    "Unable to load your orders. Please try again.",
                );
            } finally {
                setLoading(false);
            }
        };

        void loadOrders();
    }, [activeStatus, user?.id]);

    const toggleOrderDetails = async (orderId: number) => {
        if (expandedOrderId === orderId) {
            setExpandedOrderId(null);
            return;
        }

        setExpandedOrderId(orderId);

        if (orderDetails[orderId]) return;

        try {
            setDetailsLoadingId(orderId);
            const { data } = await api.get<BackendOrder>(`/orders/${orderId}`);
            setOrderDetails((previous) => ({ ...previous, [orderId]: data }));
        } catch (error: any) {
            toast.error(
                error.response?.data?.message ||
                "Unable to load this order's details. Please try again.",
            );
        } finally {
            setDetailsLoadingId(null);
        }
    };

    // Ask for a reason because it is shown to staff handling the cancellation.
    const cancelOrder = async (order: CustomerOrderSummary) => {
        const reason = window.prompt("Why would you like to cancel this order?");

        if (!reason?.trim()) return;

        try {
            const { data: cancelledOrder } = await api.post(
                `/orders/${order.orderId}/cancel`,
                { reason: reason.trim() },
            );

            setOrders((previous) =>
                previous
                    .map((item) =>
                        item.orderId === order.orderId
                            ? { ...item, status: cancelledOrder.status }
                            : item,
                    )
                    .filter((item) => !activeStatus || item.status === activeStatus),
            );

            toast.success("Order cancelled successfully.");
        } catch (error: any) {
            toast.error(
                error.response?.data?.message ||
                "Could not cancel this order. Please try again.",
            );
        }
    };

    return (
        <div className="min-h-screen bg-app-cream mb-20">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <h1 className="text-2xl font-semibold text-app-green mb-6">
                    My Orders
                </h1>

                <div className="flex gap-2 mb-6 overflow-x-auto pb-2">
                    {tabs.map((tab) => (
                        <button
                            key={tab.label}
                            onClick={() => setActiveStatus(tab.status)}
                            className={`px-4 py-2 text-sm font-medium rounded-xl whitespace-nowrap transition-colors ${
                                activeStatus === tab.status
                                    ? "bg-app-green text-white"
                                    : "bg-white text-app-text-light hover:bg-app-cream"
                            }`}
                        >
                            {tab.label}
                        </button>
                    ))}
                </div>

                {loading ? (
                    <Loading />
                ) : orders.length === 0 ? (
                    <div className="text-center py-16">
                        <PackageIcon className="size-16 text-app-border mx-auto mb-4" />
                        <h2 className="text-lg font-medium text-app-green mb-2">
                            No orders found
                        </h2>
                        <p className="text-sm text-app-text-light mb-4">
                            Your placed orders will appear here.
                        </p>
                        <Link
                            to="/products"
                            className="inline-flex px-4 py-2 bg-app-green text-white text-sm rounded-lg"
                        >
                            Start Shopping
                        </Link>
                    </div>
                ) : (
                    <div className="space-y-4">
                        {orders.map((order) => (
                            (() => {
                                const isExpanded = expandedOrderId === order.orderId;
                                const isCancelled = order.status === "CANCELLED";
                                const details = orderDetails[order.orderId];
                                const isDetailsLoading = detailsLoadingId === order.orderId;

                                return (
                            <article
                                key={order.orderId}
                                className="max-w-4xl bg-white rounded-2xl p-5"
                            >
                                <div className="flex items-start justify-between gap-4">
                                    <div>
                                        <p className="text-sm font-medium text-app-green">
                                            {order.orderNumber}
                                        </p>

                                        <div className="flex items-center gap-2 mt-1">
                                            <CalendarIcon className="size-3 text-app-text-light" />
                                            <span className="text-xs text-app-text-light">
                        {new Date(order.checkedOutAt).toLocaleDateString(
                            "en-US",
                            {
                                month: "short",
                                day: "numeric",
                                year: "numeric",
                            },
                        )}
                      </span>
                                        </div>
                                    </div>

                                    <span className={`px-3 py-1 text-xs font-medium rounded-full ${isCancelled ? "bg-red-100 text-red-700" : "bg-green-100 text-green-800"}`}>
                    {formatStatus(order.status)}
                  </span>
                                </div>

                                <div className="mt-4 pt-3 border-t border-app-border flex justify-between items-center text-sm">
                                    <span className="text-app-text-light">Order total</span>
                                    <span className="font-semibold text-app-green">
                    {currency}
                                        {Number(order.totalAmount).toFixed(2)}
                  </span>
                                </div>

                                <button
                                    type="button"
                                    aria-expanded={isExpanded}
                                    onClick={() => void toggleOrderDetails(order.orderId)}
                                    className="mt-4 inline-flex items-center gap-2 text-sm font-medium text-app-green hover:text-app-green/80"
                                >
                                    {isExpanded ? "Hide details" : "View details"}
                                    <ChevronDownIcon className={`size-4 transition-transform ${isExpanded ? "rotate-180" : ""}`} />
                                </button>

                                {isExpanded && (
                                    <div className="mt-4 border-t border-app-border pt-4">
                                        <h2 className="text-sm font-semibold text-app-green">Order details</h2>
                                        {isDetailsLoading ? (
                                            <p className="mt-3 text-sm text-app-text-light">Loading order details...</p>
                                        ) : details?.items?.length ? (
                                            <ul className="mt-3 divide-y divide-app-border rounded-xl border border-app-border">
                                                {details.items.map((item) => (
                                                    <li key={item.id ?? item.productId} className="flex items-center justify-between gap-4 p-3 text-sm">
                                                        <div>
                                                            <p className="font-medium text-app-text">{item.productName || `Product #${item.productId}`}</p>
                                                            <p className="mt-0.5 text-xs text-app-text-light">{item.quantity} × {currency}{Number(item.unitPrice).toFixed(2)}</p>
                                                        </div>
                                                        <span className="shrink-0 font-medium text-app-green">{currency}{Number(item.subtotal).toFixed(2)}</span>
                                                    </li>
                                                ))}
                                            </ul>
                                        ) : (
                                            <p className="mt-3 text-sm text-app-text-light">Product details are unavailable for this order.</p>
                                        )}

                                        <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
                                            <div><dt className="text-app-text-light">Total price</dt><dd className="mt-1 font-semibold text-app-green">{currency}{Number(order.totalAmount).toFixed(2)}</dd></div>
                                            {details?.paymentMethod && <div><dt className="text-app-text-light">Payment method</dt><dd className="mt-1 font-medium text-app-text">{formatStatus(details.paymentMethod)}</dd></div>}
                                            {details?.deliveryAddress && <div className="sm:col-span-2"><dt className="text-app-text-light">Delivery address</dt><dd className="mt-1 font-medium text-app-text">{details.deliveryAddress}</dd></div>}
                                            {isCancelled && details?.cancellationReason && <div className="sm:col-span-2"><dt className="text-red-600">Cancellation reason</dt><dd className="mt-1 text-red-700">{details.cancellationReason}</dd></div>}
                                        </dl>
                                    </div>
                                )}

                                {order.status === "PLACED" && (
                                    <div className="mt-4 border-t border-app-border pt-4">
                                        <button
                                            type="button"
                                            onClick={() => void cancelOrder(order)}
                                            className="rounded-lg border border-red-600 px-4 py-2 text-sm font-medium text-red-600 transition-colors hover:bg-red-50"
                                        >
                                            Cancel Order
                                        </button>
                                    </div>
                                )}
                            </article>
                                );
                            })()
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default MyOrders;
