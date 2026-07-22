import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children, requiredRole, blockRole }) {
  const { user } = useAuth();

  if (!user)
    return <Navigate to='/login' replace />;
  if (requiredRole && user.role !== requiredRole)
    return <Navigate to='/dashboard' replace />;
  // blockRole: bu rol bu sayfaya GIREMEZ (ör. ADMIN'in findeks raporu yok -> yönetime yolla).
  if (blockRole && user.role === blockRole)
    return <Navigate to='/admin' replace />;

  return children;
}
