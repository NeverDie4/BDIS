import { Button } from "antd";
import styles from "./teaching.module.css";

type TeachingPageHeaderProps = {
  title: string;
  description: string;
};

export function TeachingPageHeader({ title, description }: TeachingPageHeaderProps) {
  return (
    <header className={styles.pageHeader}>
      <div className={styles.pageHeaderText}>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      <div className={styles.pageHeaderAction}>
        <Button disabled title="功能开发中" type="primary">
          新增课程
        </Button>
      </div>
    </header>
  );
}
