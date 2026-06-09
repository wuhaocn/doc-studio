import test from 'node:test'
import assert from 'node:assert/strict'
import {
  DOCUMENT_QUICK_EXAMPLES,
  buildDocumentQuickExample,
  buildDocumentStarterContent,
  extractMiniAppSpecFromHtml,
  getDocumentQuickExampleByFormat,
  getDocumentQuickExamplesByFormats,
} from './documentExamples.js'
import { DOCUMENT_FORMATS, renderDocumentHtml } from './documentContent.js'

test('document quick examples cover every supported document format', () => {
  const formats = new Set(DOCUMENT_QUICK_EXAMPLES.map((example) => example.format))

  assert.equal(DOCUMENT_QUICK_EXAMPLES.length, 3)
  assert.equal(formats.has(DOCUMENT_FORMATS.RICH_TEXT), true)
  assert.equal(formats.has(DOCUMENT_FORMATS.MARKDOWN), true)
  assert.equal(formats.has(DOCUMENT_FORMATS.HTML), true)
  assert.equal(DOCUMENT_QUICK_EXAMPLES.some((example) => example.id === 'h5-mini-app-inspection'), true)
  assert.deepEqual(DOCUMENT_QUICK_EXAMPLES.map((example) => example.label), ['富文本', 'Markdown', 'H5'])
})

test('buildDocumentQuickExample returns title-matched starter content', () => {
  const formats = Object.values(DOCUMENT_FORMATS)

  formats.forEach((format) => {
    const example = buildDocumentQuickExample(format, '客户现场巡检')

    assert.equal(example.format, format)
    assert.equal(example.title, '客户现场巡检')
    assert.ok(example.summary.trim())
    assert.ok(example.difference.trim())
    assert.ok(example.outputLabel.trim())
    assert.match(example.content, /客户现场巡检/)
    assert.doesNotMatch(example.content, /<script|javascript:|\son[a-z]+=/i)
  })
})

test('document starter content falls back to rich text for unknown formats', () => {
  const example = getDocumentQuickExampleByFormat('PDF')
  assert.equal(example.format, DOCUMENT_FORMATS.RICH_TEXT)

  const content = buildDocumentStarterContent('默认示例', 'PDF')
  assert.match(content, /默认示例/)
  assert.match(renderDocumentHtml(DOCUMENT_FORMATS.RICH_TEXT, content), /默认示例/)
})

test('getDocumentQuickExamplesByFormats keeps requested formats only', () => {
  const examples = getDocumentQuickExamplesByFormats([DOCUMENT_FORMATS.MARKDOWN, DOCUMENT_FORMATS.HTML])

  assert.deepEqual([...new Set(examples.map((example) => example.format))], [
    DOCUMENT_FORMATS.MARKDOWN,
    DOCUMENT_FORMATS.HTML,
  ])
  assert.equal(examples.some((example) => example.id === 'h5-mini-app-inspection'), true)
})

test('h5 quick example stores declarative click event spec without scripts', () => {
  const example = buildDocumentQuickExample(DOCUMENT_FORMATS.HTML, '巡检登记小程序')
  const spec = extractMiniAppSpecFromHtml(renderDocumentHtml(DOCUMENT_FORMATS.HTML, example.content))

  assert.equal(spec.schemaVersion, 'miniapp.h5.v1')
  assert.equal(spec.events.submitInspection.type, 'click')
  assert.equal(spec.pages[0].components.some((component) => component.type === 'button'), true)
  assert.doesNotMatch(example.content, /<script|onclick=/i)
})
