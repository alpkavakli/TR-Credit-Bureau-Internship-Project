import api from './axiosInstance';

export const listUsers = () => api.get('/admin/users');
export const listAuditLogs = () => api.get('/admin/audit-logs');
export const updateRole = (id, role) => api.put(`/admin/users/${id}/role`, { role });
