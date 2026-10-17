import { createBrowserRouter } from "react-router";
import { MainLayout } from "./components/layout/MainLayout";
import { ProtectedRoute } from "./components/layout/ProtectedRoute";
import { LoginPage } from "./pages/auth/LoginPage";
import { Dashboard } from "./pages/Dashboard";
import { ReservasList } from "./pages/reservas/ReservasList";
import { ReservasNueva } from "./pages/reservas/ReservasNueva";
import { HuespedList } from "./pages/huespedes/HuespedList";
import { HabitacionList } from "./pages/habitaciones/HabitacionList";
import { CheckIn } from "./pages/CheckIn";
import { CheckOut } from "./pages/CheckOut";
import { EstadiaDetalle } from "./pages/EstadiaDetalle";
import { PagosList } from "./pages/pagos/PagosList";
import { UsuariosList } from "./pages/usuarios/UsuariosList";
import { NotFound } from "./pages/NotFound";
import { RolUsuario } from "./types/auth";

export const router = createBrowserRouter([
    {
        path: "/login",
        element: <LoginPage />,
    },
    {
        path: "/",
        element: (
            <ProtectedRoute>
                <MainLayout />
            </ProtectedRoute>
        ),
        children: [
            { index: true, element: <Dashboard /> },
            {
                path: "usuarios",
                element: (
                    <ProtectedRoute rolesPermitidos={[RolUsuario.ADMIN]}>
                        <UsuariosList />
                    </ProtectedRoute>
                ),
            },
            {
                path: "huespedes",
                element: (
                    <ProtectedRoute rolesPermitidos={[RolUsuario.RECEPCIONISTA]}>
                        <HuespedList />
                    </ProtectedRoute>
                ),
            },
            { path: "reservas", element: <ReservasList /> },
            { path: "reservas/nueva", element: <ReservasNueva /> },
            { path: "habitaciones", element: <HabitacionList /> },
            { path: "check-in", element: <CheckIn /> },
            { path: "check-out", element: <CheckOut /> },
            { path: "estadias/:id", element: <EstadiaDetalle /> },
            { path: "pagos", element: <PagosList /> },
            { path: "*", element: <NotFound /> },
        ],
    },
]);
