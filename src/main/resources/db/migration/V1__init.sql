-- SGS schema. Owned by Flyway; Hibernate runs with ddl-auto=none.

CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  phone VARCHAR(30) NULL,
  role VARCHAR(20) NOT NULL,
  suspension_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
  suspended_until DATETIME(6) NULL,
  suspension_reason VARCHAR(255) NULL,
  upload_permission_until DATETIME(6) NULL,
  token_version INT NOT NULL DEFAULT 0,
  must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
  last_login_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(60) NOT NULL,
  slug VARCHAR(80) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_categories_name UNIQUE (name),
  CONSTRAINT uq_categories_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE gallery_images (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  description TEXT NULL,
  social_link VARCHAR(500) NULL,
  category_id BIGINT NOT NULL,
  image_path VARCHAR(255) NOT NULL,
  thumb_path VARCHAR(255) NOT NULL,
  uploaded_by BIGINT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_gallery_category FOREIGN KEY (category_id) REFERENCES categories (id),
  CONSTRAINT fk_gallery_user FOREIGN KEY (uploaded_by) REFERENCES users (id) ON DELETE SET NULL,
  KEY idx_gallery_category_created (category_id, created_at),
  KEY idx_gallery_created (created_at),
  KEY idx_gallery_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tools (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(150) NOT NULL,
  description TEXT NULL,
  tool_link VARCHAR(500) NULL,
  image_path VARCHAR(255) NOT NULL,
  thumb_path VARCHAR(255) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE services (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(150) NOT NULL,
  slug VARCHAR(160) NOT NULL,
  short_description VARCHAR(300) NULL,
  description TEXT NULL,
  icon_class VARCHAR(60) NULL,
  warranty VARCHAR(150) NULL,
  price_note VARCHAR(120) NULL,
  featured BOOLEAN NOT NULL DEFAULT FALSE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  display_order INT NOT NULL DEFAULT 0,
  meta_title VARCHAR(160) NULL,
  meta_description VARCHAR(300) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_services_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE service_features (
  service_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  feature VARCHAR(200) NOT NULL,
  PRIMARY KEY (service_id, sort_order),
  CONSTRAINT fk_service_features_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE service_images (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  service_id BIGINT NOT NULL,
  image_path VARCHAR(255) NOT NULL,
  thumb_path VARCHAR(255) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_service_images_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE,
  KEY idx_service_images_order (service_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE projects (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  description TEXT NULL,
  category_id BIGINT NOT NULL,
  location VARCHAR(150) NULL,
  client_name VARCHAR(150) NULL,
  started_year INT NOT NULL,
  completed_year INT NULL,
  status VARCHAR(20) NOT NULL,
  area_sq_ft INT NULL,
  featured BOOLEAN NOT NULL DEFAULT FALSE,
  image_path VARCHAR(255) NOT NULL,
  thumb_path VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_projects_category FOREIGN KEY (category_id) REFERENCES categories (id),
  KEY idx_projects_category (category_id),
  KEY idx_projects_featured_year (featured, started_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE clients (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  logo_path VARCHAR(255) NOT NULL,
  thumb_path VARCHAR(255) NOT NULL,
  website_url VARCHAR(500) NULL,
  display_order INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE company_contacts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  contact_type VARCHAR(30) NOT NULL,
  label VARCHAR(100) NULL,
  contact_value VARCHAR(500) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inquiries (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NULL,
  phone VARCHAR(30) NULL,
  service_interest VARCHAR(150) NULL,
  message TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'NEW',
  ip_address VARCHAR(45) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  KEY idx_inquiries_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bank_entries (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  note VARCHAR(255) NULL,
  entry_date DATE NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at DATETIME(6) NULL,
  deleted_by BIGINT NULL,
  delete_reason VARCHAR(255) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_bank_employee FOREIGN KEY (employee_id) REFERENCES users (id),
  CONSTRAINT chk_bank_amount_positive CHECK (amount > 0),
  KEY idx_bank_employee_date (employee_id, entry_date),
  KEY idx_bank_date (entry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bank_entry_audit (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  entry_id BIGINT NOT NULL,
  action VARCHAR(20) NOT NULL,
  old_amount DECIMAL(12,2) NULL,
  new_amount DECIMAL(12,2) NULL,
  old_note VARCHAR(255) NULL,
  new_note VARCHAR(255) NULL,
  reason VARCHAR(255) NULL,
  acted_by BIGINT NOT NULL,
  acted_by_username VARCHAR(50) NOT NULL,
  acted_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_bank_audit_entry FOREIGN KEY (entry_id) REFERENCES bank_entries (id),
  KEY idx_bank_audit_entry (entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  actor_id BIGINT NULL,
  actor_username VARCHAR(50) NULL,
  action VARCHAR(60) NOT NULL,
  entity_type VARCHAR(60) NULL,
  entity_id BIGINT NULL,
  details VARCHAR(1000) NULL,
  created_at DATETIME(6) NOT NULL,
  KEY idx_audit_created (created_at),
  KEY idx_audit_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
