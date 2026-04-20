import apiClient from "./axiosInstance";

export const interactionApi = {
    record: (data) => apiClient.post("/api/interactions", data),
    toggleLike: (videoId) =>
        apiClient.post(`/api/interactions/like/${videoId}`),
    getLikeStatus: (videoId) =>
        apiClient.get(`/api/interactions/like/${videoId}/status`),
    updateWatchTime: (data) =>
        apiClient.post(`/api/interactions/watch-time`, data),
    getStats: () => apiClient.get("/api/interactions/stats"),
};
