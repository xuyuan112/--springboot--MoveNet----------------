/// <reference types="vite/client" />

// 全量注册 Element Plus 时，为模板中的 el-* 组件 / 全局配置提供类型
/// <reference types="element-plus/global" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'

  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>
  export default component
}
