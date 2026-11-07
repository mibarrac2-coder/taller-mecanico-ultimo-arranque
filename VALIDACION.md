# Validación de la revisión

Fecha de revisión: 31/10/2026 (Guatemala).

## MySQL

Se inspeccionaron directamente SHOW TABLES e information_schema.COLUMNS de la base taller. Tablas, columnas, tipos y relaciones coinciden con los DAO y SQL entregados. Los scripts corregidos de instalación se ejecutaron completos dentro de una base temporal. No se aplicaron cambios al esquema ni a los registros de tu base actual.

Cada prueba integral utiliza una base con nombre aleatorio taller_revision_test_ seguido por 32 caracteres hexadecimales. Se elimina al terminar. La firma CHECKSUM TABLE EXTENDED de las ocho tablas originales se compara antes/después.

## Maven y ejecución

Maven de NetBeans: 3.x instalado en C:/Program Files/netbeans-30-bin/netbeans/java/maven/bin/mvn.cmd. JDK 26.0.2. Se ejecutó clean verify con BUILD SUCCESS. La aplicación también se inició con el comando Ejecutar.ps1, y /salud y el módulo órdenes devolvieron HTTP 200. Ese arranque detectó una diferencia de classloader en exec:java y motivó cargar explícitamente com.mysql.cj.jdbc.Driver; se repitieron las pruebas después de corregirla.

Maven clean verify compila y empaqueta; la suite integral se ejecuta además mediante Verificar.ps1. La salida completa queda en target/pruebas-consola.log y el resumen en target/resultado-pruebas.txt, con copia en puntos_guardado/logs.

## Resultado integral

```text
Estado: CORRECTO
Comprobaciones: 86
OK: Script SQL de instalación completo sin llaves foráneas rotas
OK: Crear cliente y recuperar ID generado
OK: Buscar y actualizar cliente
OK: Validar cliente vacío
OK: Evitar ID manual duplicado al crear
OK: Crear vehículo con cliente existente
OK: Vehículo recupera propietario completo y se actualiza
OK: No eliminar cliente con vehículos
OK: Placa duplicada rechazada
OK: Cliente inexistente rechazado
OK: Crear mecánico
OK: Buscar y actualizar mecánico
OK: Crear servicio
OK: Buscar y actualizar servicio
OK: Crear repuesto
OK: Buscar y actualizar repuesto
OK: Abastecimiento suma unidades
OK: Actualizar/eliminar ID inexistente no informa éxito
OK: Rechazar abastecimiento negativo
OK: Rechazar precio negativo en constructor
OK: Rechazar monto NaN
OK: Listar las cinco entidades de catálogo
OK: Eliminar vehículo sin órdenes
OK: Eliminar cliente sin vehículos
OK: Eliminar mecánico sin órdenes
OK: Eliminar servicio sin uso
OK: Eliminar repuesto sin uso
OK: Crear orden pendiente con fecha/hora
OK: No agregar detalles en PENDIENTE
OK: Iniciar reparación
OK: No finalizar sin servicios
OK: Asignar servicio a orden
OK: Actualizar detalle de servicio
OK: Asignar repuesto y descontar inventario en transacción
OK: Total persistido suma servicios y repuestos
OK: Actualizar cantidad ajusta stock y total
OK: Rechazar inventario insuficiente
OK: Rollback no deja detalle ni descuento parcial
OK: Retirar detalle devuelve unidades
OK: Eliminar servicio recalcula total
OK: Reasignar servicio
OK: Reasignar repuesto
OK: Finalizar orden con servicios
OK: Recuperar orden completa con propietario, detalles y total
OK: No cancelar orden finalizada
OK: No retirar repuestos de orden finalizada
OK: No eliminar orden finalizada
OK: Protección de cierre también en DAO
OK: No eliminar vehículo con órdenes
OK: No eliminar mecánico con órdenes
OK: Historial de órdenes por vehículo
OK: Cancelar reintegra inventario
OK: Cancelar dos veces no duplica stock
OK: Inventario estable tras segundo intento
OK: Cancelar orden pendiente
OK: Eliminar orden pendiente vacía
OK: Dos órdenes concurrentes no consumen la misma última unidad
OK: Total fuera del rango DECIMAL revierte toda la transacci?n
OK: Rollback por desbordamiento devuelve inventario
OK: Comprobante usa total final persistido
OK: HTTP salud confirma MySQL
OK: HTTP módulo vehiculos
OK: HTTP módulo servicios
OK: HTTP módulo detalle_repuestos
OK: HTTP módulo clientes
OK: HTTP módulo ordenes
OK: HTTP módulo mecanicos
OK: HTTP módulo detalle_servicios
OK: HTTP módulo repuestos
OK: Formulario incluye token CSRF
OK: HTTP rechaza cambios sin token
OK: Formulario web crea cliente mediante controlador
OK: Formulario web muestra validación sin error interno
OK: API JSON ordenes
OK: PDF HTTP ordenes
OK: API JSON historial
OK: PDF HTTP historial
OK: API JSON inventario
OK: PDF HTTP inventario
OK: Navegación de reportes y página inexistente
OK: Escape HTML de valores almacenados
OK: PDF con varias páginas
OK: Índice PDF apunta a bytes correctos
OK: Conexión fallida se informa sin NullPointerException
OK: HTTP 503 si MySQL no está disponible
Base temporal eliminada: taller_revision_test_220ffd40cbb241c68168e99fe9af36cd
OK: Datos originales id?nticos antes y despu?s de las pruebas
```

## Arranque y lectura sobre los datos actuales

```text
Clase principal inicia y /salud confirma MySQL: OK
GET clientes con datos actuales: OK
GET vehiculos con datos actuales: OK
GET mecanicos con datos actuales: OK
GET repuestos con datos actuales: OK
GET servicios con datos actuales: OK
GET ordenes con datos actuales: OK
GET detalle_servicios con datos actuales: OK
GET detalle_repuestos con datos actuales: OK
API JSON con datos actuales: OK
Prueba solo de lectura; no se enviaron formularios ni se modificó la base actual.
```

## Android

Se compiló MainActivity contra Android SDK 36, se generó DEX con d8, se empaquetó con aapt2/jar, se alineó con zipalign y se firmó el APK de desarrollo con apksigner. La verificación confirmó firmas v2 y v3. Artefacto: android/build/taller-reportes-debug.apk.

No se realizó instalación ni prueba visual en dispositivo/emulador. El cliente consume los mismos endpoints de reportes que se verificaron por HTTP y JSON en las pruebas Java.

## Pendientes manuales y límites comprobados

- Probar visualmente formularios y PDFs en tu navegador y la aplicación Android en dispositivo/emulador.
- Revisar la entrega académica: el PDF pide cinco commits en fechas distintas con participación de los integrantes. No se fabricaron commits ni autorías.
- Los datos de ejemplo no se importan sobre la base existente; los scripts son para una instalación nueva. Los respaldos son de archivos, no copias de MySQL.

Todas las reparaciones y archivos del alcance Java quedaron implementados y comprobados. Los detalles de las modificaciones y el código completo están en REVISION_ARCHIVOS.md.
