package com.greenstage.navigation

object Routes {

    // Navigation Graphs
    const val AUTH_GRAPH = "auth_graph"
    const val STUDENT_GRAPH = "student_graph"
    const val ADMIN_GRAPH = "admin_graph"

    // Root-level Destinations
    const val LOADING = "loading"
    const val PROFILE_ERROR = "profile_error"
    const val REGISTRATION_RECOVERY_ERROR = "registration_recovery_error"
    const val ADMIN_COMMUNITY_ERROR = "admin_community_error"

    // Authentication
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val STUDENT_REGISTER = "register/student"
    const val ADMIN_REGISTER = "register/admin"
    const val FORGOT_PASSWORD = "forgot-password"

    // Student navigation
    const val STUDENT_HOME = "student/home"
    const val STUDENT_EXPLORE = "student/explore"
    const val STUDENT_SAVED = "student/saved"
    const val STUDENT_PROFILE = "student/profile"

    const val STUDENT_EVENT_DETAILS = "student/event/{eventId}"
    const val STUDENT_EVENT_DETAILS_PATTERN = "student/event/"
    const val STUDENT_EVENT_DETAILS_ROUTE = "student/event/%s"

    // Admin navigation
    const val ADMIN_DASHBOARD = "admin/dashboard"
    const val ADMIN_CREATE_EVENT = "admin/create-event"
    const val ADMIN_MY_EVENTS = "admin/my-events"
    const val ADMIN_PROFILE = "admin/profile"

    const val ADMIN_EVENT_DETAILS = "admin/event/{eventId}"
    const val ADMIN_EVENT_DETAILS_PATTERN = "admin/event/"
    const val ADMIN_EVENT_DETAILS_ROUTE = "admin/event/%s"

    const val ADMIN_EDIT_EVENT = "admin/event/{eventId}/edit"
    const val ADMIN_EDIT_EVENT_PATTERN = "admin/event/"
    const val ADMIN_EDIT_EVENT_ROUTE = "admin/event/%s/edit"

    // Navigation arguments
    const val ARG_EVENT_ID = "eventId"
}
