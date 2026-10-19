-- ============================================================
-- V2: 修复初始管理员账号的 BCrypt 密码哈希
-- ------------------------------------------------------------
-- 背景：V1 写入的种子哈希
--   $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- 无法通过 BCryptPasswordEncoder 校验明文 "password"，导致全新库的 admin
-- 账号无法登录（任何应用环境都无法登录）。此处替换为经 BCrypt(cost=10)
-- 实际计算、可通过校验的哈希，明文仍为初始密码 "password"，
-- 账号仍应在首次登录后修改密码。
-- ============================================================

UPDATE user_account
SET password_hash = '$2a$10$Kp/PcyWvNL1G5qnH40xm6ew4.kywekDG5NXhZEdXxr7xa3D0RjCey'
WHERE user_id = 1
  AND username = 'admin';
