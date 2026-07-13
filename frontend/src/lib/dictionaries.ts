import { apiGet } from "@/lib/request";

export type DictionaryItemApi = {
  id: number;
  itemCode: string;
  itemName: string;
  itemValue?: string;
  status?: number;
  children?: DictionaryItemApi[];
};

export type DictionaryOption = {
  label: string;
  value: string;
};

export async function fetchDictionaryOptions(typeCode: string): Promise<DictionaryOption[]> {
  try {
    const items = await apiGet<DictionaryItemApi[]>(`/dictionaries/${typeCode}/items`, {
      status: 1,
    });
    return flattenItems(items).map((item) => ({
      label: item.itemName,
      value: item.itemCode,
    }));
  } catch {
    return [];
  }
}

function flattenItems(items: DictionaryItemApi[]): DictionaryItemApi[] {
  return items.flatMap((item) => [item, ...flattenItems(item.children ?? [])]);
}
