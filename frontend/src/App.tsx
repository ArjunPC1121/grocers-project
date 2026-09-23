import { Toaster } from "react-hot-toast";
import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./pages/AppLayout";
import Home from "./pages/Home";
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
import { EmployeeDashboard, EmployeeLayout, EmployeeOrders, InventoryRequests, UnlockTickets } from "./pages/employee/EmployeeWorkspace";
import { AdminOverview, AdminShell, CustomerManagement, EmployeeManagement, Reports, RequestQueue } from "./pages/admin/AdminWorkspace";
export default function App() { return <><Toaster position="top-right"/><Routes>
  <Route path="/auth" element={<AuthPortal/>}/><Route path="/login" element={<Navigate to="/auth" replace/>}/>
  <Route path="/" element={<AppLayout/>}><Route index element={<Home/>}/><Route path="products" element={<Products/>}/><Route path="products/:id" element={<Product/>}/><Route path="search" element={<SearchResults/>}/><Route path="deals" element={<FlashDeals/>}/><Route element={<RoleGuard allow={["CUSTOMER"]}/> }><Route path="recipe-assistant" element={<RecipeAssistant/>}/><Route path="checkout" element={<Checkout/>}/><Route path="orders" element={<MyOrders/>}/><Route path="orders/:id" element={<OrderTracking/>}/><Route path="addresses" element={<Addresses/>}/><Route path="wishlist" element={<WishlistPage/>}/><Route path="funds" element={<FundsPage/>}/><Route path="profile" element={<ProfilePage/>}/><Route path="support" element={<SupportPage/>}/></Route></Route>
  <Route element={<RoleGuard allow={["EMPLOYEE"]}/> }><Route path="/employee" element={<EmployeeLayout/>}><Route index element={<EmployeeDashboard/>}/><Route path="requests" element={<InventoryRequests/>}/><Route path="orders" element={<EmployeeOrders/>}/><Route path="tickets" element={<UnlockTickets/>}/><Route path="profile" element={<ProfilePage/>}/></Route></Route>
  <Route element={<RoleGuard allow={["ADMIN"]}/> }><Route path="/admin" element={<AdminShell/>}><Route index element={<AdminOverview/>}/><Route path="products" element={<Products/>}/><Route path="employees" element={<EmployeeManagement/>}/><Route path="customers" element={<CustomerManagement/>}/><Route path="requests" element={<RequestQueue/>}/><Route path="reports" element={<Reports/>}/></Route></Route>
  <Route path="*" element={<Navigate to="/" replace/>}/>
</Routes></>; }
