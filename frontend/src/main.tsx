import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import "./i18n";
import "./styles/tokens.css";
import "./styles/fonts.css";
import "./styles/global.css";
import { App } from "./App";
import { AdminAuthProvider } from "./contexts/AdminAuthContext";
import { MemberAuthProvider } from "./contexts/MemberAuthContext";

const queryClient = new QueryClient({
  defaultOptions: {
    // 콘텐츠는 거의 정적이다. 도메인별 staleTime은 해당 쿼리에서 재정의한다 (CLAUDE.md §9).
    queries: { staleTime: 60 * 1000, refetchOnWindowFocus: false },
  },
});

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AdminAuthProvider>
        <MemberAuthProvider>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </MemberAuthProvider>
      </AdminAuthProvider>
    </QueryClientProvider>
  </StrictMode>,
);
