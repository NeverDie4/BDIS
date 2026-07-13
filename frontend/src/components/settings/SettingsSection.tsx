import type { ReactNode } from "react";
import styles from "./settings.module.css";

export function SettingsSection({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <section className={styles.section}>
      <header className={styles.sectionHeader}>
        <h2>{title}</h2>
        {description ? <p>{description}</p> : null}
      </header>
      <div className={styles.sectionBody}>{children}</div>
    </section>
  );
}
