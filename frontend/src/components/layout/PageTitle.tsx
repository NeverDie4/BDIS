import { Typography } from "antd";
import styles from "./PageTitle.module.css";

type PageTitleProps = {
  title: string;
  subtitle?: string;
  extra?: React.ReactNode;
};

export function PageTitle({ title, subtitle, extra }: PageTitleProps) {
  return (
    <div className={styles.titleRow}>
      <div className={styles.copy}>
        <Typography.Title className={styles.title} level={1}>
          {title}
        </Typography.Title>
        {subtitle ? (
          <Typography.Paragraph className={styles.subtitle}>{subtitle}</Typography.Paragraph>
        ) : null}
      </div>
      {extra ? <div className={styles.extra}>{extra}</div> : null}
    </div>
  );
}
