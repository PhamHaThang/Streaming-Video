import { Navigate, Outlet } from "react-router-dom";
import useAuth from "../../hooks/useAuth";
import { ROUTES } from "../../config/constant";
import { LoadingScreen } from "../common";
const ProtectedRoute = () => {
    const { isLoading, isAuthenticated } = useAuth();
    if (isLoading) {
        return <LoadingScreen />;
    }
    if (!isAuthenticated) {
        return <Navigate to={ROUTES.LOGIN} replace />;
    }
    return <Outlet />;
};

export default ProtectedRoute;
