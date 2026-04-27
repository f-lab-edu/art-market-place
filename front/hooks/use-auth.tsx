"use client"

import { useState, useEffect, createContext, useContext } from "react"
import type { ReactNode } from "react"
import { authAPI, tokenManager } from "@/lib/api"
import type { User } from "@/lib/types"

interface AuthContextType {
    user: User | null
    loading: boolean
    googleLogin: (googleToken: string) => Promise<void>
    googleSignIn: (googleToken: string) => Promise<void>
    logout: () => Promise<void>
    isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function useAuth() {
    const context = useContext(AuthContext)
    if (context === undefined) {
        throw new Error("useAuth must be used within an AuthProvider")
    }
    return context
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null)
    const [loading, setLoading] = useState(true)
    const [isAuthenticated, setIsAuthenticated] = useState(false)

    useEffect(() => {
        const initAuth = async () => {
            const token = tokenManager.getToken()
            setIsAuthenticated(!!token)
            setLoading(false)
        }

        initAuth()
    }, [])

    const googleLogin = async (googleToken: string) => {
        try {
            const response = await authAPI.googleLogin(googleToken)
            tokenManager.setToken(response.data.accessToken)
            setIsAuthenticated(true)
        } catch (error) {
            console.error("Google login failed:", error)
            throw error
        }
    }

    const googleSignIn = async (googleToken: string) => {
        try {
            const response = await authAPI.googleSignIn(googleToken)
            tokenManager.setToken(response.data.accessToken)
            setIsAuthenticated(true)
        } catch (error) {
            console.error("Google login failed:", error)
            throw error
        }
    }

    const logout = async () => {
        try {
            await authAPI.logout()
        } catch (error) {
            console.error("Logout failed:", error)
        } finally {
            tokenManager.removeToken()
            setUser(null)
            setIsAuthenticated(false)
        }
    }

    const value: AuthContextType = {
        user,
        loading,
        googleLogin,
        googleSignIn,
        logout,
        isAuthenticated,
    }

    return <AuthContext.Provider value={value}>
        {children}
        </AuthContext.Provider> ;
}
