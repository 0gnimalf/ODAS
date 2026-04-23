import ReactECharts from 'echarts-for-react';
import {formatObservationValue} from '../../shared/lib/format';
import type {ObservationReadResultDto} from '../../shared/types/read';

interface RegionCompareBarChartProps {
    result: ObservationReadResultDto | null;
}

export function RegionCompareBarChart({result}: RegionCompareBarChartProps) {
    if (!result || result.observations.length === 0) {
        return null;
    }

    const indicatorIds = new Set(result.observations.map((item) => item.indicatorYearEntryId));
    if (indicatorIds.size !== 1) {
        return null;
    }

    const sortedObservations = [...result.observations].sort((left, right) => {
        if (left.regionName === right.regionName) {
            return left.valueKindLabel.localeCompare(right.valueKindLabel, 'ru');
        }
        return left.regionName.localeCompare(right.regionName, 'ru');
    });

    const regionNames = Array.from(new Set(sortedObservations.map((item) => item.regionName)));
    const valueKinds = Array.from(new Set(sortedObservations.map((item) => item.valueKindLabel)));
    const unitCodes = Array.from(new Set(sortedObservations.map((item) => item.unitCode)));
    const unitSuffix = unitCodes.length === 1 ? unitCodes[0] : '';
    const title = sortedObservations[0]?.indicatorName ?? 'Сравнение регионов';

    const series = valueKinds.map((valueKindLabel) => ({
        name: valueKindLabel,
        type: 'bar' as const,
        data: regionNames.map((regionName) => {
            const match = sortedObservations.find(
                (item) => item.regionName === regionName && item.valueKindLabel === valueKindLabel
            );
            return match ? match.value : null;
        })
    }));

    return (
        <section className="panel chart-panel">
            <div className="panel-header compact-gap align-start">
                <div>
                    <h2>Сравнение регионов</h2>
                    <p>{title}</p>
                </div>
            </div>

            <ReactECharts
                style={{height: 420}}
                option={{
                    tooltip: {
                        trigger: 'axis',
                        axisPointer: {
                            type: 'shadow'
                        },
                        valueFormatter: (value: number) => `${formatObservationValue(value)}${unitSuffix ? ` ${unitSuffix}` : ''}`
                    },
                    legend: {
                        type: 'scroll'
                    },
                    grid: {
                        left: 60,
                        right: 24,
                        bottom: 90,
                        top: 48
                    },
                    xAxis: {
                        type: 'category',
                        data: regionNames,
                        axisLabel: {
                            interval: 0,
                            rotate: 35
                        }
                    },
                    yAxis: {
                        type: 'value',
                        name: unitSuffix || undefined
                    },
                    series
                }}
            />
        </section>
    );
}
