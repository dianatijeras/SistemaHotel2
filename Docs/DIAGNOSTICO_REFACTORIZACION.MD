# Diagnóstico técnico y refactorización — SistemaHotelManager

**Materia:** Ingeniería de Software II  
**Equipo:** Juan Jose Suarez Moncaleano y Diana Maria Garcia Alzate  
**Repositorio:** https://github.com/dianatijeras/SistemaHotel2.git  
**Funcionalidad elegida:** F-04 — Modificar o cancelar una reserva

## 1. Contexto

Nuestro proyecto **SistemaHotelManager** es una aplicación para gestionar huéspedes, habitaciones, reservas, estadías y pagos de un hotel. Utilizamos **React con TypeScript** para la interfaz y **Java 21 con Spring Boot** para el backend. Por ahora los datos se almacenan temporalmente en memoria.

Para este taller escogimos la **funcionalidad F-04: modificar o cancelar una reserva**. La elegimos porque utiliza varias partes del sistema y tiene reglas importantes como revisar las fechas, comprobar la disponibilidad y evitar reservas duplicadas.

Los requisitos relacionados son que el recepcionista pueda buscar, modificar y cancelar reservas, y que el sistema valide los cambios antes de guardarlos.

## 2. ¿Cómo funciona esta parte del proyecto?

Al modificar una reserva, la información pasa por los siguientes archivos y componentes:

`ReservasList.tsx → hotelService.ts → api.ts → ReservaController → ReservaService → repositorios → DataStore`

- **Frontend:** muestra la reserva y permite que el recepcionista cambie los datos.
- **ReservaController:** recibe la petición y la envía al servicio.
- **ReservaService:** comprueba las reglas de negocio, como fechas, estado y disponibilidad.
- **HabitacionService:** ayuda a verificar si la habitación puede usarse en las fechas solicitadas.
- **Repositorio y DataStore:** consultan y actualizan la información en memoria.

Para modificar se utiliza `PUT /api/reservas/{id}` y para cancelar, `PUT /api/reservas/{id}/cancelar`.

## 3. Hallazgos que identificamos

Después de revisar el análisis de **OpenCode** y contrastarlo con el código de reservas, registramos estos tres hallazgos:

### H-01. Validaciones de fechas repetidas

- **Tipo:** deuda técnica.
- **Archivo:** `backend/backendHotel/src/main/java/co/edu/uniquindio/sistemahotelmanager/service/ReservaService.java`.
- **Métodos:** `validarSolicitud()` y `validarRangoDeModificacion()`.
- **Qué encontramos:** al crear y modificar una reserva se repetían las comprobaciones de fechas vacías, fecha de salida posterior a la entrada y fecha de entrada no anterior a hoy.
- **Por qué importa:** si después cambiamos una regla en un método y olvidamos cambiarla en el otro, las dos operaciones podrían funcionar de manera diferente.
- **Qué propusimos:** utilizar un solo método privado para validar las fechas.
- **Estado:** cambio preparado

### H-02. Se utiliza el número 0 para indicar que no cambia la habitación

- **Tipo:** oportunidad de mejora.
- **Archivos:** `ReservaActualizacionDTO.java` y `ReservaService.java`.
- **Método:** `modificarReserva()`.
- **Qué encontramos:** si el número de habitación llega como `0`, el sistema conserva la habitación que ya tenía la reserva.
- **Por qué importa:** una persona que consulte el código podría no entender ese significado y un cliente podría enviar `0` por equivocación.
- **Qué propusimos:** más adelante, hacer explícito cuándo se mantiene una habitación y cuándo se quiere cambiar.
- **Estado:** pendiente. No lo cambiamos porque podría afectar la comunicación con el frontend.

### H-03. El repositorio devuelve directamente la lista de reservas

- **Tipo:** riesgo arquitectónico.
- **Archivo:** `InMemoryReservaRepository.java`.
- **Método:** `buscarTodas()`.
- **Qué encontramos:** este método devuelve la lista de reservas utilizada por el almacenamiento en memoria.
- **Por qué importa:** otros componentes podrían llegar a modificar esa lista sin pasar por las operaciones del repositorio. **Es un riesgo posible, no un error que hayamos demostrado en ejecución.**
- **Qué propusimos:** estudiar si se puede devolver una copia o una lista que no permita modificaciones externas.
- **Estado:** pendiente, porque requiere revisar cómo la utilizan las demás clases.

## 4. ¿Cuál hallazgo decidimos corregir primero?

Usamos una escala de **1 a 3**, donde 1 es bajo y 3 es alto:

| Hallazgo | Impacto | Riesgo de cambiarlo | Esfuerzo | Decisión |
|---|---:|---:|---:|---|
| H-01. Fechas repetidas | 2 | 1 | 1 | **Seleccionado** |
| H-02. Número 0 | 2 | 3 | 2 | Dejar para después |
| H-03. Lista compartida | 3 | 3 | 3 | Requiere más revisión |

**Escogimos H-01** porque era una mejora pequeña que no exigía cambiar lo que hace el sistema para el usuario. También evita que las validaciones se contradigan en el futuro.

## 5. Refactorización realizada

**Antes:** `ReservaService` repetía las mismas tres verificaciones de fechas en dos métodos diferentes.

**Después:** preparamos un método privado llamado `validarRangoDeFechasDeReserva(inicio, fin)` para hacer esas comprobaciones en un solo lugar. Los dos procesos lo utilizan.

El cambio se realizó únicamente en `ReservaService.java`.

Esto es una **refactorización**, porque reorganiza el código para facilitar su mantenimiento, sin buscar cambiar la funcionalidad que recibe el usuario.

## 6. ¿Cómo vamos a comprobar que sigue funcionando?

Hicimos una comparación del código anterior y del código con la modificación. En esa revisión se conservaron las tres condiciones de fechas y sus mensajes.

| Comprobación | Resultado actual |
|---|---|
| Se mantiene la ruta para modificar la reserva | Revisado en el código |
| Se mantienen las validaciones de fecha | Revisado en el código |
| Se mantienen los mensajes de error | Revisado en el código |


## 7. Conclusiones

Con este taller entendimos que refactorizar **no es agregar funciones nuevas**, sino mejorar la organización del código sin cambiar lo que debe hacer.

La mejora seleccionada deja la validación de fechas en un solo sitio. Todavía nos queda revisar cómo se representa el número de habitación en una modificación y cómo se comparte la lista de reservas desde el repositorio.

**¿cómo podríamos haber detectado estos problemas antes?**  
Creemos que con revisiones de código entre compañeros, pruebas automáticas frecuentes y una lista de aspectos por revisar antes de hacer un `pull request` habríamos detectado antes la repetición de las validaciones.

> Este texto fue elaborado por nosotros a partir de la revisión asistida por OpenCode y el contraste con el código.