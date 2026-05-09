import httpClient from '../http/axios'

export const workspaceApi = {
  getCurrentDashboard: async () => {
    return httpClient.get('/api/v1/workspaces/current/dashboard')
  },

  getJoinedWorkspaces: async () => {
    return httpClient.get('/api/v1/workspaces/joined')
  },

  switchWorkspace: async (tenantId) => {
    return httpClient.post(`/api/v1/workspaces/${tenantId}/switch`)
  },
}
