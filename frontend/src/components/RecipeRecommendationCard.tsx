import { CircleAlert, PackagePlus, ShoppingBag, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";

import type { RecommendedProductResponse } from "../config/AssistantApi";
import type { Product } from "../types";

type Props = {
  recommendation: RecommendedProductResponse;
  product?: Product;
  currency: string;
  adding: boolean;
  onAdd: () => void;
  onRemove: () => void;
};

export default function RecipeRecommendationCard({
  recommendation,
  product,
  currency,
  adding,
  onAdd,
  onRemove,
}: Props) {
  const inStock = recommendation.status === "IN_STOCK";
  const title = recommendation.productName || recommendation.ingredient;
  const packageLabel = recommendation.quantity
    ? `${recommendation.quantity} pack${recommendation.quantity === 1 ? "" : "s"}`
    : "Unavailable";

  return (
    <article
      className={`rounded-2xl border bg-white p-4 shadow-sm ${
        inStock ? "border-app-border" : "border-red-200 bg-red-50/40"
      }`}
    >
      <div className="flex gap-4">
        <div className="size-20 shrink-0 overflow-hidden rounded-xl bg-app-cream">
          {product ? (
            <img
              src={product.image}
              alt={title}
              className="size-full object-cover"
            />
          ) : (
            <div className="flex size-full items-center justify-center text-red-500">
              <ShoppingBag className="size-7" />
            </div>
          )}
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-app-text-light">
                For {recommendation.ingredient}
              </p>
              {product ? (
                <Link
                  to={`/products/${product.id}`}
                  className="line-clamp-2 text-base font-semibold text-app-green hover:underline"
                >
                  {title}
                </Link>
              ) : (
                <h3 className="text-base font-semibold text-app-green">{title}</h3>
              )}
            </div>
            <div className="flex items-center gap-2">
              <span
                className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                  inStock
                    ? "bg-emerald-100 text-emerald-800"
                    : "bg-red-100 text-red-700"
                }`}
              >
                {inStock ? "In stock" : "Out of stock"}
              </span>
              <button
                type="button"
                onClick={onRemove}
                className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-medium text-app-text-light transition-colors hover:bg-red-50 hover:text-red-700"
                aria-label={`Remove ${title} from this recipe plan`}
                title="Remove from recipe plan"
              >
                <Trash2 className="size-4" />
                Remove
              </button>
            </div>
          </div>

          <p className="mt-1 text-sm text-app-text-light">
            Recipe needs {recommendation.requiredAmount} {recommendation.requiredUnit} · {packageLabel}
            {product ? ` (${product.unit} each)` : ""}
          </p>

          {inStock ? (
            <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
              <div>
                <span className="font-semibold text-app-text">
                  {currency}{recommendation.lineTotal?.toFixed(2)}
                </span>
                <span className="ml-1 text-xs text-app-text-light">
                  {currency}{recommendation.unitPrice?.toFixed(2)} each
                </span>
              </div>
              <button
                type="button"
                disabled={!product || adding}
                onClick={onAdd}
                className="inline-flex items-center gap-1.5 rounded-lg bg-app-orange px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-app-orange-dark disabled:cursor-not-allowed disabled:opacity-60"
              >
                <PackagePlus className="size-4" />
                {adding ? "Adding…" : `Add ${packageLabel}`}
              </button>
            </div>
          ) : (
            <p className="mt-3 inline-flex items-center gap-1.5 text-sm font-medium text-red-700">
              <CircleAlert className="size-4" />
              This required ingredient cannot be added right now.
            </p>
          )}
        </div>
      </div>
    </article>
  );
}
