import assert from 'node:assert/strict'
import test from 'node:test'
import { countManagedApiKeys, resolveManagedApiKeyDisplay } from './workspaceAccess.js'

test('countManagedApiKeys sums keys from all service accounts', () => {
  const total = countManagedApiKeys([
    { id: 1, apiKeys: [{ id: 11 }, { id: 12 }] },
    { id: 2, apiKeys: [] },
    { id: 3, apiKeys: [{ id: 31 }] },
  ])

  assert.equal(total, 3)
})

test('resolveManagedApiKeyDisplay returns visible secret when list response carries it', () => {
  assert.deepEqual(resolveManagedApiKeyDisplay({
    plainTextKey: ' memora_sk_live ',
    secretRevealAvailable: true,
  }), {
    plainTextKey: 'memora_sk_live',
    state: 'visible',
    hint: '进入页面直接可见',
  })
})

test('resolveManagedApiKeyDisplay falls back to unavailable hint when secret copy exists but cannot be shown', () => {
  assert.deepEqual(resolveManagedApiKeyDisplay({
    plainTextKey: '',
    secretRevealAvailable: true,
  }), {
    plainTextKey: '',
    state: 'unavailable',
    hint: '当前密钥暂不可显示，可直接重新生成。',
  })
})

test('resolveManagedApiKeyDisplay marks legacy keys as regenerate only', () => {
  assert.deepEqual(resolveManagedApiKeyDisplay({
    secretRevealAvailable: false,
  }), {
    plainTextKey: '',
    state: 'legacy',
    hint: '这把密钥创建较早，暂不支持再次显示，可直接重新生成。',
  })
})
