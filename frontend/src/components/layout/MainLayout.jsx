import { Outlet } from "react-router-dom";
import Header from "./Header";
import Sidebar from "./Sidebar";
import Footer from "./Footer";
const MainLayout = () => {
    return (
        <div className="min-h-screen">
            <Header />
            <div className="flex pt-16">
                <Sidebar />
                <main className="flex-1 ml-0 md:ml-64 p-4">
                    <Outlet />
                </main>
            </div>
            <Footer />
        </div>
    );
};

export default MainLayout;
