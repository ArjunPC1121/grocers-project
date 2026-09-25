import api from "./api";

export type RecommendationStatus = "IN_STOCK" | "OUT_OF_STOCK";

export interface RecommendedProductResponse {
  productId: number | null;
  productName: string | null;
  ingredient: string;
  requiredAmount: number;
  requiredUnit: string;
  quantity: number | null;
  unitPrice: number | null;
  lineTotal: number | null;
  status: RecommendationStatus;
  availabilityReason?:
    | "IN_STOCK"
    | "NO_CATALOG_MATCH"
    | "INCOMPATIBLE_UNIT"
    | "INACTIVE_PRODUCT"
    | "INSUFFICIENT_STOCK";
}

export interface RecipeRecommendationResponse {
  dish: string;
  summary: string;
  recommendedProducts: RecommendedProductResponse[];
  total: number;
  budget: number | null;
  withinBudget: boolean | null;
}

export interface RecipeRecommendationRequest {
  message: string;
  servings: number;
  budget?: number;
}

export async function getRecipeRecommendations(
  request: RecipeRecommendationRequest,
): Promise<RecipeRecommendationResponse> {
  const { data } = await api.post<RecipeRecommendationResponse>(
    "/assistant/recommendations",
    request,
  );
  return data;
}
