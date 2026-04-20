import apiClient from "./axiosInstance";
export const videoApi = {
    // Public
    getPublicVideos: (page = 0, size = 12) =>
        apiClient.get(`/api/videos/public?page=${page}&size=${size}`),
    getTrending: (page = 0, size = 12) =>
        apiClient.get(`/api/videos/trending?page=${page}&size=${size}`),
    search: (keyword, page = 0, size = 12) =>
        apiClient.get(
            `/api/videos/search?keyword=${keyword}&page=${page}&size=${size}`,
        ),
    getById: (id) => {
        apiClient.get(`/api/videos/${id}`);
    },
    //Authenticated
    getMyVideos: (page = 0, size = 12) =>
        apiClient.get(`/api/videos/my-videos?page=${page}&size=${size}`),
    upload: (formData, onProgress) =>
        apiClient.post(`/api/videos/upload`, formData, {
            headers: { "Content-Type": "multipart/form-data" },
            onUploadProgress: onProgress,
            timeout: 60000, // 10 mins
        }),
    update: (id, data) => apiClient.put(`/api/videos/${id}`, data),
    delete: (id) => apiClient.delete(`/api/videos/${id}`),
};
