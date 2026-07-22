import api from './axiosInstance';

export const listUsers = () => api.get('/admin/users');
// Denetim kayıtları sayfalı + sunucu tarafı arama (performedBy/e-posta üzerinden).
export const listAuditLogs = (page = 0, size = 20, search = '') =>
  api.get('/admin/audit-logs', {
    params: { page, size, ...(search ? { search } : {}) },
  });
// Rol değişimi hassas işlem: admin kendi şifresini tekrar gönderir (re-authentication).
export const updateRole = (id, role, password) =>
  api.put(`/admin/users/${id}/role`, { role, password });
// Bir kullanıcının findeks skor geçmişi (güncel skor = listenin son elemanı).
export const getUserScores = (id) => api.get(`/admin/users/${id}/scores`);

// Sistem ayarları: e-posta 2FA açık/kapalı.
export const getSettings = () => api.get('/admin/settings');
export const setEmail2fa = (enabled) => api.put('/admin/settings/email-2fa', { enabled });
