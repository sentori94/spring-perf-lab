import type { ReactNode } from 'react';

interface MetricCardProps {
  name: string;
  unit?: string;
  icon: string;
  what: string;
  good: string;
  bad: string;
  scenarios?: string[];
}

function MetricCard({ name, unit, icon, what, good, bad, scenarios }: MetricCardProps) {
  return (
    <div className="info-metric-card">
      <div className="info-metric-header">
        <span className="info-metric-icon">{icon}</span>
        <div>
          <span className="info-metric-name">{name}</span>
          {unit && <span className="info-metric-unit">{unit}</span>}
        </div>
      </div>
      <p className="info-metric-what">{what}</p>
      <div className="info-metric-signals">
        <span className="info-signal good">✓ {good}</span>
        <span className="info-signal bad">✗ {bad}</span>
      </div>
      {scenarios && scenarios.length > 0 && (
        <div className="info-metric-scenarios">
          {scenarios.map(s => <span key={s} className="info-scenario-tag">{s}</span>)}
        </div>
      )}
    </div>
  );
}

interface SectionProps {
  title: string;
  children: ReactNode;
}

function Section({ title, children }: SectionProps) {
  return (
    <section className="info-section">
      <h2 className="info-section-title">{title}</h2>
      {children}
    </section>
  );
}

interface ConceptCardProps {
  term: string;
  icon: string;
  definition: string;
  analogy?: string;
}

function ConceptCard({ term, icon, definition, analogy }: ConceptCardProps) {
  return (
    <div className="info-concept-card">
      <div className="info-concept-header">
        <span className="info-concept-icon">{icon}</span>
        <span className="info-concept-term">{term}</span>
      </div>
      <p className="info-concept-def">{definition}</p>
      {analogy && <p className="info-concept-analogy">💡 {analogy}</p>}
    </div>
  );
}

