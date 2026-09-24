import { Toaster } from "react-hot-toast";
import "./App.css";
import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./pages/AppLayout";
import HomeRoute from "./components/HomeRoute";
import Products from "./pages/Products";
import Product from "./pages/Product";
import SearchResults from "./pages/SearchResults";
import RecipeAssistant from "./pages/RecipeAssistant";
import FlashDeals from "./pages/FlashDeals";
import Checkout from "./pages/Checkout";
import MyOrders from "./pages/MyOrders";
import OrderTracking from "./pages/OrderTracking";
import Addresses from "./pages/Addresses";
import AuthPortal from "./pages/AuthPortal";
import RoleGuard from "./components/RoleGuard";
import { FundsPage, ProfilePage, SupportPage, WishlistPage } from "./pages/customer/CustomerTools";
import { EmployeeAccessGuard, EmployeeDashboard, EmployeeHistory, EmployeeOrders, EmployeeProfile } from "./pages/employee/EmployeeWorkspace";
import { default as UnlockTickets } from "./pages/employee/LiveUnlockTickets";
import EmployeePasswordChange from "./pages/employee/EmployeePasswordChange";
import UnlockAccount from "./pages/UnlockAccount";
import SecurityQuestionRecovery from "./pages/SecurityQuestionRecovery";
import { EmployeeProductRequests } from "./pages/ProductRequests";
import { EmployeeManagementPageV2 } from "./pages/admin/AdminWorkspace";
import LiveAdminDashboard from "./pages/admin/LiveAdminDashboard";
import AdminShell from "./pages/admin/AdminShellSlim";
import ProductManagement from "./pages/admin/ProductManagement";
import AdminOrders from "./pages/admin/AdminOrders";
import UserManagement from "./pages/admin/UserManagement";
import RequestManagementFinal from "./pages/admin/RequestManagementFinal";
import AdminAccounts from "./pages/admin/AdminAccounts";
import AdminProfile from "./pages/admin/AdminProfile";
import ReportsDashboard from "./pages/admin/ReportsDashboard";
export default function App() { return <><Toaster position="top-right"/><Routes>
  <Route path="/auth" element={<AuthPortal/>}/><Route path="/login" element={<Navigate to="/auth" replace/>}/><Route path="/unlock-account" element={<UnlockAccount/>}/><Route path="/recover-account" element={<SecurityQuestionRecovery/>}/>
  <Route path="/" element={<AppLayout/>}><Route index element={<HomeRoute/>}/><Route path="products" element={<Products/>}/><Route path="products/:id" element={<Product/>}/><Route path="search" element={<SearchResults/>}/><Route path="deals" element={<FlashDeals/>}/><Route element={<RoleGuard allow={["CUSTOMER"]}/> }><Route path="recipe-assistant" element={<RecipeAssistant/>}/><Route path="checkout" element={<Checkout/>}/><Route path="orders" element={<MyOrders/>}/><Route path="orders/:id" element={<OrderTracking/>}/><Route path="addresses" element={<Addresses/>}/><Route path="wishlist" element={<WishlistPage/>}/><Route path="funds" element={<FundsPage/>}/><Route path="profile" element={<ProfilePage/>}/><Route path="support" element={<SupportPage/>}/></Route></Route>
  <Route element={<RoleGuard allow={["EMPLOYEE"]}/> }><Route path="/employee" element={<EmployeeAccessGuard/>}><Route index element={<EmployeeDashboard/>}/><Route path="product-requests" element={<EmployeeProductRequests/>}/><Route path="orders" element={<EmployeeOrders/>}/><Route path="tickets" element={<UnlockTickets/>}/><Route path="history" element={<EmployeeHistory/>}/><Route path="profile" element={<EmployeeProfile/>}/></Route><Route path="/employee/change-password" element={<EmployeePasswordChange/>}/></Route>
  <Route element={<RoleGuard allow={["ADMIN"]}/> }><Route path="/admin" element={<AdminShell/>}><Route index element={<LiveAdminDashboard/>}/><Route path="orders" element={<AdminOrders/>}/><Route path="products" element={<ProductManagement/>}/><Route path="users" element={<UserManagement/>}/><Route path="requests" element={<RequestManagementFinal/>}/><Route path="employees" element={<EmployeeManagementPageV2/>}/><Route path="admins" element={<AdminAccounts/>}/><Route path="profile" element={<AdminProfile/>}/><Route path="reports" element={<ReportsDashboard/>}/></Route></Route>
  <Route path="*" element={<Navigate to="/" replace/>}/>
</Routes></>; }
