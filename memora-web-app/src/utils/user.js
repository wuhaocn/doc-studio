// 模拟用户状态管理

// 当前登录用户信息
export const currentUser = {
  id: 1,
  username: 'testUser',
  nickname: '测试用户',
  email: 'test@example.com',
  avatar: '',
  status: 1
}

// 模拟登录函数
export const login = (username, password) => {
  // 模拟登录成功，返回当前用户
  return Promise.resolve(currentUser)
}

// 模拟登出函数
export const logout = () => {
  // 模拟登出成功
  return Promise.resolve(true)
}

// 检查用户是否登录
export const isLoggedIn = () => {
  // 模拟用户已登录
  return true
}

// 获取当前登录用户
export const getCurrentUser = () => {
  return currentUser
}
