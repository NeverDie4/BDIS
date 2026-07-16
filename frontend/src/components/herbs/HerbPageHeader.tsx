import { Button } from "antd";
import styles from "./herbs.module.css";

export function HerbPageHeader() {
  return (
    <header className={styles.pageHeader}>
      <div className={styles.pageHeaderText}>
        <h1>中药材资源中心</h1>
        <p>管理中药材基础资料、分类、基地信息，支持药材资源全生命周期管理。</p>
      </div>
      <div className={styles.pageHeaderAction}>
        <Button type="primary" onClick={() => undefined}>
          新增药材
        </Button>
      </div>
    </header>
  );
}
