import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { register as registerApi } from '../api/authService';
import EmailVerify from '../components/EmailVerify';

export default function RegisterPage() {
  const [form, setForm] = useState({
    firstName: '', lastName: '', tcNo: '', email: '', password: '',
  });
  const [error, setError] = useState('');
  const [pendingEmail, setPendingEmail] = useState(null);   // 2FA: doğrulama bekleyen e-posta
  const { login } = useAuth();
  const navigate = useNavigate();

  const finishRegister = (authData) => {
    login(authData);
    navigate('/dashboard');   // yeni kayıtlar USER'dır
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const response = await registerApi(form);
      if (response.data.verificationRequired) {
        setPendingEmail(response.data.email);   // 2FA açıksa kod ekranına geç
      } else {
        finishRegister(response.data.auth);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Kayıt başarısız oldu.');
    }
  };

  const field = (name) => ({
    value: form[name],
    onChange: (e) => setForm({ ...form, [name]: e.target.value }),
  });

  return (
    <div className='page'>
      <header className='site-header'>
        <h1 className='site-title'>findeks miniscore</h1>
        <nav className='site-nav'>
          <Link to='/login'>giriş</Link>
          <Link to='/register' className='active'>kayıt ol</Link>
        </nav>
      </header>

      {pendingEmail ? (
        <EmailVerify email={pendingEmail} onVerified={finishRegister} />
      ) : (
        <>
          <h2>Kayıt Ol</h2>
          {error && <p className='error-msg'>{error}</p>}

          <form className='narrow-form' onSubmit={handleSubmit}>
            <div className='field-row'>
              <input type='text' placeholder='Ad' {...field('firstName')} />
            </div>
            <div className='field-row'>
              <input type='text' placeholder='Soyad' {...field('lastName')} />
            </div>
            <div className='field-row'>
              <input type='text' placeholder='TC Kimlik No (11 hane)' maxLength={11} {...field('tcNo')} />
            </div>
            <div className='field-row'>
              <input type='email' placeholder='E-posta' {...field('email')} />
            </div>
            <div className='field-row'>
              <input type='password' placeholder='Şifre (en az 8 karakter)' {...field('password')} />
            </div>
            <button type='submit'>Kaydol</button>
          </form>
        </>
      )}
    </div>
  );
}
