import type { Metadata } from 'next';
import { Inter, JetBrains_Mono } from 'next/font/google';
import './globals.css';
import { ThemeProvider } from '@/components/theme-provider';
import { QueryClientProviderWrapper } from '@/components/query-client-provider';
import { StandaloneServerProvider } from '@/context/StandaloneServerContext';
import { Toaster } from '@/components/ui/toaster';

const inter = Inter({ variable: '--font-inter', subsets: ['latin'] });
const jetBrainsMono = JetBrains_Mono({ variable: '--font-jetbrains-mono', subsets: ['latin'], display: 'swap' });

export const metadata: Metadata = {
  title: 'spec0 Mock Server',
  description: 'Self-hosted OpenAPI mock server',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${inter.variable} ${jetBrainsMono.variable} antialiased`} suppressHydrationWarning>
        <QueryClientProviderWrapper>
          <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
            {/*
              API base URL: env (NEXT_PUBLIC_MOCK_SERVER_API_URL) or same-origin when served from
              the fat JAR/Docker on :8080; Next dev on :3000 defaults the API to localhost:8080.
            */}
            <StandaloneServerProvider>
              <div className="min-h-screen bg-background">
                <header className="border-b bg-card px-6 py-3 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="font-bold text-lg tracking-tight">spec0</span>
                    <span className="text-muted-foreground text-sm">/ mock server</span>
                  </div>
                  <span className="text-xs text-muted-foreground bg-muted px-2 py-0.5 rounded">standalone</span>
                </header>
                <main className="container mx-auto px-4 py-6">{children}</main>
              </div>
              <Toaster />
            </StandaloneServerProvider>
          </ThemeProvider>
        </QueryClientProviderWrapper>
      </body>
    </html>
  );
}
