import httpClient from '../http/axios'

export const authApi = {
  login: async (payload) => {
    return httpClient.post('/api/v1/auth/login', payload, { _skipAuthRefresh: true })
  },

  registerOwner: async (payload) => {
    return httpClient.post('/api/v1/auth/register-owner', payload, { _skipAuthRefresh: true })
  },

  getCurrentSession: async () => {
    return httpClient.get('/api/v1/auth/session')
  },

  logout: async () => {
    return httpClient.post('/api/v1/auth/logout')
  },

  refreshSession: async () => {
    return httpClient.post('/api/v1/auth/refresh', {}, { _skipAuthRefresh: true })
  },
}
