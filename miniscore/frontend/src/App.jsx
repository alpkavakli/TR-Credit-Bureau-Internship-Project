import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import ReportPage from './pages/ReportPage';
import ProfilePage from './pages/ProfilePage';
import AdminPage from './pages/AdminPage';

// Kök yol: ADMIN'in skor paneli yok, doğrudan yönetime; USER dashboard'a gider.
function HomeRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to='/login' replace />;
  return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path='/' element={<HomeRedirect />} />
          <Route path='/login' element={<LoginPage />} />
          <Route path='/register' element={<RegisterPage />} />
          <Route path='/dashboard' element={
            <ProtectedRoute blockRole='ADMIN'><DashboardPage /></ProtectedRoute>
          } />
          <Route path='/reports' element={
            <ProtectedRoute blockRole='ADMIN'><ReportPage /></ProtectedRoute>
          } />
          <Route path='/profile' element={
            <ProtectedRoute><ProfilePage /></ProtectedRoute>
          } />
          <Route path='/admin' element={
            <ProtectedRoute requiredRole='ADMIN'>
              <AdminPage />
            </ProtectedRoute>
          } />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
