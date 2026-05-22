import test from 'node:test'
import assert from 'node:assert/strict'
import {
  AUTH_SESSION_BROADCAST_STORAGE_KEY,
  applyAuthSessionBroadcastPayload,
  clearCurrentUser,
  getCurrentUser,
  hydrateCurrentUser,
  parseAuthSessionBroadcastStorageEvent,
} from './user.js'

const createStorage = () => {
  const store = new Map()
  return {
    getItem(key) {
      return store.has(key) ? store.get(key) : null
    },
    setItem(key, value) {
      store.set(key, String(value))
    },
    removeItem(key) {
      store.delete(key)
    },
  }
}

const installWindowMock = () => {
  const sessionStorage = createStorage()
  const localStorage = createStorage()
  const dispatchedEvents = []

  globalThis.CustomEvent = class CustomEvent {
    constructor(type, init = {}) {
      this.type = type
      this.detail = init.detail
    }
  }

  globalThis.window = {
    sessionStorage,
    localStorage,
    dispatchEvent(event) {
      dispatchedEvents.push(event)
      return true
    },
  }

  return {
    dispatchedEvents,
    restore() {
      delete globalThis.window
      delete globalThis.CustomEvent
    },
  }
}

test('hydrateCurrentUser persists normalized session snapshot', () => {
  const mock = installWindowMock()

  try {
    const currentUser = hydrateCurrentUser({
      userId: 7,
      tenantId: 9,
      username: 'owner',
      displayName: '工作区 Owner',
      email: 'owner@memora.local',
      tenantName: '北区交付中心',
      tenantSlug: 'north-delivery',
      industry: '制造',
      planName: 'TEAM',
      role: 'OWNER',
    })

    assert.equal(currentUser.id, 7)
    assert.equal(currentUser.tenantId, 9)
    assert.equal(getCurrentUser().tenantSlug, 'north-delivery')
    assert.equal(mock.dispatchedEvents.at(-1)?.type, 'memora:auth-session-changed')
  } finally {
    mock.restore()
  }
})

test('applyAuthSessionBroadcastPayload syncs another tab session locally', () => {
  const mock = installWindowMock()

  try {
    const payload = parseAuthSessionBroadcastStorageEvent({
      key: AUTH_SESSION_BROADCAST_STORAGE_KEY,
      newValue: JSON.stringify({
        action: 'SYNC',
        user: {
          id: 3,
          tenantId: 5,
          username: 'editor',
          nickname: '区域编辑',
          email: 'editor@memora.local',
          role: 'EDITOR',
          tenantName: '区域知识中心',
        },
      }),
    })

    const currentUser = applyAuthSessionBroadcastPayload(payload)
    assert.equal(currentUser.id, 3)
    assert.equal(currentUser.tenantId, 5)
    assert.equal(getCurrentUser().nickname, '区域编辑')
  } finally {
    mock.restore()
  }
})

test('applyAuthSessionBroadcastPayload clears local session on remote logout', () => {
  const mock = installWindowMock()

  try {
    hydrateCurrentUser({
      userId: 1,
      tenantId: 1,
      username: 'admin',
      displayName: '管理员',
      role: 'OWNER',
    })
    clearCurrentUser({ broadcast: false })
    hydrateCurrentUser({
      userId: 2,
      tenantId: 2,
      username: 'viewer',
      displayName: '查看者',
      role: 'VIEWER',
    })

    const payload = parseAuthSessionBroadcastStorageEvent({
      key: AUTH_SESSION_BROADCAST_STORAGE_KEY,
      newValue: JSON.stringify({ action: 'CLEAR' }),
    })

    const currentUser = applyAuthSessionBroadcastPayload(payload)
    assert.equal(currentUser, null)
    assert.equal(getCurrentUser(), null)
  } finally {
    mock.restore()
  }
})
