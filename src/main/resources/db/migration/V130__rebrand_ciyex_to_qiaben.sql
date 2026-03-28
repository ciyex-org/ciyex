-- V130: Rebrand "Ciyex" display names to "Qiaben" in app_installations
-- Slugs (ciyex-*) remain unchanged — they are technical identifiers, not user-facing.
-- Note: "Ciyex Hub" name is kept as-is per user preference.

-- ── Rename app display names in app_installations ──
UPDATE app_installations SET app_name = 'Qiaben Telehealth', updated_at = now()
WHERE app_slug = 'ciyex-telehealth' AND app_name LIKE '%Ciyex%';

UPDATE app_installations SET app_name = 'Qiaben RCM', updated_at = now()
WHERE app_slug = 'ciyex-rcm' AND app_name LIKE '%Ciyex%';

UPDATE app_installations SET app_name = 'Qiaben Credentialing', updated_at = now()
WHERE app_slug = 'ciyex-credentialing' AND app_name LIKE '%Ciyex%';

UPDATE app_installations SET app_name = 'Qiaben Codes', updated_at = now()
WHERE app_slug = 'ciyex-codes' AND app_name LIKE '%Ciyex%';
