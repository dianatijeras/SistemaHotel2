# SistemaHotelManager

**Sistema de gestión hotelera** desarrollado como proyecto académico de **Ingeniería de Software 2** de la Universidad del Quindío.

## ¿Qué hace?

Es una aplicación web que facilita la administración de huéspedes, reservas, habitaciones y estadías. Permite acompañar el proceso desde la reserva y el ingreso del huésped hasta el registro de consumos, los pagos, la salida y la limpieza de la habitación.

### Funcionalidades principales

| Requisito | Funcionalidad |
|---|---|
| **F-04** | Gestionar reservas: consultar, modificar, confirmar o cancelar, validando la disponibilidad. |
| **F-05** | Registrar, consultar y actualizar datos de huéspedes. |
| **F-06** | Realizar el **check-in** y registrar el inicio de una estadía. |
| **F-08** | Realizar el **check-out**, verificando el saldo y pasando la habitación a limpieza. |
| **F-09** | Calcular los costos de alojamiento, consumos y saldo pendiente. |
| **F-10** | Registrar y gestionar pagos parciales, con aprobación, rechazo y reembolso. |

El sistema también incluye gestión de habitaciones, personal y un panel de estadísticas.

## Tecnologías y arquitectura

- **Frontend:** React, TypeScript, React Router, Tailwind CSS y Vite.
- **Backend:** Java 21, Spring Boot 3.2, Maven y Lombok.
- **Comunicación:** API REST mediante HTTP y JSON.
- **Almacenamiento:** repositorios en memoria (`DataStore`); **no utiliza base de datos**.

El proyecto sigue una arquitectura por capas:

```text
React → Controllers → Services → Repositories → DataStore
```

Se aplican dos patrones de diseño:

- **State:** controla las operaciones permitidas según el estado de una habitación (disponible, ocupada, en limpieza, etc.).
- **Factory Method:** organiza la creación de distintos tipos de empleados.

Los diagramas **C4** de contexto, contenedores, componentes y código, junto con el diagrama dinámico.

## Estructura del proyecto

```text
SistemaHotelManager/
├── frontend/    # Interfaz web en React
├── backend/     # API y lógica de negocio en Spring Boot
└── C4/          # Diagramas de arquitectura
```
##  Roles del Sistema

### ADMIN
- Acceso completo al sistema
- Gestión de usuarios (crear, activar/desactivar)
- Visualización de habitaciones y pagos
- Panel de estadísticas en el Dashboard

### RECEPCIONISTA
- Gestión de reservas (crear, confirmar, cancelar)
- Realizar check-in y check-out
- Ver y gestionar habitaciones
- Registrar y aprobar pagos
- Gestionar huéspedes

## Cómo ejecutar el proyecto

**Requisitos:** JDK 21, Maven, Node.js y npm.

**1. Iniciar el backend** :

run HotelApplication en directorio backend


**2. Iniciar el frontend** :

```bash
cd frontend
npm install
npm run dev
```

- Frontend: **http://localhost:5173**
- API: **http://localhost:8080/api**

Para ejecutar las pruebas del backend utiliza `mvn clean test`; para compilar el frontend utiliza `npm run build` dentro de su carpeta.

## Consideraciones

- La información se guarda **temporalmente en memoria** y se reinicia al volver a ejecutar el backend.
- El sistema contempla funciones para administrador y recepción; los huéspedes son registros del negocio, **no cuentas con inicio de sesión**.
- La autenticación y los permisos actuales están orientados a la demostración académica.

---

##  Equipo de Desarrollo

> Proyecto desarrollado como trabajo académico para la asignatura  
> **Ingeniería de Software II — Universidad del Quindío**  
> **Juan Jose Suarez Moncaleano y Diana Maria Garcia Alzate**    
> Armenia, Quindío, Colombia · 2026

*Sistema desarrollado con fines académicos. No incluye autenticación real ni base de datos persistente.*
