import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import api, { errorMessage } from "../../config/api";
import { EmptyState, PageError } from "../../components/ApiState";
import { useAuth } from "../../context/AuthContext";
import type { Ticket, Wallet, WalletTransaction } from "../../types";
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
export function FundsPage() { const [wallet,setWallet]=useState<Wallet|null>(null); const [transactions,setTransactions]=useState<WalletTransaction[]>([]); const [amount,setAmount]=useState(""); const [accountNumber,setAccountNumber]=useState(""); const [error,setError]=useState(""); const load=()=>api.get("/wallet").then(({data})=>{setWallet(data.wallet);setTransactions(Array.isArray(data.transactions)?data.transactions:[]);}).catch(()=>setError("Funds are unavailable until the Spring API is connected.")); useEffect(()=>{load();},[]); return <section className="mx-auto max-w-5xl p-6"><h1 className="text-3xl font-serif">Funds</h1><div className="mt-6 rounded-2xl bg-app-green p-6 text-white"><p className="text-sm opacity-70">Available balance</p><p className="text-4xl font-semibold mt-2">₹{wallet?.availableBalance?.toFixed(2) ?? "0.00"}</p></div>{error&&<div className="mt-4"><PageError message={error}/></div>}<form onSubmit={(e)=>{e.preventDefault();api.post("/wallet/fund",{accountNumber,amount:Number(amount)}).then(()=>{toast.success("Funds added");setAmount("");load();}).catch((err)=>toast.error(errorMessage(err)));}} className="mt-6 rounded-2xl border bg-white p-5"><h2 className="font-semibold">Add funds from dummy bank account</h2><div className="mt-4 grid gap-3 sm:grid-cols-2"><input required placeholder="Account number" value={accountNumber} onChange={(e)=>setAccountNumber(e.target.value)} className="rounded-lg border p-3"/><input required min="1" type="number" placeholder="Amount" value={amount} onChange={(e)=>setAmount(e.target.value)} className="rounded-lg border p-3"/></div><button className="mt-4 rounded-lg bg-app-orange px-4 py-2 text-white">Add funds</button></form><div className="mt-6">{transactions.map((item)=><div key={item.id} className="border-b py-3 flex justify-between"><span>{item.note||item.type}</span><strong>₹{item.amount.toFixed(2)}</strong></div>)}</div></section>; }
export function ProfilePage() { const {user,updateUser}=useAuth(); const [email,setEmail]=useState(user?.email||""); const [phone,setPhone]=useState(user?.phone||""); const [password,setPassword]=useState(""); return <section className="mx-auto max-w-2xl p-6"><h1 className="text-3xl font-serif">My profile</h1><form onSubmit={(e)=>{e.preventDefault();api.put("/profile",{email,phone,password:password||undefined}).then(({data})=>{updateUser(data.user||{email,phone});toast.success("Profile updated");setPassword("");}).catch((err)=>toast.error(errorMessage(err)));}} className="mt-6 space-y-4 rounded-2xl border bg-white p-6"><label className="block">Email<input required value={email} onChange={(e)=>setEmail(e.target.value)} className="mt-1 w-full border rounded-lg p-3"/></label><label className="block">Phone<input value={phone} onChange={(e)=>setPhone(e.target.value)} className="mt-1 w-full border rounded-lg p-3"/></label><label className="block">New password<input type="password" value={password} onChange={(e)=>setPassword(e.target.value)} className="mt-1 w-full border rounded-lg p-3"/></label><button className="rounded-lg bg-app-green px-4 py-2 text-white">Save changes</button></form></section>; }
export function SupportPage() { const [reason,setReason]=useState(""); const [tickets,setTickets]=useState<Ticket[]>([]); useEffect(()=>{api.get("/tickets/mine").then(({data})=>setTickets(Array.isArray(data.tickets)?data.tickets:[])).catch(()=>undefined);},[]); return <section className="mx-auto max-w-3xl p-6"><h1 className="text-3xl font-serif">Support tickets</h1><p className="mt-2 text-app-text-light">Locked account? Tell an employee why access should be restored.</p><form onSubmit={(e)=>{e.preventDefault();api.post("/tickets",{reason}).then(({data})=>{setTickets([data.ticket,...tickets]);setReason("");toast.success("Ticket raised");}).catch((err)=>toast.error(errorMessage(err)));}} className="mt-6 rounded-2xl border bg-white p-5"><textarea required value={reason} onChange={(e)=>setReason(e.target.value)} placeholder="Describe the reason for your unlock request" className="w-full min-h-28 rounded-lg border p-3"/><button className="mt-3 rounded-lg bg-app-green px-4 py-2 text-white">Raise ticket</button></form><div className="mt-6 space-y-3">{tickets.map((ticket)=><article key={ticket.id} className="rounded-xl border bg-white p-4"><span className="text-xs font-semibold text-app-orange">{ticket.status}</span><p>{ticket.reason}</p></article>)}</div></section>; }
