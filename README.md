# TallerMecanico-ElultimoArranque

Proyecto final de Programación II para gestionar órdenes de trabajo de un taller mecánico. Java 26, Maven, MySQL y presentación web MVC.

Para iniciar en PowerShell desde esta carpeta:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Ejecutar.ps1
```

Abre http://127.0.0.1:8080/. La configuración local de MySQL está en database.properties (no versionada); usa database.properties.example al instalar en otra computadora.

- [Guía completa de ejecución, Android y continuación](GUIA_EJECUCION.md)
- [Diagnóstico inicial](DIAGNOSTICO.md)
- [Revisión y código completo por archivo](REVISION_ARCHIVOS.md)
- [Validación y resultados](VALIDACION.md)
- [Diagramas de clases y base de datos](DIAGRAMAS.md)

Verificación completa: `powershell -ExecutionPolicy Bypass -File .\herramientas\Verificar.ps1`.

Continuar tras una interrupción: `powershell -ExecutionPolicy Bypass -File .\herramientas\Continuar.ps1`.
