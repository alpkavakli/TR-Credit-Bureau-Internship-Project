import { useEffect, useState } from 'react';
import {
  CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
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

  // Grafik verisi: recharts XAxis için hafta başlangıcını, çizgi için skoru kullanıyoruz.
  const chartData = history.map((row) => ({ hafta: row.weekStart, skor: row.score }));

  return (
    <Layout>
      <p className='subtle'>
        {loading ? 'Yükleniyor...' : `${history.length} kayıt bulundu`}
      </p>

      {error && <p className='error-msg'>{error}</p>}

      {history.length > 0 && (
        <>
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
        </>
      )}
    </Layout>
  );
}
