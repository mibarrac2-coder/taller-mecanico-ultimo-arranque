# Diagnóstico previo a las correcciones

## 1. Inventario

Modelos y DAO recibidos: Cliente.java, DetalleOrdenRepuesto.java, DetalleOrdenServicio.java, Mecanico.java, OrdenTrabajo.java, Persona.java, Repuesto.java, Servicio.java, Vehiculo.java, ClienteDao.java, CrudDao.java, DetalleOrdenRepuestoDao.java, DetalleOrdenServicioDao.java, MecanicoDao.java, OrdenTrabajoDao.java, RepuestoDao.java, ServicioDao.java, VehiculoDao.java. También Taller.java, config/Conexiontaller.java, controlador/ClienteControlador.java, controlador/VehiculoControlador.java, pom.xml, README.md, query.sql, query (1).sql y PDF de requisitos.

## 2. Diagnóstico general

Java 26 compilaba. Había seis CRUD implementados, dos DAO vacíos y dos controladores vacíos. La clase principal solo listaba personas. MySQL80 está activo; sus ocho tablas y columnas se comprobaron directamente el 31/10/2026. El PDF exige web MVC, transacciones de inventario, estados protegidos, comprobante y reportes PDF; no basta con un menú de consola.

## 3. Estado inicial por archivo

| Estado | Archivos | Motivo |
|---|---|---|
| Correcto | Persona.java, Cliente.java, Mecanico.java, Vehiculo.java, CrudDao.java | Conservar herencia, composición, nombres y contratos |
| Corregir | Conexiontaller.java | Retornaba null al fallar y compartía conexión que cada DAO cerraba |
| Corregir | RepuestoDao.java | Actualizar/eliminar informaban éxito sin comprobar filas |
| Corregir | OrdenTrabajoDao.java, OrdenTrabajo.java | Fecha nula/truncada, costo no recuperado, detalles sin cargar y estados no protegidos en SQL |
| Corregir | Repuesto.java, Servicio.java, DetalleOrdenRepuesto.java, DetalleOrdenServicio.java | Montos/cantidades inválidos podían ingresar por constructor |
| Corregir | Taller.java, README.md | Falta integración utilizable y documentación |
| Falta | ClienteControlador.java, VehiculoControlador.java | Archivos vacíos |
| Falta | DetalleOrdenServicioDao.java, DetalleOrdenRepuestoDao.java | Archivos vacíos; requieren transacciones |
| Falta | Controladores de mecánicos, repuestos, servicios, órdenes y detalles; presentación web | Completar las entidades existentes |
| Corregir | query.sql, query (1).sql | Datos apuntan a IDs 7/8 inexistentes y no insertan servicios |
| Verificar | ClienteDao.java, MecanicoDao.java, VehiculoDao.java, ServicioDao.java | CRUD existente y JOIN de propietario correctos; probar sobre esquema aislado y recuperar IDs generados |
| Verificar | pom.xml | Usar Maven incluido en NetBeans y ejecutar clean verify |

## 4. Dependencias

Persona → Cliente/Mecanico. Vehiculo → Cliente (se conserva clientePropitario y getId). OrdenTrabajo → Vehiculo/Mecanico y colecciones de detalles. DetalleOrdenServicio → Servicio. DetalleOrdenRepuesto → Repuesto. Presentación → controladores → DAO → Conexiontaller → MySQL.

## 5–7. Correcciones y archivos faltantes

Se aplicarán archivo por archivo, manteniendo los CRUD funcionales. El informe final incluirá causas, cambios y código completo de archivos modificados/creados. Los detalles se gestionarán dentro de órdenes en proceso y las transacciones bloquearán orden y existencias.

## 8–12. Validación prevista

Comparación contra MySQL real; clean verify; pruebas de CRUD y flujo de órdenes en una base aislada; pruebas HTTP/PDF, errores de conexión e inventario. El alcance Android y los commits de varios integrantes en fechas distintas se evaluarán por separado: no se pueden simular entregables históricos.
