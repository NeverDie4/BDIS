export type AbstractRegion = {
  name: string;
  shortName: string;
  count: number;
  col: number;
  row: number;
  colSpan?: number;
};

export const abstractChongqingRegions: AbstractRegion[] = [
  { name: "荣昌区", shortName: "荣昌", count: 8, col: 1, row: 5 },
  { name: "大足区", shortName: "大足", count: 18, col: 2, row: 4 },
  { name: "永川区", shortName: "永川", count: 33, col: 3, row: 5 },
  { name: "江津区", shortName: "江津", count: 36, col: 4, row: 6, colSpan: 2 },
  { name: "合川区", shortName: "合川", count: 25, col: 3, row: 3 },
  { name: "渝北区", shortName: "渝北", count: 42, col: 5, row: 3 },
  { name: "江北区", shortName: "江北", count: 31, col: 6, row: 3 },
  { name: "九龙坡区", shortName: "九龙坡", count: 29, col: 5, row: 4 },
  { name: "南岸区", shortName: "南岸", count: 22, col: 6, row: 4 },
  { name: "渝中区", shortName: "渝中", count: 12, col: 7, row: 4 },
  { name: "长寿区", shortName: "长寿", count: 28, col: 7, row: 3 },
  { name: "涪陵区", shortName: "涪陵", count: 34, col: 8, row: 4 },
  { name: "南川区", shortName: "南川", count: 50, col: 7, row: 5 },
  { name: "武隆区", shortName: "武隆", count: 24, col: 8, row: 5 },
  { name: "彭水县", shortName: "彭水", count: 30, col: 9, row: 6 },
  { name: "黔江区", shortName: "黔江", count: 16, col: 10, row: 6 },
  { name: "酉阳县", shortName: "酉阳", count: 27, col: 10, row: 7 },
  { name: "秀山县", shortName: "秀山", count: 13, col: 11, row: 7 },
  { name: "丰都县", shortName: "丰都", count: 15, col: 8, row: 3 },
  { name: "忠县", shortName: "忠县", count: 26, col: 9, row: 3 },
  { name: "石柱县", shortName: "石柱", count: 20, col: 10, row: 4 },
  { name: "万州区", shortName: "万州", count: 38, col: 9, row: 2 },
  { name: "开州区", shortName: "开州", count: 21, col: 10, row: 2 },
  { name: "云阳县", shortName: "云阳", count: 32, col: 11, row: 2 },
  { name: "奉节县", shortName: "奉节", count: 48, col: 10, row: 1 },
  { name: "巫山县", shortName: "巫山", count: 45, col: 11, row: 1 },
  { name: "巫溪县", shortName: "巫溪", count: 35, col: 9, row: 1 },
  { name: "城口县", shortName: "城口", count: 11, col: 8, row: 1 },
];
