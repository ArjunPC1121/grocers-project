import axios from "axios";
// The Spring gateway is the browser-facing API entry point. It also applies
// the role-based authorization required by the admin workspace.
export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_BASE_URL || "http://localhost:8091/grocers/api";
const api = axios.create({ baseURL: apiBaseUrl, headers: { "Content-Type": "application/json" } });
api.interceptors.request.use((config) => { const token = localStorage.getItem("grocers_access_token"); if (token) config.headers.Authorization = `Bearer ${token}`; return config; });
api.interceptors.response.use((response) => response, (error) => { if (error.response?.status === 401) { localStorage.removeItem("grocers_access_token"); localStorage.removeItem("grocers_session"); } return Promise.reject(error); });
export const errorMessage = (error: unknown, fallback = "Something went wrong. Please try again.") => axios.isAxiosError(error) ? error.response?.data?.message || error.message || fallback : fallback;
export default api;
