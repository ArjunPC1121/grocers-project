import {
    Heart,
    LogIn,
    Package,
    Search,
    ShoppingBasket,
    Wallet,
} from "lucide-react";
import {Link, useNavigate} from "react-router-dom";
import {useState, type FormEvent} from "react";
import {useAuth} from "../context/AuthContext";
import {useCart} from "../context/CartContext";

export default function Navbar() {
    const {user, logout} = useAuth();
    const {cartCount, setIsCartOpen} = useCart();
    const [query, setQuery] = useState("");
    const navigate = useNavigate();

    const submitSearch = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        const normalizedQuery = query.trim();
        if (normalizedQuery) {
            navigate(`/search?q=${encodeURIComponent(normalizedQuery)}`);
        }
    };

    return (
        <nav className="sticky top-0 z-50 border-b bg-white">
            <div
                className="mx-auto flex max-w-7xl flex-wrap items-center gap-3 px-4 py-3 sm:h-16 sm:flex-nowrap sm:py-0">
                <Link to="/" className="flex items-center gap-2 font-serif text-xl">
                    <ShoppingBasket className="text-app-orange"/>
                    Grocers
                </Link>

                <form
                    onSubmit={submitSearch}
                    className="order-last w-full sm:order-none sm:max-w-md sm:flex-1"
                >
                    <div className="relative">
                        <Search
                            size={16}
                            className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-400"
                        />
                        <input
                            value={query}
                            onChange={(event) => setQuery(event.target.value)}
                            placeholder="Search groceries"
                            aria-label="Search groceries"
                            className="w-full rounded-full bg-orange-50 py-2 pl-9 pr-3 text-sm"
                        />
                    </div>
                </form>

                <div className="ml-auto flex items-center gap-3 text-sm">
                    {user?.role === "CUSTOMER" && (
                        <>
                            <Link to="/orders" title="My Orders">
                                <Package size={19}/>
                            </Link>
                            <Link to="/wishlist" title="Wishlist">
                                <Heart size={19}/>
                            </Link>
                            <Link to="/funds" title="Funds">
                                <Wallet size={19}/>
                            </Link>
                            <button
                                onClick={() => setIsCartOpen(true)}
                                className="rounded-full bg-app-green px-3 py-2 text-white"
                            >
                                Cart {cartCount}
                            </button>
                        </>
                    )}

                    {user ? (
                        <>
                            <Link
                                to={
                                    user.role === "ADMIN"
                                        ? "/admin"
                                        : user.role === "EMPLOYEE"
                                            ? "/employee"
                                            : "/profile"
                                }
                            >
                                {user.name}
                            </Link>
                            <button onClick={logout}>Logout</button>
                        </>
                    ) : (
                        <Link
                            to="/auth"
                            className="flex gap-1 rounded-full bg-app-green px-3 py-2 text-white"
                        >
                            <LogIn size={16}/>
                            Sign in
                        </Link>
                    )}
                </div>
            </div>
        </nav>
    );
}
