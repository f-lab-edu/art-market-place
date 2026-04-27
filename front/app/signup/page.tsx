"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { ArrowLeft } from "lucide-react"
import RegisterForm from "@/components/auth/register-form"
import GoogleAuthButton from "@/components/auth/google-auth-button"
import { useAuth } from "@/hooks/use-auth"

export default function SignupPage() {
    const { user, loading } = useAuth()
    const router = useRouter()

    useEffect(() => {
        if (user && !loading) {
            router.push("/blog")
        }
    }, [user, loading, router])

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
        )
    }

    if (user) {
        return null // 리다이렉트 중
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-background p-4">
            <div className="w-full max-w-md space-y-6">
                {/* 뒤로가기 버튼 */}
                <Button variant="ghost" asChild className="self-start">
                    <Link href="/blog" className="flex items-center gap-2">
                        <ArrowLeft className="h-4 w-4" />
                        블로그로 돌아가기
                    </Link>
                </Button>

                {/* 이메일 회원가입 폼 */}
                {/*<RegisterForm />*/}

                {/*<div className="relative">*/}
                {/*    <div className="absolute inset-0 flex items-center">*/}
                {/*        <Separator className="w-full" />*/}
                {/*    </div>*/}
                {/*    <div className="relative flex justify-center text-xs uppercase">*/}
                {/*        <span className="bg-background px-2 text-muted-foreground">또는</span>*/}
                {/*    </div>*/}
                {/*</div>*/}

                {/* Google 회원가입 */}
                <GoogleAuthButton mode="signin" className="w-full" />

                <div className="text-center text-sm text-muted-foreground">
                    이미 계정이 있으신가요?{" "}
                    <Link href="/login" className="text-primary hover:underline">
                        로그인
                    </Link>
                </div>

                <div className="text-center text-xs text-muted-foreground">
                    회원가입하면{" "}
                    <Link href="#" className="hover:underline">
                        서비스 약관
                    </Link>
                    과{" "}
                    <Link href="#" className="hover:underline">
                        개인정보 처리방침
                    </Link>
                    에 동의하는 것으로 간주됩니다.
                </div>
            </div>
        </div>
    )
}
