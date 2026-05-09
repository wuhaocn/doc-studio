import test from 'node:test'
import assert from 'node:assert/strict'
import { buildSearchExcerpt, groupSearchResultsByKnowledgeBase } from './workspaceSearch.js'

test('buildSearchExcerpt centers around matched keyword when available', () => {
  const excerpt = buildSearchExcerpt({
    contentText: '这是一个很长的文档摘要，包含升级处理流程、值班安排和交接信息。',
  }, '升级')

  assert.equal(excerpt.includes('升级处理流程'), true)
})

test('groupSearchResultsByKnowledgeBase groups results and keeps permission flags', () => {
  const groups = groupSearchResultsByKnowledgeBase(
    [
      { id: 1, knowledgeBaseId: 10, title: 'A', docType: 'DOC', contentText: 'alpha beta' },
      { id: 2, knowledgeBaseId: 10, title: 'B', contentText: 'beta gamma' },
      { id: 3, knowledgeBaseId: 11, title: 'C', contentText: 'gamma delta' },
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
  assert.equal(groups[0].documents.some((item) => item.title === '目录节点'), false)
  assert.equal(groups[1].permissionRestricted, true)
})
