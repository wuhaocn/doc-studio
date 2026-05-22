import test from 'node:test'
import assert from 'node:assert/strict'
import {
  DEFAULT_DOCUMENT_FORMAT,
  DOCUMENT_FORMATS,
  getDocumentRenderState,
  normalizeDocumentFormat,
  renderMarkdownToHtml,
  sanitizeDocumentHtml,
  summarizePlainText,
} from './documentContent.js'

test('normalizeDocumentFormat falls back to rich text', () => {
  assert.equal(normalizeDocumentFormat('markdown'), DOCUMENT_FORMATS.MARKDOWN)
  assert.equal(normalizeDocumentFormat('unknown-format'), DEFAULT_DOCUMENT_FORMAT)
  assert.equal(normalizeDocumentFormat(''), DEFAULT_DOCUMENT_FORMAT)
})

test('renderMarkdownToHtml renders markdown headings and emphasis', () => {
  const html = renderMarkdownToHtml('# 标题\n\n- 列表项\n\n**重点**')
  assert.match(html, /<h1>标题<\/h1>/)
  assert.match(html, /<li>列表项<\/li>/)
  assert.match(html, /<strong>重点<\/strong>/)
})

test('sanitizeDocumentHtml removes blocked tags and inline handlers', () => {
  const html = sanitizeDocumentHtml('<script>alert(1)</script><p onclick="hack()">safe</p><img src="javascript:alert(1)" onerror="hack()">')
  assert.doesNotMatch(html, /script/i)
  assert.doesNotMatch(html, /onclick/i)
  assert.doesNotMatch(html, /onerror/i)
  assert.doesNotMatch(html, /javascript:/i)
  assert.match(html, /<p>safe<\/p>/)
})

test('getDocumentRenderState supports html content and derives plain text', () => {
  const renderState = getDocumentRenderState({
    format: DOCUMENT_FORMATS.HTML,
    content: '<article><h1>交付说明</h1><p>支持直接展示 HTML 文档。</p></article>',
    contentText: '',
  })

  assert.equal(renderState.hasRenderedContent, true)
  assert.match(renderState.html, /<h1>交付说明<\/h1>/)
  assert.equal(renderState.plainText, '交付说明 支持直接展示 HTML 文档。')
})

test('summarizePlainText trims and limits output', () => {
  assert.equal(summarizePlainText('  a   b  '), 'a b')
  assert.equal(summarizePlainText('abcdef', 4), 'abcd')
})
