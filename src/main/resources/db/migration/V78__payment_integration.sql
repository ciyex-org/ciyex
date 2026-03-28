-- =============================================
-- V78: Payment Integration - Collect from Patients + Stored Payment Methods
-- =============================================

-- ── 1. Patient Payment Methods (stored cards, bank accounts, FSA, HSA) ──
CREATE TABLE patient_payment_method (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT NOT NULL,
    patient_name        VARCHAR(255),
    method_type         VARCHAR(30) NOT NULL,       -- credit_card, debit_card, bank_account, fsa, hsa, check, cash, other
    card_brand          VARCHAR(20),                -- visa, mastercard, amex, discover
    last_four           VARCHAR(4),
    exp_month           INTEGER,
    exp_year            INTEGER,
    cardholder_name     VARCHAR(255),
    bank_name           VARCHAR(255),
    account_type        VARCHAR(20),                -- checking, savings
    routing_last_four   VARCHAR(4),
    billing_address     TEXT,
    billing_zip         VARCHAR(10),
    is_default          BOOLEAN NOT NULL DEFAULT false,
    is_active           BOOLEAN NOT NULL DEFAULT true,
    stripe_payment_method_id VARCHAR(255),          -- Stripe PaymentMethod ID (pm_xxx)
    stripe_customer_id  VARCHAR(255),               -- Stripe Customer ID (cus_xxx)
    token_reference     VARCHAR(500),               -- tokenized reference for PCI compliance
    nickname            VARCHAR(100),               -- "My Visa ending 4242", "FSA Card"
    notes               TEXT,
    org_alias           VARCHAR(100) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_patient_payment_method_org ON patient_payment_method(org_alias);
CREATE INDEX idx_patient_payment_method_patient ON patient_payment_method(org_alias, patient_id);
CREATE INDEX idx_patient_payment_method_stripe ON patient_payment_method(stripe_customer_id);

-- ── 2. Practice Payment Configuration (Stripe/processor settings per practice) ──
CREATE TABLE payment_config (
    id                  BIGSERIAL PRIMARY KEY,
    processor           VARCHAR(50) NOT NULL DEFAULT 'stripe',  -- stripe, square, authorize_net
    enabled             BOOLEAN NOT NULL DEFAULT false,
    config              JSONB NOT NULL DEFAULT '{}',            -- {stripe_publishable_key, stripe_secret_key, webhook_secret}
    accepted_methods    JSONB DEFAULT '["credit_card","debit_card","bank_account","fsa","hsa"]'::jsonb,
    convenience_fee_enabled BOOLEAN DEFAULT false,
    convenience_fee_percent DECIMAL(5,2) DEFAULT 0,
    convenience_fee_flat    DECIMAL(10,2) DEFAULT 0,
    auto_receipt        BOOLEAN DEFAULT true,
    receipt_email_template_id BIGINT,
    org_alias           VARCHAR(100) NOT NULL UNIQUE,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_config_org ON payment_config(org_alias);

-- ── 3. Payment Transactions ──
CREATE TABLE payment_transaction (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT NOT NULL,
    patient_name        VARCHAR(255),
    payment_method_id   BIGINT REFERENCES patient_payment_method(id),
    amount              DECIMAL(10,2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'USD',
    status              VARCHAR(30) NOT NULL DEFAULT 'pending',  -- pending, processing, completed, failed, refunded, partial_refund, voided
    transaction_type    VARCHAR(30) NOT NULL DEFAULT 'payment',  -- payment, refund, adjustment, write_off
    payment_method_type VARCHAR(30),                              -- credit_card, debit_card, bank_account, fsa, hsa, cash, check
    card_brand          VARCHAR(20),
    last_four           VARCHAR(4),
    description         VARCHAR(500),
    reference_type      VARCHAR(50),                -- encounter, claim, invoice, copay, balance
    reference_id        BIGINT,
    invoice_number      VARCHAR(50),
    stripe_payment_intent_id VARCHAR(255),          -- Stripe PaymentIntent ID (pi_xxx)
    stripe_charge_id    VARCHAR(255),               -- Stripe Charge ID (ch_xxx)
    processor_response  JSONB DEFAULT '{}',         -- full processor response
    convenience_fee     DECIMAL(10,2) DEFAULT 0,
    refund_amount       DECIMAL(10,2) DEFAULT 0,
    refund_reason       TEXT,
    receipt_sent        BOOLEAN DEFAULT false,
    receipt_email       VARCHAR(255),
    collected_by        VARCHAR(255),
    collected_at        TIMESTAMP,
    notes               TEXT,
    org_alias           VARCHAR(100) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_transaction_org ON payment_transaction(org_alias);
CREATE INDEX idx_payment_transaction_patient ON payment_transaction(org_alias, patient_id);
CREATE INDEX idx_payment_transaction_status ON payment_transaction(org_alias, status);
CREATE INDEX idx_payment_transaction_date ON payment_transaction(org_alias, created_at DESC);
CREATE INDEX idx_payment_transaction_type ON payment_transaction(org_alias, transaction_type);
CREATE INDEX idx_payment_transaction_ref ON payment_transaction(org_alias, reference_type, reference_id);
CREATE INDEX idx_payment_transaction_stripe ON payment_transaction(stripe_payment_intent_id);

-- ── 4. Payment Plans (installment plans for large balances) ──
CREATE TABLE payment_plan (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT NOT NULL,
    patient_name        VARCHAR(255),
    total_amount        DECIMAL(10,2) NOT NULL,
    remaining_amount    DECIMAL(10,2) NOT NULL,
    installment_amount  DECIMAL(10,2) NOT NULL,
    frequency           VARCHAR(20) NOT NULL DEFAULT 'monthly',  -- weekly, biweekly, monthly
    payment_method_id   BIGINT REFERENCES patient_payment_method(id),
    auto_charge         BOOLEAN DEFAULT false,
    next_payment_date   DATE,
    start_date          DATE NOT NULL,
    end_date            DATE,
    status              VARCHAR(30) NOT NULL DEFAULT 'active',   -- active, completed, defaulted, cancelled, paused
    installments_total  INTEGER,
    installments_paid   INTEGER DEFAULT 0,
    reference_type      VARCHAR(50),
    reference_id        BIGINT,
    notes               TEXT,
    org_alias           VARCHAR(100) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_plan_org ON payment_plan(org_alias);
CREATE INDEX idx_payment_plan_patient ON payment_plan(org_alias, patient_id);
CREATE INDEX idx_payment_plan_status ON payment_plan(org_alias, status);
CREATE INDEX idx_payment_plan_next ON payment_plan(org_alias, auto_charge, next_payment_date) WHERE status = 'active';

-- ── 5. Patient Ledger / Account Balance ──
CREATE TABLE patient_ledger (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT NOT NULL,
    entry_type          VARCHAR(30) NOT NULL,       -- charge, payment, adjustment, refund, write_off, insurance_payment
    amount              DECIMAL(10,2) NOT NULL,     -- positive for charges, negative for payments/adjustments
    running_balance     DECIMAL(10,2),
    description         VARCHAR(500),
    reference_type      VARCHAR(50),                -- encounter, claim, payment_transaction, etc.
    reference_id        BIGINT,
    posted_by           VARCHAR(255),
    org_alias           VARCHAR(100) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_patient_ledger_org ON patient_ledger(org_alias);
CREATE INDEX idx_patient_ledger_patient ON patient_ledger(org_alias, patient_id, created_at DESC);

-- ── 6. Menu Item ──
INSERT INTO menu_item (menu_id, item_key, label, icon, screen_slug, position)
VALUES ('a0000000-0000-0000-0000-000000000001', 'payments', 'Payments', 'CreditCard', '/payments', 6);
