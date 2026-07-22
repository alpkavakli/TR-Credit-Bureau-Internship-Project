import { useState } from 'react';
import { verify as verifyApi } from '../api/authService';

/**
 * E-posta 2FA'nın ikinci adımı. login/register "verificationRequired" dönünce gösterilir.
 * Kod doğrulanınca backend asıl token'ları döner; onVerified(authData) ile üst bileşene iletir.
 */
export default function EmailVerify({ email, onVerified }) {
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const response = await verifyApi(email, code);
      onVerified(response.data);   // AuthResponse: { token, refreshToken, email, role }
    } catch (err) {
      setError(err.response?.data?.message || 'Doğrulama başarısız oldu.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <h2>E-posta Doğrulama</h2>
      <p className='subtle'>
        <strong>{email}</strong> adresine gönderilen doğrulama kodunu girin.
      </p>
      {error && <p className='error-msg'>{error}</p>}

      <form className='narrow-form' onSubmit={handleSubmit}>
        <div className='field-row'>
          <input type='text' inputMode='numeric' placeholder='Doğrulama kodu'
            value={code}
            onChange={(e) => setCode(e.target.value)}
            autoFocus />
        </div>
        <button type='submit' disabled={busy || !code}>
          {busy ? 'Doğrulanıyor...' : 'Doğrula'}
        </button>
      </form>
    </>
  );
}
