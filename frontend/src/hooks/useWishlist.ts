import toast from "react-hot-toast";

import api from "../config/api";
import { useAuth } from "../context/AuthContext";

export const useWishlist = () => {
    const { user } = useAuth();

    const addToWishlist = async (productId: string) => {
        if (!user) {
            toast.error("Please sign in to add products to wishlist.");
            return;
        }

        try {
            await api.post(
                `/users/${user.id}/wishlist/${productId}`,
            );

            toast.success("Added to wishlist.");
        } catch (error: any) {
            toast.error(
                error.response?.data?.message ||
                "Could not add product to wishlist.",
            );
        }
    };

    return addToWishlist;
};