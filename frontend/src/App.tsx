import { Route, Routes } from "react-router-dom";
import { Header } from "./components/layout/Header";
import HomePage from "./pages/HomePage";
import ProductionsPage from "./pages/ProductionsPage";
import AboutPage from "./pages/AboutPage";
import BookingPage from "./pages/BookingPage";
import ProposalPage from "./pages/ProposalPage";
import ReviewsPage from "./pages/ReviewsPage";

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
      </Routes>
    </>
  );
}
