import { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import { getMyHistory } from '../api/scoreService';

export default function ReportPage() {
  const [history, setHistory] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMyHistory()
      .then((response) => setHistory(response.data))
      .catch((err) => setError(err.response?.data?.message || 'Geçmiş alınamadı.'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <Layout>
      <p className='subtle'>
        {loading ? 'Yükleniyor...' : `${history.length} kayıt bulundu`}
      </p>

      {error && <p className='error-msg'>{error}</p>}

      {history.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>hafta</th>
              <th>skor</th>
              <th>risk</th>
            </tr>
          </thead>
          <tbody>
            {history.map((row) => (
              <tr key={row.weekStart}>
                <td>{row.weekStart}</td>
                <td>{row.score}</td>
                <td>{row.riskCategory.replace('_', ' ').toLowerCase()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Layout>
  );
}
