"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { getAuthRole, type UserRole } from "@/lib/fetch-client";

const links = [
  { href: "/", label: "대시보드" },
  { href: "/investors", label: "출자자 관리" },
  { href: "/investments", label: "기업 투자" },
  { href: "/distributions", label: "배분금 관리" },
  { href: "/audit-logs", label: "감시 로그", adminOnly: true },
];

export function Navigation() {
  const pathname = usePathname();
  const [role, setRole] = useState<UserRole | null>(null);

  useEffect(() => {
    setRole(getAuthRole());
    const onStorage = () => setRole(getAuthRole());
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, [pathname]);

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4">
        <Link href="/" className="text-xl font-bold text-indigo-700">
          AllocHub
        </Link>
        <nav className="flex gap-1">
          {links
            .filter((link) => !link.adminOnly || role === "admin")
            .map((link) => {
              const active =
                link.href === "/"
                  ? pathname === "/"
                  : pathname.startsWith(link.href);
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                    active
                      ? "bg-indigo-50 text-indigo-700"
                      : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                  }`}
                >
                  {link.label}
                </Link>
              );
            })}
        </nav>
      </div>
    </header>
  );
}
