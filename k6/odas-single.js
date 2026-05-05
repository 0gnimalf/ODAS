import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const YEAR = Number(__ENV.YEAR || 2024);
const MONTH = Number(__ENV.MONTH || 12);
const GROUP = __ENV.GROUP || "INCOME";
const HTTP_TIMEOUT = __ENV.HTTP_TIMEOUT || "180s";
const VALUE_KIND = __ENV.VALUE_KIND || "ACTUAL_CONSOLIDATED_SUBJECT_BUDGET";

export const options = {
    vus: 1,
    iterations: Number(__ENV.ITERATIONS || 6),
    setupTimeout: __ENV.SETUP_TIMEOUT || "10m",
    summaryTrendStats: ["avg", "min", "med", "max", "p(95)"]
};

const odasErrors = new Rate("odas_errors");

const op01Health = new Trend("op01_health", true);
const op02RegionsLocal = new Trend("op02_regions_local", true);
const op03GroupsLocal = new Trend("op03_groups_local", true);
const op04TreeLocal = new Trend("op04_tree_local", true);
const op05TreeSyncExternal = new Trend("op05_tree_sync_external", true);
const op06ObsLocalSmall = new Trend("op06_observations_local_1_region_1_indicator", true);
const op07ObsLocalWide = new Trend("op07_observations_local_10_regions_1_indicator", true);
const op08ObsExternalSmall = new Trend("op08_observations_external_1_region_1_indicator", true);
const op09ObsExternalWide = new Trend("op09_observations_external_10_regions_1_indicator", true);
const op10SeriesLocal = new Trend("op10_series_local_12_months", true);
const op11CompareLocal = new Trend("op11_compare_local_15_regions", true);
const op12MatrixLocal = new Trend("op12_matrix_local_10x10", true);

function assertOk(res, name) {
    const ok = check(res, {
        [`${name}: status 2xx`]: (r) => r.status >= 200 && r.status < 300
    });

    odasErrors.add(!ok);

    if (!ok) {
        console.error(`${name} failed: status=${res.status}, body=${String(res.body).slice(0, 1000)}`);
    }

    return ok;
}

function getJson(path, name) {
    const res = http.get(`${BASE_URL}${path}`, {
        tags: { name },
        timeout: HTTP_TIMEOUT
    });

    assertOk(res, name);

    if (!res.body || res.body.length === 0) {
        return null;
    }

    return res.json();
}

function postJson(path, body, name) {
    const res = http.post(
        `${BASE_URL}${path}`,
        JSON.stringify(body),
        {
            headers: { "Content-Type": "application/json" },
            tags: { name },
            timeout: HTTP_TIMEOUT
        }
    );

    assertOk(res, name);

    if (!res.body || res.body.length === 0) {
        return null;
    }

    return res.json();
}

function postNoBody(path, name) {
    const res = http.post(`${BASE_URL}${path}`, null, {
        tags: { name },
        timeout: HTTP_TIMEOUT
    });

    assertOk(res, name);

    if (!res.body || res.body.length === 0) {
        return null;
    }

    return res.json();
}

function flattenTree(nodes, result = []) {
    for (const node of nodes || []) {
        result.push(node);
        flattenTree(node.children || [], result);
    }
    return result;
}

function timed(metric, fn) {
    const started = Date.now();
    const result = fn();
    metric.add(Date.now() - started);
    return result;
}

function repeatedParam(name, values) {
    return values.map((value) => `${name}=${encodeURIComponent(value)}`).join("&");
}

function firstNIds(items, n, label) {
    const ids = (items || [])
        .map((item) => item.id)
        .filter((id) => id !== null && id !== undefined)
        .slice(0, n);

    if (ids.length < n) {
        throw new Error(`Not enough ${label}: expected ${n}, got ${ids.length}`);
    }

    return ids;
}

export function setup() {
    getJson("/api/health", "setup_health");

    postNoBody("/api/reference/regions/sync?force=false", "setup_regions_sync");
    postNoBody(`/api/reference/indicators/sync?year=${YEAR}&group=${GROUP}`, "setup_indicators_sync");

    const regions = getJson("/api/read/regions", "setup_regions");
    const tree = getJson(`/api/read/indicators/tree?group=${GROUP}&year=${YEAR}`, "setup_tree");

    const allIndicators = flattenTree(tree);

    const regionIds = firstNIds(regions, 20, "regions");
    const indicatorIds = firstNIds(allIndicators, 10, "indicators");

    const oneRegion = regionIds[0];
    const tenRegions = regionIds.slice(0, 10);
    const fifteenRegions = regionIds.slice(0, 15);
    const oneIndicator = indicatorIds[0];
    const tenIndicators = indicatorIds.slice(0, 10);

    // Наполнение локальной базы перед локальными замерами.
    getJson(
        `/api/read/observations?group=${GROUP}&year=${YEAR}&month=${MONTH}` +
        `&${repeatedParam("regionId", tenRegions)}` +
        `&indicatorYearEntryId=${oneIndicator}` +
        `&valueKind=${VALUE_KIND}` +
        `&includeChildren=false&forceRefresh=true`,
        "setup_seed_observations"
    );

    // Предварительный вызов ряда: при autoCollectMissing=true может подтянуть недостающие месяцы.
    postJson(
        "/api/analysis/series/monthly",
        {
            groupCode: GROUP,
            regionId: oneRegion,
            indicatorYearEntryId: oneIndicator,
            valueKind: VALUE_KIND,
            year: YEAR,
            month: MONTH,
            includeQuarterAggregates: true,
            autoCollectMissing: true,
            forceRefresh: false
        },
        "setup_seed_series"
    );

    return {
        oneRegion,
        tenRegions,
        fifteenRegions,
        oneIndicator,
        tenIndicators
    };
}

