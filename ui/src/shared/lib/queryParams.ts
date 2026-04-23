export function appendRepeatedNumberParams(
    searchParams: URLSearchParams,
    name: string,
    values: number[]
): void {
    values.forEach((value) => {
        searchParams.append(name, String(value));
    });
}
