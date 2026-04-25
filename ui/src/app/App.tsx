import {useState} from 'react';
import {ReadExplorerPage} from '../pages/read-explorer/ReadExplorerPage';
import {AnalyticsPage} from '../pages/analytics/AnalyticsPage';

type Section = 'read' | 'analytics';

export default function App() {
    const [section, setSection] = useState<Section>('read');
    return <div>
        <nav className="app-section-nav">
            <button type="button" className={`app-section-tab ${section === 'read' ? 'is-active' : ''}`}
                    onClick={() => setSection('read')}>Чтение данных
            </button>
            <button type="button" className={`app-section-tab ${section === 'analytics' ? 'is-active' : ''}`}
                    onClick={() => setSection('analytics')}>Аналитика
            </button>
        </nav>
        {section === 'read' ? <ReadExplorerPage/> : <AnalyticsPage/>}
    </div>;
}
