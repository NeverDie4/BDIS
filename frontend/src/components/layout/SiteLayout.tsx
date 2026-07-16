import { HeaderNav } from "./HeaderNav";
import styles from "./SiteLayout.module.css";

type SiteLayoutProps = {
  children: React.ReactNode;
  contentMode?: "contained" | "fluid";
};

export function SiteLayout({ children, contentMode = "contained" }: SiteLayoutProps) {
  return (
    <div className={styles.siteShell}>
      <HeaderNav />
      <main className={`${styles.main} ${contentMode === "fluid" ? styles.fluid : ""}`}>{children}</main>
    </div>
  );
}
