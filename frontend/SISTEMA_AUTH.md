# 🔐 Sistema de Autenticación y Roles - HotelManager

## Credenciales de Acceso

### 👤 Administrador
- **Usuario:** `admin`
- **Contraseña:** `admin123`
- **Rol:** ADMIN

### 👤 Recepcionista
- **Usuario:** `recepcion`
- **Contraseña:** `recep123`
- **Rol:** RECEPCIONISTA

---

## 🎯 Diferencias entre Roles

### 🔴 ADMIN (Administrador)

**Acceso completo al sistema:**

#### Módulos Disponibles:
- ✅ Dashboard administrativo
- ✅ **Gestión de Usuarios** (exclusivo)
  - Crear nuevos usuarios
  - Editar usuarios existentes
  - Activar/desactivar usuarios
  - Asignar roles
- ✅ Gestión de Huéspedes
- ✅ Gestión de Habitaciones
- ✅ Gestión de Pagos

#### Características:
- Vista completa del sistema
- Control total sobre usuarios del sistema
- Acceso a módulos administrativos
- Indicador visual: **Badge morado**

---

### 🟡 RECEPCIONISTA (Operador)

**Acceso operativo del día a día:**

#### Módulos Disponibles:
- ✅ Dashboard operativo
- ✅ Gestión de Reservas
  - Crear nuevas reservas
  - Confirmar/cancelar reservas
  - Marcar no-show
- ✅ Check-in de huéspedes
- ✅ Check-out de huéspedes
- ✅ Gestión de Estadías
  - Ver estadías activas
  - Agregar consumos
- ✅ Gestión de Habitaciones (solo visualización y cambio de estado)
- ✅ Registro de Pagos

#### Restricciones:
- ❌ NO puede gestionar usuarios
- ❌ NO tiene acceso a módulos administrativos
- Indicador visual: **Badge azul**

---

## 🧭 Navegación Dinámica

El **sidebar cambia automáticamente** según el rol del usuario autenticado:

### Menú ADMIN:
- Dashboard
- **Usuarios** ← Solo ADMIN
- Huéspedes
- Habitaciones
- Pagos

### Menú RECEPCIONISTA:
- Dashboard
- Reservas
- Check-in
- Check-out
- Habitaciones
- Pagos

---

## 🔒 Protección de Rutas

El sistema protege rutas según el rol:

- Rutas públicas: `/login`
- Rutas protegidas: requieren autenticación
- Rutas con roles específicos:
  - `/usuarios` → Solo **ADMIN**
  - Si un RECEPCIONISTA intenta acceder: **Pantalla de Acceso Denegado**

---

## 💡 Características de Seguridad

1. **Persistencia de sesión:** LocalStorage
2. **Protección de rutas:** React Router + Guards
3. **Validación de credenciales:** Mock authentication
4. **Feedback visual:** Mensajes de error claros
5. **Control de acceso:** Roles validados en cada ruta
6. **Sesión activa:** Mantiene usuario logueado
7. **Logout seguro:** Limpia sesión y redirige

---

## 🎨 Indicadores Visuales

### Header de Usuario:
- **Nombre completo** del usuario
- **Rol** con badge de color
- **Avatar** con inicial del nombre
- **Menú desplegable** con:
  - Información de sesión
  - Botón de cerrar sesión

### Colores por Rol:
- 🟣 **Morado** = ADMIN
- 🔵 **Azul** = RECEPCIONISTA

---

## 🚀 Flujo de Autenticación

1. Usuario accede a `/login`
2. Ingresa credenciales
3. Sistema valida (mock API con delay)
4. Si es válido:
   - Guarda sesión en LocalStorage
   - Redirige a `/dashboard`
   - Carga menú según rol
5. Si es inválido:
   - Muestra mensaje de error
   - Permite reintentar

---

## 📝 Notas de Implementación

- **AuthContext:** Maneja estado global de autenticación
- **ProtectedRoute:** Componente de protección de rutas
- **Usuarios mock:** Almacenados en memoria (en producción conectar con API)
- **Validación de roles:** En cada módulo sensible
- **UX profesional:** Feedback claro y diseño limpio

---

## 🔧 Próximos Pasos (Producción)

- [ ] Conectar con API REST real
- [ ] Implementar JWT para tokens
- [ ] Agregar refresh tokens
- [ ] Implementar recuperación de contraseña
- [ ] Logs de actividad de usuarios
- [ ] 2FA (autenticación de dos factores)
- [ ] Expiración de sesión por tiempo
