import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { Toaster } from "react-hot-toast";
const App = () => {
    return (
        <Router>
            <AuthProvider>
                <Routes>
                    <Route path="/" element={<div>Hello</div>} />
                </Routes>
                <Toaster
                    position="top-right"
                    toastOptions={{
                        duration: 3000,
                        style: {
                            background: "#363636",
                            color: "#fff",
                            fontSize: "13px",
                            borderRadius: "10px",
                            padding: "10px 16px",
                        },
                    }}
                    reverseOrder={false}
                />
            </AuthProvider>
        </Router>
    );
};

export default App;
