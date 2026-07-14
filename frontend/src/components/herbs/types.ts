export interface HerbTableRecord {
  id: string;
  herbName: string;
  aliasName: string;
  categoryName: string;
  medicinalPart: string;
  status: "enabled" | "disabled";
  region: string;
}
