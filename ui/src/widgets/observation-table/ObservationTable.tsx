import {formatObservationValue} from '../../shared/lib/format';
import type {ObservationReadResultDto} from '../../shared/types/read';

interface ObservationTableProps {
    result: ObservationReadResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
}

export function ObservationTable({result, loading, error, isDirty}: ObservationTableProps) {
    if (loading) {
        return (
            <section className="panel table-panel">
                <h2>Наблюдения</h2>
                <div className="empty-state">Загрузка наблюдений…</div>
            </section>
        );
    }

    if (error) {
        return (
            <section className="panel table-panel">
                <h2>Наблюдения</h2>
                <div className="error-state">{error}</div>
            </section>
        );
    }

    if (!result) {
        return (
            <section className="panel table-panel">
                <h2>Наблюдения</h2>
                <div className="empty-state">Запрос ещё не выполнялся.</div>
            </section>
        );
    }

    return (
        <section className="panel table-panel">
            <div className="panel-header compact-gap align-start">
                <div>
                    <h2>Наблюдения</h2>
                    <p>
                        Всего записей: <strong>{result.total}</strong>
                    </p>
                </div>
                {isDirty && <div className="warning-badge">Фильтры изменены — нажмите «Показать данные»</div>}
            </div>

            {result.observations.length === 0 ? (
                <div className="empty-state">По заданным параметрам наблюдения не найдены.</div>
            ) : (
                <div className="table-wrapper">
                    <table>
                        <thead>
                        <tr>
                            <th>Регион</th>
                            <th>Показатель</th>
                            <th>Вид значения</th>
                            {/*<th>Тип значения</th>*/}
                            <th>Ед. изм.</th>
                            <th>Значение</th>
                            <th>Dataset</th>
                        </tr>
                        </thead>
                        <tbody>
                        {result.observations.map((observation) => (
                            <tr key={observation.observationId}>
                                <td>{observation.regionName}</td>
                                <td>{observation.indicatorName}</td>
                                <td>{observation.valueKindLabel}</td>
                                {/*<td>{observation.valueType}</td>*/}
                                <td>{observation.unitCodeLabel}</td>
                                <td>{formatObservationValue(observation.value)}</td>
                                <td>{observation.datasetCollectionId}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    );
}
