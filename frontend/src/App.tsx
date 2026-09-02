import { Route, Routes } from "react-router-dom";
import { Header } from "./components/layout/Header";
import { Footer } from "./components/layout/Footer";
import HomePage from "./pages/HomePage";
import ProductionsPage from "./pages/ProductionsPage";
import ProductionDetailPage from "./pages/ProductionDetailPage";
import AboutPage from "./pages/AboutPage";
import ProgramsPage from "./pages/ProgramsPage";
import ProgramDetailPage from "./pages/ProgramDetailPage";
import ProposalPage from "./pages/ProposalPage";
import ReviewsPage from "./pages/ReviewsPage";
import ReviewDetailPage from "./pages/ReviewDetailPage";
import ArtistsPage from "./pages/ArtistsPage";
import ArtistDetailPage from "./pages/ArtistDetailPage";
import LoginPage from "./pages/LoginPage";
import OAuthCallbackPage from "./pages/OAuthCallbackPage";

export function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/productions" element={<ProductionsPage />} />
        <Route path="/productions/:slug" element={<ProductionDetailPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/programs" element={<ProgramsPage />} />
        <Route path="/programs/:slug" element={<ProgramDetailPage />} />
        <Route path="/proposal" element={<ProposalPage />} />
        <Route path="/reviews" element={<ReviewsPage />} />
        <Route path="/reviews/:id" element={<ReviewDetailPage />} />
        <Route path="/artists" element={<ArtistsPage />} />
        <Route path="/artists/:slug" element={<ArtistDetailPage />} />
        {/* 내비게이션엔 노출하지 않는다 — 진입점은 Footer의 작은 링크뿐이다 (CLAUDE.md §3.5). */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
      </Routes>
      <Footer />
    </>
  );
}
