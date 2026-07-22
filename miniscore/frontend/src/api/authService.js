import api from './axiosInstance';

// register/login yanıtı: { verificationRequired, email, auth }.
// verificationRequired=true ise auth null'dur; kod /auth/verify ile doğrulanır.
export const register = (data) => api.post('/auth/register', data);
export const login = (data) => api.post('/auth/login', data);
export const verify = (email, code) => api.post('/auth/verify', { email, code });
export const refresh = (refreshToken) => api.post('/auth/refresh', { refreshToken });
