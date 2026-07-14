"use client";

import { Button, Typography } from "antd";
import { ArrowLeft, House } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { SiteLayout } from "@/components/layout/SiteLayout";
import styles from "./page.module.css";

export default function ForbiddenPage() {
  const router = useRouter();

  return (
    <SiteLayout>
      <section className={styles.hero}>
        <div className={styles.content}>
          <p className={styles.eyebrow}>ACCESS RESTRICTED</p>
          <div className={styles.titleRow}>
            <h1>访问受限</h1>
            <span className={styles.seal}>权限</span>
          </div>
          <Typography.Paragraph className={styles.description}>
            当前账号没有进入此页面所需的权限。你仍然保持登录状态，可以返回上一页，或前往已授权的系统入口。
          </Typography.Paragraph>
          <div className={styles.actions}>
            <Button icon={<ArrowLeft size={16} />} size="large" onClick={() => router.back()}>
              返回上一页
            </Button>
            <Link href="/">
              <Button icon={<House size={16} />} size="large" type="primary">
                返回首页
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </SiteLayout>
  );
}
