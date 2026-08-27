import { Route, Routes } from "react-router-dom";
import { Header } from "./components/layout/Header";
import { Footer } from "./components/layout/Footer";
import HomePage from "./pages/HomePage";
import ProductionsPage from "./pages/ProductionsPage";
import AboutPage from "./pages/AboutPage";
import BookingPage from "./pages/BookingPage";
import ProposalPage from "./pages/ProposalPage";
import ReviewsPage from "./pages/ReviewsPage";
import LoginPage from "./pages/LoginPage";

export function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/productions" element={<ProductionsPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/booking" element={<BookingPage />} />
        <Route path="/proposal" element={<ProposalPage />} />
        <Route path="/reviews" element={<ReviewsPage />} />
        {/* 내비게이션엔 노출하지 않는다 — 진입점은 Footer의 작은 링크뿐이다 (CLAUDE.md §3.5). */}
        <Route path="/login" element={<LoginPage />} />
      </Routes>
      <Footer />
    </>
  );
}
