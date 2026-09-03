import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import {
  createPressClipping,
  listPressClippingsForAdmin,
  publishPressClipping,
  unpublishPressClipping,
  updatePressClippingContent,
  type PressClippingAdmin,
} from "../../api/pressClippingAdmin";
import { ApiError } from "../../api/http";
import { queryKeys } from "../../api/queryKeys";
import { ImageDropzone } from "./ImageDropzone";
import { PinModal } from "./PinModal";
import styles from "./PressClippingManagementPanel.module.css";

type PinAction = { id: number; action: "publish" | "unpublish" };

const KNOWN_ERROR_CODES = ["VALIDATION_ERROR"] as const;

function errorMessageKey(code: string): string {
  return (KNOWN_ERROR_CODES as readonly string[]).includes(code) ? `press.error.${code}` : "press.error.generic";
}

/**
 * 보도자료는 슬러그·상세 페이지가 없어(§6) 목록 하나에서 등록·수정·발행을 전부 인라인으로
 * 끝낸다 — Artist/Program처럼 별도 편집 패널 페이지로 이동하지 않는다. AboutPage의 "보도자료"
 * 탭에서 React.lazy로만 import된다(CLAUDE.md §3.5·§9).
 */
export default function PressClippingManagementPanel() {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const queryClient = useQueryClient();

  const { data: clippings, isLoading } = useQuery({
    queryKey: queryKeys.pressClippings.adminList,
    queryFn: () => listPressClippingsForAdmin(session!.accessToken),
    enabled: Boolean(session),
    staleTime: 0, // 편집 모드는 방금 저장한 값이 바로 보여야 한다 (CLAUDE.md §9).
  });

  const [newTitle, setNewTitle] = useState("");
  const [newUrl, setNewUrl] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [pinAction, setPinAction] = useState<PinAction | null>(null);

  function invalidate() {
    void queryClient.invalidateQueries({ queryKey: queryKeys.pressClippings.adminList });
    void queryClient.invalidateQueries({ queryKey: queryKeys.pressClippings.all });
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    if (!session) return;
    setCreateError(null);
    setCreating(true);
    try {
      await createPressClipping(session.accessToken, newTitle, newUrl);
      setNewTitle("");
      setNewUrl("");
      invalidate();
    } catch (err) {
      setCreateError(err instanceof ApiError ? t(errorMessageKey(err.code)) : t("press.error.generic"));
    } finally {
      setCreating(false);
    }
  }

  async function requestPublish(id: number) {
    if (!session) return;
    try {
      await publishPressClipping(session.accessToken, id);
      invalidate();
    } catch (err) {
      if (err instanceof ApiError && err.code === "PIN_REQUIRED") {
        setPinAction({ id, action: "publish" });
      }
    }
  }

  async function requestUnpublish(id: number) {
    if (!session) return;
    try {
      await unpublishPressClipping(session.accessToken, id);
      invalidate();
    } catch (err) {
      if (err instanceof ApiError && err.code === "PIN_REQUIRED") {
        setPinAction({ id, action: "unpublish" });
      }
    }
  }

  return (
    <div className={styles.panel}>
      <form className={styles.createForm} onSubmit={handleCreate}>
        <label className={styles.field}>
          <span>{t("press.titleLabel")}</span>
          <input type="text" value={newTitle} onChange={(e) => setNewTitle(e.target.value)} required />
        </label>
        <label className={styles.field}>
          <span>{t("press.urlLabel")}</span>
          <input
            type="url"
            value={newUrl}
            onChange={(e) => setNewUrl(e.target.value)}
            placeholder="https://..."
            required
          />
        </label>
        {createError && (
          <p className={styles.error} role="alert">
            {createError}
          </p>
        )}
        <button type="submit" className={styles.saveButton} disabled={creating}>
          {t("press.addNew")}
        </button>
      </form>

      {isLoading && <p className={styles.status}>{t("press.loading")}</p>}
      {!isLoading && clippings?.length === 0 && <p className={styles.status}>{t("press.empty")}</p>}

      <ul className={styles.list}>
        {clippings?.map((clipping) => (
          <PressClippingCard
            key={clipping.id}
            clipping={clipping}
            accessToken={session?.accessToken}
            onChanged={invalidate}
            onPublish={() => void requestPublish(clipping.id)}
            onUnpublish={() => void requestUnpublish(clipping.id)}
          />
        ))}
      </ul>

      {pinAction && (
        <PinModal
          onClose={() => setPinAction(null)}
          onVerified={() => {
            const action = pinAction;
            setPinAction(null);
            if (action.action === "publish") void requestPublish(action.id);
            else void requestUnpublish(action.id);
          }}
        />
      )}
    </div>
  );
}

type PressClippingCardProps = {
  clipping: PressClippingAdmin;
  accessToken: string | undefined;
  onChanged: () => void;
  onPublish: () => void;
  onUnpublish: () => void;
};

function PressClippingCard({ clipping, accessToken, onChanged, onPublish, onUnpublish }: PressClippingCardProps) {
  const { t } = useTranslation();
  const [title, setTitle] = useState(clipping.title);
  const [url, setUrl] = useState(clipping.externalUrl);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const dirty = title !== clipping.title || url !== clipping.externalUrl;

  async function handleSave() {
    if (!accessToken) return;
    setSaving(true);
    setError(null);
    try {
      await updatePressClippingContent(accessToken, clipping.id, title, url);
      onChanged();
    } catch (err) {
      setError(err instanceof ApiError ? t(errorMessageKey(err.code)) : t("press.error.generic"));
    } finally {
      setSaving(false);
    }
  }

  return (
    <li className={styles.item}>
      <div className={styles.itemRow}>
        <span className={styles.statusBadge}>{clipping.status}</span>
      </div>

      <label className={styles.field}>
        <span>{t("press.titleLabel")}</span>
        <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} />
      </label>
      <label className={styles.field}>
        <span>{t("press.urlLabel")}</span>
        <input type="url" value={url} onChange={(e) => setUrl(e.target.value)} />
      </label>

      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}

      <button type="button" className={styles.saveButton} disabled={saving || !dirty} onClick={() => void handleSave()}>
        {t("press.save")}
      </button>

      <hr className={styles.divider} />

      {accessToken && (
        <ImageDropzone
          ownerType="PRESS_CLIPPING"
          ownerId={clipping.id}
          images={clipping.images}
          onChanged={onChanged}
          maxImages={1}
        />
      )}

      <hr className={styles.divider} />

      <div className={styles.publishRow}>
        <button type="button" className={styles.publishButton} onClick={onPublish}>
          {t("editing.panel.publish")}
        </button>
        {clipping.status === "PUBLISHED" && (
          <button type="button" className={styles.unpublishButton} onClick={onUnpublish}>
            {t("editing.panel.unpublish")}
          </button>
        )}
      </div>
    </li>
  );
}
