import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Referans görseldeki başlık şeridi: solda site adı, yanında düz metin
 * bağlantılar, altında ince ayraç çizgisi. Bulunulan sayfanın bağlantısı
 * NavLink sayesinde otomatik 'active' sınıfını alır ve düz siyah görünür.
 */
export default function Layout({ children }) {
  const { user, logout } = useAuth();

  return (
    <div className='page'>
      <header className='site-header'>
        <h1 className='site-title'>findeks miniscore</h1>
        <nav className='site-nav'>
          {/* ADMIN'in findeks raporu yok -> skorum/geçmiş menüsü sadece USER'a gösterilir. */}
          {user?.role !== 'ADMIN' && <NavLink to='/dashboard'>skorum</NavLink>}
          {user?.role !== 'ADMIN' && <NavLink to='/reports'>geçmiş</NavLink>}
          <NavLink to='/profile'>profil</NavLink>
          {user?.role === 'ADMIN' && <NavLink to='/admin'>yönetim</NavLink>}
        </nav>
        <span className='spacer' />
        {user && (
          <>
            <span className='user-email'>{user.email}</span>
            <a href='#' onClick={(e) => { e.preventDefault(); logout(); }}>çıkış</a>
          </>
        )}
      </header>
      <main>{children}</main>
    </div>
  );
}
