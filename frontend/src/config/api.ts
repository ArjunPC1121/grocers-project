import axios from "axios";
// The Spring gateway is the browser-facing API entry point. It also applies
// the role-based authorization required by the admin workspace.
export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_BASE_URL || "http://localhost:8091/grocers/api";
const api = axios.create({ baseURL: apiBaseUrl, headers: { "Content-Type": "application/json" } });
api.interceptors.request.use((config) => { const token = localStorage.getItem("grocers_access_token"); if (token) config.headers.Authorization = `Bearer ${token}`; return config; });
// Keep the saved session on a failed API request.  A 401 can be caused by a
// single service being restarted or by a transient request failure; it must not
// silently sign an administrator out while they are refreshing a page.
api.interceptors.response.use((response) => response, (error) => Promise.reject(error));
export const errorMessage = (error: unknown, fallback = "Something went wrong. Please try again.") => axios.isAxiosError(error) ? error.response?.data?.message || error.response?.data?.error || error.message || fallback : fallback;
export default api;
