/**
 * 第三方库的类型补充（这些包未随包发布 .d.ts）。
 *
 * 只声明本项目实际用到的成员，保持最小面积：
 *  - bpmn-js-bpmnlint 的默认导出是 bpmn-js 的 additionalModule
 *  - bpmnlint/config/recommended 是内置的推荐规则集配置
 */
declare module 'bpmn-js-bpmnlint' {
  const lintModule: Record<string, unknown>
  export default lintModule
}

declare module 'bpmnlint/config/recommended' {
  const config: Record<string, unknown>
  export default config
}
