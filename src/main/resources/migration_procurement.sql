-- Procurement Feature Migration
-- Extends existing Supplier table and adds Purchase Order, Product-Supplier junction, and Reorder Alert tables

-- ============================================================================
-- 1. EXTEND EXISTING SUPPLIERS TABLE
-- ============================================================================

ALTER TABLE suppliers 
  ADD COLUMN IF NOT EXISTS payment_terms VARCHAR(20) DEFAULT 'NET_30',
  ADD COLUMN IF NOT EXISTS lead_time_days INT DEFAULT 7,
  ADD COLUMN IF NOT EXISTS moq INT DEFAULT 1,
  ADD COLUMN IF NOT EXISTS reliability_score DECIMAL(3,2) DEFAULT 1.00,
  ADD COLUMN IF NOT EXISTS late_delivery_count INT DEFAULT 0,
  ADD COLUMN IF NOT EXISTS tax_id VARCHAR(50),
  ADD COLUMN IF NOT EXISTS bank_account VARCHAR(100),
  ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'LKR',
  ADD COLUMN IF NOT EXISTS notes TEXT;

-- ============================================================================
-- 2. CREATE PRODUCT-SUPPLIER JUNCTION TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS product_suppliers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  supplier_id BIGINT NOT NULL,
  is_primary BOOLEAN DEFAULT FALSE,
  priority_rank INT DEFAULT 1,
  supplier_sku VARCHAR(50),
  unit_cost DECIMAL(12,2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_product_supplier (product_id, supplier_id),
  INDEX idx_product_primary (product_id, is_primary),
  INDEX idx_supplier_products (supplier_id),
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE CASCADE
);

-- ============================================================================
-- 3. CREATE PURCHASE ORDERS TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS purchase_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  po_number VARCHAR(30) UNIQUE NOT NULL,
  business_id BIGINT NOT NULL,
  supplier_id BIGINT NOT NULL,
  status ENUM('DRAFT', 'SENT', 'PARTIAL', 'RECEIVED', 'CANCELLED') DEFAULT 'DRAFT',
  order_date DATE,
  expected_delivery DATE,
  actual_delivery DATE,
  subtotal DECIMAL(12,2) DEFAULT 0,
  tax DECIMAL(12,2) DEFAULT 0,
  shipping DECIMAL(12,2) DEFAULT 0,
  total DECIMAL(12,2) DEFAULT 0,
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_business_status (business_id, status),
  INDEX idx_supplier_po (supplier_id),
  INDEX idx_order_date (order_date),
  FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

-- ============================================================================
-- 4. CREATE PURCHASE ORDER LINE ITEMS TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS po_line_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  po_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  batch_id BIGINT,
  quantity_ordered INT NOT NULL,
  quantity_received INT DEFAULT 0,
  unit_cost DECIMAL(12,2) NOT NULL,
  line_total DECIMAL(12,2) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_po_items (po_id),
  INDEX idx_product_po (product_id),
  FOREIGN KEY (po_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id),
  FOREIGN KEY (batch_id) REFERENCES product_batches(id)
);

-- ============================================================================
-- 5. CREATE REORDER ALERTS TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS reorder_alerts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  batch_id BIGINT,
  current_stock INT NOT NULL,
  reorder_point INT NOT NULL,
  suggested_qty INT NOT NULL,
  suggested_supplier_id BIGINT,
  daily_usage_rate DECIMAL(10,2) DEFAULT 0,
  triggered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP NULL,
  status ENUM('ACTIVE', 'ORDERED', 'DISMISSED') DEFAULT 'ACTIVE',
  INDEX idx_business_active (business_id, status),
  INDEX idx_product_alert (product_id),
  INDEX idx_triggered (triggered_at),
  FOREIGN KEY (product_id) REFERENCES products(id),
  FOREIGN KEY (batch_id) REFERENCES product_batches(id),
  FOREIGN KEY (suggested_supplier_id) REFERENCES suppliers(id)
);

-- ============================================================================
-- 6. ADD INDEXES FOR PERFORMANCE
-- ============================================================================

-- Product batch indexes for reorder calculations
CREATE INDEX IF NOT EXISTS idx_batch_qty ON product_batches(qty_available);

-- Purchase order number lookup
CREATE INDEX IF NOT EXISTS idx_po_number ON purchase_orders(po_number);

-- ============================================================================
-- 7. SEED DATA (Optional - remove if not needed)
-- ============================================================================

-- Update existing suppliers with default values
UPDATE suppliers 
SET payment_terms = COALESCE(payment_terms, 'NET_30'),
    lead_time_days = COALESCE(lead_time_days, 7),
    moq = COALESCE(moq, 1),
    reliability_score = COALESCE(reliability_score, 1.00),
    currency = COALESCE(currency, 'LKR')
WHERE payment_terms IS NULL 
   OR lead_time_days IS NULL 
   OR moq IS NULL;
