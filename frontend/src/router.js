
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import ConcertManager from "./components/ConcertManager"

import BookingManager from "./components/BookingManager"

import AlarmManager from "./components/AlarmManager"

import PointManager from "./components/PointManager"

import PaymentManager from "./components/PaymentManager"


import Mypage from "./components/Mypage"
export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [
            {
                path: '/Concert',
                name: 'ConcertManager',
                component: ConcertManager
            },

            {
                path: '/Booking',
                name: 'BookingManager',
                component: BookingManager
            },

            {
                path: '/Alarm',
                name: 'AlarmManager',
                component: AlarmManager
            },

            {
                path: '/Point',
                name: 'PointManager',
                component: PointManager
            },

            {
                path: '/Payment',
                name: 'PaymentManager',
                component: PaymentManager
            },


            {
                path: '/Mypage',
                name: 'Mypage',
                component: Mypage
            },


    ]
})
