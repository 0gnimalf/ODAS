import http from "k6/http";
import { check, sleep } from "k6";
import { Rate } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const YEAR = Number(__ENV.YEAR || 2024);
const MONTH = Number(__ENV.MONTH || 12);
const GROUP = __ENV.GROUP || "INCOME";
const VALUE_KIND = __ENV.VALUE_KIND || "ACTUAL_CONSOLIDATED_SUBJECT_BUDGET";
const HTTP_TIMEOUT = __ENV.HTTP_TIMEOUT || "180s";

export const options = {
    scenarios: {
        local_user_flow: {
            executor: "constant-vus",
            vus: Number(__ENV.VUS || 10),
            duration: __ENV.DURATION || "30s"
        }
    },
    setupTimeout: __ENV.SETUP_TIMEOUT || "10m",
    teardownTimeout: __ENV.TEARDOWN_TIMEOUT || "2m",
    summaryTrendStats: ["avg", "min", "med", "max", "p(95)"],
    thresholds: {
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["p(95)<3000"]
    }
};

const odasLoadErrors = new Rate("odas_load_errors");

function assertOk(res, name) {
    const ok = check(res, {
        [`${name}: status 2xx`]: (r) => r.status >= 200 && r.status < 300
    });

    odasLoadErrors.add(!ok);

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

    const tenRegions = regionIds.slice(0, 10);
    const oneIndicator = indicatorIds[0];

    getJson(
        `/api/read/observations?group=${GROUP}&year=${YEAR}&month=${MONTH}` +
        `&${repeatedParam("regionId", tenRegions)}` +
        `&indicatorYearEntryId=${oneIndicator}` +
        `&valueKind=${VALUE_KIND}` +
        `&includeChildren=false&forceRefresh=true`,
        "setup_seed_observations"
    );

    postJson(
        "/api/analysis/series/monthly",
        {
            groupCode: GROUP,
            regionId: regionIds[0],
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
        regionIds,
        indicatorIds
    };
}

export default function (data) {
    const region = data.regionIds[(__VU - 1) % data.regionIds.length];

    const oneIndicator = data.indicatorIds[0];
    const tenRegions = data.regionIds.slice(0, 10);
    const fiveIndicators = data.indicatorIds.slice(0, 5);

    getJson("/api/read/regions", "load_regions");

    getJson(
        `/api/read/observations?group=${GROUP}&year=${YEAR}&month=${MONTH}` +
        `&${repeatedParam("regionId", tenRegions)}` +
        `&indicatorYearEntryId=${oneIndicator}` +
        `&valueKind=${VALUE_KIND}` +
        `&includeChildren=false&forceRefresh=false`,
        "load_observations_local"
    );

    postJson(
        "/api/analysis/compare/regions",
        {
            groupCode: GROUP,
            year: YEAR,
            month: MONTH,
            indicatorYearEntryId: oneIndicator,
            valueKind: VALUE_KIND,
            regionIds: tenRegions,
            forceRefresh: false
        },
        "load_compare_local"
    );

    postJson(
        "/api/analysis/series/monthly",
        {
            groupCode: GROUP,
            regionId: region,
            indicatorYearEntryId: oneIndicator,
            valueKind: VALUE_KIND,
            year: YEAR,
            month: MONTH,
            includeQuarterAggregates: true,
            autoCollectMissing: false,
            forceRefresh: false
        },
        "load_series_local"
    );

    postJson(
        "/api/analysis/matrix",
        {
            groupCode: GROUP,
            year: YEAR,
            month: MONTH,
            regionIds: tenRegions,
            indicatorYearEntryIds: fiveIndicators,
            valueKind: VALUE_KIND,
            forceRefresh: false
        },
        "load_matrix_local"
    );

    sleep(1);
}