import api from "./api";
import type { Product } from "../types";

export interface ProductResponse {
  id: number;
  name: string;
  brand?: string | null;
  category?: string | null;
  subCategory?: string | null;
  description?: string | null;
  tags?: string | null;
  searchAliases?: string | null;
  unitValue?: number | null;
  unitType?: string | null;
  imageUrl?: string | null;
  price: number;
  discount: number;
  quantity: number;
  active?: boolean;
}

export interface ProductSearchResponse {
  id: number;
  name: string;
  brand?: string | null;
  category?: string | null;
  subCategory?: string | null;
  description?: string | null;
  imageUrl?: string | null;
  price: number;
  discount: number;
  quantity: number;
  lexicalScore: number;
  semanticScore: number;
  finalScore: number;
}

const fallbackImage = "/favicon.svg";

export const categorySlug = (value = "") =>
  value
    .toLowerCase()
    .trim()
    .replace(/&/g, "and")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");

export const normalizedCategorySlug = (value = "") =>
  categorySlug(value).replace("-and-", "-");

export function mapProduct(
  value: ProductResponse | ProductSearchResponse,
): Product {
  const discount = Math.max(0, Math.min(100, value.discount ?? 0));
  const originalPrice = Number(value.price);
  const salePrice = originalPrice * ((100 - discount) / 100);
  const complete = value as ProductResponse;
  const searchResult = value as ProductSearchResponse;

  return {
    id: String(value.id),
    name: value.name,
    brand: value.brand ?? "",
    description: value.description ?? "",
    category: value.category ?? "Uncategorized",
    subCategory: value.subCategory ?? "",
    image: value.imageUrl || fallbackImage,
    originalPrice,
    price: Number(salePrice.toFixed(2)),
    discount,
    stock: value.quantity ?? 0,
    unit:
      complete.unitValue && complete.unitType
        ? `${complete.unitValue} ${complete.unitType}`
        : complete.unitType || "item",
    rating: 0,
    reviewCount: 0,
    lexicalScore: searchResult.lexicalScore,
    semanticScore: searchResult.semanticScore,
    finalScore: searchResult.finalScore,
  };
}

export async function getProducts(): Promise<Product[]> {
  const { data } = await api.get<ProductResponse[]>("/products");
  return data.filter((product) => product.active !== false).map(mapProduct);
}

export async function getProduct(id: string): Promise<Product> {
  const { data } = await api.get<ProductResponse>(`/products/${id}`);
  return mapProduct(data);
}

export async function searchProducts(
  query: string,
  limit = 40,
): Promise<Product[]> {
  const { data } = await api.get<ProductSearchResponse[]>("/products/search", {
    params: { q: query.trim(), limit },
  });
  return data.map(mapProduct);
}
