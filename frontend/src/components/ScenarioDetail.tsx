import { useState } from 'react';
import type { ScenarioMetadata } from '../types';

interface Props {
  scenario: ScenarioMetadata;
}

type Tab = 'before' | 'after' | 'why';

export default function ScenarioDetail({ scenario }: Props) {
  const [activeTab, setActiveTab] = useState<Tab>('before');

  const baselineCode   = scenario.baselineCode   || '// No baseline code available.';
  const optimizedCode  = scenario.optimizedCode  || '// No optimized code available.';
  const whyExplanation = scenario.whyExplanation || 'No explanation available.';

  return (
    <div className="scenario-detail">
      <div className="detail-tabs">
        <button
          className={activeTab === 'before' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('before')}
        >
          ❌ Before
        </button>
        <button
          className={activeTab === 'after' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('after')}
        >
          ✅ After
        </button>
        <button
          className={activeTab === 'why' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('why')}
        >
          💡 Why
        </button>
      </div>

      <div className="detail-content">
        {activeTab === 'before' && (
          <pre className="code-block baseline"><code>{baselineCode}</code></pre>
        )}
        {activeTab === 'after' && (
          <pre className="code-block optimized"><code>{optimizedCode}</code></pre>
        )}
        {activeTab === 'why' && (
          <p className="why-text">{whyExplanation}</p>
        )}
      </div>
    </div>
  );
}
