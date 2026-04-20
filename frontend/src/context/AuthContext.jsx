import { createContext, useReducer } from "react";
import toast from "react-hot-toast";
import { authApi } from "../api";

const AuthContext = createContext(null);

// ── Initial State ──
const initialState = {
    user: JSON.parse(localStorage.getItem("user")) || null,
    isLoading: false,
    error: null,
    isAuthenticated: false,
};

// ── Reducer ──
const authReducer = (state, action) => {
    switch (action.type) {
        case "AUTH_START":
            return { ...state, isLoading: true, error: null };
        case "AUTH_SUCCESS":
            return {
                ...state,
                isLoading: false,
                user: action.payload,
                error: null,
                isAuthenticated: true,
            };
        case "AUTH_ERROR":
            return { ...state, isLoading: false, error: action.payload };
        case "LOGOUT":
            return {
                ...state,
                user: null,
                isLoading: false,
                error: null,
                isAuthenticated: false,
            };
        default:
            return state;
    }
};

// ── Provider ──
const AuthProvider = ({ children }) => {
    const [state, dispatch] = useReducer(authReducer, initialState);

    const login = async (credentials) => {
        dispatch({ type: "AUTH_START" });
        try {
            const { data } = await authApi.login(credentials);
            const { accessToken, refreshToken, user } = data.data;
            localStorage.setItem("user", JSON.stringify(user));
            localStorage.setItem("accessToken", accessToken);
            localStorage.setItem("refreshToken", refreshToken);
            dispatch({ type: "AUTH_SUCCESS", payload: user });
            toast.success("Đăng nhập thành công!");
            return true;
        } catch (error) {
            const message =
                error.response?.data?.message || "Đăng nhập thất bại";
            dispatch({
                type: "AUTH_ERROR",
                payload: message,
            });
            toast.error(message);
            return false;
        }
    };
    const register = async (userData) => {
        dispatch({ type: "AUTH_START" });
        try {
            const { data } = await authApi.register(userData);
            const { accessToken, refreshToken, user } = data.data;
            localStorage.setItem("user", JSON.stringify(user));
            localStorage.setItem("accessToken", accessToken);
            localStorage.setItem("refreshToken", refreshToken);
            dispatch({ type: "AUTH_SUCCESS", payload: user });
            toast.success("Đăng ký thành công!");
            return true;
        } catch (error) {
            const message = error.response?.data?.message || "Đăng ký thất bại";
            dispatch({
                type: "AUTH_ERROR",
                payload: message,
            });
            toast.error(message);
            return false;
        }
    };
    const logout = () => {
        localStorage.removeItem("user");
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        dispatch({ type: "LOGOUT" });
        toast.success("Đăng xuất thành công!");
    };

    return (
        <AuthContext.Provider value={{ ...state, login, register, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export { AuthContext, AuthProvider };
