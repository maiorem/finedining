import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import type { MediaAsset } from "../../api/media";
import { parseMarkdown, type Block, type Inline, type MarkdownOptions } from "../../utils/markdown";
import { TrailerVideo } from "./TrailerVideo";
import styles from "./MarkdownContent.module.css";

type Props = {
  source: string | null | undefined;
  /** `image:번호`를 실제 주소로 바꾸는 데 쓰는 이 글의 이미지들. 목록에 없는 번호는 그리지 않는다. */
  images?: MediaAsset[];
  options?: MarkdownOptions;
  className?: string;
};

function renderInline(nodes: Inline[], t: (key: string) => string): ReactNode {
  return nodes.map((node, index) => {
    switch (node.type) {
      case "text":
        return node.text;
      case "strong":
        return <strong key={index}>{renderInline(node.children, t)}</strong>;
      case "em":
        return <em key={index}>{renderInline(node.children, t)}</em>;
      case "link": {
        const external = /^https?:/i.test(node.href);
        return (
          <a
            key={index}
            href={node.href}
            {...(external ? { target: "_blank", rel: "noopener noreferrer" } : {})}
            aria-label={external ? `${plainText(node.children)} (${t("booking.opensNewWindow")})` : undefined}
          >
            {renderInline(node.children, t)}
          </a>
        );
      }
    }
  });
}

function plainText(nodes: Inline[]): string {
  return nodes.map((n) => (n.type === "text" ? n.text : plainText(n.children))).join("");
}

// 본문 렌더러 — 마크다운 트리를 React 요소로만 그린다. HTML 문자열을 주입하지 않는다(dangerouslySetInnerHTML 없음).
export function MarkdownContent({ source, images = [], options, className }: Props) {
  const { t } = useTranslation();
  const blocks = parseMarkdown(source, options);
  if (blocks.length === 0) return null;

  const byId = new Map(images.map((image) => [image.id, image]));

  function renderBlock(block: Block, index: number): ReactNode {
    switch (block.type) {
      case "heading":
        return block.level === 2 ? (
          <h2 key={index} className={styles.heading}>
            {renderInline(block.children, t)}
          </h2>
        ) : (
          <h3 key={index} className={styles.subheading}>
            {renderInline(block.children, t)}
          </h3>
        );
      case "question":
        return (
          <h2 key={index} className={styles.question}>
            {renderInline(block.children, t)}
          </h2>
        );
      case "paragraph":
        return (
          <p key={index} className={styles.paragraph}>
            {block.lines.map((line, lineIndex) => (
              <span key={lineIndex}>
                {lineIndex > 0 && <br />}
                {renderInline(line, t)}
              </span>
            ))}
          </p>
        );
      case "list": {
        const Tag = block.ordered ? "ol" : "ul";
        return (
          <Tag key={index} className={styles.list}>
            {block.items.map((item, itemIndex) => (
              <li key={itemIndex}>{renderInline(item, t)}</li>
            ))}
          </Tag>
        );
      }
      case "quote":
        return (
          <blockquote key={index} className={styles.quote}>
            {block.lines.map((line, lineIndex) => (
              <span key={lineIndex}>
                {lineIndex > 0 && <br />}
                {renderInline(line, t)}
              </span>
            ))}
          </blockquote>
        );
      case "hr":
        return <hr key={index} className={styles.hr} />;
      case "image": {
        const image = byId.get(block.id);
        if (!image) return null;
        // 가로 크기를 지정했으면 그 폭에 맞는 파생본을 고르고 세로는 원본 비율로 계산한다.
        const wanted = block.width ?? 0;
        const src =
          wanted > 0 && wanted <= 640
            ? (image.url640 ?? image.url960 ?? image.url1600)
            : wanted > 0 && wanted <= 960
              ? (image.url960 ?? image.url1600 ?? image.url640)
              : (image.url1600 ?? image.url960 ?? image.url640);
        if (!src) return null;
        const ratio = image.width && image.height ? image.height / image.width : null;
        return (
          <figure key={index} className={styles.figure}>
            <img
              src={src}
              alt={block.alt || image.altText || ""}
              width={block.width ?? image.width ?? undefined}
              height={block.width && ratio ? Math.round(block.width * ratio) : (image.height ?? undefined)}
              style={block.width ? { width: `${block.width}px`, maxWidth: "100%", height: "auto" } : undefined}
              loading="lazy"
              decoding="async"
            />
          </figure>
        );
      }
      case "youtube":
        return (
          <div key={index} className={styles.video}>
            <TrailerVideo videoId={block.id} title={t("trailer.title")} />
          </div>
        );
    }
  }

  return <div className={className ? `${styles.root} ${className}` : styles.root}>{blocks.map(renderBlock)}</div>;
}
