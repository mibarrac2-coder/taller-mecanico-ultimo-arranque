# Cómo ejecutar y verificar el Taller Mecánico

Revisión del 31/10/2026. Proyecto conservado en Java/Maven bajo `com.mycompany.taller`, con MySQL y presentación web MVC. No se reemplazó por otro proyecto ni se añadieron frameworks. Los archivos que agregaste manualmente se conservaron; el cambio manual a `clientePropietario` se tomó como nombre vigente.

## Iniciar la aplicación

Requisitos: JDK 26, MySQL 8 activo y Maven. En esta computadora se usa automáticamente el Maven incluido en NetBeans 30 cuando no existe `mvn` en PATH.

En PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "C:\Taller Mecanico\taller\taller\Ejecutar.ps1"
```

Abre **http://127.0.0.1:8080/**. La terminal debe permanecer abierta. Para detener el servidor, pulsa Ctrl+C y confirma S si Maven pregunta si deseas terminar el trabajo por lotes.

Para otro puerto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "C:\Taller Mecanico\taller\taller\Ejecutar.ps1" -Puerto 8081
```

El script funciona desde cualquier directorio. Si copias o clonas el repositorio, usa la ruta nueva del archivo `Ejecutar.ps1`. Todos sus scripts operativos están dentro del proyecto.

En NetBeans: abre la carpeta que contiene `pom.xml`, selecciona JDK 26, ejecuta Clean and Build y después Run Project con la clase principal `com.mycompany.taller.Taller`. La presentación se usa en el navegador.

## Conexión con tu base actual

Ya se confirmó conexión con tu base `taller` y se consultaron sus ocho tablas. Los datos originales no se modificaron. El archivo local `database.properties` conserva la configuración que usabas; está excluido de Git. No necesitas importar SQL para seguir utilizando tu base actual.

En otra computadora copia `database.properties.example` a `database.properties` dentro de la carpeta del pom y configura:

```properties
db.url=jdbc:mysql://localhost:3306/taller?connectTimeout=5000
db.user=root
db.password=TU_CLAVE
```

Las variables `TALLER_DB_URL`, `TALLER_DB_USER` y `TALLER_DB_PASSWORD` permiten cambiar esos valores. La propiedad Java `taller.config` permite indicar otro archivo de configuración. El controlador JDBC se carga explícitamente para que también funcione desde Maven/NetBeans.

Para una instalación NUEVA y vacía, usa `sql/01-esquema.sql` y después `sql/02-datos-prueba.sql` desde MySQL Workbench, seleccionando primero la base de destino. Los scripts `query.sql` y `query (1).sql` de la carpeta superior también quedaron corregidos para instalación nueva. No son migraciones y sus INSERT de ejemplo no deben ejecutarse sobre la base con registros que ya utilizas.

## Uso de los módulos

1. Crea o consulta clientes, mecánicos, servicios y repuestos. Deja ID en 0 al crear: MySQL genera el identificador. Pulsa Editar para modificar un registro existente.
2. Crea el vehículo con el ID de un cliente existente. El listado muestra el ID y el nombre completo del propietario.
3. Crea una orden con ID de vehículo, ID de mecánico y descripción del problema. Una nueva orden comienza PENDIENTE.
4. Edita la orden y cambia su estado a EN PROCESO.
5. En detalle servicios registra ID de orden, ID de servicio y costo aplicado. En detalle repuestos registra ID de orden, ID de repuesto, cantidad y precio unitario. Consulta los IDs en sus módulos. Los importes aceptan punto decimal, por ejemplo 125.50.
6. El inventario y el total se actualizan juntos. Retirar un repuesto en proceso reintegra las unidades. Abastecer inventario suma unidades al stock actual.
7. Finaliza la orden cuando tenga al menos un servicio. En Reportes puedes consultar y descargar su comprobante PDF. El total y la fecha también se muestran en el listado de órdenes.
8. Una orden pendiente o en proceso puede CANCELARSE: sus repuestos regresan al inventario una sola vez. Las finalizadas y canceladas no admiten cambios posteriores. Solo se eliminan órdenes pendientes sin detalles.

Las llaves foráneas impiden borrar clientes con vehículos, vehículos/mecánicos con órdenes y servicios/repuestos utilizados. Los montos se normalizan a centavos y se validan contra el rango DECIMAL(10,2). Los formularios validan datos y usan token CSRF; los valores se escapan al mostrar HTML.

## Reportes y API

Abre **http://127.0.0.1:8080/reportes** para órdenes por estado, historial por placa, inventario bajo o agotado y comprobantes finales. Cada reporte admite PDF, pantalla o JSON. Un comprobante requiere una orden finalizada.

| Consulta | Ruta de ejemplo |
|---|---|
| Estado de servidor y MySQL | /salud |
| Órdenes | /api/reportes?tipo=ordenes |
| Órdenes en proceso | /api/reportes?tipo=ordenes&estado=EN%20PROCESO |
| Historial de vehículo | /api/reportes?tipo=historial&placa=P-123ABC |
| Existencia menor o igual a 5 | /api/reportes?tipo=inventario&limite=5 |
| Inventario agotado | /api/reportes?tipo=inventario&limite=0 |
| Comprobante PDF | /reporte?tipo=comprobante&id=ID_ORDEN&formato=pdf |

El JSON tiene la forma `{"lineas":["texto del reporte", "..."]}`. Android consume esta API de solo lectura y consulta la misma base a través del servidor Java.

## Android

