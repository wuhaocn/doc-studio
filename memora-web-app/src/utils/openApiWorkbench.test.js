import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildCaptureDraftHash,
  buildCaptureInitialValues,
  buildCaptureBookmarkletSnippet,
  buildCaptureMarkdownContent,
  buildDefaultSourceExternalId,
  buildDialogCaptureSnippet,
  buildOpenApiConsumeCurlSnippet,
  buildOpenReaderFetchSnippet,
  buildOpenApiUpsertPayload,
  parseCaptureDraftHash,
} from './openApiWorkbench.js'

test('buildDefaultSourceExternalId uses source url when provided', () => {
  assert.equal(
    buildDefaultSourceExternalId(9, 'https://example.com/post/1'),
    'capture:https://example.com/post/1',
  )
})

test('buildDefaultSourceExternalId falls back to knowledge base stamp', () => {
  const value = buildDefaultSourceExternalId(12, '', new Date(2026, 4, 18, 10, 20, 30))
  assert.equal(value, 'manual-kb-12-20260518102030')
})

test('buildCaptureMarkdownContent keeps source url and selection', () => {
  const content = buildCaptureMarkdownContent({
    title: '示例网页',
    sourceUrl: 'https://example.com',
    selection: '第一段内容',
  })

  assert.match(content, /^# 示例网页/m)
  assert.match(content, /来源：https:\/\/example.com/)
  assert.match(content, /第一段内容/)
})

test('buildOpenApiUpsertPayload normalizes numeric ids and trimmed values', () => {
  const payload = buildOpenApiUpsertPayload({
    knowledgeBaseId: '7',
    parentId: '3',
    title: '  标题  ',
    format: 'markdown',
    content: '# 内容',
    sourceExternalId: ' source-1 ',
    sourceRevision: ' rev-1 ',
  })

  assert.deepEqual(payload, {
    knowledgeBaseId: 7,
    parentId: 3,
    title: '标题',
    format: 'MARKDOWN',
    content: '# 内容',
    sourceExternalId: 'source-1',
    sourceRevision: 'rev-1',
  })
})

test('buildOpenApiConsumeCurlSnippet includes by-source route and view', () => {
  const snippet = buildOpenApiConsumeCurlSnippet({
    apiBaseUrl: 'http://localhost:8080',
    knowledgeBaseId: 11,
    sourceExternalId: 'capture:https://example.com',
    view: 'rendered',
  })

  assert.match(snippet, /knowledge-bases\/11\/documents\/by-source/)
  assert.match(snippet, /view=rendered/)
  assert.match(snippet, /sourceExternalId=capture%3Ahttps%3A%2F%2Fexample.com/)
})

test('buildCaptureBookmarkletSnippet opens capture page instead of direct api request', () => {
  const snippet = buildCaptureBookmarkletSnippet({
    webOrigin: 'http://localhost:8800',
    knowledgeBaseId: 5,
    parentId: 9,
  })

  assert.match(snippet, /^javascript:/)
  assert.match(snippet, /\/capture\/save\?/)
  assert.match(snippet, /knowledgeBaseId/)
  assert.doesNotMatch(snippet, /api\/v1\/open\/documents\/upsert/)
})

test('buildCaptureInitialValues keeps provided content and source identity', () => {
  const initialValues = buildCaptureInitialValues({
    knowledgeBaseId: 8,
    parentId: 3,
    title: 'Dialog Draft',
    format: 'html',
    content: '<article>Ready</article>',
    sourceUrl: 'https://dialog.memora.local/thread/1',
    sourceExternalId: 'dialog:thread-1',
    sourceRevision: 'rev-9',
  })

  assert.deepEqual(initialValues, {
    knowledgeBaseId: 8,
    parentId: 3,
    title: 'Dialog Draft',
    format: 'HTML',
    content: '<article>Ready</article>',
    sourceUrl: 'https://dialog.memora.local/thread/1',
    sourceExternalId: 'dialog:thread-1',
    sourceRevision: 'rev-9',
  })
})

test('buildCaptureDraftHash and parseCaptureDraftHash round trip dialog payload', () => {
  const hash = buildCaptureDraftHash({
    knowledgeBaseId: 6,
    parentId: 2,
    title: '对话框草稿',
    format: 'markdown',
    content: '# Hello 100%',
    sourceUrl: 'https://chat.example.com/thread/7',
    sourceExternalId: 'dialog:thread-7',
    sourceRevision: '2026-05-18T09:00:00.000Z',
  })

  assert.match(hash, /^draft=/)
  assert.deepEqual(parseCaptureDraftHash(`#${hash}`), {
    knowledgeBaseId: 6,
    parentId: 2,
    title: '对话框草稿',
    format: 'MARKDOWN',
    content: '# Hello 100%',
    sourceUrl: 'https://chat.example.com/thread/7',
    sourceExternalId: 'dialog:thread-7',
    sourceRevision: '2026-05-18T09:00:00.000Z',
    selection: '',
  })
})

test('buildDialogCaptureSnippet opens capture page with encoded draft payload', () => {
  const snippet = buildDialogCaptureSnippet({
    webOrigin: 'http://localhost:8800',
    draft: {
      knowledgeBaseId: 4,
      title: '保存草稿',
      format: 'MARKDOWN',
      content: '# 内容',
    },
  })

  assert.match(snippet, /capture\/save/)
  assert.match(snippet, /encodeURIComponent\(JSON\.stringify\(draft\)\)/)
  assert.match(snippet, /window\.open/)
})

test('buildOpenReaderFetchSnippet resolves by source and opens reader url', () => {
  const snippet = buildOpenReaderFetchSnippet({
    apiBaseUrl: 'http://localhost:8080',
    knowledgeBaseId: 3,
    sourceExternalId: 'dialog:thread-3',
    webOrigin: 'http://localhost:8800',
  })

  assert.match(snippet, /knowledge-bases\/3\/documents\/by-source/)
  assert.match(snippet, /readerUrl/)
  assert.match(snippet, /window\.open/)
})
