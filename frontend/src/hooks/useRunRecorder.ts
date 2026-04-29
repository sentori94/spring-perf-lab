import { useState, useEffect, useRef } from 'react';
import type { LiveMetrics } from '../types';
import { perfLabApiService } from '../services/perfLabApiService';

export interface RecordedPoint {
  t:        number;
  heap:     number | null;
  gcPause:  number | null;
  threads:  number | null;
}

const TAIL_MS          = 2000;
const POLL_INTERVAL_MS = 500;

export function useRunRecorder(isRunning: boolean) {
  const [points, setPoints]   = useState<RecordedPoint[]>([]);
  const startRef              = useRef<number | null>(null);
  const intervalRef           = useRef<ReturnType<typeof setInterval> | null>(null);
  const tailTimeoutRef        = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [startEpochMs, setStartEpochMs] = useState<number>(0);

  function stopPolling() {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }

  function startPolling() {
    stopPolling();
    intervalRef.current = setInterval(async () => {
      try {
        const m: LiveMetrics = await perfLabApiService.getLiveMetrics();
        const t = Date.now() - startRef.current!;
        setPoints(prev => [...prev, {
          t,
          heap:    m.heapUsedMb,
          gcPause: m.gcPauseMeanMs,
          threads: m.threadsLive,
        }]);
      } catch { /* backend unreachable */ }
    }, POLL_INTERVAL_MS);
  }

  useEffect(() => {
    if (isRunning) {
      // Annule le tail d'un run précédent si on relance immédiatement
      if (tailTimeoutRef.current) {
        clearTimeout(tailTimeoutRef.current);
        tailTimeoutRef.current = null;
      }
      stopPolling();
      setPoints([]);
      const now = Date.now();
      startRef.current = now;
      setStartEpochMs(now);
      startPolling();
    } else {
      // Run terminé — on laisse tourner le poll encore TAIL_MS ms.
      // On ne stoppe PAS ici : c'est le timeout qui stoppera.
      if (tailTimeoutRef.current) clearTimeout(tailTimeoutRef.current);
      tailTimeoutRef.current = setTimeout(() => {
        stopPolling();
        tailTimeoutRef.current = null;
      }, TAIL_MS);
    }
    // Pas de stopPolling() dans le cleanup : on veut que le tail survive
    // au changement de isRunning. On nettoie seulement au démontage.
    return () => { /* noop — cleanup au démontage géré ci-dessous */ };
  }, [isRunning]);

  // Cleanup au démontage uniquement
  useEffect(() => {
    return () => {
      stopPolling();
      if (tailTimeoutRef.current) clearTimeout(tailTimeoutRef.current);
    };
  }, []);

  return { points, startEpochMs };
}
