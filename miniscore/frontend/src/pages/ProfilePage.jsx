import { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import { getMe, updateProfile } from '../api/userService';

export default function ProfilePage() {
  const [form, setForm] = useState({ firstName: '', lastName: '' });
  const [meta, setMeta] = useState(null);   // salt-okunur alanlar (e-posta, tc, rol)
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMe()
      .then((response) => {
        const u = response.data;
        setForm({ firstName: u.firstName, lastName: u.lastName });
        setMeta(u);
      })
      .catch((err) => setError(err.response?.data?.message || 'Profil alınamadı.'))
      .finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setOk('');
    try {
      const response = await updateProfile(form);
      setMeta(response.data);
      setOk('Profil güncellendi.');
    } catch (err) {
      setError(err.response?.data?.message || 'Güncelleme başarısız oldu.');
    }
  };

  return (
    <Layout>
      <h2>Profilim</h2>
      {loading && <p className='subtle'>Yükleniyor...</p>}
      {error && <p className='error-msg'>{error}</p>}
      {ok && <p className='ok-msg'>{ok}</p>}

      {meta && (
        <>
          <form className='narrow-form' onSubmit={handleSubmit}>
            <div className='field-row'>
              <input type='text' placeholder='Ad'
                value={form.firstName}
                onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
            </div>
            <div className='field-row'>
              <input type='text' placeholder='Soyad'
                value={form.lastName}
                onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
            </div>
            <button type='submit'>Kaydet</button>
          </form>

          <p className='subtle'>
            E-posta ve TC kimlik numarası değiştirilemez.
          </p>
          <table>
            <tbody>
              <tr><th>e-posta</th><td>{meta.email}</td></tr>
              <tr><th>tc kimlik no</th><td>{meta.tcNo}</td></tr>
              <tr><th>rol</th><td>{meta.role}</td></tr>
            </tbody>
          </table>
        </>
      )}
    </Layout>
  );
}
