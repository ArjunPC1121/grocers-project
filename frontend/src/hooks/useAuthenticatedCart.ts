import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import type { Product } from "../types";

export function useAuthenticatedCart() {
  const { user } = useAuth();
  const { addToCart } = useCart();
  const location = useLocation();
  const navigate = useNavigate();

  // Remember the current page so a visitor can continue shopping after sign-in.
  return (product: Product, quantity = 1) => {
    if (!user || user.role !== "CUSTOMER") {
      navigate("/auth", {
        state: {
          returnTo: location.pathname + location.search + location.hash,
        },
      });
      return;
    }

    addToCart(product, quantity);
  };
}
