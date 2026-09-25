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

const fallbackImage = "/main_logo.png";

export const categorySlug = (value = "") =>
  value
    .toLowerCase()
    .trim()
    .replace(/&/g, "and")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");

export const normalizedCategorySlug = (value = "") =>
  categorySlug(value).replace("-and-", "-");

const exactCategorySearches: Record<string, string> = {
  "fruit": "fruits-vegetables",
  "fruits": "fruits-vegetables",
  "vegetable": "fruits-vegetables",
  "vegetables": "fruits-vegetables",
  "veg": "fruits-vegetables",
  "vegs": "fruits-vegetables",
  "veggie": "fruits-vegetables",
  "veggies": "fruits-vegetables",
  "produce": "fruits-vegetables",
  "fresh produce": "fruits-vegetables",
  "greens": "fruits-vegetables",
  "leafy greens": "fruits-vegetables",
  "fruits vegetables": "fruits-vegetables",
  "fruits and vegetables": "fruits-vegetables",
  "personal": "personal-care",
  "personal care": "personal-care",
  "hygiene": "personal-care",
  "toiletry": "personal-care",
  "toiletries": "personal-care",
  "skin care": "personal-care",
  "skincare": "personal-care",
  "hair care": "personal-care",
  "haircare": "personal-care",
  "beauty": "personal-care",
  "grooming": "personal-care",
  "pantry": "pantry-staples",
  "staple": "pantry-staples",
  "staples": "pantry-staples",
  "pantry staples": "pantry-staples",
  "grocery": "pantry-staples",
  "groceries": "pantry-staples",
  "grain": "pantry-staples",
  "grains": "pantry-staples",
  "rice": "pantry-staples",
  "pulse": "pantry-staples",
  "pulses": "pantry-staples",
  "lentil": "pantry-staples",
  "lentils": "pantry-staples",
  "dal": "pantry-staples",
  "dals": "pantry-staples",
  "flour": "pantry-staples",
  "atta": "pantry-staples",
  "spice": "pantry-staples",
  "spices": "pantry-staples",
  "masala": "pantry-staples",
  "bakery": "bakery",
  "bread": "bakery",
  "breads": "bakery",
  "bun": "bakery",
  "buns": "bakery",
  "pastry": "bakery",
  "pastries": "bakery",
  "cake": "bakery",
  "cakes": "bakery",
  "cookie": "bakery",
  "cookies": "bakery",
  "drinks": "beverages",
  "beverage": "beverages",
  "beverages": "beverages",
  "drink": "beverages",
  "juice": "beverages",
  "juices": "beverages",
  "water": "beverages",
  "tea": "beverages",
  "coffee": "beverages",
  "soda": "beverages",
  "soft drink": "beverages",
  "soft drinks": "beverages",
  "meat": "meat-seafood",
  "meats": "meat-seafood",
  "seafood": "meat-seafood",
  "fish": "meat-seafood",
  "fishes": "meat-seafood",
  "chicken": "meat-seafood",
  "poultry": "meat-seafood",
  "mutton": "meat-seafood",
  "prawn": "meat-seafood",
  "prawns": "meat-seafood",
  "shrimp": "meat-seafood",
  "meat and seafood": "meat-seafood",
  "meat seafood": "meat-seafood",
  "snack": "snacks",
  "snacks": "snacks",
  "chips": "snacks",
  "namkeen": "snacks",
  "cracker": "snacks",
  "crackers": "snacks",
  "frozen": "frozen-foods",
  "frozen food": "frozen-foods",
  "frozen foods": "frozen-foods",
  "freezer": "frozen-foods",
  "dairy": "dairy-eggs",
  "milk": "dairy-eggs",
  "eggs": "dairy-eggs",
  "egg": "dairy-eggs",
  "cheese": "dairy-eggs",
  "butter": "dairy-eggs",
  "paneer": "dairy-eggs",
  "yogurt": "dairy-eggs",
  "yoghurt": "dairy-eggs",
  "curd": "dairy-eggs",
  "cream": "dairy-eggs",
  "dairy and eggs": "dairy-eggs",
  "dairy eggs": "dairy-eggs",
  "oil": "pantry-staples",
  "oils": "pantry-staples",
  "cooking oil": "pantry-staples",
  "cooking oils": "pantry-staples",
  "sauce": "pantry-staples",
  "sauces": "pantry-staples",
  "condiment": "pantry-staples",
  "condiments": "pantry-staples",
  "baby": "baby-care",
  "baby care": "baby-care",
  "infant": "baby-care",
  "infants": "baby-care",
  "newborn": "baby-care",
  "newborns": "baby-care",
  "toddler": "baby-care",
  "toddlers": "baby-care",
};

/** Returns a category filter only when the whole search query is an alias. */
export function categorySlugForExactSearch(query: string): string | undefined {
  const normalized = query
    .toLowerCase()
    .trim()
    .replace(/&/g, "and")
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ");
  return exactCategorySearches[normalized];
}

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

const categoryProductCache = new Map<string, Promise<Product[]>>();

export function getProductsByCategory(category: string): Promise<Product[]> {
  const cacheKey = category.trim().toLowerCase();
  const cached = categoryProductCache.get(cacheKey);
  if (cached) return cached;

  const request = api
    .get<ProductResponse[]>("/products", { params: { category } })
    .then(({ data }) => data.map(mapProduct))
    .catch((error) => {
      categoryProductCache.delete(cacheKey);
      throw error;
    });

  categoryProductCache.set(cacheKey, request);
  return request;
}

/** Preloads every fixed category once and reuses the result during navigation. */
export async function preloadProductsByCategory(
  categories: readonly string[],
): Promise<Product[]> {
  const groups = await Promise.all(
    categories.map((category) => getProductsByCategory(category)),
  );
  return Array.from(
    new Map(groups.flat().map((product) => [product.id, product])).values(),
  );
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
// Compatibility export for existing code that uses the client directly.
export default api;
