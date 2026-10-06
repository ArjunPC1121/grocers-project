// import {
//   createContext,
//   useContext,
//   useEffect,
//   useState,
//   type ReactNode,
// } from "react";
// import type { CartItem, Product } from "../types";
//
// interface CartContextType {
//   items: CartItem[];
//   addToCart: (product: Product, quantity?: number) => void;
//   removeFromCart: (productId: string) => void;
//   updateQuantity: (productId: string, quantity: number) => void;
//   clearCart: () => void;
//   cartCount: number;
//   cartTotal: number;
//   isCartOpen: boolean;
//   setIsCartOpen: (open: boolean) => void;
// }
//
// const CartContext = createContext<CartContextType | undefined>(undefined);
//
// export function CartProvider({ children }: { children: ReactNode }) {
//   const [items, setItems] = useState<CartItem[]>(() => {
//     const saved = localStorage.getItem("app_cart");
//     return saved ? JSON.parse(saved) : [];
//   });
//
//   const [isCartOpen, setIsCartOpen] = useState(false);
//
//   useEffect(() => {
//     localStorage.setItem("app_cart", JSON.stringify(items));
//   }, [items]);
//
//   const addToCart = (product: Product, quantity = 1) => {
//     setItems((prev) => {
//       const existing = prev.find((item) => item.product.id === product.id);
//       if (existing) {
//         return prev.map((item) =>
//           item.product.id === product.id
//             ? { ...item, quantity: item.quantity + quantity }
//             : item,
//         );
//       }
//       return [...prev, { product, quantity }];
//     });
//     setIsCartOpen(true);
//   };
//
//   const removeFromCart = (productId: string) => {
//     setItems((prev) => prev.filter((item) => item.product.id !== productId));
//   };
//
//   const updateQuantity = (productId: string, quantity: number) => {
//     if (quantity <= 0) {
//       removeFromCart(productId);
//       return;
//     }
//     setItems((prev) =>
//       prev.map((item) =>
//         item.product.id === productId ? { ...item, quantity } : item,
//       ),
//     );
//   };
//
//   const clearCart = () => {
//     setItems([]);
//     setIsCartOpen(false);
//   };
//
//   const cartCount = items.reduce((sum, item) => sum + item.quantity, 0);
//   const cartTotal = items.reduce(
//     (sum, item) => sum + item.product.price * item.quantity,
//     0,
//   );
//
//   return (
//     <CartContext.Provider
//       value={{
//         items,
//         addToCart,
//         removeFromCart,
//         updateQuantity,
//         clearCart,
//         cartCount,
//         cartTotal,
//         isCartOpen,
//         setIsCartOpen,
//       }}
//     >
//       {children}
//     </CartContext.Provider>
//   );
// }
//
// export function useCart() {
//   const context = useContext(CartContext);
//   if (!context) throw new Error("useCart must be used within CartProvider");
//   return context;
// }


import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

import type { CartItem, Product } from "../types";
import { useAuth } from "./AuthContext";
import api from "../config/api";
import productApi from "../config/ProductApi";

type BackendCartItem = {
  id: number;
  productId: number;
  quantity: number;
};

type BackendCart = {
  id: number;
  userId: number;
  status: "ACTIVE" | "CHECKED_OUT";
  items: BackendCartItem[];
};

type BackendProduct = {
  id: number;
  name: string;
  description?: string;
  price: number;
  discount?: number;
  quantity: number;
  imageUrl?: string;
  category?: string;
  unitValue?: number;
  unitType?: string;
};