export default function InfoPage() {
  return (
    <div className="info-page">

      {/* ── Hero ── */}
      <div className="info-hero">
        <h1>⚡ spring-perf-lab</h1>
        <p>
          Un outil de benchmarking Spring Boot qui mesure l'impact <strong>réel et chiffré</strong> des
          optimisations Java. Chaque scénario compare une implémentation volontairement non-optimisée
          (baseline) à son équivalent optimisé, et produit un diff de métriques JVM.
        </p>
      </div>

      {/* ── Comment ça marche ── */}
      <Section title="Comment ça marche">
        <div className="info-flow">
          <div className="info-flow-step">
            <span className="info-flow-num">1</span>
            <div>
              <strong>Sélectionne un scénario</strong>
              <p>Chaque scénario a une version baseline (mauvaise pratique) et une version optimisée.</p>
            </div>
          </div>
          <div className="info-flow-arrow">→</div>
          <div className="info-flow-step">
            <span className="info-flow-num">2</span>
            <div>
              <strong>Lance le benchmark</strong>
              <p>Le backend exécute les deux versions et capture les métriques JVM avant/après via Micrometer.</p>
            </div>
          </div>
          <div className="info-flow-arrow">→</div>
          <div className="info-flow-step">
            <span className="info-flow-num">3</span>
            <div>
              <strong>Analyse le diff</strong>
              <p>Les deltas montrent exactement ce que l'optimisation a gagné en temps, mémoire et GC.</p>
            </div>
          </div>
        </div>

        <div className="info-modes">
          <div className="info-mode-card">
            <span className="info-mode-tag">QUICK</span>
            <p>Une seule passe baseline + optimized. Idéal pour observer l'impact unitaire d'une optimisation.</p>
          </div>
          <div className="info-mode-card">
            <span className="info-mode-tag">LOAD</span>
            <p>Le baseline tourne en boucle pendant ~5s pour établir N itérations réalistes, puis l'optimized fait exactement N itérations. Compare le throughput réel.</p>
          </div>
        </div>
      </Section>

      {/* ── Vue d'ensemble JVM ── */}
      <Section title="Vue d'ensemble — La JVM en un coup d'oeil">
        <p className="info-overview-intro">
          Avant de plonger dans les concepts individuels, voici comment les grandes zones de la JVM
          s'articulent. Tout le code Java passe par ces étapes et ces zones mémoire.
        </p>

        {/* Schéma */}
        <div className="info-diagram">

          {/* Colonne gauche : exécution */}
          <div className="info-diagram-col">
            <div className="info-diagram-label">Ton code Java</div>

            <div className="info-diagram-box info-diagram-box--code">
              <span className="info-diagram-box-title">📝 Bytecode (.class)</span>
              <span className="info-diagram-box-desc">Compilé par javac</span>
            </div>

            <div className="info-diagram-arrow-v">↓</div>

            <div className="info-diagram-box info-diagram-box--jit">
              <span className="info-diagram-box-title">⚡ JIT Compiler</span>
              <span className="info-diagram-box-desc">Hot paths → code natif</span>
            </div>

            <div className="info-diagram-arrow-v">↓</div>

            <div className="info-diagram-box info-diagram-box--threads">
              <span className="info-diagram-box-title">🧵 Threads</span>
              <span className="info-diagram-box-desc">
                Platform threads (OS)<br />
                Virtual threads (Loom)
              </span>
            </div>
          </div>

          {/* Séparateur */}
          <div className="info-diagram-sep">→</div>

          {/* Colonne centrale : heap */}
          <div className="info-diagram-col info-diagram-col--main">
            <div className="info-diagram-label">Mémoire Heap (RAM)</div>

            <div className="info-diagram-heap">
              <div className="info-diagram-heap-zone info-diagram-heap-young">
                <span className="info-diagram-heap-zone-title">🌱 Young Generation</span>
                <div className="info-diagram-heap-subzones">
                  <div className="info-diagram-heap-sub">Eden<br/><small>nouveaux objets</small></div>
                  <div className="info-diagram-heap-sub">S0</div>
                  <div className="info-diagram-heap-sub">S1</div>
                </div>
                <span className="info-diagram-heap-gc">← Minor GC (fréquent, rapide)</span>
              </div>

              <div className="info-diagram-heap-arrow">↓ survit N cycles</div>

              <div className="info-diagram-heap-zone info-diagram-heap-old">
                <span className="info-diagram-heap-zone-title">🏛 Old Generation</span>
                <span className="info-diagram-heap-zone-desc">Caches · Singletons Spring · Sessions</span>
                <span className="info-diagram-heap-gc">← Major / Full GC (rare, lent)</span>
              </div>
            </div>
          </div>

          {/* Séparateur */}
          <div className="info-diagram-sep">→</div>

          {/* Colonne droite : GC */}
          <div className="info-diagram-col">
            <div className="info-diagram-label">Garbage Collector</div>

            <div className="info-diagram-box info-diagram-box--gc">
              <span className="info-diagram-box-title">🧹 G1 GC (défaut)</span>
              <span className="info-diagram-box-desc">Pauses courtes, régions</span>
            </div>

            <div className="info-diagram-arrow-v">ou</div>

            <div className="info-diagram-box info-diagram-box--zgc">
              <span className="info-diagram-box-title">🚄 ZGC</span>
              <span className="info-diagram-box-desc">Pauses &lt; 1ms, concurrent</span>
            </div>

            <div className="info-diagram-arrow-v">↓ si heap pleine</div>

            <div className="info-diagram-box info-diagram-box--oom">
              <span className="info-diagram-box-title">💀 OOM</span>
              <span className="info-diagram-box-desc">OutOfMemoryError</span>
            </div>
          </div>

        </div>

        {/* Légende du flux */}
        <div className="info-overview-flow">
          <div className="info-overview-flow-item">
            <span className="info-overview-flow-dot" style={{background: 'var(--accent)'}} />
            <span><strong>Allocation</strong> — chaque <code>new Objet()</code> atterrit dans Eden</span>
          </div>
          <div className="info-overview-flow-item">
            <span className="info-overview-flow-dot" style={{background: '#f59e0b'}} />
            <span><strong>Minor GC</strong> — Eden plein → objets morts supprimés, survivants passent en S0/S1</span>
          </div>
          <div className="info-overview-flow-item">
            <span className="info-overview-flow-dot" style={{background: 'var(--accent-cyan)'}} />
            <span><strong>Promotion</strong> — après N Minor GC, les survivants montent en Old Gen</span>
          </div>
          <div className="info-overview-flow-item">
            <span className="info-overview-flow-dot" style={{background: 'var(--bad)'}} />
            <span><strong>Full GC</strong> — Old Gen pleine → stop-the-world, toute la heap nettoyée</span>
          </div>
        </div>
      </Section>

      {/* ── Concepts JVM ── */}
      <Section title="Concepts JVM & GC">
        <div className="info-concepts-grid">

          <ConceptCard
            icon="🏗"
            term="Heap"
            definition="Zone mémoire RAM où la JVM stocke tous les objets créés à l'exécution (new String(), new ArrayList()...). Géré automatiquement par le Garbage Collector. Configuré via -Xms (taille initiale) et -Xmx (taille max)."
            analogy="Un bureau de travail : plus il est grand, plus on peut poser d'objets dessus avant de devoir ranger."
          />

          <ConceptCard
            icon="🌱"
            term="Young Generation"
            definition="Sous-zone du heap où naissent tous les nouveaux objets. Divisée en Eden (création) et deux espaces Survivor (S0/S1). Les Minor GC nettoient cette zone très fréquemment et très rapidement car la plupart des objets meurent jeunes."
            analogy="La corbeille à papier : se remplit vite, se vide vite."
          />

          <ConceptCard
            icon="🏛"
            term="Old Generation"
            definition="Sous-zone du heap pour les objets ayant survécu plusieurs cycles GC (caches, singletons Spring, connexions DB...). Nettoyée par les Major/Full GC, beaucoup plus rares mais plus lents. Une Old Gen pleine mène à l'OOM."
            analogy="L'armoire de rangement : contient ce qui dure, se vide rarement."
          />

          <ConceptCard
            icon="🧹"
            term="Garbage Collector (GC)"
            definition="Processus JVM qui identifie et libère automatiquement les objets qui ne sont plus référencés. Il existe plusieurs algorithmes : G1 (défaut Java 9+), ZGC (Java 15+, pauses ultra-courtes), Shenandoah. Le choix impacte directement les pauses et le throughput."
            analogy="Un agent de nettoyage qui passe régulièrement vider les poubelles — parfois il faut tout arrêter pour qu'il puisse travailler (stop-the-world)."
          />

          <ConceptCard
            icon="⏸"
            term="Stop-The-World (STW)"
            definition="Phase pendant laquelle le GC suspend TOUS les threads applicatifs pour effectuer son travail en toute sécurité. Pendant ce temps, l'app ne répond plus. Les GC modernes (ZGC, Shenandoah) minimisent drastiquement ces phases."
            analogy="Comme devoir fermer un restaurant pour nettoyer la cuisine — ZGC permet de nettoyer sans fermer."
          />

          <ConceptCard
            icon="🚀"
            term="Allocation Rate"
            definition="Vitesse à laquelle de nouveaux objets sont créés sur le heap (MB/s). Un taux élevé signifie que Eden se remplit vite → GC plus fréquent → plus de pauses. String += en boucle peut atteindre 16 000 MB/s vs ~0 pour StringBuilder."
            analogy="Le débit d'un robinet qui remplit une baignoire : plus c'est rapide, plus il faut vider souvent."
          />

          <ConceptCard
            icon="💀"
            term="OutOfMemoryError (OOM)"
            definition="Erreur fatale lancée quand la JVM ne peut plus allouer d'objet même après un Full GC. Ce n'est pas une Exception mais un Error — il échappe aux catch classiques. Précédé de Full GC en boucle, pauses de plusieurs secondes, heap à 99%."
            analogy="Le bureau est tellement plein qu'on ne peut plus poser le moindre stylo, même après avoir tout rangé."
          />

          <ConceptCard
            icon="📊"
            term="GC Overhead"
            definition={"Si la JVM passe plus de 98% de son temps à faire du GC pour ne récupérer que 2% du heap, elle abandonne et lance un OOM avec le message \"GC overhead limit exceeded\". Un signal que le code crée trop d'objets jetables."}
            analogy="Le nettoyeur passe tout son temps à vider la poubelle mais elle se remplit aussitôt — travail sisyphéen."
          />

          <ConceptCard
            icon="🧵"
            term="Thread virtuel (Project Loom)"
            definition="Introduit en Java 21, un thread virtuel est géré par la JVM (et non l'OS). Des milliers peuvent coexister car ils sont multipléxés sur un petit pool de threads porteurs. Quand un thread virtuel attend (I/O, sleep), il libère son thread porteur sans bloquer."
            analogy="Des postits sur un bureau : légers, nombreux, n'occupent un employé que quand ils nécessitent une action."
          />

          <ConceptCard
            icon="🔁"
            term="JIT Compilation"
            definition="Le Just-In-Time compiler (C1/C2) transforme le bytecode Java en code machine natif à l'exécution. Les méthodes appelées fréquemment (hot paths) sont optimisées progressivement. C'est pourquoi les benchmarks LOAD donnent des résultats plus réalistes que QUICK."
            analogy="Un traducteur qui mémorise les phrases répétées pour les traduire de plus en plus vite."
          />

        </div>
      </Section>

      {/* ── Métriques ── */}
      <Section title="Glossaire des métriques">
        <div className="info-metrics-grid">

          <MetricCard
            icon="⏱"
            name="Elapsed"
            unit="ms"
            what="Temps réel écoulé entre le début et la fin du scénario (wall-clock time). C'est la métrique la plus directe : ce que l'utilisateur ressent."
            good="Valeur basse — l'opération est rapide"
            bad="Valeur haute — latence perceptible"
            scenarios={['Tous les scénarios']}
          />

          <MetricCard
            icon="🖥"
            name="CPU Time"
            unit="ms"
            what="Temps passé par le CPU à exécuter le code du thread (hors temps d'attente I/O ou sleep). Si CPU Time << Elapsed, le thread passe son temps à attendre."
            good="CPU Time ≈ Elapsed → code CPU-bound efficace"
            bad="CPU Time << Elapsed → thread bloqué sur I/O ou locks"
            scenarios={['Virtual Threads', 'N+1']}
          />

          <MetricCard
            icon="🗂"
            name="Heap Used"
            unit="MB"
            what="Quantité de mémoire heap occupée par les objets vivants à la fin du scénario. Inclut Young Generation + Old Generation."
            good="Valeur basse — peu d'objets en mémoire"
            bad="Valeur haute — risque de pression GC ou OOM"
            scenarios={['Autoboxing', 'N+1', 'StringBuilder']}
          />

          <MetricCard
            icon="⏸"
            name="GC Pause"
            unit="ms"
            what="Durée pendant laquelle la JVM suspend tous les threads (stop-the-world) pour collecter les objets morts. Pendant ce temps, l&apos;app ne répond plus."
            good="< 5ms — invisible pour l'utilisateur"
            bad="> 50ms — freeze perceptible, > 500ms critique"
            scenarios={['ZGC vs G1', 'StringBuilder', 'Autoboxing']}
          />

          <MetricCard
            icon="🔢"
            name="GC Count"
            unit=""
            what="Nombre total de cycles GC déclenchés pendant le scénario. Un GC count élevé signifie que le heap se remplit et se vide très fréquemment."
            good="Valeur basse — peu de pression mémoire"
            bad="Valeur haute — allocation rate trop élevé"
            scenarios={['ZGC vs G1', 'StringBuilder']}
          />

          <MetricCard
            icon="🚀"
            name="Alloc Rate"
            unit="MB/s"
            what="Vitesse à laquelle de nouveaux objets sont créés sur le heap. Un taux élevé signifie que le GC doit travailler en permanence pour libérer la mémoire."
            good="≈ 0 — peu ou pas d'allocation (ex: StringBuilder)"
            bad="> 1000 MB/s — objets jetables en masse (ex: String +=)"
            scenarios={['Autoboxing', 'StringBuilder', 'ZGC vs G1']}
          />

          <MetricCard
            icon="🗄"
            name="SQL Queries"
            unit=""
            what="Nombre de requêtes SQL exécutées pendant le scénario. Le problème N+1 se lit directement ici : N entités = N+1 requêtes au lieu d'une seule avec JOIN FETCH."
            good="1 requête — JOIN FETCH ou cache actif"
            bad="N+1 requêtes — lazy loading non maîtrisé"
            scenarios={['N+1', 'Caffeine Cache']}
          />

        </div>
      </Section>

      {/* ── Scénarios ── */}
      <Section title="Les scénarios">
        <div className="info-scenarios-list">
          {[
            { id: 'N+1',            impact: 'HIGH',   desc: 'Lazy loading JPA qui déclenche N requêtes SQL au lieu d\'une seule avec JOIN FETCH.' },
            { id: 'Caffeine Cache', impact: 'HIGH',   desc: 'Mise en cache des résultats coûteux avec Caffeine. Mesure le cache hit rate via Micrometer.' },
            { id: 'ZGC vs G1',      impact: 'MEDIUM', desc: 'Compare les pauses GC entre G1 (défaut) et ZGC sur un workload avec beaucoup d\'objets court-lived.' },
            { id: 'Autoboxing',     impact: 'LOW',    desc: 'HashMap<Integer,Integer> vs int[] — le boxing silencieux génère des milliers d\'objets inutiles.' },
            { id: 'Virtual Threads',impact: 'HIGH',   desc: '500 tâches I/O-bound sur 50 threads OS vs threads virtuels Java 21 — throughput x10.' },
          ].map(s => (
            <div key={s.id} className="info-scenario-row">
              <span className={`impact-badge impact-${s.impact.toLowerCase()}`}>{s.impact}</span>
              <strong className="info-scenario-name">{s.id}</strong>
              <span className="info-scenario-desc">{s.desc}</span>
            </div>
          ))}
        </div>
      </Section>

      {/* ── Stack ── */}
      <Section title="Stack technique">
        <div className="info-stack">
          {[
            { label: 'Java 21',        color: '#f59e0b' },
            { label: 'Spring Boot 4',  color: '#6366f1' },
            { label: 'Micrometer',     color: '#22d3ee' },
            { label: 'HikariCP',       color: '#22c55e' },
            { label: 'Caffeine',       color: '#f59e0b' },
            { label: 'PostgreSQL',     color: '#60a5fa' },
            { label: 'React 18',       color: '#22d3ee' },
            { label: 'TypeScript',     color: '#6366f1' },
            { label: 'Recharts',       color: '#a78bfa' },
          ].map(t => (
            <span key={t.label} className="info-stack-tag" style={{ borderColor: t.color, color: t.color }}>
              {t.label}
            </span>
          ))}
        </div>
      </Section>

    </div>
  );
}
