import { Space, Tag } from "antd";
import { PageBanner } from "./PageBanner";
import styles from "./PageHeader.module.css";

type PageHeaderProps = {
  eyebrow: string;
  title: string;
  description: string;
  tags?: string[];
};

export function PageHeader({ eyebrow, title, description, tags = [] }: PageHeaderProps) {
  return (
    <PageBanner
      sealText={eyebrow}
      title={title}
      subtitle={description}
      extra={
        tags.length > 0 ? (
          <Space className={styles.tags} size={[8, 8]} wrap>
            {tags.map((tag) => (
              <Tag className={styles.tag} key={tag}>
                {tag}
              </Tag>
            ))}
          </Space>
        ) : null
      }
    />
  );
}
