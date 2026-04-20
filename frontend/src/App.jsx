import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { Toaster } from "react-hot-toast";
import { AuthLayout, MainLayout, ProtectedRoute } from "./components/layout";
import { ScrollToTop } from "./components/common";
import { ROUTES } from "./config/constant";
const App = () => {
    return (
        <Router>
            <ScrollToTop />
            <AuthProvider>
                <Routes>
                    {/* Auth routes */}
                    <Route element={<AuthLayout />}>
                        <Route path={ROUTES.LOGIN} element={<div>Login</div>} />
                        <Route
                            path={ROUTES.REGISTER}
                            element={<div>Register</div>}
                        />
                    </Route>
                    <Route element={<MainLayout />}>
                        {/* Public routes */}
                        <Route path={ROUTES.HOME} element={<div>Home</div>} />
                        <Route
                            path={ROUTES.TRENDING}
                            element={<div>Trending</div>}
                        />
                        <Route
                            path={ROUTES.SEARCH}
                            element={<div>Search</div>}
                        />
                        <Route
                            path={ROUTES.VIDEO_WATCH}
                            element={<div>Video Watch</div>}
                        />
                        {/* Private routes */}
                        <Route element={<ProtectedRoute />}>
                            <Route
                                path={ROUTES.UPLOAD}
                                element={<div>Upload</div>}
                            />
                            <Route
                                path={ROUTES.PROFILE}
                                element={<div>Profile</div>}
                            />
                        </Route>
                        <Route path="*" element={<div>Not Found</div>} />
                    </Route>
                </Routes>
                <Toaster
                    position="top-right"
                    toastOptions={{
                        duration: 3000,
                        style: {
                            background: "#363636",
                            color: "#fff",
                            fontSize: "13px",
                            borderRadius: "10px",
                            padding: "10px 16px",
                        },
                    }}
                    reverseOrder={false}
                />
            </AuthProvider>
        </Router>
    );
};

export default App;
