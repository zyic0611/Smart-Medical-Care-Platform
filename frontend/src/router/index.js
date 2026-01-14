import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../views/Layout.vue' // 引入Layout
import HomeView from '../views/HomeView.vue' // 引入 员工列表页
import LoginView from '../views/LoginView.vue' // 引入 登录页
import RegisterView from '../views/RegisterView.vue'
import BedView from "@/views/BedView.vue"; // 引入 注册页

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        // 登录页 (和 Layout 平级)
        {
            path: '/login',
            name: 'login',
            component: LoginView
        },
        // 主页 （Layout
        {
            path: '/',          // 浏览器访问根路径时
            name: 'home',
            component: Layout, // 1. 访问根路径时，先加载 Layout (大框框)
            redirect: '/dashboard', // 默认跳转到员工页
            children: [ // 2. 这里的页面会显示在 Layout 的 <router-view /> 里
                {
                    path: 'dashboard',
                    name: 'dashboard',
                    component: () => import('../views/Dashboard.vue')
                },
                {
                    path: 'employee', // 访问 /employee
                    name: 'employee',
                    component: HomeView // 显示员工列表
                },
                {
                    path: 'bed', // 访问 /bed
                    name: 'bed',
                    component: BedView // 显示员工列表
                },
                {
                    path: 'about', // 访问 /about (为了演示菜单跳转)
                    name: 'about',
                    // 懒加载写法 (临时写一个简单的关于页面)
                    component: () => import('../views/AboutView.vue')
                },
                {
                    path: 'person',
                    name: 'person',
                    component: () => import('../views/Person.vue')
                },
                {
                    path: 'elderly',
                    name: 'elderly',
                    component: () => import('../views/ElderlyView.vue')
                },
                {
                    path: 'diagnosis',
                    name: 'diagnosis',
                    component: () => import('../views/DiagnosisView.vue')
                }

            ]
        },

        // 注册页路由
        { path: '/register', name: 'register', component: RegisterView }


    ]
})

// 路由守卫：每次路由跳转前都会执行这个函数
router.beforeEach((to, from, next) => {
    // 1. 获取兜里的 Token
    const token = localStorage.getItem('token')

    // 2. 定义白名单（不需要登录就能访问的页面）
    // 如果你有注册页，也加进去，比如 ['/login', '/register']
    const whiteList = ['/login', '/register']

    // 3. 判断逻辑
    if (whiteList.includes(to.path)) {
        // 情况 A：如果你要去的是登录页，直接放行
        next()
    } else {
        // 情况 B：如果你要去的是其他页面 (比如 /employee)
        if (token) {
            // 如果你有 Token，放行
            next()
        } else {
            // 如果你没 Token，无情地把你踢回登录页
            next('/login')
        }
    }
})

export default router
