import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizeDocumentId } from './documentId.js'

test('normalizeDocumentId accepts positive integer ids', () => {
  assert.equal(normalizeDocumentId(7), 7)
  assert.equal(normalizeDocumentId('12'), 12)
})

test('normalizeDocumentId rejects missing or invalid ids', () => {
  assert.throws(() => normalizeDocumentId(undefined), /文档ID不合法/)
  assert.throws(() => normalizeDocumentId('undefined'), /文档ID不合法/)
  assert.throws(() => normalizeDocumentId(0), /文档ID不合法/)
  assert.throws(() => normalizeDocumentId('1.5'), /文档ID不合法/)
})
