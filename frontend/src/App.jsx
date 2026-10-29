import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import Layout from './Layout.jsx';
import { Spinner } from './ui.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Sales from './pages/Sales.jsx';
import Demands from './pages/Demands.jsx';
import Supplies from './pages/Supplies.jsx';
import Orders from './pages/Orders.jsx';
import Match from './pages/Match.jsx';
import Users from './pages/Users.jsx';
import Crops from './pages/Crops.jsx';
import Reports from './pages/Reports.jsx';
import Forecast from './pages/Forecast.jsx';
import Profile from './pages/Profile.jsx';

function Guard({ roles, children }) {
  const { user, loading } = useAuth();
  if (loading) return <Spinner />;
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/dashboard" replace />;
  return children;
}

function PublicOnly({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <Spinner />;
  return user ? <Navigate to="/dashboard" replace /> : children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<PublicOnly><Login /></PublicOnly>} />
      <Route path="/register" element={<PublicOnly><Register /></PublicOnly>} />
      <Route element={<Guard><Layout /></Guard>}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/orders" element={<Orders />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/sales" element={<Guard roles={['SHOP']}><Sales /></Guard>} />
        <Route path="/demands" element={<Guard roles={['SHOP']}><Demands /></Guard>} />
        <Route path="/supplies" element={<Guard roles={['FARMER']}><Supplies /></Guard>} />
        <Route path="/forecast" element={<Guard roles={['SHOP', 'ADMIN']}><Forecast /></Guard>} />
        <Route path="/match" element={<Guard roles={['ADMIN']}><Match /></Guard>} />
        <Route path="/users" element={<Guard roles={['ADMIN']}><Users /></Guard>} />
        <Route path="/crops" element={<Guard roles={['ADMIN']}><Crops /></Guard>} />
        <Route path="/reports" element={<Guard roles={['ADMIN']}><Reports /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
