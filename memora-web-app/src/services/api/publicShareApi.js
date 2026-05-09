import httpClient from '../http/axios'

export const publicShareApi = {
  getShareInfo: async (token) => {
    return httpClient.get(`/api/v1/public-shares/${token}`, { _skipAuthRefresh: true })
  },

  accessShare: async (token, payload = {}) => {
    return httpClient.post(`/api/v1/public-shares/${token}/access`, payload, { _skipAuthRefresh: true })
  },
}