Fuente nativa Java: `android/src/com/mycompany/taller/reportes/MainActivity.java`. APK compilado y firmado para pruebas: `android/build/taller-reportes-debug.apk`. Requiere Android 8.0 o posterior. No requiere Gradle ni librerías adicionales para este cliente: el script usa las herramientas del SDK instalado.

Para volver a compilar:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "C:\Taller Mecanico\taller\taller\herramientas\Compilar-Android.ps1"
```

Requiere Android SDK Platform 36 y Build Tools 36.0.0. Puedes indicar otra ubicación con `-Sdk "RUTA_DEL_SDK"`. El script empaqueta DEX, alinea el APK y verifica su firma v2/v3. La llave local de desarrollo queda en `android/build/debug.keystore` y no se añade a Git.

Con un emulador iniciado, puedes instalarlo usando:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "C:\Taller Mecanico\taller\taller\android\build\taller-reportes-debug.apk"
```

Abre Reportes Taller. En el emulador usa `http://10.0.2.2:8080`; inicia primero el servidor Java. Para un teléfono en la misma red, inicia el servidor con `-HostTaller 0.0.0.0` y usa la IP local de la computadora en la aplicación. El acceso de red dependerá de la configuración de Windows. El cliente permite consultar órdenes, inventario e historial por placa.

Se comprobaron compilación, empaquetado y firma del APK. La instalación y prueba visual en un dispositivo/emulador se dejan como comprobación manual; no se instaló nada en tus dispositivos.

## Verificación reproducible

Desde cualquier carpeta:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "C:\Taller Mecanico\taller\taller\herramientas\Verificar.ps1"
```

Este comando ejecuta Maven clean verify y después las pruebas integrales Java. Son **86 comprobaciones**, incluyendo CRUD, propietarios, fechas, estados, totales, rollback, cancelación, concurrencia de inventario, errores de conexión, formularios HTTP, JSON y estructura de PDF multipágina. Las pruebas crean una base `taller_revision_test_...`, la eliminan al terminar y comparan los checksums de las ocho tablas originales antes y después.

El usuario de pruebas necesita permiso para crear y eliminar esa base temporal. Si una interrupción forzosa impide el bloque de limpieza, el nombre exacto aparece en la salida; no se debe confundir con `taller` ni borrar otras bases.

`mvn clean verify` por sí solo compila y empaqueta; estas pruebas sin framework se ejecutan mediante `Verificar.ps1`. Resultados: `target/resultado-pruebas.txt` y `target/pruebas-consola.log`. También se conserva copia del resultado y del log Maven en `puntos_guardado/logs`.

Para comprobar el arranque y los ocho módulos sobre los datos actuales con solicitudes de solo lectura:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "C:\Taller Mecanico\taller\taller\herramientas\Probar-Web.ps1"
```

## Punto de guardado y continuación

Estado y fase actual: `C:Taller MecanicoESTADO_REVISION.md`, con copia dentro del proyecto. Respaldos: `C:Taller Mecanicopuntos_guardado`.

El respaldo inicial conserva tus fuentes, SQL y diferencias anteriores. Los puntos posteriores conservan fuentes, scripts y documentación, más un manifiesto SHA-256. No incluyen el contenido de MySQL, `target` ni el APK: se regeneran con los comandos anteriores. La configuración privada se conserva en tu archivo local.

Para continuar una verificación interrumpida y regenerar documentación/punto de guardado:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "C:\Taller Mecanico\taller\taller\herramientas\Continuar.ps1"
```

El comando registra la fase antes de cada paso y se detiene si falla una comprobación. Repite la validación desde las fuentes actuales sin restaurarlas encima de tus cambios. Puedes usar `-SinAndroid` si solo deseas verificar Java en una máquina sin SDK; el estado indicará esa omisión.

Si una interrupción dejó pendiente la CREACIÓN o reparación de un archivo, pega en este chat:

> Continúa la revisión de C:\Taller Mecanico. Lee ESTADO_REVISION.md y prompt_revision_proyecto_taller.md, compara los archivos actuales con el último punto de guardado, conserva los cambios manuales y retoma la fase pendiente. Ejecuta las comprobaciones y actualiza la documentación.

Los generadores históricos de fases ya completadas están protegidos para evitar que sobrescriban reparaciones posteriores. Para recuperar un archivo, compáralo con el punto elegido y restaura solamente el necesario. El respaldo no es un comando automático para deshacer todo el proyecto.

## Qué quedó completado y qué verificar manualmente

Completado: revisión de todas las capas, reparaciones compatibles, CRUD de las entidades existentes, presentación web Java MVC, transacciones de inventario, flujo de órdenes y comprobante, reportes PDF/JSON, cliente Android compilado, scripts SQL coherentes, guía e informes con código completo. Maven y las 86 comprobaciones finalizaron correctamente; también se verificó el comando documentado de arranque con HTTP 200 y MySQL real.

Comprobaciones manuales: navegación visual en tu navegador e instalación del APK en teléfono/emulador. El PDF académico solicita además un historial Git con cinco commits en fechas distintas y participación de los integrantes; no se fabricaron commits ni autorías. Los diagramas editables están en DIAGRAMAS.md. Ninguna de estas comprobaciones se presenta como realizada automáticamente.

Consulta también DIAGNOSTICO.md (estado inicial), REVISION_ARCHIVOS.md (problema → causa → corrección y código completo por archivo), VALIDACION.md (evidencias) y DIAGRAMAS.md.
