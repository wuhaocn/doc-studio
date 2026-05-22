import httpClient from '../http/axios'

export const publicSiteApi = {
  getSite: async (siteSlug) => {
    return httpClient.get(`/api/public/sites/${siteSlug}`, { _skipAuthRefresh: true })
  },

  getDocument: async (siteSlug, publicSlug) => {
    return httpClient.get(`/api/public/sites/${siteSlug}/${publicSlug}`, { _skipAuthRefresh: true })
  },
}
