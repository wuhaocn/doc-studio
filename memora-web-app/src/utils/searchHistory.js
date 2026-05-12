const STORAGE_KEY = 'memora_recent_searches'
const MAX_ITEMS = 5

export const getRecentSearches = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const addRecentSearch = (keyword) => {
  if (!keyword?.trim()) return
  const trimmed = keyword.trim()
  const current = getRecentSearches().filter((item) => item !== trimmed)
  const next = [trimmed, ...current].slice(0, MAX_ITEMS)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
}

export const clearRecentSearches = () => {
  localStorage.removeItem(STORAGE_KEY)
}
