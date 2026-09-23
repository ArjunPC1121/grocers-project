import { useEffect, useState, type FormEvent } from "react";
import toast from "react-hot-toast";
import api, { errorMessage } from "../../config/api";
import { EmptyState, PageError } from "../../components/ApiState";
import { useAuth } from "../../context/AuthContext";
import type { Ticket} from "../../types";
import productApi from "../../config/ProductApi";

type WishlistResponse = {
    id: number;
    userId: number;
    productId: number;
    createdAt: string;
};

type ProductResponse = {
    id: number;
    name: string;
    price: number;
    discount?: number;
    quantity: number;
    imageUrl?: string;
    unitValue?: number;
    unitType?: string;
};

type WishlistProduct = {
    wishlistId: number;
    productId: number;
    name: string;
    price: number;
    imageUrl?: string;
    quantity: number;
    unit: string;
};

export function WishlistPage() {
    const { user } = useAuth();

    const [items, setItems] = useState<WishlistProduct[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const loadWishlist = async () => {
        if (!user) {
            setLoading(false);
            return;
        }

        try {
            setLoading(true);
            setError("");

            const [{ data: wishlist }, { data: products }] = await Promise.all([
                api.get<WishlistResponse[]>(
                    `/users/${user.id}/wishlist`,
                ),
                productApi.get<ProductResponse[]>("/products"),
            ]);

            const productsById = new Map(
                products.map((product) => [product.id, product]),
            );

            const mappedItems = wishlist
                .map((wishlistItem) => {
                    const product = productsById.get(wishlistItem.productId);

                    if (!product) return null;

                    const discount = product.discount || 0;

                    return {
                        wishlistId: wishlistItem.id,
                        productId: product.id,
                        name: product.name,
                        price: product.price * (1 - discount / 100),
                        imageUrl: product.imageUrl,
                        quantity: product.quantity,
                        unit: `${product.unitValue || 1} ${product.unitType || "unit"}`,
                    };
                })
                .filter((item): item is WishlistProduct => item !== null);

            setItems(mappedItems);
        } catch (error: any) {
            setError(
                error.response?.data?.message ||
                "Could not load wishlist.",
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void loadWishlist();
    }, [user?.id]);

    const removeItem = async (productId: number) => {
        if (!user) return;

        try {
            await api.delete(
                `/users/${user.id}/wishlist/${productId}`,
            );

            setItems((previous) =>
                previous.filter((item) => item.productId !== productId),
            );

            toast.success("Removed from wishlist.");
        } catch (error: any) {
            toast.error(
                error.response?.data?.message ||
                "Could not remove product.",
            );
        }
    };

    if (loading) {
        return <section className="mx-auto max-w-5xl p-6">Loading wishlist...</section>;
    }

    return (
        <section className="mx-auto max-w-5xl p-6">
            <h1 className="text-3xl font-serif">My Wishlist</h1>

            {error && <PageError message={error} />}

            {!error && items.length === 0 && (
                <EmptyState
                    title="Your wishlist is empty"
                    detail="Save grocery favourites here for your next shop."
                />
            )}

            <div className="mt-6 grid gap-4 sm:grid-cols-2 md:grid-cols-3">
                {items.map((item) => (
                    <article
                        key={item.wishlistId}
                        className="rounded-2xl border bg-white p-4"
                    >
                        {item.imageUrl && (
                            <img
                                src={item.imageUrl}
                                alt={item.name}
                                className="w-full aspect-square object-contain mb-4"
                            />
                        )}

                        <h2 className="font-semibold text-app-green">
                            {item.name}
                        </h2>

                        <p className="mt-1 text-sm text-app-text-light">
                            ₹{item.price.toFixed(2)} / {item.unit}
                        </p>

                        <p className="mt-1 text-xs text-app-text-light">
                            {item.quantity > 0 ? "In stock" : "Out of stock"}
                        </p>

                        <button
                            onClick={() => void removeItem(item.productId)}
                            className="mt-4 text-sm text-red-600 hover:underline"
                        >
                            Remove from wishlist
                        </button>
                    </article>
                ))}
            </div>
        </section>
    );
}
export function FundsPage() {
    const { user } = useAuth();
    const [balance, setBalance] = useState<number | null>(null);
    const [amount, setAmount] = useState("");
    const [error, setError] = useState("");

    const loadFunds = async () => {
        if (!user) {
            setBalance(null);
            return;
        }

        try {
            setError("");

            const { data } = await api.get(`/users/${user.id}`);
            setBalance(Number(data.funds));
        } catch (error) {
            setError(errorMessage(error, "Could not load your funds."));
        }
    };

    useEffect(() => {
        void loadFunds();
    }, [user?.id]);

    const addFunds = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!user) {
            toast.error("Please sign in to add funds.");
            return;
        }

        try {
            await api.post(`/users/${user.id}/funds`, {
                amount: Number(amount),
            });

            toast.success("Funds added.");
            setAmount("");
            await loadFunds();
        } catch (error) {
            toast.error(errorMessage(error, "Could not add funds."));
        }
    };

    return (
        <section className="mx-auto max-w-5xl p-6">
            <h1 className="text-3xl font-serif">Funds</h1>

            <div className="mt-6 rounded-2xl bg-app-green p-6 text-white">
                <p className="text-sm opacity-70">Available balance</p>
                <p className="mt-2 text-4xl font-semibold">
                    ₹{balance?.toFixed(2) ?? "0.00"}
                </p>
            </div>

            {error && (
                <div className="mt-4">
                    <PageError message={error} />
                </div>
            )}

            <form
                onSubmit={addFunds}
                className="mt-6 rounded-2xl border bg-white p-5"
            >
                <h2 className="font-semibold">Add funds from dummy bank account</h2>

                <input
                    required
                    min="1"
                    type="number"
                    placeholder="Amount"
                    value={amount}
                    onChange={(event) => setAmount(event.target.value)}
                    className="mt-4 w-full rounded-lg border p-3"
                />

                <button className="mt-4 rounded-lg bg-app-orange px-4 py-2 text-white">
                    Add funds
                </button>
            </form>
        </section>
    );
}
export function ProfilePage() {
    const { user, updateUser } = useAuth();

    const [profile, setProfile] = useState({
        firstName: user?.firstName || "",
        lastName: user?.lastName || "",
        email: user?.email || "",
        phoneNumber: user?.phone || "",
        address: user?.address || "",
    });


    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");

    if (!user) return null;

    const saveProfile = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        try {
            const { data } = await api.patch(`/users/${user.id}`, profile);

            updateUser({
                firstName: data.firstName,
                lastName: data.lastName,
                name: `${data.firstName} ${data.lastName}`,
                email: data.email,
                phone: data.phoneNumber,
                address: data.address,
            });

            toast.success("Profile updated.");
        } catch (error) {
            toast.error(errorMessage(error, "Could not update profile."));
        }
    };
    const changePassword = async (
        event: React.FormEvent<HTMLFormElement>,
    ) => {
        event.preventDefault();

        if (newPassword.length < 8) {
            toast.error("New password must be at least 8 characters.");
            return;
        }

        if (newPassword !== confirmPassword) {
            toast.error("New password and confirmation do not match.");
            return;
        }

        try {
            await api.patch(`/users/${user.id}/password`, {
                newPassword,
            });

            setNewPassword("");
            setConfirmPassword("");
            toast.success("Password changed successfully.");
        } catch (error) {
            toast.error(errorMessage(error, "Could not change password."));
        }
    };

    return (
        <section className="mx-auto max-w-2xl p-6">
            <h1 className="text-3xl font-serif">My profile</h1>

            <form
                onSubmit={saveProfile}
                className="mt-6 space-y-4 rounded-2xl border bg-white p-6"
            >
                <h2 className="text-lg font-semibold text-app-green">
                    Personal details
                </h2>

                <div className="grid gap-4 sm:grid-cols-2">
                    <label className="block">
                        First name
                        <input
                            required
                            value={profile.firstName}
                            onChange={(event) =>
                                setProfile({ ...profile, firstName: event.target.value })
                            }
                            className="mt-1 w-full rounded-lg border p-3"
                        />
                    </label>

                    <label className="block">
                        Last name
                        <input
                            required
                            value={profile.lastName}
                            onChange={(event) =>
                                setProfile({ ...profile, lastName: event.target.value })
                            }
                            className="mt-1 w-full rounded-lg border p-3"
                        />
                    </label>
                </div>

                <label className="block">
                    Email
                    <input
                        required
                        type="email"
                        value={profile.email}
                        onChange={(event) =>
                            setProfile({ ...profile, email: event.target.value })
                        }
                        className="mt-1 w-full rounded-lg border p-3"
                    />
                </label>

                <label className="block">
                    Phone number
                    <input
                        required
                        value={profile.phoneNumber}
                        onChange={(event) =>
                            setProfile({ ...profile, phoneNumber: event.target.value })
                        }
                        className="mt-1 w-full rounded-lg border p-3"
                    />
                </label>

                <label className="block">
                    Address
                    <textarea
                        required
                        value={profile.address}
                        onChange={(event) =>
                            setProfile({ ...profile, address: event.target.value })
                        }
                        className="mt-1 min-h-24 w-full rounded-lg border p-3"
                    />
                </label>

                <button className="rounded-lg bg-app-green px-4 py-2 text-white">
                    Save profile
                </button>
            </form>

            <form
                onSubmit={changePassword}
                className="mt-6 space-y-4 rounded-2xl border bg-white p-6"
            >
                <h2 className="text-lg font-semibold text-app-green">
                    Change password
                </h2>




                <label className="block">
                    New password
                    <input
                        required
                        minLength={8}
                        type="password"
                        value={newPassword}
                        onChange={(event) => setNewPassword(event.target.value)}
                        className="mt-1 w-full rounded-lg border p-3"
                    />
                </label>

                <label className="block">
                    Confirm new password
                    <input
                        required
                        minLength={8}
                        type="password"
                        value={confirmPassword}
                        onChange={(event) => setConfirmPassword(event.target.value)}
                        className="mt-1 w-full rounded-lg border p-3"
                    />
                </label>

                <button className="rounded-lg bg-app-orange px-4 py-2 text-white">
                    Change password
                </button>
            </form>
        </section>
    );
}
export function SupportPage() { const [reason,setReason]=useState(""); const [tickets,setTickets]=useState<Ticket[]>([]); useEffect(()=>{api.get("/tickets/mine").then(({data})=>setTickets(Array.isArray(data.tickets)?data.tickets:[])).catch(()=>undefined);},[]); return <section className="mx-auto max-w-3xl p-6"><h1 className="text-3xl font-serif">Support tickets</h1><p className="mt-2 text-app-text-light">Locked account? Tell an employee why access should be restored.</p><form onSubmit={(e)=>{e.preventDefault();api.post("/tickets",{reason}).then(({data})=>{setTickets([data.ticket,...tickets]);setReason("");toast.success("Ticket raised");}).catch((err)=>toast.error(errorMessage(err)));}} className="mt-6 rounded-2xl border bg-white p-5"><textarea required value={reason} onChange={(e)=>setReason(e.target.value)} placeholder="Describe the reason for your unlock request" className="w-full min-h-28 rounded-lg border p-3"/><button className="mt-3 rounded-lg bg-app-green px-4 py-2 text-white">Raise ticket</button></form><div className="mt-6 space-y-3">{tickets.map((ticket)=><article key={ticket.id} className="rounded-xl border bg-white p-4"><span className="text-xs font-semibold text-app-orange">{ticket.status}</span><p>{ticket.reason}</p></article>)}</div></section>; }