export default function (data) {
    timed(op01Health, () => {
        getJson("/api/health", "op01_health");
    });

    timed(op02RegionsLocal, () => {
        getJson("/api/read/regions", "op02_regions_local");
    });

    timed(op03GroupsLocal, () => {
        getJson("/api/read/groups", "op03_groups_local");
    });

    timed(op04TreeLocal, () => {
        getJson(`/api/read/indicators/tree?group=${GROUP}&year=${YEAR}`, "op04_tree_local");
    });

    timed(op05TreeSyncExternal, () => {
        postNoBody(`/api/reference/indicators/sync?year=${YEAR}&group=${GROUP}`, "op05_tree_sync_external");
    });

    timed(op06ObsLocalSmall, () => {
        getJson(
            `/api/read/observations?group=${GROUP}&year=${YEAR}&month=${MONTH}` +
            `&regionId=${data.oneRegion}` +
            `&indicatorYearEntryId=${data.oneIndicator}` +
            `&valueKind=${VALUE_KIND}` +
            `&includeChildren=false&forceRefresh=false`,
            "op06_observations_local_1_region_1_indicator"
        );
    });

    timed(op07ObsLocalWide, () => {
        getJson(
            `/api/read/observations?group=${GROUP}&year=${YEAR}&month=${MONTH}` +
            `&${repeatedParam("regionId", data.tenRegions)}` +
            `&indicatorYearEntryId=${data.oneIndicator}` +
            `&valueKind=${VALUE_KIND}` +
            `&includeChildren=false&forceRefresh=false`,
            "op07_observations_local_10_regions_1_indicator"
        );
    });

    timed(op08ObsExternalSmall, () => {
        getJson(
            `/api/read/observations?group=${GROUP}&year=${YEAR}&month=${MONTH}` +
            `&regionId=${data.oneRegion}` +
            `&indicatorYearEntryId=${data.oneIndicator}` +
            `&valueKind=${VALUE_KIND}` +
            `&includeChildren=false&forceRefresh=true`,
            "op08_observations_external_1_region_1_indicator"
        );
    });

    timed(op09ObsExternalWide, () => {
        getJson(
            `/api/read/observations?group=${GROUP}&year=${YEAR}&month=${MONTH}` +
            `&${repeatedParam("regionId", data.tenRegions)}` +
            `&indicatorYearEntryId=${data.oneIndicator}` +
            `&valueKind=${VALUE_KIND}` +
            `&includeChildren=false&forceRefresh=true`,
            "op09_observations_external_10_regions_1_indicator"
        );
    });

    timed(op10SeriesLocal, () => {
        postJson(
            "/api/analysis/series/monthly",
            {
                groupCode: GROUP,
                regionId: data.oneRegion,
                indicatorYearEntryId: data.oneIndicator,
                valueKind: VALUE_KIND,
                year: YEAR,
                month: MONTH,
                includeQuarterAggregates: true,
                autoCollectMissing: false,
                forceRefresh: false
            },
            "op10_series_local_12_months"
        );
    });

    timed(op11CompareLocal, () => {
        postJson(
            "/api/analysis/compare/regions",
            {
                groupCode: GROUP,
                year: YEAR,
                month: MONTH,
                indicatorYearEntryId: data.oneIndicator,
                valueKind: VALUE_KIND,
                regionIds: data.fifteenRegions,
                forceRefresh: false
            },
            "op11_compare_local_15_regions"
        );
    });

    timed(op12MatrixLocal, () => {
        postJson(
            "/api/analysis/matrix",
            {
                groupCode: GROUP,
                year: YEAR,
                month: MONTH,
                regionIds: data.tenRegions,
                indicatorYearEntryIds: data.tenIndicators,
                valueKind: VALUE_KIND,
                forceRefresh: false
            },
            "op12_matrix_local_10x10"
        );
    });

    sleep(0.2);
}