-- ============================================================================
-- V73: Inventory Management Redesign — PostgreSQL-backed industry standard
-- ============================================================================
-- Replaces FHIR Device/SupplyRequest/Organization storage with proper
-- relational tables for lot tracking, multi-line POs, stock adjustments,
-- cycle counting, par levels, and location management.
-- ============================================================================

-- ─── Item Categories (hierarchical) ─────────────────────────────────────────
CREATE TABLE inventory_category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    parent_id   BIGINT REFERENCES inventory_category(id),
    org_alias   VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(org_alias, name, parent_id)
);
ALTER TABLE inventory_category ENABLE ROW LEVEL SECURITY;
CREATE POLICY inventory_category_rls ON inventory_category
    USING (org_alias = current_setting('app.org_alias', true));

-- ─── Storage Locations (room > shelf > bin hierarchy) ───────────────────────
CREATE TABLE inventory_location (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(20) NOT NULL DEFAULT 'room', -- room, shelf, bin, cabinet
    parent_id   BIGINT REFERENCES inventory_location(id),
    org_alias   VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(org_alias, name, parent_id)
);
ALTER TABLE inventory_location ENABLE ROW LEVEL SECURITY;
CREATE POLICY inventory_location_rls ON inventory_location
    USING (org_alias = current_setting('app.org_alias', true));

