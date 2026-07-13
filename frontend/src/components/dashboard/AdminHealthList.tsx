import { ArrowRight, CheckCircle2, CircleAlert } from "lucide-react";
import Link from "next/link";
import styles from "./AdminHealthList.module.css";

export type AdminHealthItem = {
  key: string;
  title: string;
  description: string;
  count?: number;
  href?: string;
};

export function AdminHealthList({ items }: { items: AdminHealthItem[] }) {
  if (!items.length) {
    return (
      <div className={styles.empty}>
        <CheckCircle2 size={20} />
        <div>
          <strong>当前未发现需要处理的异常</strong>
          <span>统计结果会随账号权限和后台数据实时变化。</span>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.list}>
      {items.map((item) => {
        const content = (
          <>
            <span className={styles.icon}>
              <CircleAlert size={18} />
            </span>
            <span className={styles.text}>
              <strong>{item.title}</strong>
              <span>{item.description}</span>
            </span>
            {item.count !== undefined ? (
              <strong className={styles.count}>{item.count}</strong>
            ) : null}
            {item.href ? <ArrowRight className={styles.arrow} size={18} /> : null}
          </>
        );
        return item.href ? (
          <Link className={styles.item} href={item.href} key={item.key}>
            {content}
          </Link>
        ) : (
          <div className={styles.item} key={item.key}>
            {content}
          </div>
        );
      })}
    </div>
  );
}
