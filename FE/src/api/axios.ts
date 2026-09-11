import axios from "axios";
import { loadMemberAccess } from "../utils/memberAccessStorage";

const api = axios.create({
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// 요청 URL의 slug에 해당하는 멤버 인증 토큰이 있으면 자동으로 첨부
api.interceptors.request.use((config) => {
  const match = config.url?.match(/^\/rooms\/([^/]+)/);
  const slug = match?.[1];

  if (slug) {
    const memberAccess = loadMemberAccess(slug);

    if (memberAccess) {
      config.headers.Authorization = `Bearer ${memberAccess.accessToken}`;
    }
  }

  return config;
});

export default api;
