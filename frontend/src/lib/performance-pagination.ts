export type ServerPagination = {
  current: number;
  pageSize: number;
};

export function resolveServerPagination(
  current: number,
  pageSize: number,
  nextCurrent: number,
  nextPageSize: number,
): ServerPagination {
  return {
    current: nextPageSize === pageSize ? nextCurrent : 1,
    pageSize: nextPageSize,
  };
}

export function toPageRequest({ current, pageSize }: ServerPagination) {
  return { pageNum: current, pageSize };
}
