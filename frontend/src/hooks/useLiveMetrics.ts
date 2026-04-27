import { useState, useEffect, useRef } from 'react';
import type { LiveMetrics } from '../types';
import { perfLabApiService } from '../services/perfLabApiService';

const POLL_INTERVAL_MS = 500;

export function useLiveMetrics() {
  const [metrics, setMetrics] = useState<LiveMetrics | null>(null);
  const [error, setError]     = useState(false);
  const intervalRef           = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    let cancelled = false;

    const poll = async () => {
      try {
        const data = await perfLabApiService.getLiveMetrics();
        if (!cancelled) { setMetrics(data); setError(false); }
      } catch {
        if (!cancelled) setError(true);
      }
    };

    poll(); // premier appel immédiat
    intervalRef.current = setInterval(poll, POLL_INTERVAL_MS);

    return () => {
      cancelled = true;
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, []);

  return { metrics, error };
}
