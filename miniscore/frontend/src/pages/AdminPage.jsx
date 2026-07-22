import { useEffect, useState } from 'react';
import {
  CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import Layout from '../components/Layout';
import { useAuth } from '../context/AuthContext';
import {
  listUsers, listAuditLogs, updateRole, getUserScores, getSettings, setEmail2fa,
} from '../api/adminService';

const PAGE_SIZE = 20;

export default function AdminPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [logs, setLogs] = useState([]);
  const [logPage, setLogPage] = useState(0);
  const [logTotalPages, setLogTotalPages] = useState(0);
  const [error, setError] = useState('');

  // Seçili kullanıcının skor geçmişi paneli.
  const [scoreUser, setScoreUser] = useState(null);
  const [scores, setScores] = useState([]);
  const [scoresLoading, setScoresLoading] = useState(false);

  // Arama kutuları: kullanıcılar istemci tarafı (tüm liste yüklü), denetim kayıtları
  // sunucu tarafı (sayfalı) filtrelenir.
  const [userSearch, setUserSearch] = useState('');
  const [logSearch, setLogSearch] = useState('');

  // Rol değişimi onay penceresi (uyarı + şifre tekrar girişi).
  const [roleTarget, setRoleTarget] = useState(null);   // rolü değişecek kullanıcı
  const [rolePassword, setRolePassword] = useState('');
  const [roleError, setRoleError] = useState('');
  const [roleBusy, setRoleBusy] = useState(false);

  // Sistem ayarı: e-posta 2FA açık mı?
  const [email2faOn, setEmail2faOn] = useState(false);
  const [settingBusy, setSettingBusy] = useState(false);

  const loadUsers = () =>
    listUsers()
      .then((response) => setUsers(response.data))
      .catch((err) => setError(err.response?.data?.message || 'Kullanıcılar alınamadı.'));

  // Denetim kayıtları sayfalı + aramalı geliyor: backend Page nesnesi döner.
  const loadLogs = (page, search) =>
    listAuditLogs(page, PAGE_SIZE, search)
      .then((response) => {
        setLogs(response.data.content);
        setLogPage(response.data.number);
        setLogTotalPages(response.data.totalPages);
      })
      .catch((err) => setError(err.response?.data?.message || 'Denetim kayıtları alınamadı.'));

  useEffect(() => {
    loadUsers();
    getSettings()
      .then((response) => setEmail2faOn(response.data.emailTwoFactorEnabled))
      .catch(() => {});
  }, []);

  const toggle2fa = async () => {
    setSettingBusy(true);
    setError('');
    try {
      const response = await setEmail2fa(!email2faOn);
      setEmail2faOn(response.data.emailTwoFactorEnabled);
    } catch (err) {
      setError(err.response?.data?.message || 'Ayar değiştirilemedi.');
    } finally {
      setSettingBusy(false);
    }
  };

  // Arama yazılırken her tuşta istek atmamak için 300ms debounce; arama değişince
  // ilk sayfaya döneriz (aksi halde filtreli sonuçta olmayan bir sayfada kalabilirdik).
  useEffect(() => {
    const t = setTimeout(() => loadLogs(0, logSearch), 300);
    return () => clearTimeout(t);
  }, [logSearch]);

  // Rol değişimini doğrudan yapmıyoruz: önce onay + şifre penceresini açıyoruz.
  const askRole = (user) => {
    setRoleTarget(user);
    setRolePassword('');
    setRoleError('');
  };

  const closeRole = () => {
    setRoleTarget(null);
    setRolePassword('');
    setRoleError('');
  };

  const confirmRole = async () => {
    const next = roleTarget.role === 'ADMIN' ? 'USER' : 'ADMIN';
    setRoleBusy(true);
    setRoleError('');
    try {
      await updateRole(roleTarget.id, next, rolePassword);
      closeRole();
      await loadUsers();
    } catch (err) {
      setRoleError(err.response?.data?.message || 'Rol değiştirilemedi.');
    } finally {
      setRoleBusy(false);
    }
  };

  const viewScores = async (user) => {
    setScoreUser(user);
    setScores([]);
    setScoresLoading(true);
    setError('');
    try {
      const response = await getUserScores(user.id);
      setScores(response.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Skorlar alınamadı.');
    } finally {
      setScoresLoading(false);
    }
  };

  const current = scores.length > 0 ? scores[scores.length - 1] : null;
  const chartData = scores.map((row) => ({ hafta: row.weekStart, skor: row.score }));

  // Kullanıcı araması: ad soyad + e-posta üzerinde, büyük/küçük harf duyarsız.
  const q = userSearch.trim().toLowerCase();
  const filteredUsers = q
    ? users.filter((u) =>
        `${u.firstName} ${u.lastName}`.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q))
    : users;

  return (
    <Layout>
      {error && <p className='error-msg'>{error}</p>}

      <h2>Sistem Ayarları</h2>
      <div className='setting-row'>
        <div>
          <strong>E-posta ile 2 adımlı doğrulama</strong>
          <p className='subtle'>
            Açıkken kullanıcılar giriş ve kayıtta e-postalarına gelen kodu girer.
            Yöneticiler bundan muaftır.
          </p>
        </div>
        <button
          className={email2faOn ? 'danger' : ''}
          onClick={toggle2fa}
          disabled={settingBusy}>
          {settingBusy ? '...' : email2faOn ? 'Kapat' : 'Aç'}
        </button>
      </div>
      <p className='subtle'>
        Durum: <strong>{email2faOn ? 'AÇIK' : 'KAPALI'}</strong>
      </p>

      <h2>Kullanıcılar</h2>
      <div className='search-box'>
        <input type='text' placeholder='Ad veya e-posta ile ara'
          value={userSearch}
          onChange={(e) => setUserSearch(e.target.value)} />
      </div>
      <table>
        <thead>
          <tr>
            <th>ad soyad</th>
            <th>e-posta</th>
            <th>rol</th>
            <th>findeks</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {filteredUsers.map((user) => (
            <tr key={user.id}>
              <td>{user.firstName} {user.lastName}</td>
              <td>{user.email}</td>
              <td>{user.role}</td>
              <td>
                {/* Yöneticilerin findeks raporu yok. */}
                {user.role === 'ADMIN' ? (
                  <span className='subtle'>—</span>
                ) : (
                  <a href='#' onClick={(e) => { e.preventDefault(); viewScores(user); }}>
                    skorları gör
                  </a>
                )}
              </td>
              <td>
                {/* Admin kendi rolünü değiştiremez -> kendi satırında düğme yok. */}
                {user.email === currentUser?.email ? (
                  <span className='subtle'>siz</span>
                ) : (
                  <a href='#' onClick={(e) => { e.preventDefault(); askRole(user); }}>
                    {user.role === 'ADMIN' ? 'USER yap' : 'ADMIN yap'}
                  </a>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {scoreUser && (
        <div className='score-panel'>
          <div className='score-panel-head'>
            <h2>{scoreUser.firstName} {scoreUser.lastName} — findeks geçmişi</h2>
            <a href='#' onClick={(e) => { e.preventDefault(); setScoreUser(null); }}>kapat</a>
          </div>

          {scoresLoading && <p className='subtle'>Yükleniyor...</p>}

          {!scoresLoading && current && (
            <>
              <div className={`score-card risk-${current.riskCategory}`}>
                <p className='score-number'>{current.score}</p>
                <p className='risk-label'>
                  güncel · {current.riskCategory.replace('_', ' ').toLowerCase()}
                </p>
              </div>

              <div className='chart-box'>
                <ResponsiveContainer width='100%' height='100%'>
                  <LineChart data={chartData} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
                    <CartesianGrid stroke='#eee' strokeDasharray='3 3' />
                    <XAxis dataKey='hafta' tick={{ fontSize: 11 }} minTickGap={24} />
                    <YAxis domain={[0, 1900]} tick={{ fontSize: 11 }} width={40} />
                    <Tooltip />
                    <Line type='monotone' dataKey='skor' stroke='#2a6437' strokeWidth={2} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              </div>

              <table>
                <thead>
                  <tr><th>hafta</th><th>skor</th><th>risk</th></tr>
                </thead>
                <tbody>
                  {scores.map((row) => (
                    <tr key={row.weekStart}>
                      <td>{row.weekStart}</td>
                      <td>{row.score}</td>
                      <td>{row.riskCategory.replace('_', ' ').toLowerCase()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}
        </div>
      )}

      <h2>Denetim Kayıtları</h2>
      <div className='search-box'>
        <input type='text' placeholder='Yapan e-posta ile ara'
          value={logSearch}
          onChange={(e) => setLogSearch(e.target.value)} />
      </div>
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
          {logs.length === 0 && (
            <tr><td colSpan={4} className='subtle'>kayıt bulunamadı</td></tr>
          )}
        </tbody>
      </table>

      {logTotalPages > 1 && (
        <div className='pager'>
          <button disabled={logPage <= 0} onClick={() => loadLogs(logPage - 1, logSearch)}>
            ‹ önceki
          </button>
          <span className='subtle'>sayfa {logPage + 1} / {logTotalPages}</span>
          <button disabled={logPage >= logTotalPages - 1} onClick={() => loadLogs(logPage + 1, logSearch)}>
            sonraki ›
          </button>
        </div>
      )}

      {roleTarget && (
        <div className='modal-overlay' onClick={closeRole}>
          <div className='modal' onClick={(e) => e.stopPropagation()}>
            {roleTarget.role === 'ADMIN' ? (
              <p>
                <strong>{roleTarget.firstName} {roleTarget.lastName}</strong> kullanıcısının
                <strong> yöneticiliğini kaldırmak</strong> üzeresiniz.
              </p>
            ) : (
              <p>
                <strong>{roleTarget.firstName} {roleTarget.lastName}</strong> kullanıcısını
                <strong> YÖNETİCİ</strong> yapmak üzeresiniz. Bu tehlikeli bir işlemdir —
                yöneticiler tüm kullanıcıları ve kayıtları görebilir, rolleri değiştirebilir.
              </p>
            )}

            <p className='subtle'>Onaylamak için şifrenizi tekrar girin.</p>
            {roleError && <p className='error-msg'>{roleError}</p>}

            <input type='password' placeholder='Şifreniz'
              value={rolePassword}
              onChange={(e) => setRolePassword(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter' && rolePassword) confirmRole(); }}
              autoFocus />

            <div className='modal-actions'>
              <button onClick={closeRole} disabled={roleBusy}>Vazgeç</button>
              <button className='danger' onClick={confirmRole} disabled={roleBusy || !rolePassword}>
                {roleBusy ? 'İşleniyor...' : 'Onayla'}
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
