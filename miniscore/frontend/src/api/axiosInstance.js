import axios from 'axios';

const BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: BASE_URL,
});

// Her isteğe access token'ı ekle.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Oturumu tamamen temizleyip giriş sayfasına atan ortak yardımcı.
// DÜZELTME: eskiden yalnızca 'token' siliniyordu; email/role/refreshToken localStorage'da
// kalıyordu (bayat veri). logout() gibi hepsini temizliyoruz.
function forceLogout() {
  localStorage.clear();
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

// Aynı anda birden çok istek 401 alırsa refresh çağrısını TEK sefer yapıp
// diğerlerini aynı sonuca bağlamak için ufak bir kilit.
let refreshing = null;

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status;
    const original = error.config;

    // 403 = kimlik doğru ama yetki yok (ör. USER, admin ucuna gitti). Oturumu KAPATMA;
    // sayfa kendi hata mesajını göstersin. Sadece 401 için (token süresi/bozuk) uğraşırız.
    if (status !== 401 || !original || original._retried) {
      return Promise.reject(error);
    }

    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      forceLogout();
      return Promise.reject(error);
    }

    try {
      // Refresh çağrısını ÇIPLAK axios ile yapıyoruz: bu instance'ın interceptor'ına
      // takılmasın. Yoksa refresh isteği de 401 alırsa sonsuz döngüye gireriz.
      refreshing = refreshing || axios.post(`${BASE_URL}/auth/refresh`, { refreshToken });
      const { data } = await refreshing;
      refreshing = null;

      localStorage.setItem('token', data.token);
      if (data.refreshToken) {
        localStorage.setItem('refreshToken', data.refreshToken);
      }

      // Orijinal isteği yeni token'la BİR kez tekrar dene.
      original._retried = true;
      original.headers.Authorization = `Bearer ${data.token}`;
      return api(original);
    } catch (refreshError) {
      refreshing = null;
      forceLogout();
      return Promise.reject(refreshError);
    }
  }
);

export default api;
