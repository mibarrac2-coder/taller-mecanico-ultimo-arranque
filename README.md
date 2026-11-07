
# Taller Mecánico El Último Arranque

## Módulo E - Gestión de Servicios

Proyecto Final de Programación II.

### Descripción

Este módulo permite administrar los servicios que ofrece un taller mecánico mediante una aplicación web desarrollada en Java.

Cada servicio contiene un identificador, una descripción y un costo de mano de obra.

### Funcionalidades

- Registrar nuevos servicios.
- Consultar los servicios almacenados.
- Modificar la descripción y el costo.
- Eliminar servicios.
- Validar los datos ingresados.
- Guardar la información en MySQL.

### Tecnologías utilizadas

- Java 21
- Spring Boot
- Apache NetBeans
- MySQL
- JDBC
- HTML
- Thymeleaf
- Maven

### Requisitos

- JDK 21
- MySQL Server
- MySQL Workbench
- Apache NetBeans o un entorno compatible

### Instalación

1. Descargar el repositorio.
2. Ejecutar el archivo servicios.sql en MySQL.
3. Abrir el proyecto Maven.
4. Configurar la contraseña de MySQL mediante la variable DB_PASSWORD.
5. Ejecutar la aplicación Spring Boot.
6. Abrir http://localhost:8080/servicios

### Ejecución en Windows

Desde CMD, ubicado en la carpeta del proyecto:

Configurar la contraseña:

    set /p DB_PASSWORD=Introduce tu contraseña de MySQL:

Iniciar la aplicación:

    mvnw.cmd spring-boot:run

### Arquitectura

El módulo utiliza una estructura basada en MVC:

- Modelo: Servicio.java
- Acceso a datos: ServicioDAO.java
- Controlador: ServicioControlador.java
- Vistas: servicios.html y editar-servicio.html

### Estado del proyecto

El módulo de Gestión de Servicios funciona de manera independiente con MySQL.

La integración con el módulo general de órdenes de trabajo está pendiente.