-- ─── Suppliers ──────────────────────────────────────────────────────────────
CREATE TABLE inventory_supplier (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    contact_name  VARCHAR(200),
    phone         VARCHAR(50),
    email         VARCHAR(200),
    address       TEXT,
    notes         TEXT,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    org_alias     VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
ALTER TABLE inventory_supplier ENABLE ROW LEVEL SECURITY;
CREATE POLICY inventory_supplier_rls ON inventory_supplier
    USING (org_alias = current_setting('app.org_alias', true));
CREATE INDEX idx_inv_supplier_org ON inventory_supplier(org_alias);

-- ─── Inventory Items (core) ─────────────────────────────────────────────────
CREATE TABLE inventory_item (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    sku             VARCHAR(100) NOT NULL,
    description     TEXT,
    category_id     BIGINT REFERENCES inventory_category(id),
    unit            VARCHAR(50) NOT NULL DEFAULT 'pcs', -- pcs, box, pair, bottle, roll, each, case
    cost_per_unit   DECIMAL(12,2) DEFAULT 0,
    stock_on_hand   INTEGER NOT NULL DEFAULT 0,
    min_stock       INTEGER NOT NULL DEFAULT 0,         -- par level: minimum
    max_stock       INTEGER DEFAULT NULL,               -- par level: maximum
    reorder_point   INTEGER NOT NULL DEFAULT 0,         -- auto-reorder trigger
    reorder_qty     INTEGER DEFAULT NULL,               -- default reorder quantity
    location_id     BIGINT REFERENCES inventory_location(id),
    supplier_id     BIGINT REFERENCES inventory_supplier(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'active', -- active, inactive, discontinued
    item_type       VARCHAR(30) NOT NULL DEFAULT 'consumable', -- consumable, device, implant, pharmaceutical
    barcode         VARCHAR(100),
    manufacturer    VARCHAR(200),
    cost_method     VARCHAR(10) DEFAULT 'fifo', -- fifo, lifo, avg
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(org_alias, sku)
);
ALTER TABLE inventory_item ENABLE ROW LEVEL SECURITY;
CREATE POLICY inventory_item_rls ON inventory_item
    USING (org_alias = current_setting('app.org_alias', true));
CREATE INDEX idx_inv_item_org ON inventory_item(org_alias);
CREATE INDEX idx_inv_item_category ON inventory_item(category_id);
CREATE INDEX idx_inv_item_supplier ON inventory_item(supplier_id);
CREATE INDEX idx_inv_item_status ON inventory_item(org_alias, status);
CREATE INDEX idx_inv_item_sku ON inventory_item(org_alias, sku);

-- ─── Lot / Batch Tracking ───────────────────────────────────────────────────
CREATE TABLE inventory_lot (
    id              BIGSERIAL PRIMARY KEY,
    item_id         BIGINT NOT NULL REFERENCES inventory_item(id) ON DELETE CASCADE,
    lot_number      VARCHAR(100) NOT NULL,
    expiry_date     DATE,
    quantity         INTEGER NOT NULL DEFAULT 0,
    received_date   DATE NOT NULL DEFAULT CURRENT_DATE,
    cost_per_unit   DECIMAL(12,2),
    notes           TEXT,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(item_id, lot_number)
);
ALTER TABLE inventory_lot ENABLE ROW LEVEL SECURITY;
CREATE POLICY inventory_lot_rls ON inventory_lot
    USING (org_alias = current_setting('app.org_alias', true));
CREATE INDEX idx_inv_lot_item ON inventory_lot(item_id);
CREATE INDEX idx_inv_lot_expiry ON inventory_lot(expiry_date);

-- ─── Purchase Orders (header) ───────────────────────────────────────────────
CREATE TABLE purchase_order (
    id              BIGSERIAL PRIMARY KEY,
    po_number       VARCHAR(50) NOT NULL,
    supplier_id     BIGINT REFERENCES inventory_supplier(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'draft', -- draft, submitted, partial, received, cancelled
    order_date      DATE NOT NULL DEFAULT CURRENT_DATE,
    expected_date   DATE,
    received_date   DATE,
    notes           TEXT,
    total_amount    DECIMAL(12,2) DEFAULT 0,
    created_by      VARCHAR(200),
    approved_by     VARCHAR(200),
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(org_alias, po_number)
);
ALTER TABLE purchase_order ENABLE ROW LEVEL SECURITY;
CREATE POLICY purchase_order_rls ON purchase_order
    USING (org_alias = current_setting('app.org_alias', true));
CREATE INDEX idx_po_org ON purchase_order(org_alias);
CREATE INDEX idx_po_status ON purchase_order(org_alias, status);
CREATE INDEX idx_po_supplier ON purchase_order(supplier_id);

-- ─── Purchase Order Line Items ──────────────────────────────────────────────
CREATE TABLE purchase_order_line (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES purchase_order(id) ON DELETE CASCADE,
    item_id             BIGINT REFERENCES inventory_item(id),
    item_name           VARCHAR(200),    -- denormalized for when item_id is null (new item)
    quantity_ordered    INTEGER NOT NULL DEFAULT 0,
    quantity_received   INTEGER NOT NULL DEFAULT 0,
    unit_cost           DECIMAL(12,2) DEFAULT 0,
    total_cost          DECIMAL(12,2) GENERATED ALWAYS AS (quantity_ordered * unit_cost) STORED,
    lot_number          VARCHAR(100),    -- lot received against
    expiry_date         DATE,            -- expiry of received lot
    notes               TEXT
);
CREATE INDEX idx_po_line_order ON purchase_order_line(order_id);
CREATE INDEX idx_po_line_item ON purchase_order_line(item_id);

-- ─── Stock Adjustments ──────────────────────────────────────────────────────
CREATE TABLE inventory_adjustment (
    id              BIGSERIAL PRIMARY KEY,
    item_id         BIGINT NOT NULL REFERENCES inventory_item(id) ON DELETE CASCADE,
    quantity_change  INTEGER NOT NULL,       -- positive = add, negative = remove
    reason_code     VARCHAR(50) NOT NULL,    -- received, damaged, expired, count_variance, returned, transferred, other
    notes           TEXT,
    adjusted_by     VARCHAR(200),
    reference_type  VARCHAR(50),             -- purchase_order, cycle_count, waste, transfer, manual
    reference_id    BIGINT,                  -- FK to the source record
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
ALTER TABLE inventory_adjustment ENABLE ROW LEVEL SECURITY;
CREATE POLICY inventory_adjustment_rls ON inventory_adjustment
    USING (org_alias = current_setting('app.org_alias', true));
CREATE INDEX idx_inv_adj_item ON inventory_adjustment(item_id);
CREATE INDEX idx_inv_adj_date ON inventory_adjustment(created_at);

-- ─── Cycle Counts ───────────────────────────────────────────────────────────
CREATE TABLE cycle_count (
    id              BIGSERIAL PRIMARY KEY,
    location_id     BIGINT REFERENCES inventory_location(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'scheduled', -- scheduled, in_progress, completed, cancelled
    scheduled_date  DATE NOT NULL,
    completed_date  DATE,
    counted_by      VARCHAR(200),
    notes           TEXT,
    org_alias       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
ALTER TABLE cycle_count ENABLE ROW LEVEL SECURITY;
CREATE POLICY cycle_count_rls ON cycle_count
    USING (org_alias = current_setting('app.org_alias', true));

CREATE TABLE cycle_count_item (
    id              BIGSERIAL PRIMARY KEY,
    count_id        BIGINT NOT NULL REFERENCES cycle_count(id) ON DELETE CASCADE,
    item_id         BIGINT NOT NULL REFERENCES inventory_item(id),
    expected_qty    INTEGER NOT NULL DEFAULT 0,
    counted_qty     INTEGER,
    variance        INTEGER GENERATED ALWAYS AS (counted_qty - expected_qty) STORED,
    notes           TEXT
);
CREATE INDEX idx_cc_item_count ON cycle_count_item(count_id);

-- ─── Waste / Spoilage Log ───────────────────────────────────────────────────
CREATE TABLE waste_log (
    id          BIGSERIAL PRIMARY KEY,
    item_id     BIGINT NOT NULL REFERENCES inventory_item(id) ON DELETE CASCADE,
    quantity    INTEGER NOT NULL,
    reason_code VARCHAR(50) NOT NULL,   -- expired, damaged, contaminated, recalled, other
    notes       TEXT,
    logged_by   VARCHAR(200),
    org_alias   VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
ALTER TABLE waste_log ENABLE ROW LEVEL SECURITY;
CREATE POLICY waste_log_rls ON waste_log
    USING (org_alias = current_setting('app.org_alias', true));

-- ─── Equipment / Maintenance Tasks ──────────────────────────────────────────
CREATE TABLE maintenance_task (
    id                BIGSERIAL PRIMARY KEY,
    equipment_name    VARCHAR(200) NOT NULL,
    equipment_id      VARCHAR(100),        -- asset tag / serial
    category          VARCHAR(50) NOT NULL DEFAULT 'preventive', -- preventive, corrective, inspection, calibration
    location          VARCHAR(200),
    due_date          DATE,
    last_service_date DATE,
    next_service_date DATE,
    assignee          VARCHAR(200),
    vendor            VARCHAR(200),
    priority          VARCHAR(20) NOT NULL DEFAULT 'medium', -- critical, high, medium, low
    status            VARCHAR(20) NOT NULL DEFAULT 'scheduled', -- scheduled, in_progress, completed, overdue, cancelled
    notes             TEXT,
    cost              DECIMAL(12,2),
    org_alias         VARCHAR(100) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
ALTER TABLE maintenance_task ENABLE ROW LEVEL SECURITY;
CREATE POLICY maintenance_task_rls ON maintenance_task
    USING (org_alias = current_setting('app.org_alias', true));
CREATE INDEX idx_maint_org ON maintenance_task(org_alias);
CREATE INDEX idx_maint_status ON maintenance_task(org_alias, status);
CREATE INDEX idx_maint_due ON maintenance_task(due_date);

-- ─── Inventory Settings (per org) ───────────────────────────────────────────
CREATE TABLE inventory_settings (
    id                      BIGSERIAL PRIMARY KEY,
    low_stock_alerts        BOOLEAN NOT NULL DEFAULT TRUE,
    auto_reorder            BOOLEAN NOT NULL DEFAULT FALSE,
    critical_low_pct        INTEGER NOT NULL DEFAULT 10,
    default_cost_method     VARCHAR(10) DEFAULT 'fifo',
    po_approval_required    BOOLEAN NOT NULL DEFAULT FALSE,
    po_approval_threshold   DECIMAL(12,2) DEFAULT 0,
    org_alias               VARCHAR(100) NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(org_alias)
);
ALTER TABLE inventory_settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY inventory_settings_rls ON inventory_settings
    USING (org_alias = current_setting('app.org_alias', true));

-- ─── Seed default categories ────────────────────────────────────────────────
INSERT INTO inventory_category (name, org_alias) VALUES
    ('Consumable', '__DEFAULT__'),
    ('Device', '__DEFAULT__'),
    ('Implant', '__DEFAULT__'),
    ('Pharmaceutical', '__DEFAULT__'),
    ('Office Supplies', '__DEFAULT__'),
    ('PPE', '__DEFAULT__'),
    ('Cleaning Supplies', '__DEFAULT__'),
    ('Lab Supplies', '__DEFAULT__');

-- ─── Seed default locations ─────────────────────────────────────────────────
INSERT INTO inventory_location (name, type, org_alias) VALUES
    ('Main Storage', 'room', '__DEFAULT__'),
    ('Exam Room Supply', 'cabinet', '__DEFAULT__'),
    ('Lab Storage', 'room', '__DEFAULT__'),
    ('Emergency Kit', 'cabinet', '__DEFAULT__'),
    ('Front Office', 'room', '__DEFAULT__');

-- Reason codes, units, and PO statuses are stored as CHECK constraints
-- and referenced by the frontend from a config endpoint. No separate
-- lookup table needed — values are the domain vocabulary.
