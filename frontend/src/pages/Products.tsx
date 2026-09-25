import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import toast from "react-hot-toast";
import { ChevronDown, Home, SlidersHorizontal, XIcon } from "lucide-react";

import type { Product } from "../types";
import { categoriesData } from "../assets/assets";
import ProductCard from "../components/ProductCard";
import Loading from "../components/Loading";
import FilterPanel from "../components/FilterPanel";
import { errorMessage } from "../config/api";
import {
  normalizedCategorySlug,
  preloadProductsByCategory,
} from "../config/ProductApi";

const Products = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);

  const category = searchParams.get("category") || "";
  const organic = searchParams.get("organic") || "";
  const sort = searchParams.get("sort") || "";
  const page = Number(searchParams.get("page")) || 1;
  const minPrice = searchParams.get("minPrice") || "";
  const maxPrice = searchParams.get("maxPrice") || "";

  // Preload all fixed categories so filters can run locally without another request per change.
  const fetchProducts = async () => {
    setLoading(true);

    try {
      setProducts(
          await preloadProductsByCategory(
              categoriesData.map(({ name }) => name),
          ),
      );
    } catch (error) {
      setProducts([]);
      toast.error(errorMessage(error, "Unable to load products."));
    } finally {
      setLoading(false);
    }
  };

  // Keep filter choices in the URL, making the current product view shareable and refresh-safe.
  const updateFilter = (key: string, value: string) => {
    const newParams = new URLSearchParams(searchParams);

    if (value) {
      newParams.set(key, value);
    } else {
      newParams.delete(key);
    }

    if (key !== "page") {
      newParams.delete("page");
    }

    setSearchParams(newParams);
  };

  const clearFilters = () => setSearchParams({});

  const activeCategory = categoriesData.find(
      (item) => item.slug === category,
  );

  const hasFilters = category || organic || minPrice || maxPrice;
  const pageSize = 12;

  // Apply every selected filter, sort the result, then paginate it below.
  const filteredProducts = useMemo(() => {
    const minimum = minPrice ? Number(minPrice) : undefined;
    const maximum = maxPrice ? Number(maxPrice) : undefined;

    return products
        .filter(
            (product) =>
                !category || normalizedCategorySlug(product.category) === category,
        )
        .filter(
            (product) => minimum === undefined || product.price >= minimum,
        )
        .filter(
            (product) => maximum === undefined || product.price <= maximum,
        )
        .sort((left, right) => {
          switch (sort) {
            case "price_asc":
              return left.price - right.price;
            case "price_desc":
              return right.price - left.price;
            case "rating":
              return right.rating - left.rating;
            case "name":
              return left.name.localeCompare(right.name);
            default:
              return Number(right.id) - Number(left.id);
          }
        });
  }, [products, category, minPrice, maxPrice, sort]);

  const totalPages = Math.max(
      1,
      Math.ceil(filteredProducts.length / pageSize),
  );

  const visibleProducts = filteredProducts.slice(
      (page - 1) * pageSize,
      page * pageSize,
  );

  useEffect(() => {
    void fetchProducts();
  }, []);

  return (
      <div className="min-h-screen bg-app-cream">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <nav className="flex items-center gap-2 text-sm text-app-text-light mb-6">
            <Link to="/" className="hover:text-app-green transition-colors">
              <Home className="size-4" />
            </Link>

            <span>/</span>

            <span className="text-app-green font-medium">
            {activeCategory ? activeCategory.name : "All Products"}
          </span>
          </nav>

          <div className="flex gap-8 xl:gap-10">
            <aside className="hidden lg:block w-64 shrink-0">
              <div className="bg-white rounded-2xl p-4 sticky top-24">
                <FilterPanel
                    categories={categoriesData}
                    category={category}
                    organic={organic}
                    minPrice={minPrice}
                    maxPrice={maxPrice}
                    updateFilter={updateFilter}
                    clearFilters={clearFilters}
                    hasFilters={hasFilters}
                />
              </div>
            </aside>

            <main className="flex-1">
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h1 className="text-2xl font-semibold text-app-green">
                    {activeCategory ? activeCategory.name : "All Products"}
                  </h1>

                  <p className="text-sm text-app-text-light mt-0.5">
                    {filteredProducts.length} products found
                  </p>
                </div>

                <div className="flex flex-col lg:items-center gap-3">
                  <button
                      onClick={() => setMobileFiltersOpen(true)}
                      className="lg:hidden flex items-center gap-2 px-3 py-2 text-sm bg-white rounded-xl border border-app-border hover:bg-app-cream transition-colors"
                  >
                    <SlidersHorizontal className="size-4" />
                    Filters
                  </button>

                  <div className="relative">
                    <select
                        value={sort}
                        onChange={(event) =>
                            updateFilter("sort", event.target.value)
                        }
                        className="appearance-none pl-3 pr-8 py-2 text-sm bg-white rounded-xl border border-app-border focus:border-app-green outline-none cursor-pointer"
                    >
                      <option value="">Newest</option>
                      <option value="price_asc">Price: Low → High</option>
                      <option value="price_desc">Price: High → Low</option>
                      <option value="rating">Top Rated</option>
                      <option value="name">A → Z</option>
                    </select>

                    <ChevronDown className="absolute right-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-app-text-light pointer-events-none" />
                  </div>
                </div>
              </div>

              {loading ? (
                  <Loading />
              ) : filteredProducts.length === 0 ? (
                  <div className="text-center py-16">
                    <p className="text-lg font-semibold text-app-green mb-2">
                      No products found
                    </p>

                    <p className="text-sm text-app-text-light mb-4">
                      Try adjusting your filters or search terms
                    </p>

                    <button
                        onClick={clearFilters}
                        className="px-5 py-2 text-sm font-medium bg-app-green text-white rounded-xl hover:bg-app-green-light transition-colors"
                    >
                      Clear Filters
                    </button>
                  </div>
              ) : (
                  <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 xl:gap-8">
                    {visibleProducts.map((product) => (
                        <ProductCard key={product.id} product={product} />
                    ))}
                  </div>
              )}

              {totalPages > 1 && (
                  <div className="flex-center gap-2 mt-16">
                    {Array.from({ length: totalPages }).map((_, index) => (
                        <button
                            key={index}
                            onClick={() => {
                              updateFilter("page", String(index + 1));
                              scrollTo(0, 0);
                            }}
                            className={`size-9 rounded-lg text-sm font-medium transition-colors ${
                                page === index + 1
                                    ? "bg-app-green text-white"
                                    : "bg-white text-app-text-light hover:bg-app-cream"
                            }`}
                        >
                          {index + 1}
                        </button>
                    ))}
                  </div>
              )}
            </main>
          </div>
        </div>

        {mobileFiltersOpen && (
            <>
              <div
                  className="fixed inset-0 bg-black/40 z-50"
                  onClick={() => setMobileFiltersOpen(false)}
              />

              <div className="fixed bottom-0 left-0 right-0 bg-white z-50 rounded-t-2xl max-h-[80vh] overflow-y-auto animate-slide-in-up">
                <div className="flex items-center justify-between p-4 border-b border-app-border">
                  <h3 className="text-lg font-semibold text-app-green">Filters</h3>

                  <button
                      onClick={() => setMobileFiltersOpen(false)}
                      className="p-2 hover:bg-app-cream rounded-lg"
                  >
                    <XIcon className="size-5" />
                  </button>
                </div>

                <div className="p-4">
                  <FilterPanel
                      categories={categoriesData}
                      category={category}
                      organic={organic}
                      minPrice={minPrice}
                      maxPrice={maxPrice}
                      updateFilter={updateFilter}
                      clearFilters={clearFilters}
                      hasFilters={hasFilters}
                  />
                </div>
              </div>
            </>
        )}
      </div>
  );
};

export default Products;