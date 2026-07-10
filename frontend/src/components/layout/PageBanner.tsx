import { PageTitle } from "./PageTitle";
import styles from "./PageBanner.module.css";

type PageBannerProps = {
  title: string;
  subtitle?: string;
  sealText?: string;
  extra?: React.ReactNode;
  children?: React.ReactNode;
};

export function PageBanner({ title, subtitle, sealText, extra, children }: PageBannerProps) {
  return (
    <section className={styles.banner}>
      <div className={styles.texture} aria-hidden="true" />
      {sealText ? <p className={styles.seal}>{sealText}</p> : null}
      <PageTitle title={title} subtitle={subtitle} extra={extra} />
      {children ? <div className={styles.content}>{children}</div> : null}
    </section>
  );
}
