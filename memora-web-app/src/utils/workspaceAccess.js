const normalizePlainTextKey = (plainTextKey) => {
  return typeof plainTextKey === 'string' ? plainTextKey.trim() : ''
}

export const countManagedApiKeys = (serviceAccounts = []) => {
  return serviceAccounts.reduce((count, account) => count + (account.apiKeys?.length || 0), 0)
}

export const resolveManagedApiKeyDisplay = (apiKey = {}) => {
  const plainTextKey = normalizePlainTextKey(apiKey.plainTextKey)
  if (plainTextKey) {
    return {
      plainTextKey,
      state: 'visible',
      hint: '进入页面直接可见',
    }
  }

  if (apiKey.secretRevealAvailable) {
    return {
      plainTextKey: '',
      state: 'unavailable',
      hint: '当前密钥暂不可显示，可直接重新生成。',
    }
  }

  return {
    plainTextKey: '',
    state: 'legacy',
    hint: '这把密钥创建较早，暂不支持再次显示，可直接重新生成。',
  }
}
