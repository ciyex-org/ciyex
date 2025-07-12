CREATE DATABASE IF NOT EXISTS ciyexdb;
USE ciyexdb;

-- Roles Table
CREATE TABLE IF NOT EXISTS roles (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     name ENUM('ADMIN','DOCTOR','NURSE','PATIENT','RECEPTIONIST','SUPER_ADMIN') NOT NULL
    );

-- Users Table
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    date_of_birth DATE,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(255),
    profile_image VARCHAR(255),
    street VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    postal_code VARCHAR(255),
    country VARCHAR(255),
    security_question VARCHAR(255),
    security_answer VARCHAR(255)
    );

-- User-Roles Join Table (Many-to-Many)
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL,
                                          role_id BIGINT NOT NULL,
                                          PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
    );

-- Optional: Add an index for fast lookups
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

