"use client"

import Link from "next/link"
import { Button } from "@/components/ui/button"

export default function HomePage() {
  return (
    <main className="min-h-screen bg-background">
      <div className="border-b bg-background/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center px-6">
          <nav className="flex items-center gap-2">
            <Button asChild variant="default" className="rounded-full px-5">
              <Link href="/blog">기술블로그</Link>
            </Button>
          </nav>
        </div>
      </div>

      <section className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-6xl items-center px-6">
        <div className="space-y-4">
          <p className="text-sm font-medium uppercase tracking-[0.2em] text-muted-foreground">
            Woobeee
          </p>
          <h1 className="text-4xl font-bold tracking-tight sm:text-5xl">
            새 서비스 진입 화면
          </h1>
          <p className="max-w-2xl text-base text-muted-foreground sm:text-lg">
            이 화면을 루트 랜딩으로 사용하고, 기존 기술 블로그는 상단 탭을 통해 <code>/blog</code>로
            이동하도록 분리했습니다.
          </p>
        </div>
      </section>
    </main>
  )
}
