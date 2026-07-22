import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { login as loginApi } from '../api/authService';

export default function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await loginApi(form);
      login(response.data);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Giriş başarısız oldu.');
    }
  };

  return (
    <div className='page'>
      <header className='site-header'>
        <h1 className='site-title'>findeks miniscore</h1>
        <nav className='site-nav'>
          <Link to='/login' className='active'>giriş</Link>
          <Link to='/register'>kayıt ol</Link>
        </nav>
      </header>

      <h2>Oturum Aç</h2>
      {error && <p className='error-msg'>{error}</p>}

      <form className='narrow-form' onSubmit={handleSubmit}>
        <div className='field-row'>
          <input type='email' placeholder='E-posta'
            value={form.email}
            onChange={e => setForm({ ...form, email: e.target.value })} />
        </div>
        <div className='field-row'>
          <input type='password' placeholder='Şifre'
            value={form.password}
            onChange={e => setForm({ ...form, password: e.target.value })} />
        </div>
        <button type='submit'>Giriş Yap</button>
      </form>
    </div>
  );
}
