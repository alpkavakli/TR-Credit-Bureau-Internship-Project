import api from './axiosInstance';

// Giriş yapmış kullanıcının kendi profili.
export const getMe = () => api.get('/users/me');
export const updateProfile = (data) => api.put('/users/me', data);
