import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { CalendarIcon, PackageIcon } from "lucide-react";
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

const tabs = [
  { label: "All Orders", status: null },
  { label: "Placed", status: "PLACED" },
  { label: "Out for Delivery", status: "OUT_FOR_DELIVERY" },
  { label: "Delivered", status: "DELIVERED" },
];

const formatStatus = (status: string) =>
    status.replaceAll("_", " ");

const MyOrders = () => {
  const currency = import.meta.env.VITE_CURRENCY_SYMBOL || "$";

  const [orders, setOrders] = useState<CustomerOrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeStatus, setActiveStatus] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();

  const { user } = useAuth();
  const { clearCart } = useCart();

  useEffect(() => {
    if (searchParams.get("clearCart")) {
      clearCart();
      setSearchParams({}, { replace: true });
    }
  }, [clearCart, searchParams, setSearchParams]);

  useEffect(() => {
    const loadOrders = async () => {
      if (!user?.id) {
        setOrders([]);
        setLoading(false);
        return;
      }

      setLoading(true);

      try {
        const { data } = await api.get<CustomerOrderSummary[]>(
            `/users/${user.id}/orders`
        );

        setOrders(
            activeStatus
                ? data.filter((order) => order.status === activeStatus)
                : data
        );
      } catch (error: any) {
        toast.error(
            error.response?.data?.message ||
            "Unable to load your orders. Please try again."
        );
      } finally {
        setLoading(false);
      }
    };

    loadOrders();
  }, [activeStatus, user?.id]);

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
                  No orders yet
                </h2>
                <p className="text-sm text-app-text-light mb-4">
                  Checked-out orders will appear here.
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
                    <Link
                        key={order.orderId}
                        to={`/orders/${order.orderId}`}
                        className="block max-w-4xl bg-white rounded-2xl p-5 hover:shadow transition-all"
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
                            }
                        )}
                      </span>
                          </div>
                        </div>

                        <span className="px-3 py-1 text-xs font-medium rounded-full bg-green-100 text-green-800">
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
                    </Link>
                ))}
              </div>
          )}
        </div>
      </div>
  );
};

export default MyOrders;