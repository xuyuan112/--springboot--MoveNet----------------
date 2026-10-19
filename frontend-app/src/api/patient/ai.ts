import { post } from '@/api/request'

/** AI 问答响应：回答 + 强制免责声明 */
export interface AiQaResult {
  answer: string
  disclaimer: string
}

/** 平台统一免责声明（与后端 Constants.MEDICAL_DISCLAIMER 对齐） */
export const MEDICAL_DISCLAIMER = '仅供参考，不能替代专业医疗建议'

/**
 * AI 问答（会话不持久化，每次独立问答）。
 * 后端按用户限流（默认 20 次/天），超限返回 429。
 */
export function askAi(question: string) {
  return post<AiQaResult>('/patient/ai/qa', { question })
}
