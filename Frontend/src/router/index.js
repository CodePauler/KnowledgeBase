import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/views/Layout.vue'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/login',
            name: 'login',
            component: Login
        },
        {
            path: '/register',
            name: 'register',
            component: Register
        },
        {
            path: '/',
            component: Layout,
            redirect: '/spaces',
            children: [
                {
                    path: 'spaces',
                    name: 'spaces',
                    component: () => import('@/views/SpaceList.vue')
                },
                {
                    path: 'space/:spaceId',
                    name: 'space-detail',
                    component: () => import('@/views/SpaceDetail.vue'),
                    children: [
                        {
                            path: '',
                            name: 'space-home',
                            component: () => import('@/views/SpaceHome.vue') // Shows file list
                        },
                        {
                            path: 'doc/:docId',
                            name: 'document',
                            component: () => import('@/views/Document.vue')
                        },
                        {
                            path: 'chat',
                            name: 'chat',
                            component: () => import('@/views/Chat.vue')
                        }
                    ]
                }
            ]
        }
    ]
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.name !== 'login' && to.name !== 'register' && !token) {
        next({ name: 'login' })
    } else {
        next()
    }
})

export default router
