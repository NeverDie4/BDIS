"use client";

import { Input } from "antd";
import type { ChangeEvent } from "react";
import styles from "./SearchBar.module.css";

type SearchBarProps = {
  placeholder?: string;
  value?: string;
  loading?: boolean;
  disabled?: boolean;
  className?: string;
  onChange?: (value: string) => void;
  onSearch?: (value: string) => void;
};

export function SearchBar({
  placeholder = "请输入关键词",
  value,
  loading = false,
  disabled = false,
  className,
  onChange,
  onSearch,
}: SearchBarProps) {
  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange?.(event.target.value);
  };

  return (
    <Input.Search
      allowClear
      className={`${styles.searchBar} ${className ?? ""}`}
      disabled={disabled}
      loading={loading}
      placeholder={placeholder}
      value={value}
      onChange={handleChange}
      onSearch={onSearch}
    />
  );
}
