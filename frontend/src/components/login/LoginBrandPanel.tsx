import Image from "next/image";
import styles from "./LoginBrandPanel.module.css";

const copy = {
  description: "\u6c47\u805a\u672c\u8349\u8d44\u6e90\u3001\u6559\u5b66\u79d1\u7814\u4e0e\u751f\u957f\u6570\u636e",
  gallery: "\u672c\u8349\u6807\u672c\u9986",
};

export function LoginBrandPanel() {
  return (
    <section aria-labelledby="login-system-title" className={styles.brandPanel}>
      <Image
        alt=""
        aria-hidden="true"
        className={styles.backgroundImage}
        fill
        priority
        sizes="(max-width: 900px) 100vw, (max-width: 1280px) 50vw, 54vw"
        src="/images/login/herbal-login-background.png"
      />
      <div className={styles.readabilityLayer} />
      <div aria-hidden="true" className={styles.frame}>
        <span className={`${styles.frameCorner} ${styles.frameCornerTopLeft}`} />
        <span className={`${styles.frameCorner} ${styles.frameCornerTopRight}`} />
        <span className={`${styles.frameCorner} ${styles.frameCornerBottomLeft}`} />
        <span className={`${styles.frameCorner} ${styles.frameCornerBottomRight}`} />
      </div>
      <div className={styles.brandContent}>
        <Image
          alt={copy.gallery}
          className={styles.logo}
          height={112}
          sizes="180px"
          src="/images/login/herbal-gallery-logo.png"
          width={180}
        />
        <div className={styles.titleBlock}>
          <p className={styles.eyebrow}>HERBAL SPECIMEN GALLERY</p>
          <h1 id="login-system-title">
            <span>{"\u751f\u7269\u533b\u836f\u6570\u5b57"}</span>
            <span>{"\u4fe1\u606f\u7cfb\u7edf"}</span>
          </h1>
          <p className={styles.subtitle}>Biomedical Digital Information System</p>
          <p className={styles.description}>{copy.description}</p>
        </div>
      </div>
      <Image
        alt=""
        aria-hidden="true"
        className={`${styles.floatingLeaf} ${styles.leafOne}`}
        height={116}
        sizes="84px"
        src="/images/login/floating-leaf.png"
        width={116}
      />
      <Image
        alt=""
        aria-hidden="true"
        className={`${styles.floatingLeaf} ${styles.leafTwo}`}
        height={116}
        sizes="68px"
        src="/images/login/floating-leaf.png"
        width={116}
      />
      <Image
        alt=""
        aria-hidden="true"
        className={`${styles.floatingLeaf} ${styles.leafThree}`}
        height={116}
        sizes="58px"
        src="/images/login/floating-leaf.png"
        width={116}
      />
    </section>
  );
}
