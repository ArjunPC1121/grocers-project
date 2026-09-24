import { CookingPot, Heart,
    LogIn,
    Package,
    Search,
    Wallet,
} from "lucide-react";
import {Link, useNavigate} from "react-router-dom";
import {useState, type FormEvent} from "react";
import {useAuth} from "../context/AuthContext";
import {useCart} from "../context/CartContext";
import {categorySlugForExactSearch} from "../config/ProductApi";

export default function Navbar() {
    const {user, logout} = useAuth();
    const {cartCount, setIsCartOpen} = useCart();
    const [query, setQuery] = useState("");
    const navigate = useNavigate();

    const submitSearch = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        const normalizedQuery = query.trim();
        if (normalizedQuery) {
            const categorySlug = categorySlugForExactSearch(normalizedQuery);
            navigate(categorySlug
                ? `/products?category=${encodeURIComponent(categorySlug)}`
                : `/search?q=${encodeURIComponent(normalizedQuery)}`);
        }
    };

    return (
        <nav className="sticky top-0 z-50 border-b bg-white">
            <div
                className="mx-auto flex max-w-7xl flex-wrap items-center gap-3 px-4 py-3 sm:h-16 sm:flex-nowrap sm:py-0">
                <Link to="/" className="flex items-center gap-2 font-serif text-xl">
                    <img src="/main_logo.png" alt="Grocers" className="size-6 object-contain"/>
                    Grocers
                </Link>

                <div className="order-last flex w-full gap-2 sm:order-none sm:max-w-xl sm:flex-1">
          <form onSubmit={submitSearch} className="min-w-0 flex-1">
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
          {user?.role === "CUSTOMER" && (
            <button
              type="button"
              onClick={() => navigate(`/recipe-assistant${query.trim() ? `?recipe=${encodeURIComponent(query.trim())}` : ""}`)}
              className="inline-flex shrink-0 items-center gap-1.5 rounded-full border border-app-orange bg-orange-50 px-3 py-2 text-xs font-semibold text-app-orange transition-colors hover:bg-app-orange hover:text-white"
            >
              <CookingPot className="size-3.5" />
              Plan a recipe
            </button>
          )}
        </div>

                <div className="ml-auto flex items-center gap-3 text-sm">
                    {user?.role === "CUSTOMER" && (
                        <>
                            <Link to="/orders" title="My orders">
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
