import { DOCUMENT_FORMATS, normalizeDocumentFormat } from './documentContent.js'

const normalizeExampleTitle = (title, fallback) => `${title || ''}`.replace(/\s+/g, ' ').trim() || fallback

const escapeHtml = (value) =>
  `${value || ''}`
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')

const buildRichTextExampleContent = (title) => {
  const safeTitle = escapeHtml(title)
  return [
    `<h1>${safeTitle}</h1>`,
    '<p><strong>发布人：</strong>产品运营 · <strong>频道：</strong>项目复盘 · <strong>状态：</strong>待讨论</p>',
    '<blockquote>这是一篇适合富文本编辑器的帖子示例，重点是可视化排版、段落强调和后续人工补充。</blockquote>',
    '<h2>发生了什么</h2>',
    '<p>本周完成了文档创建、阅读、版本和发布链路的主流程验证。团队需要把关键经验沉淀下来，方便后续项目复用。</p>',
    '<h2>关键经验</h2>',
    '<ul>',
    '<li>先明确正文格式，再进入对应编辑器。</li>',
    '<li>AI 生成内容需要保留人工可读的标题、摘要和版本记录。</li>',
    '<li>发布前先在阅读页确认最终展示效果。</li>',
    '</ul>',
    '<h2>讨论问题</h2>',
    '<p>哪些内容应该由 AI 直接生成，哪些内容必须由负责人确认后再发布？</p>',
    '<p><strong>下一步：</strong>请在评论或后续版本中补充真实案例。</p>',
  ].join('')
}

const buildMarkdownExampleContent = (title) => {
  const normalizedTitle = normalizeExampleTitle(title, '上线操作规范')
  return [
    `# ${normalizedTitle}`,
    '',
    '## 1. 目的',
    '',
    '规范一次标准上线操作，确保执行人、检查人和回滚责任明确。',
    '',
    '## 2. 适用范围',
    '',
    '- Web 前端构建发布',
    '- 后端接口配置变更',
    '- 文档、公开站点和开放接入联调',
    '',
    '## 3. 前置检查',
    '',
    '| 检查项 | 负责人 | 结果 |',
    '| --- | --- | --- |',
    '| 单元测试通过 |  |  |',
    '| lint 通过 |  |  |',
    '| 生产构建通过 |  |  |',
    '| 发布说明已同步 |  |  |',
    '',
    '## 4. 操作步骤',
    '',
    '1. 拉取最新代码并确认分支状态。',
    '2. 执行前端构建和后端验证。',
    '3. 更新发布配置并记录版本号。',
    '4. 发布后打开阅读页、公开页和开放接入调试台复核。',
    '',
    '## 5. 回滚策略',
    '',
    '- 如果构建失败：停止发布，保留失败日志。',
    '- 如果上线后主流程不可用：回滚到上一稳定版本。',
    '- 如果仅文档内容异常：使用文档版本回滚。',
    '',
    '## 6. 完成标准',
    '',
    '- [ ] 主流程可创建、编辑、阅读、发布',
    '- [ ] 错误反馈清晰',
    '- [ ] 验收记录已归档',
  ].join('\n')
}

export const buildMiniAppSpec = (title) => ({
  schemaVersion: 'miniapp.h5.v1',
  entry: 'index.html',
  title,
  runtime: 'declarative-click-demo',
  permissions: ['document:read', 'document:write'],
  events: {
    submitInspection: {
      type: 'click',
      label: '提交登记',
      effect: 'count-and-show-last-status',
    },
  },
  pages: [
    {
      name: 'index',
      title: '巡检登记',
      components: [
        { type: 'input', name: 'inspector', label: '巡检人', required: true },
        { type: 'select', name: 'status', label: '状态', options: ['正常', '待复核', '需整改'] },
        { type: 'textarea', name: 'remark', label: '现场备注' },
        { type: 'button', name: 'submitInspection', label: '提交登记', onClick: 'submitInspection' },
      ],
    },
  ],
  publish: {
    mode: 'public-read',
    slug: 'inspection-register-mini-app',
  },
})

