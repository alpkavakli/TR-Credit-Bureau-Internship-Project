import { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import { listUsers, listAuditLogs, updateRole } from '../api/adminService';

export default function AdminPage() {
  const [users, setUsers] = useState([]);
  const [logs, setLogs] = useState([]);
  const [error, setError] = useState('');

  const loadUsers = () =>
    listUsers()
      .then((response) => setUsers(response.data))
      .catch((err) => setError(err.response?.data?.message || 'Kullanıcılar alınamadı.'));

  useEffect(() => {
    loadUsers();
    listAuditLogs()
      .then((response) => setLogs(response.data))
      .catch((err) => setError(err.response?.data?.message || 'Denetim kayıtları alınamadı.'));
  }, []);

  const toggleRole = async (user) => {
    const next = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
    try {
      await updateRole(user.id, next);
      await loadUsers();
    } catch (err) {
      setError(err.response?.data?.message || 'Rol değiştirilemedi.');
    }
  };

  return (
    <Layout>
      {error && <p className='error-msg'>{error}</p>}

      <h2>Kullanıcılar</h2>
      <table>
        <thead>
          <tr>
            <th>ad soyad</th>
            <th>e-posta</th>
            <th>rol</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id}>
              <td>{user.firstName} {user.lastName}</td>
              <td>{user.email}</td>
              <td>{user.role}</td>
              <td>
                <a href='#' onClick={(e) => { e.preventDefault(); toggleRole(user); }}>
                  {user.role === 'ADMIN' ? 'USER yap' : 'ADMIN yap'}
                </a>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2>Denetim Kayıtları</h2>
      <table>
        <thead>
          <tr>
            <th>tarih</th>
            <th>işlem</th>
            <th>yapan</th>
            <th>detay</th>
          </tr>
        </thead>
        <tbody>
          {logs.map((log) => (
            <tr key={log.id}>
              <td>{log.createdAt?.replace('T', ' ').slice(0, 16)}</td>
              <td>{log.action}</td>
              <td>{log.performedBy}</td>
              <td>{log.details}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Layout>
  );
}
