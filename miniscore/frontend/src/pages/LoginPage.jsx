import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { login as loginApi } from '../api/authService';
import EmailVerify from '../components/EmailVerify';

export default function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [pendingEmail, setPendingEmail] = useState(null);   // 2FA: doğrulama bekleyen e-posta
  const { login } = useAuth();
  const navigate = useNavigate();

  // Hem 2FA'sız giriş hem de kod doğrulaması sonrası ortak: oturumu aç, yönlendir.
  const finishLogin = (authData) => {
    login(authData);
    navigate(authData.role === 'ADMIN' ? '/admin' : '/dashboard');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const response = await loginApi(form);
      if (response.data.verificationRequired) {
        setPendingEmail(response.data.email);   // kod ekranına geç
      } else {
        finishLogin(response.data.auth);
      }
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

      {pendingEmail ? (
        <EmailVerify email={pendingEmail} onVerified={finishLogin} />
      ) : (
        <>
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
        </>
      )}
    </div>
  );
}
