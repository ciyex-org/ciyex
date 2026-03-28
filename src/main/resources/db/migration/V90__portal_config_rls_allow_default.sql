-- Allow all orgs to read the __DEFAULT__ portal config as fallback
DROP POLICY IF EXISTS portal_config_rls ON portal_config;
CREATE POLICY portal_config_rls ON portal_config
    USING (org_alias = current_setting('app.org_alias', true) OR org_alias = '__DEFAULT__');
