import apiClient from "./axiosInstance";

export const authApi = {
    register: (data) => apiClient.post("/api/auth/register", data),
    login: (data) => apiClient.post("/api/auth/login", data),
    refreshToken: (refreshToken) =>
        apiClient.post("/api/auth/refresh-token", { refreshToken }),
};
