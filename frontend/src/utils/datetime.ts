/**
 * 后端时间字段格式化（零依赖）。
 *
 * 后端 Jackson 未定制 JSR-310 输出，LocalDateTime 默认序列化为 ISO-8601：
 *   "2026-09-26T15:30:00" / "2026-09-26T15:13:34.6"（秒可能带小数位）
 * LocalDate 序列化为 "1990-01-01"。
 * 表格展示统一转为 "yyyy-MM-dd HH:mm:ss"；el-date-picker 回显需要保留 ISO 前 19 位。
 */

/** 格式化为 yyyy-MM-dd HH:mm:ss；空值返回空串 */
export function formatDateTime(value?: string | null): string {
  if (!value) {
    return ''
  }
  // 去掉秒的小数位后，将日期与时间之间的 T 替换为空格
  return value.slice(0, 19).replace('T', ' ')
}

/**
 * 转为 el-date-picker（value-format=YYYY-MM-DDTHH:mm:ss）可回显的值。
 * 后端 ISO 串前 19 位恰好匹配；空值返回空串。
 */
export function toPickerDateTime(value?: string | null): string {
  if (!value) {
    return ''
  }
  return value.slice(0, 19)
}
