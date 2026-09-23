import { useEffect, useMemo, useState, type FormEvent } from "react";
import { ChefHat, CircleAlert, PackagePlus, Search } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import toast from "react-hot-toast";

import {
  getRecipeRecommendations,
  type RecipeRecommendationResponse,
  type RecommendedProductResponse,
} from "../config/AssistantApi";
import { errorMessage } from "../config/api";
import { getProducts } from "../config/ProductApi";
import RecipeRecommendationCard from "../components/RecipeRecommendationCard";
import Loading from "../components/Loading";
import { useCart } from "../context/CartContext";
import type { Product } from "../types";

const currency = import.meta.env.VITE_CURRENCY_SYMBOL || "₹";

export default function RecipeAssistant() {
  const [searchParams, setSearchParams] = useSearchParams();
  const prefilledRecipe = searchParams.get("recipe") || "";
  const [message, setMessage] = useState(prefilledRecipe);
  const [servings, setServings] = useState(2);
  const [budget, setBudget] = useState("");
  const [recommendation, setRecommendation] = useState<RecipeRecommendationResponse | null>(null);
  const [productsById, setProductsById] = useState<Map<string, Product>>(new Map());
  const [loading, setLoading] = useState(false);
  const [addingProductId, setAddingProductId] = useState<number | null>(null);
  const [addingAll, setAddingAll] = useState(false);
  const { addToCart } = useCart();

  const recommendationKey = (item: RecommendedProductResponse) =>
    `${item.productId ?? "missing"}-${item.ingredient}`;

  useEffect(() => {
    if (prefilledRecipe) {
      setMessage(prefilledRecipe);
    }
  }, [prefilledRecipe]);

  const availableItems = useMemo(
    () =>
      (recommendation?.recommendedProducts || []).filter(
        (item): item is RecommendedProductResponse & { productId: number; quantity: number } =>
          item.status === "IN_STOCK"
          && item.productId !== null
          && item.quantity !== null
          && productsById.has(String(item.productId)),
      ),
    [productsById, recommendation],
  );

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const recipe = message.trim();
    const parsedBudget = budget.trim() ? Number(budget) : undefined;

    if (!recipe) {
      toast.error("Enter a recipe or meal idea first.");
      return;
    }
    if (!Number.isInteger(servings) || servings < 1) {
      toast.error("Servings must be at least 1.");
      return;
    }
    if (parsedBudget !== undefined && (!Number.isFinite(parsedBudget) || parsedBudget <= 0)) {
      toast.error("Budget must be greater than zero.");
      return;
    }

    setLoading(true);
    setRecommendation(null);
    setSearchParams({ recipe });
    try {
      const [result, products] = await Promise.all([
        getRecipeRecommendations({ message: recipe, servings, budget: parsedBudget }),
        getProducts(),
      ]);
      setRecommendation(result);
      setProductsById(new Map(products.map((product) => [product.id, product])));
    } catch (error) {
      toast.error(errorMessage(error, "Unable to plan this recipe. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  const addRecommendation = async (item: RecommendedProductResponse) => {
    if (item.productId === null || item.quantity === null) return;
    const product = productsById.get(String(item.productId));
    if (!product) {
      toast.error("This product is no longer available. Please refresh the recipe plan.");
      return;
    }

    setAddingProductId(item.productId);
    try {
      await addToCart(product, item.quantity);
      toast.success(`${item.quantity} × ${product.name} added to your cart.`);
    } catch (error) {
      toast.error(errorMessage(error, "Unable to add this product to your cart."));
    } finally {
      setAddingProductId(null);
    }
  };

  const addAllAvailable = async () => {
    if (!availableItems.length) return;
    setAddingAll(true);
    try {
      for (const item of availableItems) {
        const product = productsById.get(String(item.productId));
        if (product) await addToCart(product, item.quantity);
      }
      toast.success("All available recipe products were added to your cart.");
    } catch (error) {
      toast.error(errorMessage(error, "Some products could not be added to your cart."));
    } finally {
      setAddingAll(false);
    }
  };

  const removeRecommendation = (item: RecommendedProductResponse) => {
    const itemKey = recommendationKey(item);
    setRecommendation((current) => {
      if (!current) return current;

      const recommendedProducts = current.recommendedProducts.filter(
        (candidate) => recommendationKey(candidate) !== itemKey,
      );
      const total = Number(recommendedProducts
        .filter((candidate) => candidate.status === "IN_STOCK")
        .reduce((sum, candidate) => sum + (candidate.lineTotal || 0), 0)
        .toFixed(2));

      return {
        ...current,
        recommendedProducts,
        total,
        withinBudget: current.budget === null ? null : total <= current.budget,
      };
    });
  };

  const difference = recommendation?.budget !== null && recommendation?.budget !== undefined
    ? Math.max(0, recommendation.total - recommendation.budget)
    : 0;

  return (
    <main className="min-h-screen bg-app-cream">
      <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
        <section className="rounded-3xl bg-app-green px-5 py-8 text-white shadow-sm sm:px-8">
          <div className="flex items-start gap-3">
            <ChefHat className="mt-1 size-7 shrink-0 text-app-orange" />
            <div>
              <h1 className="text-2xl font-semibold">Plan a recipe</h1>
              <p className="mt-1 max-w-2xl text-sm text-white/80">
                Tell us what you want to cook. We will find the packs you need from products currently in stock.
              </p>
            </div>
          </div>

          <form onSubmit={submit} className="mt-6 grid gap-3 md:items-end md:grid-cols-[1fr_130px_150px_auto]">
            <div>
              <label htmlFor="recipe-message" className="mb-1 block text-xs font-semibold text-white">
                Recipe name
              </label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-zinc-500" />
                <input
                  id="recipe-message"
                  value={message}
                  onChange={(event) => setMessage(event.target.value)}
                  placeholder="e.g. Paneer butter masala"
                  maxLength={500}
                  className="w-full rounded-xl bg-white py-3 pl-9 pr-3 text-sm text-app-text outline-none ring-app-orange focus:ring-2"
                />
              </div>
            </div>
            <div>
              <label htmlFor="recipe-servings" className="mb-1 block text-xs font-semibold text-white">
                Serves (number of people)
              </label>
              <input
                id="recipe-servings"
                type="number"
                min="1"
                max="50"
                value={servings}
                onChange={(event) => setServings(Number(event.target.value))}
                className="w-full rounded-xl bg-white px-3 py-3 text-sm text-app-text outline-none ring-app-orange focus:ring-2"
                aria-label="Number of people to serve"
              />
            </div>
            <div>
              <label htmlFor="recipe-budget" className="mb-1 block text-xs font-semibold text-white">
                Budget (optional)
              </label>
              <input
                id="recipe-budget"
                type="number"
                min="1"
                step="0.01"
                value={budget}
                onChange={(event) => setBudget(event.target.value)}
                placeholder={`Amount in ${currency}`}
                className="w-full rounded-xl bg-white px-3 py-3 text-sm text-app-text outline-none ring-app-orange focus:ring-2"
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="h-12 self-end rounded-xl bg-app-orange px-5 text-sm font-semibold text-white transition-colors hover:bg-app-orange-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? "Planning…" : "Find ingredients"}
            </button>
          </form>
        </section>

        {loading && <div className="py-12"><Loading /></div>}

        {recommendation && !loading && (
          <section className="mt-8">
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
              <div>
                <p className="text-sm font-medium text-app-orange">Recipe recommendation</p>
                <h2 className="text-2xl font-semibold capitalize text-app-green">{recommendation.dish}</h2>
                <p className="mt-1 text-sm text-app-text-light">{recommendation.summary}</p>
              </div>
              <div className="rounded-xl bg-white px-4 py-3 text-right shadow-sm">
                <p className="text-xs font-medium uppercase tracking-wide text-app-text-light">Available items total</p>
                <p className="text-xl font-semibold text-app-green">{currency}{recommendation.total.toFixed(2)}</p>
              </div>
            </div>

            <div className="mt-5 space-y-3">
              {recommendation.recommendedProducts.map((item, index) => (
                <RecipeRecommendationCard
                  key={`${item.productId ?? item.ingredient}-${index}`}
                  recommendation={item}
                  product={item.productId === null ? undefined : productsById.get(String(item.productId))}
                  currency={currency}
                  adding={addingProductId === item.productId || addingAll}
                  onAdd={() => void addRecommendation(item)}
                  onRemove={() => removeRecommendation(item)}
                />
              ))}
            </div>

            {difference > 0 && (
              <div className="mt-5 flex items-start gap-3 rounded-2xl border border-red-300 bg-red-50 p-4 text-red-900">
                <CircleAlert className="mt-0.5 size-5 shrink-0 text-red-600" />
                <div>
                  <p className="font-semibold">Over budget by {currency}{difference.toFixed(2)}</p>
                  <p className="mt-0.5 text-sm text-red-800">
                    Available items total {currency}{recommendation.total.toFixed(2)} · Budget {currency}{recommendation.budget?.toFixed(2)}
                  </p>
                </div>
              </div>
            )}

            {availableItems.length > 0 && (
              <div className="mt-6 flex justify-end">
                <button
                  type="button"
                  disabled={addingAll || addingProductId !== null}
                  onClick={() => void addAllAvailable()}
                  className="inline-flex items-center gap-2 rounded-xl bg-app-green px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-app-green-light disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <PackagePlus className="size-4" />
                  {addingAll ? "Adding available items…" : "Add all available to cart"}
                </button>
              </div>
            )}
          </section>
        )}
      </div>
    </main>
  );
}
