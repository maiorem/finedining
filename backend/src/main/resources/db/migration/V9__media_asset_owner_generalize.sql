-- media 패키지를 Production 전용에서 범용(Production·Artist 등)으로 일반화한다(CLAUDE.md §6).
-- 기존 production_id를 owner_id로 옮기고, owner_type을 추가한다. 지금까지 저장된 행은 전부
-- Production 이미지이므로 owner_type='PRODUCTION'으로 채운다.

ALTER TABLE media_asset
    ADD COLUMN owner_type VARCHAR(20) NULL AFTER id;

UPDATE media_asset SET owner_type = 'PRODUCTION' WHERE owner_type IS NULL;

ALTER TABLE media_asset
    MODIFY COLUMN owner_type VARCHAR(20) NOT NULL,
    CHANGE COLUMN production_id owner_id BIGINT NOT NULL,
    DROP KEY idx_media_asset_production,
    ADD KEY idx_media_asset_owner (owner_type, owner_id, sort_order);
