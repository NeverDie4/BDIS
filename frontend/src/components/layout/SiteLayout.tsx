import { HeaderNav } from "./HeaderNav";
import styles from "./SiteLayout.module.css";

export function SiteLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className={styles.siteShell}>
      <HeaderNav />
      <main className={styles.main}>{children}</main>
    </div>
  );
}
