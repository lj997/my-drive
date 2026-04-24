import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/files',
    children: [
      {
        path: 'files',
        name: 'Files',
        component: () => import('@/views/files/index.vue'),
        meta: { title: '我的文件' }
      },
      {
        path: 'recycle',
        name: 'Recycle',
        component: () => import('@/views/recycle/index.vue'),
        meta: { title: '回收站' }
      },
      {
        path: 'shares',
        name: 'Shares',
        component: () => import('@/views/shares/index.vue'),
        meta: { title: '我的分享' }
      }
    ]
  },
  {
    path: '/s/:shareCode',
    name: 'ShareAccess',
    component: () => import('@/views/share-access/index.vue'),
    meta: { title: '文件分享' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 我的网盘` : '我的网盘'
  
  const userStore = useUserStore()
  const token = localStorage.getItem('token')
  
  if (to.path === '/login' || to.path.startsWith('/s/')) {
    next()
  } else {
    if (token || userStore.token) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
