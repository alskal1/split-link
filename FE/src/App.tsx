import { Route, Routes } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import Layout from "./MainLayout";
import MobileLayout from "./MobileLayout";
import MainPage from "./pages/MainPage";
import NewSettlementRoom from "./pages/NewSettlementRoom";
import SettlementRoomCreated from "./pages/SettlementRoomCreated";

function App() {
  return (
    <>
      <Routes>
        <Route
          path="/"
          element={
            <Layout>
              <MainPage />
            </Layout>
          }
        />
        <Route element={<MobileLayout />}>
          <Route path="/new-settlement-room" element={<NewSettlementRoom />} />
          <Route
            path="/settlement-room-created"
            element={<SettlementRoomCreated />}
          />
        </Route>
      </Routes>
      <Toaster />
    </>
  );
}

export default App;