interface CartContextType {
  items: CartItem[];
  cartId: number | null;
  addToCart: (product: Product, quantity?: number) => Promise<void>;
  removeFromCart: (productId: string) => Promise<void>;
  updateQuantity: (productId: string, quantity: number) => Promise<void>;
  checkoutCart: () => Promise<void>;
  clearCart: () => void;
  cartCount: number;
  cartTotal: number;
  isCartOpen: boolean;
  setIsCartOpen: (open: boolean) => void;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

// Convert the cart service's product shape into the shape used by the UI.
const mapProduct = (product: BackendProduct): Product => {
  const discount = product.discount || 0;

  return {
    id: String(product.id),
    name: product.name,
    description: product.description || "",
    price: Number(
        (product.price * (1 - discount / 100)).toFixed(2),
    ),
    originalPrice: discount > 0 ? product.price : undefined,
    image: product.imageUrl,
    category: product.category || "Groceries",
    unit: `${product.unitValue || 1} ${product.unitType || "unit"}`,
    stock: product.quantity,
    discount,
    rating: 0,
    reviewCount: 0,
  };
};

export function CartProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();

  const [items, setItems] = useState<CartItem[]>([]);
  const [cartId, setCartId] = useState<number | null>(null);
  const [isCartOpen, setIsCartOpen] = useState(false);

  // Cart responses contain product IDs only, so fetch product details before rendering items.
  const setCartFromResponse = async (cart: BackendCart) => {
    const { data: backendProducts } =
        await productApi.get<BackendProduct[]>("/products");

    const productsById = new Map(
        backendProducts.map((product) => [product.id, mapProduct(product)]),
    );

    const mappedItems = cart.items
        .map((item) => {
          const product = productsById.get(item.productId);

          return product
              ? { product, quantity: item.quantity }
              : null;
        })
        .filter((item): item is CartItem => item !== null);

    setCartId(cart.id);
    setItems(mappedItems);
  };

  // Only customers own carts; clear local cart data when another role signs in.
  const loadCart = async () => {
    if (!user || user.role !== "CUSTOMER") {
      setItems([]);
      setCartId(null);
      return;
    }

    try {
      const { data } = await api.get<BackendCart>(
          `/carts/users/${user.id}/active`,
      );

      await setCartFromResponse(data);
    } catch {
      // A new user has no active cart until their first item is added.
      setItems([]);
      setCartId(null);
    }
  };

  useEffect(() => {
    void loadCart();
  }, [user?.id, user?.role]);

  // The server is the source of truth for quantities and cart contents.
  const addToCart = async (product: Product, quantity = 1) => {
    if (!user) {
      throw new Error("Please sign in before adding products to cart.");
    }

    const { data } = await api.post<BackendCart>(
        `/carts/users/${user.id}/items`,
        {
          productId: Number(product.id),
          quantity,
        },
    );

    await setCartFromResponse(data);
    setIsCartOpen(true);
  };

  const removeFromCart = async (productId: string) => {
    if (!cartId) return;

    await api.delete(`/carts/${cartId}/items/${productId}`);

    setItems((previous) =>
        previous.filter((item) => item.product.id !== productId),
    );
  };

  const updateQuantity = async (productId: string, quantity: number) => {
    if (!cartId) return;

    if (quantity <= 0) {
      await removeFromCart(productId);
      return;
    }

    const { data } = await api.patch<BackendCart>(
        `/carts/${cartId}/items/${productId}`,
        { quantity },
    );

    await setCartFromResponse(data);
  };

  // Close the active backend cart only after checkout has completed successfully.
  const checkoutCart = async () => {
    if (!cartId) return;

    await api.post(`/carts/${cartId}/checkout`);

    setItems([]);
    setCartId(null);
    setIsCartOpen(false);
  };

  const clearCart = () => {
    setItems([]);
    setCartId(null);
    setIsCartOpen(false);
  };

  const cartCount = items.reduce(
      (total, item) => total + item.quantity,
      0,
  );

  const cartTotal = items.reduce(
      (total, item) => total + item.product.price * item.quantity,
      0,
  );

  return (
      <CartContext.Provider
          value={{
            items,
            cartId,
            addToCart,
            removeFromCart,
            updateQuantity,
            checkoutCart,
            clearCart,
            cartCount,
            cartTotal,
            isCartOpen,
            setIsCartOpen,
          }}
      >
        {children}
      </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);

  if (!context) {
    throw new Error("useCart must be used within CartProvider");
  }

  return context;
}
