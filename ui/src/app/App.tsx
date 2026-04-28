import {useState} from 'react';
import {ReadExplorerPage} from '../pages/read-explorer/ReadExplorerPage';
import {AnalyticsPage} from '../pages/analytics/AnalyticsPage';

type Section = 'read' | 'analytics';

const SECTIONS: Array<{ key: Section; title: string; description: string }> = [
    {key: 'read', title: 'Чтение данных', description: 'Запрос наблюдений, сравнение и таблица'},
    {key: 'analytics', title: 'Аналитика', description: 'Ряды, сравнение, поддерево и матрицы'}
];

export default function App() {
    const [section, setSection] = useState<Section>('read');
    return (
        <div className="app-shell">
            <nav className="app-section-nav" aria-label="Основные разделы ODAS">
                <div className="app-brand">
                    <strong>ODAS</strong>
                    <span>Платформа для анализа открытых финансовых данных регионов РФ</span>
                </div>
                <div className="app-section-tabs">
                    {SECTIONS.map((item) => (
                        <button
                            key={item.key}
                            type="button"
                            className={`app-section-tab ${section === item.key ? 'is-active' : ''}`}
                            onClick={() => setSection(item.key)}
                        >
                            <strong>{item.title}</strong>
                            <span>{item.description}</span>
                        </button>
                    ))}
                </div>
            </nav>
            {section === 'read' ? <ReadExplorerPage/> : <AnalyticsPage/>}
        </div>
    );
}
