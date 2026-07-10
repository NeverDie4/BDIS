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
        <Button type="primary" onClick={() => undefined}>
          新增课程
        </Button>
      </div>
    </header>
  );
}
