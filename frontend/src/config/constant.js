export const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const ROUTES = {
    HOME: "/",
    LOGIN: "/login",
    REGISTER: "/register",
    TRENDING: "/trending",
    SEARCH: "/search",
    VIDEO_WATCH: "/watch/:videoId",
    UPLOAD: "/upload",
    PROFILE: "/profile",
};

export const PLACEHOLDER_IMAGE = "https://placehold.co/600x400?text=Image";
