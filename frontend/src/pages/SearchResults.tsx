import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Home, Search } from "lucide-react";
import toast from "react-hot-toast";

import type { Product } from "../types";
import Loading from "../components/Loading";
import ProductCard from "../components/ProductCard";
import { errorMessage } from "../config/api";
import { searchProducts } from "../config/ProductApi";

const SearchResults = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchParams] = useSearchParams();
  const query = searchParams.get("q") || "";

  useEffect(() => {
    let cancelled = false;

    const runSearch = async () => {
      const normalizedQuery = query.trim();

      if (!normalizedQuery) {
        setProducts([]);
        setLoading(false);
        return;
      }

      setLoading(true);
      try {
        const results = await searchProducts(normalizedQuery, 40);
        if (!cancelled) setProducts(results);
      } catch (error) {
        if (!cancelled) {
          setProducts([]);
          toast.error(errorMessage(error, "Unable to search products."));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    void runSearch();
    return () => {
      cancelled = true;
    };
  }, [query]);

  return (
    <div className="min-h-screen bg-app-cream">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-sm text-app-text-light mb-6">
          <Link to="/" className="hover:text-app-green transition-colors">
            <Home className="size-4" />
          </Link>
          <span>/</span>
          <span className="text-app-green font-medium">Search Results</span>
        </nav>

        {/* Header */}

        <div className="mb-8">
          <h1 className="text-2xl font-semibold text-app-green mb-1">
            Results for "{query}"
          </h1>
          <p className="text-sm text-app-text-light">
            {loading ? "Searching..." : `${products.length} items found`}
          </p>
        </div>

        {/* Results */}
        {loading ? (
          <Loading />
        ) : products.length === 0 ? (
          <div className="text-center py-20">
            <Search className="size-16 text-app-border mx-auto mb-4" />
            <h2 className="text-xl font-semibold text-app-green mb-2">
              No results found
            </h2>
            <p className="text-sm text-app-text-light mb-6 max-w-md mx-auto">
              We couldn't find any products matching "{query}". Try a different
              search term.
            </p>
            <Link
              to="/products"
              className="inline-flex px-5 py-2.5 bg-app-green text-white text-sm font-medium rounded-lg"
            >
              Browse All Products
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default SearchResults;
