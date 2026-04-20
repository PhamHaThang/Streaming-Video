import apiClient from "./axiosInstance";

export const recommendationApi = {
    getRecommendations: (limit = 20) => {
        return apiClient.get(`/api/recommendations?limit=${limit}`);
    },
    getSimilarVideos: (videoId, limit = 10) => {
        return apiClient.get(
            `/api/recommendations/similar/${videoId}?limit=${limit}`,
        );
    },
    refreshRecommendations: () => {
        return apiClient.post("/api/recommendations/refresh");
    },
};
