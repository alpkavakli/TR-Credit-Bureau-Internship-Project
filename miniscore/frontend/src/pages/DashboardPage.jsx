import { useState } from 'react';
import Layout from '../components/Layout';
import { getMyScore } from '../api/scoreService';

export default function DashboardPage() {
  const [score, setScore] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const fetchScore = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await getMyScore();
      setScore(response.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Skor sorgulanamadı.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <p className='subtle'>
        Kredi skorunuz haftalık olarak hesaplanır. Sorgulamak için düğmeye basın.
      </p>

      <button onClick={fetchScore} disabled={loading}>
        {loading ? 'Sorgulanıyor...' : 'Kredi Skorum'}
      </button>

      {error && <p className='error-msg'>{error}</p>}

      {score && (
        <div className={`score-card risk-${score.riskCategory}`}>
          <h2>{score.firstName} {score.lastName}</h2>
          <p className='score-number'>{score.score}</p>
          <p className='risk-label'>{score.riskCategory.replace('_', ' ').toLowerCase()}</p>
        </div>
      )}
    </Layout>
  );
}