const buildMiniAppExampleContent = (title) => {
  const safeTitle = escapeHtml(title)
  const spec = buildMiniAppSpec(title)
  const miniAppSpec = escapeHtml(JSON.stringify(spec, null, 2))
  const miniAppSpecAttribute = escapeHtml(JSON.stringify(spec))

  return [
    '<article class="mini-app-spec-example">',
    `  <h1>${safeTitle}</h1>`,
    `  <div data-type="mini-app-example" data-content="${miniAppSpecAttribute}"></div>`,
    '  <section>',
    '    <h2>小程序目标</h2>',
    '    <p>这是给 AI 生成 H5 小程序时使用的起步规范，平台会基于声明式事件渲染可点击的演示按钮。</p>',
    '  </section>',
    '  <section>',
    '    <h2>生成规范</h2>',
    `    <pre><code>${miniAppSpec}</code></pre>`,
    '  </section>',
    '  <section>',
    '    <h2>首屏结构</h2>',
    '    <ul>',
    '      <li>标题区：展示小程序名称和当前业务场景。</li>',
    '      <li>表单区：巡检人、状态、现场备注三个基础字段。</li>',
    '      <li>结果区：提交后展示本次登记摘要。</li>',
    '    </ul>',
    '  </section>',
    '  <section>',
    '    <h2>运行边界</h2>',
    '    <p>点击事件由平台的安全预览组件执行，不在普通 HTML 文档中保存 script 或 onclick。</p>',
    '  </section>',
    '</article>',
  ].join('\n')
}

const decodeHtmlEntities = (value) =>
  `${value || ''}`
    .replaceAll('&quot;', '"')
    .replaceAll('&#39;', "'")
    .replaceAll('&gt;', '>')
    .replaceAll('&lt;', '<')
    .replaceAll('&amp;', '&')

export const extractMiniAppSpecFromHtml = (html) => {
  const normalizedHtml = `${html || ''}`
  const markerMatch = normalizedHtml.match(/<div[^>]*data-type=(["'])mini-app-example\1[^>]*>/i)
  if (!markerMatch) {
    return null
  }

  const marker = markerMatch[0]
  const contentMatch = marker.match(/data-content=(["'])(.*?)\1/i)
  if (!contentMatch) {
    return null
  }

  try {
    const spec = JSON.parse(decodeHtmlEntities(contentMatch[2]))
    return spec?.schemaVersion === 'miniapp.h5.v1' ? spec : null
  } catch {
    return null
  }
}

export const DOCUMENT_QUICK_EXAMPLES = Object.freeze([
  Object.freeze({
    id: 'rich-text-delivery-record',
    format: DOCUMENT_FORMATS.RICH_TEXT,
    label: '富文本',
    outputLabel: '富文本',
    title: '项目复盘帖子',
    summary: '用于快速创建一篇可视化帖子，适合公告、复盘和社区讨论。',
    difference: '像发帖一样排版，适合人工边写边改，正文保存为安全富文本 HTML。',
    buildContent: buildRichTextExampleContent,
  }),
  Object.freeze({
    id: 'markdown-project-record',
    format: DOCUMENT_FORMATS.MARKDOWN,
    label: 'Markdown',
    outputLabel: 'Markdown',
    title: '上线操作规范',
    summary: '用于快速创建结构化操作规范，适合 AI 生成步骤、表格和检查清单。',
    difference: '源码轻、结构清楚，适合 AI 批量生成标题、列表、表格和待办。',
    buildContent: buildMarkdownExampleContent,
  }),
  Object.freeze({
    id: 'h5-mini-app-inspection',
    format: DOCUMENT_FORMATS.HTML,
    label: 'H5',
    outputLabel: 'H5 源码',
    title: '巡检登记小程序',
    summary: '用于快速创建带点击事件的 H5 小程序演示，先按 HTML 源码文档沉淀和评审。',
    difference: '页面/表单型体验，由平台按声明式事件渲染点击按钮，不写入脚本。',
    buildContent: buildMiniAppExampleContent,
  }),
])

export const getDocumentQuickExampleByFormat = (format) => {
  const normalizedFormat = normalizeDocumentFormat(format)
  return DOCUMENT_QUICK_EXAMPLES.find((example) => example.format === normalizedFormat)
    || DOCUMENT_QUICK_EXAMPLES[0]
}

export const getDocumentQuickExamplesByFormats = (formats = []) => {
  const formatSet = new Set(formats.map((format) => normalizeDocumentFormat(format)))
  return DOCUMENT_QUICK_EXAMPLES.filter((example) => formatSet.has(example.format))
}

export const buildDocumentQuickExample = (format, title = '') => {
  const example = getDocumentQuickExampleByFormat(format)
  const normalizedTitle = normalizeExampleTitle(title, example.title)
  return {
    id: example.id,
    format: example.format,
    label: example.label,
    outputLabel: example.outputLabel,
    title: normalizedTitle,
    summary: example.summary,
    difference: example.difference,
    content: example.buildContent(normalizedTitle),
  }
}

export const buildDocumentStarterContent = (title, format) =>
  buildDocumentQuickExample(format, title).content
