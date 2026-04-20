import { Outlet, Navigate } from "react-router-dom";
import useAuth from "../../hooks/useAuth";
import LoadingScreen from "../common/LoadingScreen";
import { ROUTES } from "../../config/constant";
const AuthLayout = () => {
    const { isAuthenticated, isLoading } = useAuth();

    if (isLoading) {
        return <LoadingScreen />;
    }

    if (isAuthenticated) {
        return <Navigate to={ROUTES.HOME} replace />;
    }

    return (
        <div className="min-h-screen bg-beige-50 flex items-center justify-center p-4">
            <div className="w-full max-w-md">
                <Outlet />
            </div>
        </div>
    );
};

export default AuthLayout;
