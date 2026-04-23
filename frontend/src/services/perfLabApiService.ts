import axios from 'axios';
import type { ScenarioMetadata, TestRunRequest, TestRunResult } from '../types';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

export const perfLabApiService = {
  getScenarios: (): Promise<ScenarioMetadata[]> =>
    api.get<ScenarioMetadata[]>('/scenarios').then(r => r.data),

  runTest: (request: TestRunRequest): Promise<TestRunResult> =>
    api.post<TestRunResult>('/test/run', request).then(r => r.data),
};
