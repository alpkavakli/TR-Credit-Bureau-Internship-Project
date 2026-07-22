import api from './axiosInstance';

export const getMyScore = () => api.get('/scores/my-score');
export const getMyHistory = () => api.get('/scores/my-history');
