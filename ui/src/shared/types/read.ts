export type IndicatorGroupCode = 'INCOME' | 'OUTCOME' | 'CREDIT' | 'FIN_SOURCE' | 'OTHER';

export interface IndicatorGroupReadDto {
    code: IndicatorGroupCode;
    label: string;
}

export interface RegionReadDto {
    id: number;
    name: string;
    federalDistrictCode: string;
    federalDistrictName: string;
    federalDistrictFullName: string;
    federalDistrictShortName: string;
}

export interface IndicatorTreeNodeReadDto {
    id: number;
    indicatorId: number;
    name: string;
    groupCode: IndicatorGroupCode;
    parentIndicatorYearEntryId: number | null;
    level: number;
    sortOrder: number | null;
    hasChildren: boolean;
    children: IndicatorTreeNodeReadDto[];
}

export interface ObservationReadDto {
    observationId: number;
    regionId: number;
    regionName: string;
    indicatorYearEntryId: number;
    indicatorName: string;
    valueKind: string;
    valueKindLabel: string;
    unitCode: string;
    unitCodeLabel: string;
    valueType: string;
    value: number;
    datasetCollectionId: number;
}

export interface ObservationReadResultDto {
    groupCode: IndicatorGroupCode;
    year: number;
    month: number;
    periodId: number | null;
    regionIds: number[];
    indicatorYearEntryIds: number[];
    total: number;
    observations: ObservationReadDto[];
}

export interface ObservationQuery {
    groupCode: IndicatorGroupCode;
    year: number;
    month: number;
    regionIds: number[];
    indicatorYearEntryIds: number[];
    valueKinds?: string[];
    includeChildren: boolean;
    forceRefresh: boolean;
}
