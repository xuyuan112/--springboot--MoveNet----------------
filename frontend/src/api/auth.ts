import { get, post } from './request'

/** 系统角色枚举（与后端约定一致） */
export type Role = 'PATIENT' | 'THERAPIST' | 'ADMIN'

/** 登录请求参数 */
export interface LoginParams {
  username: string
  password: string
  /** 图形验证码标识（GET /auth/captcha 返回） */
  captchaId: string
  /** 用户输入的图形验证码 */
  captchaCode: string
}

/** GET /auth/captcha 响应 data */
export interface CaptchaResult {
  /** 登录时回传的验证码标识 */
  captchaId: string
  /** 验证码图片 Data URI，可直接作为 img 的 src */
  image: string
  /** 有效期（秒） */
  expireSeconds: number
}

/** 登录用户基础信息 */
export interface UserInfo {
  id: number
  role: Role
  name: string
}

/** POST /auth/login 响应 data */
export interface LoginResult {
  accessToken: string
  expiresIn: number
  user: UserInfo
}

/**
 * 获取登录图形验证码
 * GET /auth/captcha（公开接口）
 * @returns 响应体 data 为 { captchaId, image, expireSeconds }
 */
export function getCaptcha() {
  return get<CaptchaResult>('/auth/captcha')
}

/**
 * 账号密码登录
 * POST /auth/login（公开接口）
 * @returns 响应体 data 为 { accessToken, expiresIn, user }
 */
export function login(data: LoginParams) {
  return post<LoginResult>('/auth/login', data)
}

/**
 * 退出登录（使当前令牌失效）
 * POST /auth/logout
 */
export function logout() {
  return post<null>('/auth/logout')
}

/**
 * 查询当前登录用户信息
 * GET /auth/me
 */
export function getMyInfo() {
  return get<UserInfo>('/auth/me')
}
