# Estado de la revisión

Estado: COMPLETADO. Fase 5/5 finalizada: archivos, verificación, documentación y punto de guardado.

| Fase | Resultado |
|---|---|
| 1. Inventario, diagnóstico y respaldo | Completada; originales y cambios manuales conservados |
| 2. Modelos, conexión y DAO | Completada |
| 3. Controladores, web Java MVC y reportes | Completada |
| 4. MySQL, Maven y pruebas | Completada; BUILD SUCCESS y 86 comprobaciones |
| 4b. Android | APK compilado y firmas verificadas. Prueba visual en dispositivo/emulador pendiente. |
| 5. Documentación y recuperación | Completada |

Estado: CORRECTO
Comprobaciones: 86

Se comprobaron los ocho módulos web con datos reales mediante solicitudes de solo lectura. Las pruebas integrales usan una base temporal y verifican que los checksums de los datos originales sean idénticos antes y después.

Guía: taller/taller/GUIA_EJECUCION.md. Informes: DIAGNOSTICO.md, REVISION_ARCHIVOS.md, VALIDACION.md y DIAGRAMAS.md dentro del proyecto.

Para ejecutar:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "C:\Taller Mecanico\taller\taller\Ejecutar.ps1"
```
Abre http://127.0.0.1:8080/.

Para continuar o repetir una verificación interrumpida:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "C:\Taller Mecanico\taller\taller\herramientas\Continuar.ps1"
```
El comando registra la fase y el error si se detiene; no restaura encima de tus fuentes. Si falta terminar la creación/reparación de un archivo, solicita en este chat continuar desde ESTADO_REVISION.md conservando los cambios manuales.

Respaldos: puntos_guardado/inicial-* y puntos posteriores, con fuentes y manifiestos SHA-256. La bitácora previa se conserva en puntos_guardado/bitacora-revision.md. Los puntos son copias de archivos, no respaldos de MySQL. La configuración local de conexión se conserva en database.properties y se excluye de Git.

Pendientes manuales: prueba visual de navegador y Android; completar con tu equipo el historial académico de commits en fechas distintas. No se fabricaron commits, autorías ni pruebas en dispositivos.

Punto de guardado creado: C:\Taller Mecanico\puntos_guardado\final-completado-20261031-100314-115

Punto de guardado creado: C:\Taller Mecanico\puntos_guardado\final-completado-20261031-100416-381
