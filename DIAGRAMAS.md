# Diagramas del proyecto

## Clases del modelo

```mermaid
classDiagram
    class Persona {
        <<abstract>>
        #int id
        #String nombres
        #String apellidos
        #String telefono
        +obtenerInformacionDetallada() String
    }
    Persona <|-- Cliente
    Persona <|-- Mecanico
    Vehiculo --> Cliente : clientePropietario
    OrdenTrabajo --> Vehiculo
    OrdenTrabajo --> Mecanico
    OrdenTrabajo "1" *-- "0..*" DetalleOrdenServicio
    OrdenTrabajo "1" *-- "0..*" DetalleOrdenRepuesto
    DetalleOrdenServicio --> Servicio
    DetalleOrdenRepuesto --> Repuesto
    class Cliente {
        -String direccion
        -String correo
    }
    class Mecanico {
        -String especialidad
    }
    class Vehiculo {
        -int idVehiculo
        -String placa
        -String marca
        -String modelo
        -int anio
    }
    class OrdenTrabajo {
        -int idOrden
        -Date fechaIngreso
        -String estado
        -String descripcionProblema
        -double costoTotal
        +agregarServicio(DetalleOrdenServicio)
        +agregarRepuesto(DetalleOrdenRepuesto)
        +calcularCostoTotalFinal()
    }
    class Servicio {
        -int idServicio
        -String nombreServicio
        -double costoManoObra
    }
    class Repuesto {
        -int idRepuesto
        -String nombre
        -double precio
        -int existencia
    }
    class DetalleOrdenServicio {
        -int idDetalleServicio
        -int idOrden
        -double costoAplicado
    }
    class DetalleOrdenRepuesto {
        -int idDetalleRepuesto
        -int idOrden
        -int cantidad
        -double precioUnitario
        -double subtotal
    }
```

Los campos privados, getters/setters y validaciones conservan el encapsulamiento. Cliente y Mecanico implementan polimórficamente obtenerInformacionDetallada y se usan en los comprobantes. Las colecciones de detalles participan en las órdenes y reportes. Los montos conservan double para compatibilidad con la implementación recibida y se normalizan a centavos; MySQL almacena DECIMAL(10,2).

## Entidad-relación

```mermaid
erDiagram
    clientes ||--o{ vehiculos : posee
    vehiculos ||--o{ ordenes_trabajo : tiene
    mecanicos ||--o{ ordenes_trabajo : atiende
    ordenes_trabajo ||--o{ detalle_orden_servicios : incluye
    ordenes_trabajo ||--o{ detalle_orden_repuestos : utiliza
    servicios ||--o{ detalle_orden_servicios : aplica
    repuestos ||--o{ detalle_orden_repuestos : asigna
    clientes {
        int id_cliente PK
        varchar nombres
        varchar apellidos
        varchar telefono
        varchar correo
        varchar direccion
    }
    vehiculos {
        int id_vehiculo PK
        int id_cliente FK
        varchar placa UK
        varchar marca
        varchar modelo
        int anio
    }
    mecanicos {
        int id_mecanico PK
        varchar nombres
        varchar apellidos
        varchar telefono
        varchar especialidad
    }
    repuestos {
        int id_repuesto PK
        varchar nombre
        text descripcion
        decimal precio
        int existencia
    }
    servicios {
        int id_servicio PK
        varchar nombre_servicio
        text descripcion
        decimal costo_mano_obra
    }
    ordenes_trabajo {
        int id_orden PK
        int id_vehiculo FK
        int id_mecanico FK
        datetime fecha_ingreso
        datetime fecha_entrega
        text descripcion_problema
        varchar estado
        decimal costo_total
    }
    detalle_orden_servicios {
        int id_detalle_servicio PK
        int id_orden FK
        int id_servicio FK
        decimal costo_aplicado
    }
    detalle_orden_repuestos {
        int id_detalle_repuesto PK
        int id_orden FK
        int id_repuesto FK
        int cantidad
        decimal precio_unitario
        decimal subtotal
    }
```

## Capas y flujo

```mermaid
flowchart LR
    navegador[Navegador] --> vista[ServidorWeb y Html]
    vista --> web[WebControlador y ReporteControlador]
    web --> controladores[Controladores de las entidades]
    controladores --> dao[DAO existentes y DAO de detalles]
    dao --> conexion[Conexiontaller / JDBC]
    conexion --> mysql[(MySQL)]
    android[Cliente Android Java] -->|GET API reportes| vista
```

TransaccionOrden bloquea la orden al cambiar sus detalles y bloquea/actualiza existencias mediante SQL dentro de la misma conexión. La operación confirma detalle, stock y total juntos o revierte todo. Los datos de prueba del SQL usan exclusivamente las ocho entidades ya existentes.
