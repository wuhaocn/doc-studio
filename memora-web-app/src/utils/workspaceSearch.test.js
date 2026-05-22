import test from 'node:test'
import assert from 'node:assert/strict'
import { buildKeywordHighlightParts, buildSearchExcerpt, groupSearchResultsByKnowledgeBase } from './workspaceSearch.js'

test('buildSearchExcerpt centers around matched keyword when available', () => {
  const excerpt = buildSearchExcerpt({
    contentText: '这是一个很长的文档摘要，包含升级处理流程、值班安排和交接信息。',
  }, '升级')

  assert.equal(excerpt.includes('升级处理流程'), true)
})

test('groupSearchResultsByKnowledgeBase groups results and keeps permission flags', () => {
  const groups = groupSearchResultsByKnowledgeBase(
    [
      { id: 1, knowledgeBaseId: 10, title: 'beta 操作手册', docType: 'DOC', contentText: 'alpha beta', updatedAt: '2026-05-10T10:00:00' },
      { id: 2, knowledgeBaseId: 10, title: 'B', contentText: 'beta gamma', updatedAt: '2026-05-12T10:00:00' },
      { id: 3, knowledgeBaseId: 11, title: 'C', contentText: 'gamma delta', updatedAt: '2026-05-11T10:00:00' },
      { id: 4, knowledgeBaseId: 10, title: '目录节点', docType: 'FOLDER', contentText: 'beta folder' },
    ],
    [
      { id: 10, name: '知识库一', currentRole: 'EDITOR', canWrite: true, permissionRestricted: false },
      { id: 11, name: '知识库二', currentRole: 'VIEWER', canWrite: false, permissionRestricted: true },
    ],
    'beta'
  )

  assert.equal(groups.length, 2)
  assert.equal(groups[0].knowledgeBaseName, '知识库一')
  assert.equal(groups[0].canWrite, true)
  assert.equal(groups[0].documents[0].title, 'beta 操作手册')
  assert.equal(groups[0].documents[0].matchLabel, '标题命中')
  assert.equal(groups[0].documents.some((item) => item.title === '目录节点'), false)
  assert.equal(groups[1].permissionRestricted, true)
})

test('buildKeywordHighlightParts marks matched segments', () => {
  const parts = buildKeywordHighlightParts('知识库支持 beta 搜索', 'beta')
  assert.deepEqual(parts, [
    { text: '知识库支持 ', matched: false },
    { text: 'beta', matched: true },
    { text: ' 搜索', matched: false },
  ])
})

test('buildKeywordHighlightParts marks multi token segments', () => {
  const parts = buildKeywordHighlightParts('支持 API key 搜索和 key 轮换', 'API key')
  assert.deepEqual(parts, [
    { text: '支持 ', matched: false },
    { text: 'API key', matched: true },
    { text: ' 搜索和 ', matched: false },
    { text: 'key', matched: true },
    { text: ' 轮换', matched: false },
  ])
})

test('groupSearchResultsByKnowledgeBase keeps more relevant title match before newer content match', () => {
  const groups = groupSearchResultsByKnowledgeBase(
    [
      { id: 1, knowledgeBaseId: 10, title: 'API key 管理', docType: 'DOC', contentText: '权限说明', updatedAt: '2026-05-10T10:00:00' },
      { id: 2, knowledgeBaseId: 10, title: '轮换策略', docType: 'DOC', contentText: '介绍 api key 轮换步骤', updatedAt: '2026-05-12T10:00:00' },
    ],
    [
      { id: 10, name: '平台接入', currentRole: 'OWNER', canWrite: true, permissionRestricted: false },
    ],
    'API key'
  )

  assert.equal(groups[0].documents[0].title, 'API key 管理')
  assert.equal(groups[0].documents[0].matchLabel, '标题命中')
})
