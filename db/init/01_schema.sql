CREATE TABLE customers (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  customer_no     VARCHAR(20) NOT NULL,
  full_name       VARCHAR(120) NOT NULL,
  mobile          VARCHAR(20) NOT NULL,
  status          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  role            ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  password_hash   VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_customers_customer_no (customer_no),
  UNIQUE KEY uk_customers_mobile (mobile)
);

CREATE TABLE accounts (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  account_no        VARCHAR(20) NOT NULL,
  customer_id       BIGINT UNSIGNED NOT NULL,
  balance           DECIMAL(19,4) NOT NULL DEFAULT 0,
  reserved_balance  DECIMAL(19,4) NOT NULL DEFAULT 0,
  currency          CHAR(3) NOT NULL,
  account_type      ENUM('SAVINGS','CURRENT') NOT NULL DEFAULT 'CURRENT',
  iban              VARCHAR(34) NOT NULL,
  status            ENUM('ACTIVE','CLOSED') NOT NULL DEFAULT 'ACTIVE',
  opened_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_accounts_account_no (account_no),
  UNIQUE KEY uk_accounts_iban (iban),
  KEY idx_accounts_customer (customer_id),
  FOREIGN KEY (customer_id) REFERENCES customers(id),
  CHECK (reserved_balance >= 0),
  CHECK (reserved_balance <= balance)
);

CREATE TABLE transactions (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  transaction_ref   VARCHAR(40) NOT NULL,
  account_id        BIGINT UNSIGNED NOT NULL,
  type              ENUM('DEBIT','CREDIT') NOT NULL,
  amount            DECIMAL(19,4) NOT NULL,
  transaction_date  DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_transactions_ref (transaction_ref),
  KEY idx_tx_account_date (account_id, transaction_date DESC),
  FOREIGN KEY (account_id) REFERENCES accounts(id),
  CHECK (amount > 0)
);
