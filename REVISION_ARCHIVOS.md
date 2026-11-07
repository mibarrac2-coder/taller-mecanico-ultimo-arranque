# Revisión por archivo y código completo

El diagnóstico previo y la tabla inicial están en DIAGNOSTICO.md. Este informe se genera desde las fuentes finales: no son fragmentos ni reemplazos pendientes de copiar. Los cambios ya se aplicaron en el proyecto. Se conservan las clases correctas y los nombres vigentes de los archivos manuales.

## android/src/com/mycompany/taller/reportes/MainActivity.java

**Estado:** Crear.

**Problema encontrado:** El PDF requiere un cliente Android que consulte un reporte con los mismos datos.

**Cambio realizado:** Cliente nativo que consulta API JSON de órdenes, inventario e historial mediante HTTP fuera del hilo de interfaz.

**Código completo final:**

```java
package com.mycompany.taller.reportes;

import android.app.Activity;
import android.os.Bundle;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends Activity {
    private final ExecutorService ejecutor=Executors.newSingleThreadExecutor();
    private EditText servidor,placa;
    private Spinner tipo;
    private TextView resultado;
    private Button consultar;
    public void onCreate(Bundle estado){
        super.onCreate(estado);
        LinearLayout pagina=new LinearLayout(this);pagina.setOrientation(LinearLayout.VERTICAL);pagina.setPadding(24,32,24,24);
        TextView titulo=new TextView(this);titulo.setText("El Último Arranque — Reportes");titulo.setTextSize(22);pagina.addView(titulo);
        TextView ayuda=new TextView(this);ayuda.setText("Emulador: http://10.0.2.2:8080. Teléfono: dirección IP de la computadora en la misma red.");pagina.addView(ayuda);
        servidor=new EditText(this);servidor.setSingleLine(true);servidor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);servidor.setText(getPreferences(MODE_PRIVATE).getString("servidor","http://10.0.2.2:8080"));servidor.setContentDescription("Dirección del servidor");pagina.addView(servidor);
        tipo=new Spinner(this);tipo.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Órdenes por estado","Inventario bajo o agotado","Historial por placa"}));pagina.addView(tipo);
        placa=new EditText(this);placa.setSingleLine(true);placa.setHint("Placa para consultar historial");pagina.addView(placa);
        consultar=new Button(this);consultar.setText("Consultar reporte");pagina.addView(consultar);consultar.setOnClickListener(v->consultar());
        resultado=new TextView(this);resultado.setTextIsSelectable(true);resultado.setText("Seleccione un reporte y pulse Consultar.");ScrollView scroll=new ScrollView(this);scroll.addView(resultado);pagina.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));setContentView(pagina);
    }
    private void consultar(){
        String direccion=servidor.getText().toString().trim();String[] tipos={"ordenes","inventario","historial"};String reporte=tipos[tipo.getSelectedItemPosition()];String matricula=placa.getText().toString().trim();
        if(!(direccion.startsWith("http://")||direccion.startsWith("https://"))){resultado.setText("Ingrese una dirección http:// o https:// válida.");return;}
        if(reporte.equals("historial")&&matricula.isEmpty()){resultado.setText("Ingrese la placa.");return;}
        getPreferences(MODE_PRIVATE).edit().putString("servidor",direccion).apply();consultar.setEnabled(false);resultado.setText("Consultando…");
        ejecutor.execute(()->{
            String texto;HttpURLConnection conexion=null;
            try{
                String base=direccion.replaceAll("/+$","");Uri uri=Uri.parse(base+"/api/reportes").buildUpon().appendQueryParameter("tipo",reporte).appendQueryParameter("placa",matricula).appendQueryParameter("limite","5").build();
                conexion=(HttpURLConnection)new URL(uri.toString()).openConnection();conexion.setConnectTimeout(5000);conexion.setReadTimeout(10000);conexion.setRequestProperty("Accept","application/json");
                int codigo=conexion.getResponseCode();if(codigo!=200)throw new IOException("El servidor respondió HTTP "+codigo);
                StringBuilder cuerpo=new StringBuilder();try(BufferedReader lector=new BufferedReader(new InputStreamReader(conexion.getInputStream(),"UTF-8"))){String linea;while((linea=lector.readLine())!=null)cuerpo.append(linea);}
                JSONArray lineas=new JSONObject(cuerpo.toString()).getJSONArray("lineas");StringBuilder salida=new StringBuilder();for(int i=0;i<lineas.length();i++)salida.append(lineas.getString(i)).append("\n\n");texto=salida.toString();
            }catch(Exception e){texto="No se pudo consultar: "+e.getMessage()+"\nCompruebe que el servidor esté iniciado y que la dirección sea accesible.";}
            finally{if(conexion!=null)conexion.disconnect();}
            final String mensaje=texto;runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){resultado.setText(mensaje);consultar.setEnabled(true);}});
        });
    }
    protected void onDestroy(){ejecutor.shutdownNow();super.onDestroy();}
}
```

## src/main/java/com/mycompany/taller/Taller.java

**Estado:** Corregir.

**Problema encontrado:** La clase principal solo listaba clientes y mecánicos.

**Cambio realizado:** Arrancar el servidor web Java con puerto/host configurables y cierre ordenado.

**Código completo final:**

```java
package com.mycompany.taller;

import com.mycompany.taller.vista.ServidorWeb;

public class Taller {
    public static void main(String[] args){
        try{
            String host=System.getenv().getOrDefault("TALLER_HOST","127.0.0.1");int puerto=Integer.parseInt(System.getenv().getOrDefault("TALLER_PORT","8080"));
            ServidorWeb servidor=new ServidorWeb(host,puerto);Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));servidor.iniciar();
            System.out.println("Taller disponible en http://"+host+":"+servidor.puerto()+"/");
        }catch(Exception e){System.err.println("No se pudo iniciar el servidor: "+e.getMessage());System.exit(1);}
    }
}
```

## src/main/java/com/mycompany/taller/config/Conexiontaller.java

**Estado:** Corregir.

**Problema encontrado:** La conexión compartida podía cerrarse desde otro DAO y se retornaba null al fallar. Maven exec:java requería carga explícita del driver.

**Cambio realizado:** Crear/cerrar una conexión por operación, propagar SQLException, cargar MySQL JDBC, leer configuración privada y devolver consultas desconectadas.

**Código completo final:**

```java
package com.mycompany.taller.config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;

public class Conexiontaller {
    private static final Properties CONFIG = cargarConfiguracion();
    private Conexiontaller() { }

    private static Properties cargarConfiguracion() {
        Properties p = new Properties();
        Path archivo = Path.of(System.getProperty("taller.config", "database.properties"));
        if (Files.exists(archivo)) {
            try (InputStream entrada = Files.newInputStream(archivo)) { p.load(entrada); }
            catch (java.io.IOException e) { throw new IllegalStateException("No se pudo leer database.properties", e); }
        }
        return p;
    }
    private static String valor(String variable, String clave, String defecto) {
        String propiedad = System.getProperty("taller." + clave);
        if (propiedad != null) return propiedad;
        String v = System.getenv(variable);
        return v != null ? v : CONFIG.getProperty(clave, defecto);
    }
    public static Connection IniciarConexion() throws SQLException {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new SQLException("No se encontr? el controlador MySQL JDBC en el classpath.",e); }
        return DriverManager.getConnection(
            valor("TALLER_DB_URL", "db.url", "jdbc:mysql://localhost:3306/taller?connectTimeout=5000"),
            valor("TALLER_DB_USER", "db.user", "root"), valor("TALLER_DB_PASSWORD", "db.password", ""));
    }
    public static boolean ejecutarInstruccion(String sql) {
        try (Connection con = IniciarConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new IllegalStateException("No se pudo ejecutar la instrucción SQL", e); }
    }
    public static ResultSet ejecutarConsulta(String sql) {
        try (Connection con = IniciarConexion(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            CachedRowSet copia = RowSetProvider.newFactory().createCachedRowSet();
            copia.populate(rs);
            return copia;
        } catch (SQLException e) { throw new IllegalStateException("No se pudo ejecutar la consulta SQL", e); }
    }
    public static void cerrarConexion() { }
}
```

## src/main/java/com/mycompany/taller/controlador/ClienteControlador.java

**Estado:** Corregir.

**Problema encontrado:** El controlador faltaba o estaba vacío y no validaba datos ni relaciones.

**Cambio realizado:** Completar CRUD mediante el DAO existente, validar campos y IDs y coordinar referencias sin duplicar SQL.

**Código completo final:**

```java
package com.mycompany.taller.controlador;

import com.mycompany.taller.dao.*;
import com.mycompany.taller.modelo.*;

public class ClienteControlador extends CrudControlador<Cliente> {
    public ClienteControlador(){super(new ClienteDao());}
    protected int obtenerId(Cliente o){return o.getId();}
    protected void validar(Cliente o){Validacion.texto(o.getNombres(),"Nombres",100);Validacion.texto(o.getApellidos(),"Apellidos",100);Validacion.texto(o.getTelefono(),"Teléfono",20);Validacion.opcional(o.getDireccion(),"Dirección",255);Validacion.opcional(o.getCorreo(),"Correo",100);if(o.getCorreo()!=null && !o.getCorreo().isBlank() && !o.getCorreo().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))throw new IllegalArgumentException("Correo inválido.");}
    
}
```

## src/main/java/com/mycompany/taller/controlador/CrudControlador.java

**Estado:** Crear.

**Problema encontrado:** Faltaba coordinación común del CRUD.

**Cambio realizado:** Usar los DAO existentes y separar validación de persistencia.

**Código completo final:**

```java
package com.mycompany.taller.controlador;

import com.mycompany.taller.dao.CrudDao;
import java.util.List;

public abstract class CrudControlador<T> implements CrudDao<T> {
    protected final CrudDao<T> dao;
    protected CrudControlador(CrudDao<T> dao){this.dao=dao;}
    protected abstract int obtenerId(T objeto);
    protected abstract void validar(T objeto);
    public boolean insertar(T objeto){if(objeto==null)throw new IllegalArgumentException("Faltan los datos.");if(obtenerId(objeto)!=0)throw new IllegalArgumentException("Al crear, deje el ID en 0; MySQL lo genera.");validar(objeto);return dao.insertar(objeto);}
    public T buscarPorId(int id){Validacion.id(id);return dao.buscarPorId(id);}
    public List<T> listarTodos(){return dao.listarTodos();}
    public boolean actualizar(T objeto){if(objeto==null)throw new IllegalArgumentException("Faltan los datos.");Validacion.id(obtenerId(objeto));validar(objeto);return dao.actualizar(objeto);}
    public boolean eliminar(int id){Validacion.id(id);return dao.eliminar(id);}
}
```

## src/main/java/com/mycompany/taller/controlador/DetalleOrdenRepuestoControlador.java

**Estado:** Crear.

**Problema encontrado:** El controlador faltaba o estaba vacío y no validaba datos ni relaciones.

**Cambio realizado:** Completar CRUD mediante el DAO existente, validar campos y IDs y coordinar referencias sin duplicar SQL.

**Código completo final:**

```java
package com.mycompany.taller.controlador;

import com.mycompany.taller.dao.*;
import com.mycompany.taller.modelo.*;

public class DetalleOrdenRepuestoControlador extends CrudControlador<DetalleOrdenRepuesto> {
    public DetalleOrdenRepuestoControlador(){super(new DetalleOrdenRepuestoDao());}
    protected int obtenerId(DetalleOrdenRepuesto o){return o.getIdDetalleRepuesto();}
    protected void validar(DetalleOrdenRepuesto o){Validacion.id(o.getIdOrden());if(o.getRepuesto()==null || new RepuestoDao().buscarPorId(o.getRepuesto().getIdRepuesto())==null)throw new IllegalArgumentException("Repuesto inexistente.");if(o.getCantidad()<=0)throw new IllegalArgumentException("Cantidad inválida.");Validacion.monto(o.getPrecioUnitario());}
    
}
```

## src/main/java/com/mycompany/taller/controlador/DetalleOrdenServicioControlador.java

**Estado:** Crear.

**Problema encontrado:** El controlador faltaba o estaba vacío y no validaba datos ni relaciones.

**Cambio realizado:** Completar CRUD mediante el DAO existente, validar campos y IDs y coordinar referencias sin duplicar SQL.

**Código completo final:**

```java
package com.mycompany.taller.controlador;

import com.mycompany.taller.dao.*;
import com.mycompany.taller.modelo.*;

public class DetalleOrdenServicioControlador extends CrudControlador<DetalleOrdenServicio> {
    public DetalleOrdenServicioControlador(){super(new DetalleOrdenServicioDao());}
    protected int obtenerId(DetalleOrdenServicio o){return o.getIdDetalleServicio();}
    protected void validar(DetalleOrdenServicio o){Validacion.id(o.getIdOrden());if(o.getServicio()==null || new ServicioDao().buscarPorId(o.getServicio().getIdServicio())==null)throw new IllegalArgumentException("Servicio inexistente.");Validacion.monto(o.getCostoAplicado());}
    
}
```

## src/main/java/com/mycompany/taller/controlador/MecanicoControlador.java

**Estado:** Crear.

**Problema encontrado:** El controlador faltaba o estaba vacío y no validaba datos ni relaciones.

**Cambio realizado:** Completar CRUD mediante el DAO existente, validar campos y IDs y coordinar referencias sin duplicar SQL.

**Código completo final:**

```java
package com.mycompany.taller.controlador;

import com.mycompany.taller.dao.*;
import com.mycompany.taller.modelo.*;

public class MecanicoControlador extends CrudControlador<Mecanico> {
    public MecanicoControlador(){super(new MecanicoDao());}
    protected int obtenerId(Mecanico o){return o.getId();}
    protected void validar(Mecanico o){Validacion.texto(o.getNombres(),"Nombres",100);Validacion.texto(o.getApellidos(),"Apellidos",100);Validacion.texto(o.getTelefono(),"Teléfono",20);Validacion.opcional(o.getEspecialidad(),"Especialidad",100);}
    
}
```

## src/main/java/com/mycompany/taller/controlador/OrdenTrabajoControlador.java

**Estado:** Crear.

**Problema encontrado:** El controlador faltaba o estaba vacío y no validaba datos ni relaciones.

**Cambio realizado:** Completar CRUD mediante el DAO existente, validar campos y IDs y coordinar referencias sin duplicar SQL.

**Código completo final:**

```java
package com.mycompany.taller.controlador;

import com.mycompany.taller.dao.*;
import com.mycompany.taller.modelo.*;

public class OrdenTrabajoControlador extends CrudControlador<OrdenTrabajo> {
    public OrdenTrabajoControlador(){super(new OrdenTrabajoDao());}
    protected int obtenerId(OrdenTrabajo o){return o.getIdOrden();}
    protected void validar(OrdenTrabajo o){if(o.getVehiculo()==null || o.getMecanico()==null)throw new IllegalArgumentException("Seleccione vehículo y mecánico.");Vehiculo v=new VehiculoDao().buscarPorId(o.getVehiculo().getIdVehiculo());Mecanico m=new MecanicoDao().buscarPorId(o.getMecanico().getId());if(v==null || m==null)throw new IllegalArgumentException("Vehículo o mecánico inexistente.");o.setVehiculo(v);o.setMecanico(m);if(o.getFechaIngreso()==null)o.setFechaIngreso(new java.util.Date());Validacion.texto(o.getdescripcionProblema(),"Descripción del problema",16000);}
    public boolean cambiarEstado(int id,String estado){OrdenTrabajo o=buscarPorId(id);if(o==null)throw new IllegalArgumentException("La orden no existe.");o.setEstado(estado);return actualizar(o);}
    public java.util.List<OrdenTrabajo> listarPorEstado(String estado){if(!java.util.Set.of("PENDIENTE","EN PROCESO","FINALIZADA","CANCELADA").contains(estado))throw new IllegalArgumentException("Estado inválido.");return listarTodos().stream().filter(o->o.getEstado().equals(estado)).toList();}
}
```

## src/main/java/com/mycompany/taller/controlador/ReporteControlador.java

**Estado:** Crear.

**Problema encontrado:** Faltaban reportes y comprobante final.

**Cambio realizado:** Construir reportes desde los modelos y detalles, incluyendo uso polimórfico de Persona en comprobantes.

**Código completo final:**

```java
package com.mycompany.taller.controlador;

import com.mycompany.taller.modelo.*;
import java.util.*;

public class ReporteControlador {
    public List<String> generar(Map<String,String> p){
        List<String> lineas=new ArrayList<>();String tipo=p.getOrDefault("tipo","ordenes");lineas.add("Taller Mecánico - El Último Arranque");
        switch(tipo){
            case "ordenes" -> {String estado=WebControlador.texto(p,"estado");lineas.add("Órdenes por estado: "+(estado.isEmpty()?"TODOS":estado));List<OrdenTrabajo> lista=estado.isEmpty()?new OrdenTrabajoControlador().listarTodos():new OrdenTrabajoControlador().listarPorEstado(estado);lista.forEach(o->lineas.add(resumen(o)));}
            case "historial" -> {String placa=WebControlador.texto(p,"placa");Validacion.texto(placa,"Placa",20);lineas.add("Historial del vehículo "+placa);new OrdenTrabajoControlador().listarTodos().stream().filter(o->o.getVehiculo().getPlaca().equalsIgnoreCase(placa)).forEach(o->lineas.add(resumen(o)));}
            case "inventario" -> {int limite=WebControlador.entero(p,"limite",5);lineas.add("Repuestos con existencia menor o igual a "+limite);new RepuestoControlador().listarBajoInventario(limite).forEach(r->lineas.add(r.getIdRepuesto()+" | "+r.getNombre()+" | Existencia: "+r.getExistencia()+" | Precio Q"+dinero(r.getPrecio())));}
            case "comprobante" -> {OrdenTrabajo o=new OrdenTrabajoControlador().buscarPorId(WebControlador.entero(p,"id",0));if(o==null || !o.getEstado().equals("FINALIZADA"))throw new IllegalArgumentException("El comprobante requiere una orden FINALIZADA.");lineas.add("COMPROBANTE DE TRABAJO #"+o.getIdOrden());lineas.add(resumen(o));Cliente c=o.getVehiculo().getClientePropietario();lineas.add(c.obtenerInformacionDetallada());lineas.add(o.getMecanico().obtenerInformacionDetallada());lineas.add("Problema: "+o.getdescripcionProblema());o.getListaServicios().forEach(d->lineas.add("Servicio: "+d.getServicio().getNombreServicio()+" Q"+dinero(d.getCostoAplicado())));o.getListaRepuestos().forEach(d->lineas.add("Repuesto: "+d.getRepuesto().getNombre()+" x"+d.getCantidad()+" Q"+dinero(d.getSubtotal())));lineas.add("TOTAL Q"+dinero(o.getCostoTotal()));}
            default -> throw new IllegalArgumentException("Reporte desconocido.");
        }
        if(lineas.size()==2)lineas.add("Sin registros.");return lineas;
    }
    private String resumen(OrdenTrabajo o){return "Orden #"+o.getIdOrden()+" | "+o.getEstado()+" | Placa "+o.getVehiculo().getPlaca()+" | Mecánico "+o.getMecanico().getNombres()+" | "+o.getFechaIngreso()+" | Total Q"+dinero(o.getCostoTotal());}
    private String dinero(double n){return String.format(Locale.ROOT,"%.2f",n);}
}
```

## src/main/java/com/mycompany/taller/controlador/RepuestoControlador.java

**Estado:** Crear.

**Problema encontrado:** El controlador faltaba o estaba vacío y no validaba datos ni relaciones.

**Cambio realizado:** Completar CRUD mediante el DAO existente, validar campos y IDs y coordinar referencias sin duplicar SQL.

**Código completo final:**

```java
package com.mycompany.taller.controlador;

import com.mycompany.taller.dao.*;
import com.mycompany.taller.modelo.*;

public class RepuestoControlador extends CrudControlador<Repuesto> {
    public RepuestoControlador(){super(new RepuestoDao());}
    protected int obtenerId(Repuesto o){return o.getIdRepuesto();}
    protected void validar(Repuesto o){Validacion.texto(o.getNombre(),"Nombre",100);Validacion.opcional(o.getDescripcion(),"Descripción",16000);Validacion.monto(o.getPrecio());if(o.getExistencia()<0)throw new IllegalArgumentException("Existencia inválida.");}
    public boolean abastecer(int id,int cantidad){Validacion.id(id);if(cantidad<=0)throw new IllegalArgumentException("La cantidad debe ser positiva.");return ((RepuestoDao)dao).abastecer(id,cantidad);}
    public java.util.List<Repuesto> listarBajoInventario(int limite){if(limite<0)throw new IllegalArgumentException("Límite inválido.");return listarTodos().stream().filter(r->r.getExistencia()<=limite).toList();}
}
```

## src/main/java/com/mycompany/taller/controlador/ServicioControlador.java

**Estado:** Crear.

**Problema encontrado:** El controlador faltaba o estaba vacío y no validaba datos ni relaciones.

**Cambio realizado:** Completar CRUD mediante el DAO existente, validar campos y IDs y coordinar referencias sin duplicar SQL.

**Código completo final:**

```java
package com.mycompany.taller.controlador;

import com.mycompany.taller.dao.*;
import com.mycompany.taller.modelo.*;

public class ServicioControlador extends CrudControlador<Servicio> {
    public ServicioControlador(){super(new ServicioDao());}
    protected int obtenerId(Servicio o){return o.getIdServicio();}
    protected void validar(Servicio o){Validacion.texto(o.getNombreServicio(),"Nombre del servicio",100);Validacion.opcional(o.getDescripcion(),"Descripción",16000);Validacion.monto(o.getCostoManoObra());}
    
}
```

## src/main/java/com/mycompany/taller/controlador/Validacion.java

**Estado:** Crear.

**Problema encontrado:** No había validaciones compartidas para formularios/controladores.

**Cambio realizado:** Validar campos obligatorios, longitudes, IDs y montos.

**Código completo final:**

```java
package com.mycompany.taller.controlador;

public final class Validacion {
    private Validacion() { }
    public static void id(int id) { if(id<=0)throw new IllegalArgumentException("El ID debe ser positivo."); }
    public static void texto(String valor,String campo,int maximo) { if(valor==null || valor.isBlank() || valor.length()>maximo)throw new IllegalArgumentException(campo+" es obligatorio (máximo "+maximo+" caracteres)."); }
    public static void opcional(String valor,String campo,int maximo) { if(valor!=null && valor.length()>maximo)throw new IllegalArgumentException(campo+" supera "+maximo+" caracteres."); }
    public static void monto(double valor) { if(!Double.isFinite(valor) || valor<0 || valor>99999999.99)throw new IllegalArgumentException("El monto debe estar entre 0 y 99999999.99."); }
}
```

## src/main/java/com/mycompany/taller/controlador/VehiculoControlador.java

**Estado:** Corregir.

**Problema encontrado:** El controlador faltaba o estaba vacío y no validaba datos ni relaciones.

**Cambio realizado:** Completar CRUD mediante el DAO existente, validar campos y IDs y coordinar referencias sin duplicar SQL.

**Código completo final:**

```java
package com.mycompany.taller.controlador;

import com.mycompany.taller.dao.*;
import com.mycompany.taller.modelo.*;

public class VehiculoControlador extends CrudControlador<Vehiculo> {
    public VehiculoControlador(){super(new VehiculoDao());}
    protected int obtenerId(Vehiculo o){return o.getIdVehiculo();}
    protected void validar(Vehiculo o){Validacion.texto(o.getPlaca(),"Placa",20);Validacion.texto(o.getMarca(),"Marca",50);Validacion.texto(o.getModelo(),"Modelo",50);if(o.getAnio()<1886 || o.getAnio()>java.time.Year.now().getValue()+1)throw new IllegalArgumentException("Año inválido.");if(o.getClientePropietario()==null)throw new IllegalArgumentException("Seleccione un cliente.");Validacion.id(o.getClientePropietario().getId());Cliente cliente=new ClienteDao().buscarPorId(o.getClientePropietario().getId());if(cliente==null)throw new IllegalArgumentException("El cliente no existe.");o.setClientePropietario(cliente);}
    public java.util.List<OrdenTrabajo> historial(int id){Validacion.id(id);return new OrdenTrabajoDao().listarPorVehiculo(id);}
}
```

## src/main/java/com/mycompany/taller/controlador/WebControlador.java

**Estado:** Crear.

**Problema encontrado:** No había integración de formularios con las capas de negocio.

**Cambio realizado:** Traducir parámetros a modelos y coordinar los controladores sin SQL en presentación.

**Código completo final:**

```java
package com.mycompany.taller.controlador;

import com.mycompany.taller.modelo.*;
import java.util.*;

public class WebControlador {
    public record Pantalla(String[] campos,List<String[]> filas) { }
    public static final Map<String,String[]> CAMPOS = Map.ofEntries(
        Map.entry("clientes",new String[]{"id","nombres","apellidos","telefono","direccion","correo"}),
        Map.entry("vehiculos",new String[]{"id","placa","marca","modelo","anio","id_cliente","propietario"}),
        Map.entry("mecanicos",new String[]{"id","nombres","apellidos","telefono","especialidad"}),
        Map.entry("repuestos",new String[]{"id","nombre","descripcion","precio","existencia"}),
        Map.entry("servicios",new String[]{"id","nombre_servicio","descripcion","costo_mano_obra"}),
        Map.entry("ordenes",new String[]{"id","id_vehiculo","id_mecanico","descripcion_problema","estado","fecha_ingreso","costo_total"}),
        Map.entry("detalle_servicios",new String[]{"id","id_orden","id_servicio","costo_aplicado"}),
        Map.entry("detalle_repuestos",new String[]{"id","id_orden","id_repuesto","cantidad","precio_unitario"}));
    private static String[] fila(Object... valores){return Arrays.stream(valores).map(v->v==null?"":v.toString()).toArray(String[]::new);}
    public Pantalla consultar(String modulo){
        List<String[]> filas=new ArrayList<>();
        switch(modulo){
            case "clientes" -> new ClienteControlador().listarTodos().forEach(c->filas.add(fila(c.getId(),c.getNombres(),c.getApellidos(),c.getTelefono(),c.getDireccion(),c.getCorreo())));
            case "vehiculos" -> new VehiculoControlador().listarTodos().forEach(v->filas.add(fila(v.getIdVehiculo(),v.getPlaca(),v.getMarca(),v.getModelo(),v.getAnio(),v.getClientePropietario().getId(),v.getClientePropietario().getNombres()+" "+v.getClientePropietario().getApellidos())));
            case "mecanicos" -> new MecanicoControlador().listarTodos().forEach(m->filas.add(fila(m.getId(),m.getNombres(),m.getApellidos(),m.getTelefono(),m.getEspecialidad())));
            case "repuestos" -> new RepuestoControlador().listarTodos().forEach(r->filas.add(fila(r.getIdRepuesto(),r.getNombre(),r.getDescripcion(),r.getPrecio(),r.getExistencia())));
            case "servicios" -> new ServicioControlador().listarTodos().forEach(s->filas.add(fila(s.getIdServicio(),s.getNombreServicio(),s.getDescripcion(),s.getCostoManoObra())));
            case "ordenes" -> new OrdenTrabajoControlador().listarTodos().forEach(o->filas.add(fila(o.getIdOrden(),o.getVehiculo().getIdVehiculo(),o.getMecanico().getId(),o.getdescripcionProblema(),o.getEstado(),o.getFechaIngreso(),o.getCostoTotal())));
            case "detalle_servicios" -> new DetalleOrdenServicioControlador().listarTodos().forEach(d->filas.add(fila(d.getIdDetalleServicio(),d.getIdOrden(),d.getServicio().getIdServicio(),d.getCostoAplicado())));
            case "detalle_repuestos" -> new DetalleOrdenRepuestoControlador().listarTodos().forEach(d->filas.add(fila(d.getIdDetalleRepuesto(),d.getIdOrden(),d.getRepuesto().getIdRepuesto(),d.getCantidad(),d.getPrecioUnitario())));
            default -> throw new IllegalArgumentException("Módulo desconocido.");
        }
        return new Pantalla(CAMPOS.get(modulo),filas);
    }
    public void operar(Map<String,String> p){
        String modulo=p.getOrDefault("modulo","");String accion=p.getOrDefault("accion","guardar");
        if(!CAMPOS.containsKey(modulo))throw new IllegalArgumentException("Módulo desconocido.");
        int id=entero(p,"id",0);
        if(accion.equals("eliminar")){
            CrudControlador<?> c=switch(modulo){case "clientes"->new ClienteControlador();case "vehiculos"->new VehiculoControlador();case "mecanicos"->new MecanicoControlador();case "repuestos"->new RepuestoControlador();case "servicios"->new ServicioControlador();case "ordenes"->new OrdenTrabajoControlador();case "detalle_servicios"->new DetalleOrdenServicioControlador();case "detalle_repuestos"->new DetalleOrdenRepuestoControlador();default->throw new IllegalArgumentException("Módulo inválido.");};
            comprobar(c.eliminar(id));return;
        }
        if(accion.equals("abastecer") && modulo.equals("repuestos")){comprobar(new RepuestoControlador().abastecer(id,entero(p,"cantidad",0)));return;}
        if(accion.equals("estado") && modulo.equals("ordenes")){comprobar(new OrdenTrabajoControlador().cambiarEstado(id,texto(p,"estado")));return;}
        if(!accion.equals("guardar"))throw new IllegalArgumentException("Acción desconocida.");
        switch(modulo){
            case "clientes" -> guardar(new ClienteControlador(),new Cliente(id,texto(p,"nombres"),texto(p,"apellidos"),texto(p,"telefono"),texto(p,"direccion"),texto(p,"correo")),id);
            case "mecanicos" -> guardar(new MecanicoControlador(),new Mecanico(id,texto(p,"nombres"),texto(p,"apellidos"),texto(p,"telefono"),texto(p,"especialidad")),id);
            case "vehiculos" -> {Cliente cliente=new Cliente();cliente.setId(entero(p,"id_cliente",0));guardar(new VehiculoControlador(),new Vehiculo(id,texto(p,"placa"),texto(p,"marca"),texto(p,"modelo"),entero(p,"anio",0),cliente),id);}
            case "servicios" -> guardar(new ServicioControlador(),new Servicio(id,texto(p,"nombre_servicio"),texto(p,"descripcion"),decimal(p,"costo_mano_obra")),id);
            case "repuestos" -> guardar(new RepuestoControlador(),new Repuesto(id,texto(p,"nombre"),texto(p,"descripcion"),decimal(p,"precio"),entero(p,"existencia",0)),id);
            case "ordenes" -> {OrdenTrabajo o=id==0?new OrdenTrabajo():new OrdenTrabajoControlador().buscarPorId(id);if(o==null)throw new IllegalArgumentException("La orden no existe.");Vehiculo v=new Vehiculo();v.setIdVehiculo(entero(p,"id_vehiculo",0));Mecanico m=new Mecanico();m.setId(entero(p,"id_mecanico",0));o.setVehiculo(v);o.setMecanico(m);o.setdescripcionProblema(texto(p,"descripcion_problema"));o.setEstado(p.getOrDefault("estado","PENDIENTE"));guardar(new OrdenTrabajoControlador(),o,id);}
            case "detalle_servicios" -> {Servicio s=new Servicio();s.setIdServicio(entero(p,"id_servicio",0));guardar(new DetalleOrdenServicioControlador(),new DetalleOrdenServicio(id,entero(p,"id_orden",0),s,decimal(p,"costo_aplicado")),id);}
            case "detalle_repuestos" -> {Repuesto r=new Repuesto();r.setIdRepuesto(entero(p,"id_repuesto",0));guardar(new DetalleOrdenRepuestoControlador(),new DetalleOrdenRepuesto(id,entero(p,"id_orden",0),r,entero(p,"cantidad",0),decimal(p,"precio_unitario")),id);}
            default -> throw new IllegalArgumentException("Módulo inválido.");
        }
    }
    private static <T> void guardar(CrudControlador<T> c,T objeto,int id){comprobar(id==0?c.insertar(objeto):c.actualizar(objeto));}
    private static void comprobar(boolean ok){if(!ok)throw new IllegalArgumentException("No se modificaron registros. Compruebe el ID y las relaciones.");}
    public static String texto(Map<String,String> p,String campo){return p.getOrDefault(campo,"").trim();}
    public static int entero(Map<String,String> p,String campo,int defecto){String v=texto(p,campo);if(v.isEmpty())return defecto;try{return Integer.parseInt(v);}catch(NumberFormatException e){throw new IllegalArgumentException(campo+" debe ser un entero.");}}
    public static double decimal(Map<String,String> p,String campo){try{double n=Double.parseDouble(texto(p,campo));Validacion.monto(n);return n;}catch(NumberFormatException e){throw new IllegalArgumentException(campo+" debe ser un número.");}}
}
```

## src/main/java/com/mycompany/taller/dao/ClienteDao.java

**Estado:** Corregir.

**Problema encontrado:** El insert no devolvía el ID generado y las excepciones se ocultaban.

**Cambio realizado:** Conservar sus consultas y CRUD, añadir claves generadas, contrato CrudDao y propagación de errores.

**Código completo final:**

```java
package com.mycompany.taller.dao;

import com.mycompany.taller.config.Conexiontaller;
import com.mycompany.taller.modelo.Cliente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteDao implements CrudDao<Cliente> {

    public boolean insertar(Cliente cliente) {
        String sql = "INSERT INTO clientes (nombres, apellidos, telefono, direccion, correo) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cliente.getNombres());
            ps.setString(2, cliente.getApellidos());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getDireccion());
            ps.setString(5, cliente.getCorreo());

            int filasInsertadas = ps.executeUpdate();
            if (filasInsertadas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) cliente.setId(keys.getInt(1));
                }
            }
            return filasInsertadas > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error al insertar cliente: " + e.getMessage(), e);
        }
    }

    public Cliente buscarPorId(int id) {
        String sql = "SELECT * FROM clientes WHERE id_cliente = ?";
        Cliente cliente = null;

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cliente = new Cliente(
                        rs.getInt("id_cliente"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        rs.getString("direccion"),
                        rs.getString("correo")
                    );
                }
            }

            System.out.println("--- RESULTADO DE BUSQUEDA POR ID ---");

            if (cliente != null) {
                System.out.println("Cliente encontrado con exito");
                System.out.println("ID: " + cliente.getId());
                System.out.println("Nombre Completo: "
                        + cliente.getNombres() + " "
                        + cliente.getApellidos());
                System.out.println("Telefono: " + cliente.getTelefono());
                System.out.println("Direccion: " + cliente.getDireccion());
                System.out.println("Correo: " + cliente.getCorreo());
            } else {
                System.out.println("No se encontro ningun cliente con el ID " + id);
            }

            System.out.println("------------------------------------");

        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar cliente por ID: " + e.getMessage(), e);
        }

        return cliente;
    }

    public boolean actualizar(Cliente cliente) {
        String sql = "UPDATE clientes SET nombres = ?, apellidos = ?, telefono = ?, direccion = ?, correo = ? WHERE id_cliente = ?";

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, cliente.getNombres());
            ps.setString(2, cliente.getApellidos());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getDireccion());
            ps.setString(5, cliente.getCorreo());
            ps.setInt(6, cliente.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error al actualizar cliente: " + e.getMessage(), e);
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM clientes WHERE id_cliente = ?";

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error al eliminar cliente: " + e.getMessage(), e);
        }
    }

    public List<Cliente> listarTodos() {
        String sql = "SELECT * FROM clientes";
        List<Cliente> listaClientes = new ArrayList<>();

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Cliente cliente = new Cliente(
                    rs.getInt("id_cliente"),
                    rs.getString("nombres"),
                    rs.getString("apellidos"),
                    rs.getString("telefono"),
                    rs.getString("direccion"),
                    rs.getString("correo")
                );

                listaClientes.add(cliente);
            }

            System.out.println("--- LISTADO DE CLIENTES ---");

            if (listaClientes.isEmpty()) {
                System.out.println("No hay clientes registrados en la base de datos.");
            } else {
                System.out.println("Se encontraron "
                        + listaClientes.size()
                        + " cliente(s)");

                for (Cliente c : listaClientes) {
                    System.out.println("ID: " + c.getId());
                    System.out.println("Nombre Completo: "
                            + c.getNombres() + " "
                            + c.getApellidos());
                    System.out.println("Telefono: " + c.getTelefono());
                    System.out.println("Direccion: " + c.getDireccion());
                    System.out.println("Correo: " + c.getCorreo());
                    System.out.println("-----------------------------------");
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Error al listar clientes: " + e.getMessage(), e);
        }

        return listaClientes;
    }
}
```

## src/main/java/com/mycompany/taller/dao/CrudDao.java

**Estado: Correcto. No requiere cambios.** Se conserva el archivo recibido.

## src/main/java/com/mycompany/taller/dao/DetalleOrdenRepuestoDao.java

**Estado:** Corregir.

**Problema encontrado:** El archivo estaba vacío.

**Cambio realizado:** CRUD transaccional, validación/descuento/devolución de existencias y recálculo de total.

**Código completo final:**

```java
package com.mycompany.taller.dao;

import com.mycompany.taller.config.Conexiontaller;
import com.mycompany.taller.modelo.*;
import java.sql.*;
import java.util.*;

public class DetalleOrdenRepuestoDao implements CrudDao<DetalleOrdenRepuesto> {
    public List<DetalleOrdenRepuesto> listarTodos(){return consultar(null);}
    public List<DetalleOrdenRepuesto> listarPorOrden(int orden){return consultar(orden);}
    private List<DetalleOrdenRepuesto> consultar(Integer orden){
        List<DetalleOrdenRepuesto> lista=new ArrayList<>();String sql="SELECT d.*,c.* FROM detalle_orden_repuestos d JOIN repuestos c ON c.id_repuesto=d.id_repuesto"+(orden==null?"":" WHERE d.id_orden=?")+" ORDER BY d.id_detalle_repuesto";
        try(Connection con=Conexiontaller.IniciarConexion();PreparedStatement ps=con.prepareStatement(sql)){
            if(orden!=null)ps.setInt(1,orden);try(ResultSet rs=ps.executeQuery()){while(rs.next())lista.add(new DetalleOrdenRepuesto(rs.getInt("id_detalle_repuesto"),rs.getInt("id_orden"),new Repuesto(rs.getInt("id_repuesto"),rs.getString("nombre"),rs.getString("descripcion"),rs.getDouble("precio"),rs.getInt("existencia")),rs.getInt("cantidad"),rs.getDouble("precio_unitario")));}return lista;
        }catch(SQLException e){throw new IllegalStateException("No se pudieron consultar los detalles.",e);}
    }
    public DetalleOrdenRepuesto buscarPorId(int id){
        String sql="SELECT d.*,c.* FROM detalle_orden_repuestos d JOIN repuestos c ON c.id_repuesto=d.id_repuesto WHERE d.id_detalle_repuesto=?";
        try(Connection con=Conexiontaller.IniciarConexion();PreparedStatement ps=con.prepareStatement(sql)){ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()?new DetalleOrdenRepuesto(rs.getInt("id_detalle_repuesto"),rs.getInt("id_orden"),new Repuesto(rs.getInt("id_repuesto"),rs.getString("nombre"),rs.getString("descripcion"),rs.getDouble("precio"),rs.getInt("existencia")),rs.getInt("cantidad"),rs.getDouble("precio_unitario")):null;}}
        catch(SQLException e){throw new IllegalStateException("No se pudo buscar el detalle.",e);}
    }
    public boolean insertar(DetalleOrdenRepuesto d){return guardar(d,false);}
    public boolean actualizar(DetalleOrdenRepuesto d){return guardar(d,true);}
    private boolean guardar(DetalleOrdenRepuesto d,boolean actualizar){
        if(d==null || d.getRepuesto()==null || d.getIdOrden()<=0)throw new IllegalArgumentException("Detalle incompleto.");
        if(d.getCantidad()<=0)throw new IllegalArgumentException("Cantidad inválida.");
        return TransaccionOrden.ejecutar(d.getIdOrden(),true,con->{
            int anterior=0;int idAnterior=0;
            if(actualizar){
                try(PreparedStatement ps=con.prepareStatement("SELECT * FROM detalle_orden_repuestos WHERE id_detalle_repuesto=? AND id_orden=? FOR UPDATE")){
                    ps.setInt(1,d.getIdDetalleRepuesto());ps.setInt(2,d.getIdOrden());try(ResultSet rs=ps.executeQuery()){
                        if(!rs.next())throw new IllegalArgumentException("El detalle no pertenece a la orden.");
                        anterior=rs.getInt("cantidad");idAnterior=rs.getInt("id_repuesto");if(idAnterior!=d.getRepuesto().getIdRepuesto())throw new IllegalArgumentException("Retire el detalle antes de cambiar de repuesto.");
                    }
                }
            }
            int cambio=d.getCantidad()-anterior;
            try(PreparedStatement ps=con.prepareStatement("UPDATE repuestos SET existencia=existencia-? WHERE id_repuesto=? AND existencia>=?")){
                ps.setInt(1,cambio);ps.setInt(2,d.getRepuesto().getIdRepuesto());ps.setInt(3,Math.max(cambio,0));
                if(ps.executeUpdate()==0)throw new IllegalArgumentException("Repuesto inexistente o inventario insuficiente.");
            }
            String sql=actualizar?"UPDATE detalle_orden_repuestos SET id_repuesto=?,cantidad=?,precio_unitario=?,subtotal=? WHERE id_detalle_repuesto=? AND id_orden=?":"INSERT INTO detalle_orden_repuestos(id_repuesto,cantidad,precio_unitario,subtotal,id_orden) VALUES(?,?,?,?,?)";
            try(PreparedStatement ps=con.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
                ps.setInt(1,d.getRepuesto().getIdRepuesto());
                ps.setInt(2,d.getCantidad());ps.setBigDecimal(3,java.math.BigDecimal.valueOf(d.getPrecioUnitario()).setScale(2,java.math.RoundingMode.HALF_UP));ps.setBigDecimal(4,java.math.BigDecimal.valueOf(d.getPrecioUnitario()).setScale(2,java.math.RoundingMode.HALF_UP).multiply(java.math.BigDecimal.valueOf(d.getCantidad())));
                if(actualizar){ps.setInt(5,d.getIdDetalleRepuesto());ps.setInt(6,d.getIdOrden());}else ps.setInt(5,d.getIdOrden());
                ps.executeUpdate();if(!actualizar){try(ResultSet rs=ps.getGeneratedKeys()){if(rs.next())d.setIdDetalleRepuesto(rs.getInt(1));}}
            }
        });
    }
    public boolean eliminar(int id){
        DetalleOrdenRepuesto d=buscarPorId(id);if(d==null)return false;
        return TransaccionOrden.ejecutar(d.getIdOrden(),true,con->{
            int cantidad;
            try(PreparedStatement ps=con.prepareStatement("SELECT cantidad FROM detalle_orden_repuestos WHERE id_detalle_repuesto=? FOR UPDATE")){ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("El detalle ya fue retirado.");cantidad=rs.getInt(1);}}
            try(PreparedStatement ps=con.prepareStatement("UPDATE repuestos SET existencia=existencia+? WHERE id_repuesto=?")){ps.setInt(1,cantidad);ps.setInt(2,d.getRepuesto().getIdRepuesto());ps.executeUpdate();}
            try(PreparedStatement ps=con.prepareStatement("DELETE FROM detalle_orden_repuestos WHERE id_detalle_repuesto=?")){ps.setInt(1,id);ps.executeUpdate();}
        });
    }
}
```

## src/main/java/com/mycompany/taller/dao/DetalleOrdenServicioDao.java

**Estado:** Corregir.

**Problema encontrado:** El archivo estaba vacío.

**Cambio realizado:** CRUD de detalles con PreparedStatement y cambios solo dentro de órdenes EN PROCESO.

**Código completo final:**

```java
package com.mycompany.taller.dao;

import com.mycompany.taller.config.Conexiontaller;
import com.mycompany.taller.modelo.*;
import java.sql.*;
import java.util.*;

public class DetalleOrdenServicioDao implements CrudDao<DetalleOrdenServicio> {
    public List<DetalleOrdenServicio> listarTodos(){return consultar(null);}
    public List<DetalleOrdenServicio> listarPorOrden(int orden){return consultar(orden);}
    private List<DetalleOrdenServicio> consultar(Integer orden){
        List<DetalleOrdenServicio> lista=new ArrayList<>();String sql="SELECT d.*,c.* FROM detalle_orden_servicios d JOIN servicios c ON c.id_servicio=d.id_servicio"+(orden==null?"":" WHERE d.id_orden=?")+" ORDER BY d.id_detalle_servicio";
        try(Connection con=Conexiontaller.IniciarConexion();PreparedStatement ps=con.prepareStatement(sql)){
            if(orden!=null)ps.setInt(1,orden);try(ResultSet rs=ps.executeQuery()){while(rs.next())lista.add(new DetalleOrdenServicio(rs.getInt("id_detalle_servicio"),rs.getInt("id_orden"),new Servicio(rs.getInt("id_servicio"),rs.getString("nombre_servicio"),rs.getString("descripcion"),rs.getDouble("costo_mano_obra")),rs.getDouble("costo_aplicado")));}return lista;
        }catch(SQLException e){throw new IllegalStateException("No se pudieron consultar los detalles.",e);}
    }
    public DetalleOrdenServicio buscarPorId(int id){
        String sql="SELECT d.*,c.* FROM detalle_orden_servicios d JOIN servicios c ON c.id_servicio=d.id_servicio WHERE d.id_detalle_servicio=?";
        try(Connection con=Conexiontaller.IniciarConexion();PreparedStatement ps=con.prepareStatement(sql)){ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()?new DetalleOrdenServicio(rs.getInt("id_detalle_servicio"),rs.getInt("id_orden"),new Servicio(rs.getInt("id_servicio"),rs.getString("nombre_servicio"),rs.getString("descripcion"),rs.getDouble("costo_mano_obra")),rs.getDouble("costo_aplicado")):null;}}
        catch(SQLException e){throw new IllegalStateException("No se pudo buscar el detalle.",e);}
    }
    public boolean insertar(DetalleOrdenServicio d){return guardar(d,false);}
    public boolean actualizar(DetalleOrdenServicio d){return guardar(d,true);}
    private boolean guardar(DetalleOrdenServicio d,boolean actualizar){
        if(d==null || d.getServicio()==null || d.getIdOrden()<=0)throw new IllegalArgumentException("Detalle incompleto.");

        return TransaccionOrden.ejecutar(d.getIdOrden(),true,con->{

            if(actualizar){
                try(PreparedStatement ps=con.prepareStatement("SELECT * FROM detalle_orden_servicios WHERE id_detalle_servicio=? AND id_orden=? FOR UPDATE")){
                    ps.setInt(1,d.getIdDetalleServicio());ps.setInt(2,d.getIdOrden());try(ResultSet rs=ps.executeQuery()){
                        if(!rs.next())throw new IllegalArgumentException("El detalle no pertenece a la orden.");

                    }
                }
            }

            String sql=actualizar?"UPDATE detalle_orden_servicios SET id_servicio=?,costo_aplicado=? WHERE id_detalle_servicio=? AND id_orden=?":"INSERT INTO detalle_orden_servicios(id_servicio,costo_aplicado,id_orden) VALUES(?,?,?)";
            try(PreparedStatement ps=con.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
                ps.setInt(1,d.getServicio().getIdServicio());
                ps.setBigDecimal(2,java.math.BigDecimal.valueOf(d.getCostoAplicado()).setScale(2,java.math.RoundingMode.HALF_UP));
                if(actualizar){ps.setInt(3,d.getIdDetalleServicio());ps.setInt(4,d.getIdOrden());}else ps.setInt(3,d.getIdOrden());
                ps.executeUpdate();if(!actualizar){try(ResultSet rs=ps.getGeneratedKeys()){if(rs.next())d.setIdDetalleServicio(rs.getInt(1));}}
            }
        });
    }
    public boolean eliminar(int id){
        DetalleOrdenServicio d=buscarPorId(id);if(d==null)return false;
        return TransaccionOrden.ejecutar(d.getIdOrden(),true,con->{

            try(PreparedStatement ps=con.prepareStatement("DELETE FROM detalle_orden_servicios WHERE id_detalle_servicio=?")){ps.setInt(1,id);ps.executeUpdate();}
        });
    }
}
```

## src/main/java/com/mycompany/taller/dao/MecanicoDao.java

**Estado:** Corregir.

**Problema encontrado:** El ID generado no se asignaba y las excepciones se ocultaban.

**Cambio realizado:** Conservar el CRUD y recuperar el ID; informar fallos SQL.

**Código completo final:**

```java
package com.mycompany.taller.dao;

import com.mycompany.taller.config.Conexiontaller;
import com.mycompany.taller.modelo.Mecanico;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MecanicoDao implements CrudDao<Mecanico> {

    public boolean insertar(Mecanico mecanico) {
        String sql = "INSERT INTO mecanicos (nombres, apellidos, telefono, especialidad) VALUES (?, ?, ?, ?)";

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, mecanico.getNombres());
            ps.setString(2, mecanico.getApellidos());
            ps.setString(3, mecanico.getTelefono());
            ps.setString(4, mecanico.getEspecialidad());

            int filasInsertadas = ps.executeUpdate();
            if (filasInsertadas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) mecanico.setId(keys.getInt(1));
                }
            }
            return filasInsertadas > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error al insertar mecanico: " + e.getMessage(), e);
        }
    }

    public Mecanico buscarPorId(int id) {
        String sql = "SELECT * FROM mecanicos WHERE id_mecanico = ?";
        Mecanico mecanico = null;

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    mecanico = new Mecanico(
                        rs.getInt("id_mecanico"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        rs.getString("especialidad")
                    );
                }
            }

            System.out.println("--- RESULTADO DE BUSQUEDA POR ID ---");

            if (mecanico != null) {
                System.out.println("Mecanico encontrado con exito");
                System.out.println("ID: " + mecanico.getId());
                System.out.println("Nombre Completo: "
                        + mecanico.getNombres() + " "
                        + mecanico.getApellidos());
                System.out.println("Telefono: " + mecanico.getTelefono());
                System.out.println("Especialidad: " + mecanico.getEspecialidad());
            } else {
                System.out.println("No se encontro ningun mecanico con el ID " + id);
            }

            System.out.println("------------------------------------");

        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar mecanico por ID: " + e.getMessage(), e);
        }

        return mecanico;
    }

    public boolean actualizar(Mecanico mecanico) {
        String sql = "UPDATE mecanicos SET nombres = ?, apellidos = ?, telefono = ?, especialidad = ? WHERE id_mecanico = ?";

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, mecanico.getNombres());
            ps.setString(2, mecanico.getApellidos());
            ps.setString(3, mecanico.getTelefono());
            ps.setString(4, mecanico.getEspecialidad());
            ps.setInt(5, mecanico.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error al actualizar mecanico: " + e.getMessage(), e);
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM mecanicos WHERE id_mecanico = ?";

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error al eliminar mecanico: " + e.getMessage(), e);
        }
    }

    public List<Mecanico> listarTodos() {
        String sql = "SELECT * FROM mecanicos";
        List<Mecanico> listaMecanicos = new ArrayList<>();

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Mecanico mecanico = new Mecanico(
                    rs.getInt("id_mecanico"),
                    rs.getString("nombres"),
                    rs.getString("apellidos"),
                    rs.getString("telefono"),
                    rs.getString("especialidad")
                );

                listaMecanicos.add(mecanico);
            }

            System.out.println("--- LISTADO DE MECANICOS ---");

            if (listaMecanicos.isEmpty()) {
                System.out.println("No hay mecanicos registrados en la base de datos.");
            } else {
                System.out.println("Se encontraron "
                        + listaMecanicos.size()
                        + " mecanico(s)");

                for (Mecanico m : listaMecanicos) {
                    System.out.println("ID: " + m.getId());
                    System.out.println("Nombre Completo: "
                            + m.getNombres() + " "
                            + m.getApellidos());
                    System.out.println("Telefono: " + m.getTelefono());
                    System.out.println("Especialidad: " + m.getEspecialidad());
                    System.out.println("-----------------------------------");
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Error al listar mecanicos: " + e.getMessage(), e);
        }

        return listaMecanicos;
    }
}
```

## src/main/java/com/mycompany/taller/dao/OrdenTrabajoDao.java

**Estado:** Corregir.

**Problema encontrado:** Fecha nula/truncada, total no recuperado, relaciones incompletas y falta de bloqueo por estado.

**Cambio realizado:** Usar timestamp, reconstruir propietario/mecánico/detalles/total, validar transiciones bajo bloqueo y devolver stock al cancelar.

**Código completo final:**

```java
package com.mycompany.taller.dao;

import com.mycompany.taller.config.Conexiontaller;
import com.mycompany.taller.modelo.*;
import java.sql.*;
import java.util.*;

public class OrdenTrabajoDao implements CrudDao<OrdenTrabajo> {
    private static final String CONSULTA="SELECT o.*,v.id_cliente,v.placa,v.marca,v.modelo,v.anio,c.nombres AS cn,c.apellidos AS ca,c.telefono AS ct,c.direccion,c.correo,m.nombres AS mn,m.apellidos AS ma,m.telefono AS mt,m.especialidad FROM ordenes_trabajo o JOIN vehiculos v ON v.id_vehiculo=o.id_vehiculo JOIN clientes c ON c.id_cliente=v.id_cliente JOIN mecanicos m ON m.id_mecanico=o.id_mecanico";
    public boolean insertar(OrdenTrabajo o) {
        if (o==null || o.getVehiculo()==null || o.getMecanico()==null) throw new IllegalArgumentException("Seleccione vehículo y mecánico.");
        if (!"PENDIENTE".equals(o.getEstado())) throw new IllegalArgumentException("Una orden nueva debe estar PENDIENTE.");
        String sql="INSERT INTO ordenes_trabajo(id_vehiculo,id_mecanico,fecha_ingreso,estado,descripcion_problema,costo_total) VALUES(?,?,?,'PENDIENTE',?,0)";
        try(Connection con=Conexiontaller.IniciarConexion();PreparedStatement ps=con.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,o.getVehiculo().getIdVehiculo());ps.setInt(2,o.getMecanico().getId());
            if(o.getFechaIngreso()==null)o.setFechaIngreso(new java.util.Date());
            ps.setTimestamp(3,new Timestamp(o.getFechaIngreso().getTime()));ps.setString(4,o.getdescripcionProblema());
            if(ps.executeUpdate()==0)return false;
            try(ResultSet rs=ps.getGeneratedKeys()){if(rs.next())o.setIdOrden(rs.getInt(1));}o.setCostoTotal(0);return true;
        }catch(SQLException e){throw new IllegalStateException("No se pudo crear la orden: "+e.getMessage(),e);}
    }
    private OrdenTrabajo mapear(ResultSet rs)throws SQLException {
        Cliente c=new Cliente(rs.getInt("id_cliente"),rs.getString("cn"),rs.getString("ca"),rs.getString("ct"),rs.getString("direccion"),rs.getString("correo"));
        Vehiculo v=new Vehiculo(rs.getInt("id_vehiculo"),rs.getString("placa"),rs.getString("marca"),rs.getString("modelo"),rs.getInt("anio"),c);
        Mecanico m=new Mecanico(rs.getInt("id_mecanico"),rs.getString("mn"),rs.getString("ma"),rs.getString("mt"),rs.getString("especialidad"));
        OrdenTrabajo o=new OrdenTrabajo();o.setIdOrden(rs.getInt("id_orden"));o.setVehiculo(v);o.setMecanico(m);o.setFechaIngreso(rs.getTimestamp("fecha_ingreso"));o.setdescripcionProblema(rs.getString("descripcion_problema"));o.setCostoTotal(rs.getDouble("costo_total"));
        o.getListaServicios().addAll(new DetalleOrdenServicioDao().listarPorOrden(o.getIdOrden()));
        o.getListaRepuestos().addAll(new DetalleOrdenRepuestoDao().listarPorOrden(o.getIdOrden()));
        o.setEstado(rs.getString("estado"));return o;
    }
    public OrdenTrabajo buscarPorId(int id){List<OrdenTrabajo> lista=consultar(" WHERE o.id_orden=?",id);return lista.isEmpty()?null:lista.get(0);}
    public List<OrdenTrabajo> listarTodos(){return consultar(" ORDER BY o.id_orden",null);}
    public List<OrdenTrabajo> listarPorVehiculo(int id){return consultar(" WHERE o.id_vehiculo=? ORDER BY o.fecha_ingreso DESC",id);}
    private List<OrdenTrabajo> consultar(String filtro,Integer id){
        List<OrdenTrabajo> lista=new ArrayList<>();
        try(Connection con=Conexiontaller.IniciarConexion();PreparedStatement ps=con.prepareStatement(CONSULTA+filtro)){
            if(id!=null)ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){while(rs.next())lista.add(mapear(rs));}return lista;
        }catch(SQLException e){throw new IllegalStateException("No se pudieron consultar las órdenes: "+e.getMessage(),e);}
    }
    public boolean actualizar(OrdenTrabajo o){
        return TransaccionOrden.ejecutar(o.getIdOrden(),false,con->{
            String anterior=TransaccionOrden.bloquear(con,o.getIdOrden());String nuevo=o.getEstado();
            if (!(nuevo.equals(anterior) || (anterior.equals("PENDIENTE") && nuevo.equals("EN PROCESO")) || (anterior.equals("EN PROCESO") && nuevo.equals("FINALIZADA")) || nuevo.equals("CANCELADA"))) throw new IllegalArgumentException("Transición de estado no permitida.");
            if(nuevo.equals("FINALIZADA")){
                try(PreparedStatement ps=con.prepareStatement("SELECT COUNT(*) FROM detalle_orden_servicios WHERE id_orden=?")){ps.setInt(1,o.getIdOrden());try(ResultSet rs=ps.executeQuery()){rs.next();if(rs.getInt(1)==0)throw new IllegalStateException("No se puede finalizar sin servicios.");}}
            }
            if(nuevo.equals("CANCELADA"))TransaccionOrden.devolverRepuestos(con,o.getIdOrden());
            String sql="UPDATE ordenes_trabajo SET id_vehiculo=?,id_mecanico=?,fecha_ingreso=?,estado=?,descripcion_problema=?,fecha_entrega=CASE WHEN ?='FINALIZADA' THEN CURRENT_TIMESTAMP ELSE NULL END WHERE id_orden=?";
            try(PreparedStatement ps=con.prepareStatement(sql)){ps.setInt(1,o.getVehiculo().getIdVehiculo());ps.setInt(2,o.getMecanico().getId());ps.setTimestamp(3,new Timestamp(o.getFechaIngreso().getTime()));ps.setString(4,nuevo);ps.setString(5,o.getdescripcionProblema());ps.setString(6,nuevo);ps.setInt(7,o.getIdOrden());ps.executeUpdate();}
        });
    }
    public boolean eliminar(int id){
        try(Connection con=Conexiontaller.IniciarConexion();PreparedStatement ps=con.prepareStatement("DELETE FROM ordenes_trabajo WHERE id_orden=? AND estado='PENDIENTE'")){ps.setInt(1,id);return ps.executeUpdate()>0;}
        catch(SQLException e){throw new IllegalStateException("Solo se pueden eliminar órdenes pendientes sin detalles.",e);}
    }
}
```

## src/main/java/com/mycompany/taller/dao/RepuestoDao.java

**Estado:** Corregir.

**Problema encontrado:** Actualizar/eliminar podían retornar éxito sin filas; faltaba abastecimiento atómico.

**Cambio realizado:** Comprobar executeUpdate, recuperar ID y añadir abastecimiento con suma protegida contra desbordamiento.

**Código completo final:**

```java
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */package com.mycompany.taller.dao;


import com.mycompany.taller.config.Conexiontaller; //
import com.mycompany.taller.modelo.Repuesto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RepuestoDao implements CrudDao<Repuesto> {
    public boolean abastecer(int id,int cantidad) {
        if(cantidad<=0)throw new IllegalArgumentException("Cantidad inválida.");
        try(Connection con=Conexiontaller.IniciarConexion();PreparedStatement ps=con.prepareStatement("UPDATE repuestos SET existencia=existencia+? WHERE id_repuesto=? AND existencia<=2147483647-?")){
            ps.setInt(1,cantidad);ps.setInt(2,id);ps.setInt(3,cantidad);return ps.executeUpdate()>0;
        }catch(SQLException e){throw new IllegalStateException("No se pudo abastecer el repuesto.",e);}
    }


    @Override
   public boolean insertar(Repuesto repuesto) {
        String sql = "INSERT INTO repuestos (nombre, descripcion, precio, existencia) VALUES (?, ?, ?, ?)";
        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, repuesto.getNombre());
            ps.setString(2, repuesto.getDescripcion());
            ps.setDouble(3, repuesto.getPrecio());
            ps.setInt(4, repuesto.getExistencia());

            int filasInsertadas = ps.executeUpdate();
            if (filasInsertadas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) repuesto.setIdRepuesto(keys.getInt(1));
                }
            }
            int filasAfectadas = filasInsertadas; // Verificamos si afectó filas
            System.out.println("Filas insertadas en BD: " + filasAfectadas);
            return filasAfectadas > 0;

        } catch (SQLException e) {
            if(e.getSQLState()!=null && e.getSQLState().startsWith("23")) throw new IllegalArgumentException("Registro duplicado o relacionado con otros datos: " + e.getMessage(),e);
            throw new IllegalStateException("No se pudo completar la operaci?n de Repuesto: " + e.getMessage(),e);
        }
    }

    @Override
    public boolean actualizar(Repuesto repuesto) {
        String sql = "UPDATE repuestos SET nombre = ?, descripcion = ?, precio = ?, existencia = ? WHERE id_repuesto = ?";
        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, repuesto.getNombre());
            ps.setString(2, repuesto.getDescripcion());
            ps.setDouble(3, repuesto.getPrecio());
            ps.setInt(4, repuesto.getExistencia());
            ps.setInt(5, repuesto.getIdRepuesto());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al actualizar repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(int id) {
        String sql = "DELETE FROM repuestos WHERE id_repuesto = ?";
        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al eliminar repuesto: " + e.getMessage(), e);
        }
    }

    @Override
    public Repuesto buscarPorId(int id) {
        String sql = "SELECT * FROM repuestos WHERE id_repuesto = ?";
        Repuesto repuesto = null;
        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    repuesto = new Repuesto(
                        rs.getInt("id_repuesto"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getDouble("precio"),
                        rs.getInt("existencia")
                    );
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar repuesto por ID: " + e.getMessage(), e);
        }
        return repuesto;
    }

    @Override
public List<Repuesto> listarTodos() {
    String sql = "SELECT * FROM repuestos";
    List<Repuesto> listaRepuestos = new ArrayList<>();

    try (Connection con = Conexiontaller.IniciarConexion();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            Repuesto repuesto = new Repuesto(
                rs.getInt("id_repuesto"), // variable real en la tabla repuestos
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getDouble("precio"),
                rs.getInt("existencia")
            );
            listaRepuestos.add(repuesto);
        }


        System.out.println("--- LISTANDO DE REPUESTOS ---");
        if (listaRepuestos.isEmpty()) {
            System.out.println(">>> No hay repuestos registrados en la base de datos. <<<");
        } else {
            System.out.println(">>> Se encontraron " + listaRepuestos.size() + " repuesto(s): <<<\n");
            for (Repuesto r : listaRepuestos) {
                System.out.println("ID: " + r.getIdRepuesto());
                System.out.println("Nombre: " + r.getNombre());
                System.out.println("Descripción: " + r.getDescripcion());
                System.out.println("Precio: Q" + r.getPrecio());
                System.out.println("Existencia: " + r.getExistencia());
                System.out.println("-----------------------------------");
            }
        }

    } catch (SQLException e) {
        throw new IllegalStateException("Error al listar repuestos: " + e.getMessage(), e);
    }

    return listaRepuestos;

    }
}
```

## src/main/java/com/mycompany/taller/dao/ServicioDao.java

**Estado:** Corregir.

**Problema encontrado:** Faltaba recuperar ID generado y distinguir fallo de consulta de lista vacía.

**Cambio realizado:** Mantener CRUD, recuperar ID e informar excepciones.

**Código completo final:**

```java
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.taller.dao;
import com.mycompany.taller.config.Conexiontaller;
import com.mycompany.taller.modelo.Servicio;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Auder
 */
public class ServicioDao implements CrudDao<Servicio> {

    public boolean insertar(Servicio servicio) {
        String sql = "INSERT INTO servicios (nombre_servicio, descripcion, costo_mano_obra) VALUES (?, ?, ?)";
        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS))
        {
            ps.setString(1, servicio.getNombreServicio());
            ps.setString(2, servicio.getDescripcion());
            ps.setDouble(3, servicio.getCostoManoObra());

            int filasInsertadas = ps.executeUpdate();
            if (filasInsertadas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) servicio.setIdServicio(keys.getInt(1));
                }
            }
            boolean insertado = filasInsertadas > 0;

            System.out.println("--- RESULTADO DE INSERCIÓN ---");
            if (insertado) {
                System.out.println(">>> ¡Servicio '" + servicio.getNombreServicio() + "' insertado con éxito! <<<");
            } else {
                System.out.println(">>> No se pudo insertar el servicio. <<<");
            }
            System.out.println("------------------------------");

            return insertado;

        } catch (SQLException e) {
            throw new IllegalStateException("Error al insertar servicio: " + e.getMessage(), e);
        }
    }

    public Servicio buscarPorId(int idServicio) {
        String sql = "SELECT * FROM servicios WHERE id_servicio = ?";
        Servicio servicio = null;

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idServicio);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    servicio = new Servicio(
                        rs.getInt("id_servicio"),
                        rs.getString("nombre_servicio"),
                        rs.getString("descripcion"),
                        rs.getDouble("costo_mano_obra")
                    );
                }
            }

            System.out.println("--- RESULTADO DE BÚSQUEDA POR ID ---");
            if (servicio != null) {
                System.out.println(">>> ¡Servicio encontrado con éxito! <<<");
                System.out.println("ID Servicio: " + servicio.getIdServicio());
                System.out.println("Nombre: " + servicio.getNombreServicio());
                System.out.println("Descripción: " + servicio.getDescripcion());
                System.out.println("Costo Mano de Obra: Q" + servicio.getCostoManoObra());
            } else {
                System.out.println(">>> No se encontró ningún servicio con el ID " + idServicio + " <<<");
            }
            System.out.println("------------------------------------");

        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar servicio por ID: " + e.getMessage(), e);
        }

        return servicio;
    }

    public boolean actualizar(Servicio servicio) {
        String sql = "UPDATE servicios SET nombre_servicio = ?, descripcion = ?, costo_mano_obra = ? WHERE id_servicio = ?";
        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql))
        {
            ps.setString(1, servicio.getNombreServicio());
            ps.setString(2, servicio.getDescripcion());
            ps.setDouble(3, servicio.getCostoManoObra());
            ps.setInt(4, servicio.getIdServicio());

            boolean actualizado = ps.executeUpdate() > 0;

            System.out.println("--- RESULTADO DE ACTUALIZACIÓN ---");
            if (actualizado) {
                System.out.println(">>> ¡Servicio con ID " + servicio.getIdServicio() + " actualizado con éxito! <<<");
            } else {
                System.out.println(">>> No se pudo actualizar. Es posible que el ID " + servicio.getIdServicio() + " no exista. <<<");
            }
            System.out.println("-----------------------------------");

            return actualizado;

        } catch (SQLException e) {
            throw new IllegalStateException("Error al actualizar servicio: " + e.getMessage(), e);
        }
    }

    public boolean eliminar(int idServicio) {
        String sql = "DELETE FROM servicios WHERE id_servicio = ?";
        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idServicio);
            boolean eliminado = ps.executeUpdate() > 0;

            System.out.println("--- RESULTADO DE ELIMINACIÓN ---");
            if (eliminado) {
                System.out.println(">>> ¡Servicio con ID " + idServicio + " eliminado con éxito! <<<");
            } else {
                System.out.println(">>> No se pudo eliminar. Es posible que el ID " + idServicio + " no exista. <<<");
            }
            System.out.println("---------------------------------");

            return eliminado;

        } catch (SQLException e) {
            throw new IllegalStateException("Error al eliminar servicio: " + e.getMessage(), e);
        }
    }

    public List<Servicio> listarTodos() {
        String sql = "SELECT * FROM servicios";
        List<Servicio> listaServicios = new ArrayList<>();

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Servicio servicio = new Servicio(
                    rs.getInt("id_servicio"),
                    rs.getString("nombre_servicio"),
                    rs.getString("descripcion"),
                    rs.getDouble("costo_mano_obra")
                );
                listaServicios.add(servicio);
            }

            System.out.println("--- LISTANDO DE SERVICIOS ---");
            if (listaServicios.isEmpty()) {
                System.out.println(">>> No hay servicios registrados en la base de datos. <<<");
            } else {
                System.out.println(">>> Se encontraron " + listaServicios.size() + " servicio(s): <<<\n");
                for (Servicio s : listaServicios) {
                    System.out.println("ID Servicio: " + s.getIdServicio());
                    System.out.println("Nombre: " + s.getNombreServicio());
                    System.out.println("Descripción: " + s.getDescripcion());
                    System.out.println("Costo Mano de Obra: Q" + s.getCostoManoObra());
                    System.out.println("-----------------------------------");
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Error al listar servicios: " + e.getMessage(), e);
        }

        return listaServicios;
    }
}
```

## src/main/java/com/mycompany/taller/dao/TransaccionOrden.java

**Estado:** Crear.

**Problema encontrado:** No había coordinación transaccional de detalle, inventario y total.

**Cambio realizado:** Bloquear orden, confirmar o revertir todos los cambios y recalcular montos mediante DECIMAL en SQL.

**Código completo final:**

```java
package com.mycompany.taller.dao;

import com.mycompany.taller.config.Conexiontaller;
import java.sql.*;

final class TransaccionOrden {
    interface Operacion { void ejecutar(Connection con) throws SQLException; }
    private TransaccionOrden() { }
    static boolean ejecutar(int idOrden, boolean soloEnProceso, Operacion operacion) {
        try (Connection con = Conexiontaller.IniciarConexion()) {
            con.setAutoCommit(false);
            try {
                String estado = bloquear(con, idOrden);
                if ("FINALIZADA".equals(estado) || "CANCELADA".equals(estado)) throw new IllegalStateException("La orden está cerrada.");
                if (soloEnProceso && !"EN PROCESO".equals(estado)) throw new IllegalStateException("La orden debe estar EN PROCESO.");
                operacion.ejecutar(con);
                recalcular(con, idOrden);
                con.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo guardar la operación de la orden: " + e.getMessage(), e); }
    }
    static String bloquear(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT estado FROM ordenes_trabajo WHERE id_orden=? FOR UPDATE")) {
            ps.setInt(1,id);
            try (ResultSet rs=ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("La orden no existe.");
                return rs.getString(1);
            }
        }
    }
    static void recalcular(Connection con,int id) throws SQLException {
        String sql="UPDATE ordenes_trabajo SET costo_total=(SELECT COALESCE(SUM(costo_aplicado),0) FROM detalle_orden_servicios WHERE id_orden=?) + (SELECT COALESCE(SUM(subtotal),0) FROM detalle_orden_repuestos WHERE id_orden=?) WHERE id_orden=?";
        try (PreparedStatement ps=con.prepareStatement(sql)) { ps.setInt(1,id);ps.setInt(2,id);ps.setInt(3,id);ps.executeUpdate(); }
    }
    static void devolverRepuestos(Connection con,int id) throws SQLException {
        String sql="UPDATE repuestos r JOIN (SELECT id_repuesto,SUM(cantidad) AS unidades FROM detalle_orden_repuestos WHERE id_orden=? GROUP BY id_repuesto) d ON r.id_repuesto=d.id_repuesto SET r.existencia=r.existencia+d.unidades";
        try (PreparedStatement ps=con.prepareStatement(sql)) {ps.setInt(1,id);ps.executeUpdate();}
    }
}
```

## src/main/java/com/mycompany/taller/dao/VehiculoDao.java

**Estado:** Corregir.

**Problema encontrado:** Se conservaron cambios manuales; faltaba coherencia en el manejo de excepciones e ID generado.

**Cambio realizado:** Mantener el JOIN que reconstruye al propietario completo, recuperar ID y propagar errores sin reemplazar el CRUD.

**Código completo final:**

```java
package com.mycompany.taller.dao;

import com.mycompany.taller.config.Conexiontaller;
import com.mycompany.taller.modelo.Cliente;
import com.mycompany.taller.modelo.Vehiculo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VehiculoDao implements CrudDao<Vehiculo> {

    public boolean insertar(Vehiculo vehiculo) {
        String sql = "INSERT INTO vehiculos (placa, marca, modelo, anio, id_cliente) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, vehiculo.getPlaca());
            ps.setString(2, vehiculo.getMarca());
            ps.setString(3, vehiculo.getModelo());
            ps.setInt(4, vehiculo.getAnio());
            ps.setInt(5, vehiculo.getClientePropietario().getId());

            int filasInsertadas = ps.executeUpdate();
            if (filasInsertadas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) vehiculo.setIdVehiculo(keys.getInt(1));
                }
            }
            return filasInsertadas > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Error al insertar vehiculo: " + e.getMessage(), e);
        }
    }

    public Vehiculo buscarPorId(int idVehiculo) {
        String sql = "SELECT v.*, c.nombres, c.apellidos, c.telefono, "
                + "c.direccion, c.correo "
                + "FROM vehiculos v "
                + "INNER JOIN clientes c ON v.id_cliente = c.id_cliente "
                + "WHERE v.id_vehiculo = ?";

        Vehiculo vehiculo = null;

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVehiculo);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    Cliente cliente = new Cliente(
                            rs.getInt("id_cliente"),
                            rs.getString("nombres"),
                            rs.getString("apellidos"),
                            rs.getString("telefono"),
                            rs.getString("direccion"),
                            rs.getString("correo")
                    );

                    vehiculo = new Vehiculo(
                            rs.getInt("id_vehiculo"),
                            rs.getString("placa"),
                            rs.getString("marca"),
                            rs.getString("modelo"),
                            rs.getInt("anio"),
                            cliente
                    );
                }
            }

            System.out.println("--- RESULTADO DE BUSQUEDA POR ID ---");

            if (vehiculo != null) {
                System.out.println("Vehiculo encontrado con exito");
                System.out.println("ID Vehiculo: " + vehiculo.getIdVehiculo());
                System.out.println("Placa: " + vehiculo.getPlaca());
                System.out.println("Marca: " + vehiculo.getMarca());
                System.out.println("Modelo: " + vehiculo.getModelo());
                System.out.println("Anio: " + vehiculo.getAnio());
                System.out.println(
                        "Propietario: "
                        + vehiculo.getClientePropietario().getNombres()
                        + " "
                        + vehiculo.getClientePropietario().getApellidos()
                );
            } else {
                System.out.println(
                        "No se encontro ningun vehiculo con el ID "
                        + idVehiculo
                );
            }

            System.out.println("------------------------------------");

        } catch (SQLException e) {
            if(e.getSQLState()!=null && e.getSQLState().startsWith("23")) throw new IllegalArgumentException("Registro duplicado o relacionado con otros datos: " + e.getMessage(),e);
            throw new IllegalStateException("No se pudo completar la operaci?n de Vehiculo: " + e.getMessage(),e);
        }

        return vehiculo;
    }

    public boolean actualizar(Vehiculo vehiculo) {
        String sql = "UPDATE vehiculos SET placa = ?, marca = ?, modelo = ?, "
                + "anio = ?, id_cliente = ? WHERE id_vehiculo = ?";

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, vehiculo.getPlaca());
            ps.setString(2, vehiculo.getMarca());
            ps.setString(3, vehiculo.getModelo());
            ps.setInt(4, vehiculo.getAnio());
            ps.setInt(5, vehiculo.getClientePropietario().getId());
            ps.setInt(6, vehiculo.getIdVehiculo());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            if(e.getSQLState()!=null && e.getSQLState().startsWith("23")) throw new IllegalArgumentException("Registro duplicado o relacionado con otros datos: " + e.getMessage(),e);
            throw new IllegalStateException("No se pudo completar la operaci?n de Vehiculo: " + e.getMessage(),e);
        }
    }

    public boolean eliminar(int idVehiculo) {
        String sql = "DELETE FROM vehiculos WHERE id_vehiculo = ?";

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVehiculo);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            if(e.getSQLState()!=null && e.getSQLState().startsWith("23")) throw new IllegalArgumentException("Registro duplicado o relacionado con otros datos: " + e.getMessage(),e);
            throw new IllegalStateException("No se pudo completar la operaci?n de Vehiculo: " + e.getMessage(),e);
        }
    }

    public List<Vehiculo> listarTodos() {
        String sql = "SELECT v.*, c.nombres, c.apellidos, c.telefono, "
                + "c.direccion, c.correo "
                + "FROM vehiculos v "
                + "INNER JOIN clientes c ON v.id_cliente = c.id_cliente";

        List<Vehiculo> listaVehiculos = new ArrayList<>();

        try (Connection con = Conexiontaller.IniciarConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Cliente cliente = new Cliente(
                        rs.getInt("id_cliente"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        rs.getString("direccion"),
                        rs.getString("correo")
                );

                Vehiculo vehiculo = new Vehiculo(
                        rs.getInt("id_vehiculo"),
                        rs.getString("placa"),
                        rs.getString("marca"),
                        rs.getString("modelo"),
                        rs.getInt("anio"),
                        cliente
                );

                listaVehiculos.add(vehiculo);
            }

            System.out.println("--- LISTADO DE VEHICULOS ---");

            if (listaVehiculos.isEmpty()) {

                System.out.println(
                        "No hay vehiculos registrados en la base de datos."
                );

            } else {

                System.out.println(
                        "Se encontraron "
                        + listaVehiculos.size()
                        + " vehiculo(s)"
                );

                for (Vehiculo v : listaVehiculos) {

                    System.out.println(
                            "ID Vehiculo: "
                            + v.getIdVehiculo()
                    );

                    System.out.println(
                            "Placa: "
                            + v.getPlaca()
                    );

                    System.out.println(
                            "Marca/Modelo: "
                            + v.getMarca()
                            + " "
                            + v.getModelo()
                            + " ("
                            + v.getAnio()
                            + ")"
                    );

                    System.out.println(
                            "Propietario: "
                            + v.getClientePropietario().getNombres()
                            + " "
                            + v.getClientePropietario().getApellidos()
                    );

                    System.out.println(
                            "-----------------------------------"
                    );
                }
            }

        } catch (SQLException e) {
            if(e.getSQLState()!=null && e.getSQLState().startsWith("23")) throw new IllegalArgumentException("Registro duplicado o relacionado con otros datos: " + e.getMessage(),e);
            throw new IllegalStateException("No se pudo completar la operaci?n de Vehiculo: " + e.getMessage(),e);
        }

        return listaVehiculos;
    }
}
```

## src/main/java/com/mycompany/taller/modelo/Cliente.java

**Estado: Correcto. No requiere cambios.** Se conserva el archivo recibido.

## src/main/java/com/mycompany/taller/modelo/DetalleOrdenRepuesto.java

**Estado:** Corregir.

**Problema encontrado:** El constructor/setter admitía montos o cantidades inválidas.

**Cambio realizado:** Validar tanto construcción como setters, normalizar importes a centavos y conservar nombres y tipos existentes.

**Código completo final:**

```java
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.taller.modelo;

/**
 *
 * @author Auder
 */
public class DetalleOrdenRepuesto {
    private int idDetalleRepuesto;
    private int idOrden;
    private Repuesto repuesto; // Composición
    private int cantidad;
    private double precioUnitario;
    private double subtotal; // Calculado

    public DetalleOrdenRepuesto() {}

    public DetalleOrdenRepuesto(int idDetalleRepuesto, int idOrden, Repuesto repuesto, int cantidad, double precioUnitario) {
        this.idDetalleRepuesto = idDetalleRepuesto;
        this.idOrden = idOrden;
        this.repuesto = repuesto;
        setCantidad(cantidad);
        setPrecioUnitario(precioUnitario);
        this.subtotal = calcularSubtotal(); // Cálculo
    }

    // calculo subtotal
    public double calcularSubtotal() {
        return this.cantidad * this.precioUnitario;
    }

    public int getIdDetalleRepuesto()
    {
        return idDetalleRepuesto;
    }
    public void setIdDetalleRepuesto(int idDetalleRepuesto)
    { 
        this.idDetalleRepuesto = idDetalleRepuesto; 
    }

    public int getIdOrden() 
    { 
        return idOrden; 
    }
    public void setIdOrden(int idOrden)
    { 
        this.idOrden = idOrden;
    }

    public Repuesto getRepuesto() 
    { 
        return repuesto;
    }
    public void setRepuesto(Repuesto repuesto)
    { 
        this.repuesto = repuesto; 
    }

    public int getCantidad() 
    { 
        return cantidad; 
    }
    public void setCantidad(int cantidad) { 
        if (cantidad <= 0)
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        this.cantidad = cantidad;
        this.subtotal = calcularSubtotal(); // evalua si cambia el todal
    }

    public double getPrecioUnitario()
    { 
        return precioUnitario; 
    }
    public void setPrecioUnitario(double precioUnitario) {
        if (!Double.isFinite(precioUnitario) || precioUnitario < 0 || precioUnitario > 99999999.99) throw new IllegalArgumentException("Monto fuera de rango.");
        this.precioUnitario = java.math.BigDecimal.valueOf(precioUnitario).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
        this.subtotal = calcularSubtotal();
    }

    public double getSubtotal()
    { 
        return subtotal;
    }
}
```

## src/main/java/com/mycompany/taller/modelo/DetalleOrdenServicio.java

**Estado:** Corregir.

**Problema encontrado:** El constructor/setter admitía montos o cantidades inválidas.

**Cambio realizado:** Validar tanto construcción como setters, normalizar importes a centavos y conservar nombres y tipos existentes.

**Código completo final:**

```java
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.taller.modelo;

/**
 *
 * @author Auder
 */
public class DetalleOrdenServicio {
    private int idDetalleServicio;
    private int idOrden;
    private Servicio servicio; // Composición
    private double costoAplicado;

    public DetalleOrdenServicio() {}

    public DetalleOrdenServicio(int idDetalleServicio, int idOrden, Servicio servicio, double costoAplicado) {
        this.idDetalleServicio = idDetalleServicio;
        this.idOrden = idOrden;
        this.servicio = servicio;
        setCostoAplicado(costoAplicado);
    }

    public int getIdDetalleServicio()
    { return idDetalleServicio;
    }
    public void setIdDetalleServicio(int idDetalleServicio)
    {
        this.idDetalleServicio = idDetalleServicio;
    }

    public int getIdOrden()
    {
        return idOrden;
    }
    public void setIdOrden(int idOrden)
    {
        this.idOrden = idOrden;
    }

    public Servicio getServicio()
    {
        return servicio;
    }
    public void setServicio(Servicio servicio)
    {
        this.servicio = servicio;
    }

    public double getCostoAplicado()
    {
        return costoAplicado;
    }
    public void setCostoAplicado(double costoAplicado)
    {
        if (!Double.isFinite(costoAplicado) || costoAplicado < 0 || costoAplicado > 99999999.99) throw new IllegalArgumentException("Monto fuera de rango.");
        this.costoAplicado = java.math.BigDecimal.valueOf(costoAplicado).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
```

## src/main/java/com/mycompany/taller/modelo/Mecanico.java

**Estado: Correcto. No requiere cambios.** Se conserva el archivo recibido.

## src/main/java/com/mycompany/taller/modelo/OrdenTrabajo.java

**Estado:** Corregir.

**Problema encontrado:** No permitía restaurar el total persistido.

**Cambio realizado:** Añadir setter validado de total y normalización a centavos; conservar colecciones y estados.

**Código completo final:**

```java
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.taller.modelo;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Auder
 */
public class OrdenTrabajo {
    private int idOrden;
    private Vehiculo vehiculo; // Composición
    private Mecanico mecanico; // Composición
    private Date fechaIngreso;
    private String estado;     // PENDIENTE, EN PROCESO, FINALIZADA, CANCELADA
    private String descripcionProblema;
    private double costoTotal;

    //(Listas para almacenar los detalles de forma orientada a objetos)
    private List<DetalleOrdenServicio> listaServicios;
    private List<DetalleOrdenRepuesto> listaRepuestos;

    public OrdenTrabajo() {
        this.listaServicios = new ArrayList<>();
        this.listaRepuestos = new ArrayList<>();
        this.estado = "PENDIENTE";
    }

    // Método para agregar elementos a las colecciones y calcular totales en tiempo de ejecución
    public void agregarServicio(DetalleOrdenServicio servicio) {
        if (!this.estado.equals("EN PROCESO")) {
            throw new IllegalStateException("Solo se pueden agregar servicios a una orden en proceso.");
        }
        this.listaServicios.add(servicio);
    }

    public void agregarRepuesto(DetalleOrdenRepuesto repuesto) {
        if (!this.estado.equals("EN PROCESO")) {
            throw new IllegalStateException("Solo se pueden asignar repuestos a una orden en proceso.");
        }
        
        this.listaRepuestos.add(repuesto);
    }

    // Método para calcular el costo total sumando mano de obra y subtotales de repuesto
    public void calcularCostoTotalFinal() {
        double totalManoObra = listaServicios.stream().mapToDouble(DetalleOrdenServicio::getCostoAplicado).sum();
        double totalRepuestos = listaRepuestos.stream().mapToDouble(DetalleOrdenRepuesto::getSubtotal).sum();
        setCostoTotal(totalManoObra + totalRepuestos);
    }


    public int getIdOrden() 
    { 
        return idOrden;
    }
    public void setIdOrden(int idOrden) 
    { 
        this.idOrden = idOrden; 
    }

    public Vehiculo getVehiculo() 
    { 
        return vehiculo; 
    }
    public void setVehiculo(Vehiculo vehiculo) 
    { 
        this.vehiculo = vehiculo; 
    }

    public Mecanico getMecanico() 
    { 
        return mecanico;
    }
    public void setMecanico(Mecanico mecanico) 
    {
        this.mecanico = mecanico;
    }

    public Date getFechaIngreso()
    { 
        return fechaIngreso; 
    }
    public void setFechaIngreso(Date fechaIngreso)
    { 
        this.fechaIngreso = fechaIngreso; 
    }

    public String getEstado() 
    { 
        return estado; 
    }
  public void setEstado(String estado) { 
        // Si ya está finalizada o cancelada se bloquea por completo
        if (this.estado != null && (this.estado.equals("FINALIZADA") || this.estado.equals("CANCELADA"))) {
            throw new IllegalStateException("Las órdenes finalizadas o canceladas no admiten modificaciones posteriores.");
        }
        
        // Validación y asignación dentro del mismo bloque
        if (estado != null && (estado.equals("PENDIENTE") || estado.equals("EN PROCESO") || 
                               estado.equals("FINALIZADA") || estado.equals("CANCELADA"))) {
            this.estado = estado;
        } else {
            throw new IllegalArgumentException("Estado no válido. Los estados permitidos son: PENDIENTE, EN PROCESO, FINALIZADA, CANCELADA.");
        }
       
   }
    public String getdescripcionProblema() 
    {
        return descripcionProblema;
    }
    public void setdescripcionProblema(String descripcionProblema) 
    { 
        this.descripcionProblema = descripcionProblema;
    }

    public double getCostoTotal() 
    { 
        return costoTotal; 
    }

    public void setCostoTotal(double costoTotal) {
        if (!Double.isFinite(costoTotal) || costoTotal < 0 || costoTotal > 99999999.99) throw new IllegalArgumentException("Total fuera de rango.");
        this.costoTotal = java.math.BigDecimal.valueOf(costoTotal).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    public List<DetalleOrdenServicio> getListaServicios() { return listaServicios; }
    public List<DetalleOrdenRepuesto> getListaRepuestos() { return listaRepuestos; }
}
```

## src/main/java/com/mycompany/taller/modelo/Persona.java

**Estado: Correcto. No requiere cambios.** Se conserva el archivo recibido.

## src/main/java/com/mycompany/taller/modelo/Repuesto.java

**Estado:** Corregir.

**Problema encontrado:** El constructor/setter admitía montos o cantidades inválidas.

**Cambio realizado:** Validar tanto construcción como setters, normalizar importes a centavos y conservar nombres y tipos existentes.

**Código completo final:**

```java
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.taller.modelo;

/**
 *
 * @author Auder
 */
public class Repuesto {
    private int idRepuesto;
    private String nombre;
    private String descripcion;
    private double precio;
    private int existencia;

    public Repuesto() {}

    public Repuesto(int idRepuesto, String nombre, String descripcion, double precio, int existencia) {
        this.idRepuesto = idRepuesto;
        this.nombre = nombre;
        this.descripcion = descripcion;
        setPrecio(precio);
        setExistencia(existencia); // Valida mediante el setter
    }

    public int getIdRepuesto()
    {
        return idRepuesto;
    }
    public void setIdRepuesto(int idRepuesto)
    {
        this.idRepuesto = idRepuesto;
    }

    public String getNombre()
    {
        return nombre;
    }
    public void setNombre(String nombre)
    {
        this.nombre = nombre;
    }

    public String getDescripcion()
    {
        return descripcion;
    }
    public void setDescripcion(String descripcion)
    {
        this.descripcion = descripcion;
    }

    public double getPrecio()
    {
        return precio;
    }
    public void setPrecio(double precio)
    {
        if (!Double.isFinite(precio) || precio < 0 || precio > 99999999.99) throw new IllegalArgumentException("Monto fuera de rango.");
        this.precio = java.math.BigDecimal.valueOf(precio).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    public int getExistencia()
    {
        return existencia;
    }

    // Manejo de Excepciones (no existencias negativas)
    public void setExistencia(int existencia) {
        if (existencia < 0) {
            throw new IllegalArgumentException("La existencia de un repuesto no puede ser negativa.");
        }
        this.existencia = existencia;
    }
}
```

## src/main/java/com/mycompany/taller/modelo/Servicio.java

**Estado:** Corregir.

**Problema encontrado:** El constructor/setter admitía montos o cantidades inválidas.

**Cambio realizado:** Validar tanto construcción como setters, normalizar importes a centavos y conservar nombres y tipos existentes.

**Código completo final:**

```java
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.taller.modelo;

/**
 *
 * @author Auder
 */
public class Servicio {
    private int idServicio;
    private String nombreServicio;
    private String descripcion;
    private double costoManoObra;

    public Servicio() {}

    public Servicio(int idServicio, String nombreServicio, String descripcion, double costoManoObra) {
        this.idServicio = idServicio;
        this.nombreServicio = nombreServicio;
        this.descripcion = descripcion;
        setCostoManoObra(costoManoObra);
    }

    public int getIdServicio() {
        return idServicio;
    }
    public void setIdServicio(int idServicio)
    {
        this.idServicio = idServicio;
    }

    public String getNombreServicio()
    {
        return nombreServicio;
    }
    public void setNombreServicio(String nombreServicio)
    {
        this.nombreServicio = nombreServicio;
    }

    public String getDescripcion()
    { return descripcion;
    }
    public void setDescripcion(String descripcion)
    {
        this.descripcion = descripcion;
    }

    public double getCostoManoObra()
    {
        return costoManoObra;
    }
    public void setCostoManoObra(double costoManoObra)
    {
        if (!Double.isFinite(costoManoObra) || costoManoObra < 0 || costoManoObra > 99999999.99) throw new IllegalArgumentException("Monto fuera de rango.");
        this.costoManoObra = java.math.BigDecimal.valueOf(costoManoObra).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
```

## src/main/java/com/mycompany/taller/modelo/Vehiculo.java

**Estado:** Corregir.

**Problema encontrado:** Se detectó durante la revisión una actualización manual a clientePropietario.

**Cambio realizado:** Conservar el nombre y getters/setters de la versión manual vigente; adaptar sus consumidores.

**Código completo final:**

```java
package com.mycompany.taller.modelo;

public class Vehiculo {

    private int idVehiculo;
    private String placa;
    private String marca;
    private String modelo;
    private int anio;
    private Cliente clientePropietario;

    public Vehiculo() {
    }

    public Vehiculo(int idVehiculo, String placa, String marca,
                    String modelo, int anio, Cliente clientePropietario) {
        this.idVehiculo = idVehiculo;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.anio = anio;
        this.clientePropietario = clientePropietario;
    }

    public int getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(int idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    public Cliente getClientePropietario() {
        return clientePropietario;
    }

    public void setClientePropietario(Cliente clientePropietario) {
        this.clientePropietario = clientePropietario;
    }
}
```

## src/main/java/com/mycompany/taller/vista/Html.java

**Estado:** Crear.

**Problema encontrado:** Faltaba presentación web.

**Cambio realizado:** Formularios/listados por módulo, edición, abastecimiento y consultas de reportes con escape HTML.

**Código completo final:**

```java
package com.mycompany.taller.vista;

import com.mycompany.taller.controlador.WebControlador;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Html {
    private Html() { }
    public static String escapar(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    public static String pagina(String titulo,String contenido){return "<!doctype html><html lang='es'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>"+escapar(titulo)+"</title><style>body{font:16px system-ui;margin:2rem auto;max-width:1200px;padding:0 1rem;color:#152735;background:#f4f7f9}nav{display:flex;gap:1rem;flex-wrap:wrap;margin:1rem 0}a{color:#096277}table{border-collapse:collapse;width:100%;background:white}td,th{border:1px solid #ccd8df;padding:.5rem;text-align:left}form{background:white;padding:1rem;margin:1rem 0}label{display:inline-flex;flex-direction:column;margin:.5rem;gap:.25rem}input,select,button{font:inherit;padding:.4rem;max-width:100%}button{cursor:pointer}section{overflow-x:auto}small{display:block;color:#435968}</style><h1>El Último Arranque</h1>"+contenido+"</html>";}
    private static String oculto(String nombre,String valor){return "<input type='hidden' name='"+nombre+"' value='"+escapar(valor)+"'>";}
    public static String modulo(String modulo,WebControlador.Pantalla datos,String editar,String token){
        StringBuilder b=new StringBuilder("<nav>");for(String m:List.of("clientes","vehiculos","mecanicos","repuestos","servicios","ordenes","detalle_servicios","detalle_repuestos"))b.append("<a href='/?modulo=").append(m).append("'>").append(m.replace('_',' ')).append("</a>");b.append("<a href='/reportes'>Reportes</a></nav><h2>").append(escapar(modulo.replace('_',' '))).append("</h2><p>Use ID 0 al crear. Para modificar, pulse Editar. Consulte los IDs relacionados en los módulos correspondientes.</p>");
        Map<String,String> valores=new HashMap<>();if(editar!=null){String[] fila=datos.filas().stream().filter(f->f[0].equals(editar)).findFirst().orElseThrow(()->new IllegalArgumentException("El registro no existe."));for(int i=0;i<datos.campos().length;i++)valores.put(datos.campos()[i],fila[i]);}
        b.append("<form method='post' action='/operar'>").append(oculto("csrf",token)).append(oculto("modulo",modulo)).append(oculto("accion","guardar"));
        for(String campo:datos.campos()){
            if(Set.of("propietario","fecha_ingreso","costo_total").contains(campo))continue;
            String valor=valores.getOrDefault(campo,campo.equals("id")?"0":campo.equals("estado")?"PENDIENTE":"");b.append("<label>").append(escapar(campo));
            if(campo.equals("estado")){b.append("<select name='estado'>");for(String e:List.of("PENDIENTE","EN PROCESO","FINALIZADA","CANCELADA"))b.append("<option ").append(e.equals(valor)?"selected":"").append('>').append(e).append("</option>");b.append("</select>");}
            else {boolean numero=campo.equals("id") || campo.startsWith("id_") || Set.of("anio","precio","existencia","costo_mano_obra","cantidad","precio_unitario","costo_aplicado").contains(campo);b.append("<input name='").append(campo).append("' type='").append(numero?"number":"text").append("' ").append(numero?"min='0' step='any' ":"").append(campo.equals("id")?"readonly ":"").append("value='").append(escapar(valor)).append("'>");}b.append("</label>");
        }
        b.append("<button>Guardar</button> <a href='/?modulo=").append(modulo).append("'>Nuevo</a></form><section><table><tr>");for(String c:datos.campos())b.append("<th>").append(escapar(c)).append("</th>");b.append("<th>Acciones</th></tr>");
        for(String[] fila:datos.filas()){
            b.append("<tr>");for(String v:fila)b.append("<td>").append(escapar(v)).append("</td>");b.append("<td><a href='/?modulo=").append(modulo).append("&amp;editar=").append(escapar(fila[0])).append("'>Editar</a><form method='post' action='/operar'>").append(oculto("csrf",token)).append(oculto("modulo",modulo)).append(oculto("id",fila[0])).append(oculto("accion","eliminar")).append("<button>Eliminar</button></form></td></tr>");
        }
        b.append("</table></section>");
        if(modulo.equals("vehiculos"))b.append("<p>El propietario se guarda con su ID. El reporte de historial permite buscar sus órdenes por placa.</p>");
        if(modulo.equals("repuestos"))b.append("<form method='post' action='/operar'>").append(oculto("csrf",token)).append(oculto("modulo",modulo)).append(oculto("accion","abastecer")).append("<h3>Abastecer inventario</h3><label>ID repuesto<input name='id' type='number' min='1' required></label><label>Unidades a sumar<input name='cantidad' type='number' min='1' required></label><button>Abastecer</button></form>");
        if(modulo.equals("ordenes"))b.append("<p>Flujo: PENDIENTE → EN PROCESO → FINALIZADA. Puede CANCELAR una orden pendiente o en proceso. Agregue servicios y repuestos desde los módulos de detalles. El costo total se consulta en Reportes; al finalizar se habilita el comprobante.</p>");
        return pagina(modulo,b.toString());
    }
    public static String reportes(){return pagina("Reportes","<a href='/'>Volver</a><h2>Reportes y comprobantes</h2><form action='/reporte'><label>Reporte<select name='tipo'><option value='ordenes'>Órdenes por estado</option><option value='historial'>Historial por placa</option><option value='inventario'>Inventario bajo o agotado</option><option value='comprobante'>Comprobante de orden finalizada</option></select></label><label>Estado (órdenes)<select name='estado'><option value=''>TODOS</option><option>PENDIENTE</option><option>EN PROCESO</option><option>FINALIZADA</option><option>CANCELADA</option></select></label><label>Placa (historial)<input name='placa'></label><label>Límite de inventario<input name='limite' type='number' value='5' min='0'></label><label>ID orden (comprobante)<input name='id' type='number' min='1'></label><label>Formato<select name='formato'><option value='pdf'>PDF</option><option value='html'>Ver en pantalla</option><option value='json'>JSON</option></select></label><button>Consultar</button></form>");}
    public static String reporte(List<String> lineas){StringBuilder b=new StringBuilder("<a href='/reportes'>Volver a reportes</a><pre>");for(String linea:lineas)b.append(escapar(linea)).append('\n');return pagina("Reporte",b.append("</pre>").toString());}
    public static String url(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
}
```

## src/main/java/com/mycompany/taller/vista/PdfTexto.java

**Estado:** Crear.

**Problema encontrado:** Faltaba exportación PDF.

**Cambio realizado:** Generar documentos de texto multipágina con tabla xref y codificación para caracteres españoles sin dependencias nuevas.

**Código completo final:**

```java
package com.mycompany.taller.vista;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.*;

public final class PdfTexto {
    private static final Charset CODIFICACION=Charset.forName("windows-1252");
    private PdfTexto() { }
    public static byte[] crear(List<String> texto){
        List<String> lineas=new ArrayList<>();for(String original:texto){for(String linea:original.replace('\r',' ').split("\n",-1)){if(linea.isEmpty())lineas.add("");for(int i=0;i<linea.length();i+=95)lineas.add(linea.substring(i,Math.min(i+95,linea.length())));}}
        int paginas=Math.max(1,(lineas.size()+44)/45);List<byte[]> objetos=new ArrayList<>();
        objetos.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));StringBuilder hijos=new StringBuilder();for(int i=0;i<paginas;i++)hijos.append(4+i*2).append(" 0 R ");objetos.add(bytes("<< /Type /Pages /Kids ["+hijos+"] /Count "+paginas+" >>"));objetos.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        for(int i=0;i<paginas;i++){
            int contenido=5+i*2;objetos.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 3 0 R >> >> /Contents "+contenido+" 0 R >>"));
            StringBuilder flujo=new StringBuilder("BT /F1 10 Tf 14 TL 40 752 Td\n");for(int j=i*45;j<Math.min((i+1)*45,lineas.size());j++)flujo.append('(').append(escapar(lineas.get(j))).append(") Tj T*\n");flujo.append("ET");byte[] datos=bytes(flujo.toString());ByteArrayOutputStream obj=new ByteArrayOutputStream();obj.writeBytes(bytes("<< /Length "+datos.length+" >>\nstream\n"));obj.writeBytes(datos);obj.writeBytes(bytes("\nendstream"));objetos.add(obj.toByteArray());
        }
        ByteArrayOutputStream salida=new ByteArrayOutputStream();salida.writeBytes(bytes("%PDF-1.4\n"));List<Integer> posiciones=new ArrayList<>();
        for(int i=0;i<objetos.size();i++){posiciones.add(salida.size());salida.writeBytes(bytes((i+1)+" 0 obj\n"));salida.writeBytes(objetos.get(i));salida.writeBytes(bytes("\nendobj\n"));}
        int xref=salida.size();salida.writeBytes(bytes("xref\n0 "+(objetos.size()+1)+"\n0000000000 65535 f \n"));for(int posicion:posiciones)salida.writeBytes(bytes(String.format(Locale.ROOT,"%010d 00000 n \n",posicion)));salida.writeBytes(bytes("trailer\n<< /Size "+(objetos.size()+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF\n"));return salida.toByteArray();
    }
    private static String escapar(String s){return s.replace("\\","\\\\").replace("(","\\(").replace(")","\\)");}
    private static byte[] bytes(String s){return s.getBytes(CODIFICACION);}
}
```

## src/main/java/com/mycompany/taller/vista/ServidorWeb.java

**Estado:** Crear.

**Problema encontrado:** No existían rutas web ni API.

**Cambio realizado:** Servidor HTTP Java, POST protegido por token, JSON/PDF y respuestas de validación/conexión.

**Código completo final:**

```java
package com.mycompany.taller.vista;

import com.mycompany.taller.config.Conexiontaller;
import com.mycompany.taller.controlador.*;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;

public class ServidorWeb {
    private final HttpServer servidor;
    private final java.util.concurrent.ExecutorService ejecutor=Executors.newFixedThreadPool(8);
    private final String token=UUID.randomUUID().toString();
    public ServidorWeb(String host,int puerto)throws IOException{
        servidor=HttpServer.create(new InetSocketAddress(host,puerto),0);servidor.setExecutor(ejecutor);servidor.createContext("/",this::atender);
    }
    public void iniciar(){servidor.start();}
    public void detener(){servidor.stop(0);ejecutor.shutdownNow();}
    public int puerto(){return servidor.getAddress().getPort();}
    private void atender(HttpExchange x)throws IOException{
        try{
            String ruta=x.getRequestURI().getPath();Map<String,String> p=parametros(x.getRequestURI().getRawQuery());
            if(ruta.equals("/operar")){
                if(!x.getRequestMethod().equals("POST")){enviar(x,405,"text/plain","Use POST.");return;}
                byte[] cuerpo=x.getRequestBody().readNBytes(65537);if(cuerpo.length>65536){enviar(x,413,"text/plain","Formulario demasiado grande.");return;}
                p=parametros(new String(cuerpo,StandardCharsets.UTF_8));if(!token.equals(p.get("csrf"))){enviar(x,403,"text/plain","Formulario no válido; vuelva a abrir la página.");return;}
                new WebControlador().operar(p);x.getResponseHeaders().set("Location","/?modulo="+Html.url(p.get("modulo")));x.sendResponseHeaders(303,-1);return;
            }
            if(!x.getRequestMethod().equals("GET")){enviar(x,405,"text/plain","Use GET.");return;}
            switch(ruta){
                case "/" -> {String modulo=p.getOrDefault("modulo","clientes");enviar(x,200,"text/html",Html.modulo(modulo,new WebControlador().consultar(modulo),p.get("editar"),token));}
                case "/reportes" -> enviar(x,200,"text/html",Html.reportes());
                case "/reporte", "/api/reportes" -> {
                    List<String> lineas=new ReporteControlador().generar(p);String formato=p.getOrDefault("formato",ruta.startsWith("/api/")?"json":"pdf");
                    if(formato.equals("pdf")){x.getResponseHeaders().set("Content-Disposition","inline; filename=reporte-taller.pdf");enviar(x,200,"application/pdf",PdfTexto.crear(lineas));}
                    else if(formato.equals("json"))enviar(x,200,"application/json",json(lineas));
                    else if(formato.equals("html"))enviar(x,200,"text/html",Html.reporte(lineas));
                    else throw new IllegalArgumentException("Formato desconocido.");
                }
                case "/salud" -> {try(Connection con=Conexiontaller.IniciarConexion();PreparedStatement ps=con.prepareStatement("SELECT 1");ResultSet rs=ps.executeQuery()){rs.next();enviar(x,200,"application/json","{\"estado\":\"ok\",\"baseDatos\":true}");}}
                default -> enviar(x,404,"text/plain","Página no encontrada.");
            }
        }catch(IllegalArgumentException e){enviar(x,400,"text/html",Html.pagina("Datos inválidos","<h2>Revise los datos</h2><p>"+Html.escapar(e.getMessage())+"</p><a href='/'>Volver</a>"));}
        catch(IllegalStateException e){enviar(x,e.getCause() instanceof SQLException?503:409,"text/html",Html.pagina("Operación no realizada","<h2>No se pudo completar la operación</h2><p>"+Html.escapar(e.getMessage())+"</p><a href='/'>Volver</a>"));}
        catch(SQLException e){enviar(x,503,"text/plain","No se pudo conectar con MySQL. Revise la configuración y el servicio.");}
        catch(Exception e){System.err.println("Error HTTP: "+e.getClass().getSimpleName());enviar(x,500,"text/plain","Ocurrió un error al atender la solicitud.");}
        finally{x.close();}
    }
    private static Map<String,String> parametros(String texto){Map<String,String> p=new HashMap<>();if(texto!=null && !texto.isEmpty())for(String par:texto.split("&")){String[] partes=par.split("=",2);p.put(URLDecoder.decode(partes[0],StandardCharsets.UTF_8),URLDecoder.decode(partes.length>1?partes[1]:"",StandardCharsets.UTF_8));}return p;}
    private static String json(List<String> lineas){StringJoiner j=new StringJoiner(",","{\"lineas\":[","]}");for(String linea:lineas){StringBuilder s=new StringBuilder("\"");for(char c:linea.toCharArray()){if(c=='\"'||c=='\\')s.append('\\').append(c);else if(c<32)s.append(String.format("\\u%04x",(int)c));else s.append(c);}j.add(s.append('\"').toString());}return j.toString();}
    private static void enviar(HttpExchange x,int codigo,String tipo,String texto)throws IOException{enviar(x,codigo,tipo+"; charset=utf-8",texto.getBytes(StandardCharsets.UTF_8));}
    private static void enviar(HttpExchange x,int codigo,String tipo,byte[] datos)throws IOException{x.getResponseHeaders().set("Content-Type",tipo);x.getResponseHeaders().set("X-Content-Type-Options","nosniff");x.getResponseHeaders().set("Cache-Control","no-store");x.sendResponseHeaders(codigo,datos.length);x.getResponseBody().write(datos);}
}
```

## src/test/java/com/mycompany/taller/PruebasIntegracion.java

**Estado:** Crear.

**Problema encontrado:** No existía una verificación reproducible del CRUD y las transacciones.

**Cambio realizado:** 86 comprobaciones reales con base aislada, HTTP/PDF, concurrencia, rollback y checksums de datos originales.

**Código completo final:**

```java
package com.mycompany.taller;

import com.mycompany.taller.config.Conexiontaller;
import com.mycompany.taller.controlador.*;
import com.mycompany.taller.dao.*;
import com.mycompany.taller.modelo.*;
import com.mycompany.taller.vista.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class PruebasIntegracion {
    private static int comprobaciones;
    private static final List<String> RESULTADOS=new ArrayList<>();
    private static void verificar(boolean condicion,String nombre){if(!condicion)throw new AssertionError(nombre);comprobaciones++;RESULTADOS.add("OK: "+nombre);System.out.println("OK: "+nombre);}
    private static void rechazar(Runnable accion,String nombre){try{accion.run();}catch(IllegalArgumentException | IllegalStateException e){verificar(true,nombre);return;}throw new AssertionError("Se aceptó una operación inválida: "+nombre);}
    private static void sql(Connection con,String instruccion)throws SQLException{try(PreparedStatement ps=con.prepareStatement(instruccion)){ps.execute();}}
    private static void cargar(Connection con,Path archivo)throws Exception{String texto=Files.readString(archivo).replaceAll("(?m)--.*$","");for(String s:texto.split(";"))if(!s.isBlank())sql(con,s);}
    public static void main(String[] args)throws Exception{
        String temporal="taller_revision_test_"+UUID.randomUUID().toString().replace("-","");String urlOriginal=null;boolean creada=false;Throwable fallo=null;
        try(Connection admin=Conexiontaller.IniciarConexion()){
            Map<String,String> firmaInicial=firmaDatos(admin);
            urlOriginal=admin.getMetaData().getURL();sql(admin,"CREATE DATABASE "+temporal+" CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");creada=true;
            try{
                String nueva=urlOriginal.replaceFirst("(jdbc:mysql://[^/]+/)[^?]*","$1"+temporal);System.setProperty("taller.db.url",nueva);
                try(Connection con=Conexiontaller.IniciarConexion()){cargar(con,Path.of("sql/01-esquema.sql"));cargar(con,Path.of("sql/02-datos-prueba.sql"));}
                verificar(new ClienteDao().listarTodos().size()==2,"Script SQL de instalación completo sin llaves foráneas rotas");
                probarCrud();probarOrdenes();probarHttp();probarConexionFallida();
            }catch(Throwable e){fallo=e;throw e;}
            finally{
                System.clearProperty("taller.db.url");
                if(creada && temporal.matches("taller_revision_test_[a-f0-9]{32}")){sql(admin,"DROP DATABASE "+temporal);RESULTADOS.add("Base temporal eliminada: "+temporal);}
                if(fallo==null)verificar(firmaDatos(admin).equals(firmaInicial),"Datos originales id?nticos antes y despu?s de las pruebas");
                Files.createDirectories(Path.of("target"));Files.writeString(Path.of("target/resultado-pruebas.txt"),"Estado: "+(fallo==null?"CORRECTO":"FALLÓ: "+fallo)+"\nComprobaciones: "+comprobaciones+"\n"+String.join("\n",RESULTADOS));
            }
        }
        System.out.println("PRUEBAS COMPLETADAS: "+comprobaciones+" comprobaciones. Base original sin cambios.");
    }
    private static Map<String,String> firmaDatos(Connection con)throws SQLException{
        Map<String,String> firma=new HashMap<>();try(PreparedStatement ps=con.prepareStatement("CHECKSUM TABLE clientes,vehiculos,mecanicos,repuestos,servicios,ordenes_trabajo,detalle_orden_servicios,detalle_orden_repuestos EXTENDED");ResultSet rs=ps.executeQuery()){while(rs.next())firma.put(rs.getString(1),rs.getString(2));}return firma;
    }
    private static void probarCrud(){
        ClienteControlador cc=new ClienteControlador();Cliente c=new Cliente(0,"Ana","Prueba","5555-1234","Zona 1","ana@example.com");verificar(cc.insertar(c)&&c.getId()>0,"Crear cliente y recuperar ID generado");c.setDireccion("Zona 2");verificar(cc.actualizar(c)&&cc.buscarPorId(c.getId()).getDireccion().equals("Zona 2"),"Buscar y actualizar cliente");
        rechazar(()->cc.insertar(new Cliente(0,"","","",null,null)),"Validar cliente vacío");rechazar(()->cc.insertar(c),"Evitar ID manual duplicado al crear");
        VehiculoControlador vc=new VehiculoControlador();Vehiculo v=new Vehiculo(0,"REV-CRUD","Toyota","Prueba",2020,c);verificar(vc.insertar(v)&&v.getIdVehiculo()>0,"Crear vehículo con cliente existente");v.setModelo("Actualizado");verificar(vc.actualizar(v)&&vc.buscarPorId(v.getIdVehiculo()).getClientePropietario().getCorreo().equals(c.getCorreo()),"Vehículo recupera propietario completo y se actualiza");rechazar(()->cc.eliminar(c.getId()),"No eliminar cliente con vehículos");
        rechazar(()->vc.insertar(new Vehiculo(0,"REV-CRUD","Toyota","Prueba",2020,c)),"Placa duplicada rechazada");Cliente inexistente=new Cliente();inexistente.setId(Integer.MAX_VALUE);rechazar(()->vc.insertar(new Vehiculo(0,"REV-NO","Toyota","X",2020,inexistente)),"Cliente inexistente rechazado");
        MecanicoControlador mc=new MecanicoControlador();Mecanico m=new Mecanico(0,"Luis","Prueba","5555","Motor");verificar(mc.insertar(m)&&m.getId()>0,"Crear mecánico");m.setEspecialidad("Frenos");verificar(mc.actualizar(m)&&mc.buscarPorId(m.getId()).getEspecialidad().equals("Frenos"),"Buscar y actualizar mecánico");
        ServicioControlador sc=new ServicioControlador();Servicio s=new Servicio(0,"Servicio CRUD","Descripción",12.5);verificar(sc.insertar(s)&&s.getIdServicio()>0,"Crear servicio");s.setCostoManoObra(14.5);verificar(sc.actualizar(s)&&sc.buscarPorId(s.getIdServicio()).getCostoManoObra()==14.5,"Buscar y actualizar servicio");
        RepuestoControlador rc=new RepuestoControlador();Repuesto r=new Repuesto(0,"Repuesto CRUD","Descripción",8,3);verificar(rc.insertar(r)&&r.getIdRepuesto()>0,"Crear repuesto");r.setPrecio(9);verificar(rc.actualizar(r)&&rc.buscarPorId(r.getIdRepuesto()).getPrecio()==9,"Buscar y actualizar repuesto");verificar(rc.abastecer(r.getIdRepuesto(),4)&&rc.buscarPorId(r.getIdRepuesto()).getExistencia()==7,"Abastecimiento suma unidades");
        verificar(!rc.actualizar(new Repuesto(Integer.MAX_VALUE,"No existe","",1,0))&&!rc.eliminar(Integer.MAX_VALUE),"Actualizar/eliminar ID inexistente no informa éxito");rechazar(()->rc.abastecer(r.getIdRepuesto(),-1),"Rechazar abastecimiento negativo");rechazar(()->new Repuesto(0,"X","",-1,2),"Rechazar precio negativo en constructor");rechazar(()->new Servicio(0,"X","",Double.NaN),"Rechazar monto NaN");
        verificar(!cc.listarTodos().isEmpty()&&!mc.listarTodos().isEmpty()&&!vc.listarTodos().isEmpty()&&!sc.listarTodos().isEmpty()&&!rc.listarTodos().isEmpty(),"Listar las cinco entidades de catálogo");
        verificar(vc.eliminar(v.getIdVehiculo())&&vc.buscarPorId(v.getIdVehiculo())==null,"Eliminar vehículo sin órdenes");verificar(cc.eliminar(c.getId())&&cc.buscarPorId(c.getId())==null,"Eliminar cliente sin vehículos");verificar(mc.eliminar(m.getId())&&mc.buscarPorId(m.getId())==null,"Eliminar mecánico sin órdenes");verificar(sc.eliminar(s.getIdServicio())&&sc.buscarPorId(s.getIdServicio())==null,"Eliminar servicio sin uso");verificar(rc.eliminar(r.getIdRepuesto())&&rc.buscarPorId(r.getIdRepuesto())==null,"Eliminar repuesto sin uso");
    }
    private static OrdenTrabajo nuevaOrden(){OrdenTrabajo o=new OrdenTrabajo();o.setVehiculo(new VehiculoDao().buscarPorId(1));o.setMecanico(new MecanicoDao().buscarPorId(1));o.setdescripcionProblema("Prueba de integración");new OrdenTrabajoControlador().insertar(o);return o;}
    private static void probarOrdenes()throws Exception{
        OrdenTrabajoControlador oc=new OrdenTrabajoControlador();DetalleOrdenServicioControlador ds=new DetalleOrdenServicioControlador();DetalleOrdenRepuestoControlador dr=new DetalleOrdenRepuestoControlador();RepuestoDao rd=new RepuestoDao();
        OrdenTrabajo o=nuevaOrden();verificar(o.getIdOrden()>0&&oc.buscarPorId(o.getIdOrden()).getFechaIngreso()!=null&&oc.buscarPorId(o.getIdOrden()).getEstado().equals("PENDIENTE"),"Crear orden pendiente con fecha/hora");
        rechazar(()->ds.insertar(new DetalleOrdenServicio(0,o.getIdOrden(),new ServicioDao().buscarPorId(1),10)),"No agregar detalles en PENDIENTE");
        verificar(oc.cambiarEstado(o.getIdOrden(),"EN PROCESO"),"Iniciar reparación");rechazar(()->oc.cambiarEstado(o.getIdOrden(),"FINALIZADA"),"No finalizar sin servicios");
        DetalleOrdenServicio serv=new DetalleOrdenServicio(0,o.getIdOrden(),new ServicioDao().buscarPorId(1),100);verificar(ds.insertar(serv)&&serv.getIdDetalleServicio()>0,"Asignar servicio a orden");serv.setCostoAplicado(125);verificar(ds.actualizar(serv)&&ds.buscarPorId(serv.getIdDetalleServicio()).getCostoAplicado()==125,"Actualizar detalle de servicio");
        int stock=rd.buscarPorId(1).getExistencia();DetalleOrdenRepuesto rep=new DetalleOrdenRepuesto(0,o.getIdOrden(),rd.buscarPorId(1),2,45);verificar(dr.insertar(rep)&&rd.buscarPorId(1).getExistencia()==stock-2,"Asignar repuesto y descontar inventario en transacción");verificar(oc.buscarPorId(o.getIdOrden()).getCostoTotal()==215,"Total persistido suma servicios y repuestos");
        rep.setCantidad(3);verificar(dr.actualizar(rep)&&rd.buscarPorId(1).getExistencia()==stock-3&&oc.buscarPorId(o.getIdOrden()).getCostoTotal()==260,"Actualizar cantidad ajusta stock y total");
        rechazar(()->dr.insertar(new DetalleOrdenRepuesto(0,o.getIdOrden(),rd.buscarPorId(1),stock+100,45)),"Rechazar inventario insuficiente");verificar(rd.buscarPorId(1).getExistencia()==stock-3&&oc.buscarPorId(o.getIdOrden()).getListaRepuestos().size()==1,"Rollback no deja detalle ni descuento parcial");
        verificar(dr.eliminar(rep.getIdDetalleRepuesto())&&rd.buscarPorId(1).getExistencia()==stock,"Retirar detalle devuelve unidades");verificar(ds.eliminar(serv.getIdDetalleServicio())&&oc.buscarPorId(o.getIdOrden()).getCostoTotal()==0,"Eliminar servicio recalcula total");
        serv.setIdDetalleServicio(0);verificar(ds.insertar(serv),"Reasignar servicio");rep.setIdDetalleRepuesto(0);rep.setCantidad(2);verificar(dr.insertar(rep),"Reasignar repuesto");verificar(oc.cambiarEstado(o.getIdOrden(),"FINALIZADA"),"Finalizar orden con servicios");OrdenTrabajo cerrada=oc.buscarPorId(o.getIdOrden());verificar(cerrada.getCostoTotal()==215&&cerrada.getListaServicios().size()==1&&cerrada.getVehiculo().getClientePropietario()!=null,"Recuperar orden completa con propietario, detalles y total");
        rechazar(()->oc.cambiarEstado(o.getIdOrden(),"CANCELADA"),"No cancelar orden finalizada");rechazar(()->dr.eliminar(rep.getIdDetalleRepuesto()),"No retirar repuestos de orden finalizada");verificar(!oc.eliminar(o.getIdOrden()),"No eliminar orden finalizada");
        OrdenTrabajo falsa=new OrdenTrabajo();falsa.setIdOrden(o.getIdOrden());falsa.setVehiculo(cerrada.getVehiculo());falsa.setMecanico(cerrada.getMecanico());falsa.setFechaIngreso(new java.util.Date());falsa.setdescripcionProblema("Intento");rechazar(()->new OrdenTrabajoDao().actualizar(falsa),"Protección de cierre también en DAO");
        rechazar(()->new VehiculoControlador().eliminar(1),"No eliminar vehículo con órdenes");rechazar(()->new MecanicoControlador().eliminar(1),"No eliminar mecánico con órdenes");verificar(!new VehiculoControlador().historial(1).isEmpty(),"Historial de órdenes por vehículo");
        OrdenTrabajo cancel=nuevaOrden();oc.cambiarEstado(cancel.getIdOrden(),"EN PROCESO");int antes=rd.buscarPorId(1).getExistencia();DetalleOrdenRepuesto rc=new DetalleOrdenRepuesto(0,cancel.getIdOrden(),rd.buscarPorId(1),2,45);dr.insertar(rc);verificar(oc.cambiarEstado(cancel.getIdOrden(),"CANCELADA")&&rd.buscarPorId(1).getExistencia()==antes,"Cancelar reintegra inventario");rechazar(()->oc.cambiarEstado(cancel.getIdOrden(),"CANCELADA"),"Cancelar dos veces no duplica stock");verificar(rd.buscarPorId(1).getExistencia()==antes,"Inventario estable tras segundo intento");
        OrdenTrabajo pendiente=nuevaOrden();verificar(oc.cambiarEstado(pendiente.getIdOrden(),"CANCELADA"),"Cancelar orden pendiente");OrdenTrabajo borrar=nuevaOrden();verificar(oc.eliminar(borrar.getIdOrden())&&oc.buscarPorId(borrar.getIdOrden())==null,"Eliminar orden pendiente vacía");
        Repuesto uno=new Repuesto(0,"Concurrente","",1,1);new RepuestoControlador().insertar(uno);OrdenTrabajo a=nuevaOrden(),b=nuevaOrden();oc.cambiarEstado(a.getIdOrden(),"EN PROCESO");oc.cambiarEstado(b.getIdOrden(),"EN PROCESO");ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch inicio=new CountDownLatch(1);
        try{List<Future<Boolean>> resultados=new ArrayList<>();for(OrdenTrabajo orden:List.of(a,b))resultados.add(pool.submit(()->{inicio.await();try{return new DetalleOrdenRepuestoControlador().insertar(new DetalleOrdenRepuesto(0,orden.getIdOrden(),uno,1,1));}catch(IllegalArgumentException e){return false;}}));inicio.countDown();int exitos=0;for(Future<Boolean> f:resultados)if(f.get(10,TimeUnit.SECONDS))exitos++;verificar(exitos==1&&rd.buscarPorId(uno.getIdRepuesto()).getExistencia()==0,"Dos órdenes concurrentes no consumen la misma última unidad");}finally{pool.shutdownNow();}
        OrdenTrabajo excedida=nuevaOrden();oc.cambiarEstado(excedida.getIdOrden(),"EN PROCESO");ds.insertar(new DetalleOrdenServicio(0,excedida.getIdOrden(),new ServicioDao().buscarPorId(1),99999999));int disponible=rd.buscarPorId(1).getExistencia();rechazar(()->dr.insertar(new DetalleOrdenRepuesto(0,excedida.getIdOrden(),rd.buscarPorId(1),1,2)),"Total fuera del rango DECIMAL revierte toda la transacci?n");verificar(rd.buscarPorId(1).getExistencia()==disponible && oc.buscarPorId(excedida.getIdOrden()).getListaRepuestos().isEmpty(),"Rollback por desbordamiento devuelve inventario");
        verificar(new ReporteControlador().generar(Map.of("tipo","comprobante","id",Integer.toString(o.getIdOrden()))).stream().anyMatch(l->l.equals("TOTAL Q215.00")),"Comprobante usa total final persistido");
    }
    private static HttpResponse<String> get(HttpClient client,String url)throws Exception{return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private static HttpResponse<String> post(HttpClient client,String url,String body)throws Exception{return client.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
    private static void probarHttp()throws Exception{
        ServidorWeb servidor=new ServidorWeb("127.0.0.1",0);servidor.iniciar();String url="http://127.0.0.1:"+servidor.puerto();HttpClient client=HttpClient.newHttpClient();
        try{
            verificar(get(client,url+"/salud").statusCode()==200,"HTTP salud confirma MySQL");
            String html="";for(String modulo:WebControlador.CAMPOS.keySet()){HttpResponse<String> r=get(client,url+"/?modulo="+modulo);verificar(r.statusCode()==200,"HTTP módulo "+modulo);html=r.body();}
            Matcher m=Pattern.compile("name='csrf' value='([^']+)'").matcher(html);verificar(m.find(),"Formulario incluye token CSRF");String csrf=m.group(1);
            verificar(post(client,url+"/operar","modulo=clientes&accion=eliminar&id=1").statusCode()==403,"HTTP rechaza cambios sin token");
            verificar(post(client,url+"/operar","csrf="+csrf+"&modulo=clientes&accion=guardar&id=0&nombres=Web&apellidos=Prueba&telefono=123&correo=web%40example.com").statusCode()==303,"Formulario web crea cliente mediante controlador");
            verificar(post(client,url+"/operar","csrf="+csrf+"&modulo=clientes&accion=guardar&id=0&nombres=&apellidos=&telefono=").statusCode()==400,"Formulario web muestra validación sin error interno");
            for(String tipo:List.of("ordenes","historial","inventario")){
                String params="?tipo="+tipo+"&placa=P-123ABC&limite=5";HttpResponse<String> json=get(client,url+"/api/reportes"+params);verificar(json.statusCode()==200&&json.headers().firstValue("Content-Type").orElse("").contains("application/json")&&json.body().startsWith("{\"lineas\":["),"API JSON "+tipo);
                HttpResponse<byte[]> pdf=client.send(HttpRequest.newBuilder(URI.create(url+"/reporte"+params+"&formato=pdf")).build(),HttpResponse.BodyHandlers.ofByteArray());verificar(pdf.statusCode()==200&&new String(pdf.body(),0,8,StandardCharsets.US_ASCII).equals("%PDF-1.4"),"PDF HTTP "+tipo);
            }
            verificar(get(client,url+"/reportes").statusCode()==200&&get(client,url+"/no-existe").statusCode()==404,"Navegación de reportes y página inexistente");
            verificar(Html.escapar("<script>'\"").equals("&lt;script&gt;&#39;&quot;"),"Escape HTML de valores almacenados");
            List<String> paginas=new ArrayList<>();for(int i=0;i<100;i++)paginas.add("Línea "+i+" (prueba) \\ áéñ");byte[] pdf=PdfTexto.crear(paginas);String texto=new String(pdf,StandardCharsets.ISO_8859_1);verificar(texto.contains("/Count 3"),"PDF con varias páginas");Matcher xref=Pattern.compile("startxref\\n(\\d+)\\n").matcher(texto);verificar(xref.find()&&texto.startsWith("xref",Integer.parseInt(xref.group(1))),"Índice PDF apunta a bytes correctos");Files.write(Path.of("target/prueba-reporte.pdf"),pdf);
        }finally{servidor.detener();}
    }
    private static void probarConexionFallida()throws Exception{
        String anterior=System.getProperty("taller.db.url");System.setProperty("taller.db.url","jdbc:mysql://127.0.0.1:1/inexistente?connectTimeout=500");
        try{rechazar(()->new ClienteDao().listarTodos(),"Conexión fallida se informa sin NullPointerException");ServidorWeb servidor=new ServidorWeb("127.0.0.1",0);servidor.iniciar();try{verificar(get(HttpClient.newHttpClient(),"http://127.0.0.1:"+servidor.puerto()+"/salud").statusCode()==503,"HTTP 503 si MySQL no está disponible");}finally{servidor.detener();}}
        finally{System.setProperty("taller.db.url",anterior);}
    }
}
```

## Configuración, SQL y herramientas

| Archivo | Problema → corrección |
|---|---|
| pom.xml | Faltaba fijar plugins de compilación/ejecución/empaquetado → configurar versiones instaladas y clase principal; conservar Java 26 y mysql-connector-j 9.1.0 |
| database.properties.example y .gitignore | Configuración privada mezclada con Java → plantilla sin clave y exclusión del archivo local, target y build Android |
| sql/01-esquema.sql | Separar instalación → ocho tablas y restricciones de los archivos recibidos |
| sql/02-datos-prueba.sql | Referencias 7/8 inexistentes y servicios ausentes → IDs coherentes, servicios de ejemplo y stock descontado de una orden en proceso |
| query.sql y query (1).sql de la carpeta superior | Mismos errores de datos → scripts de instalación nueva coherentes con el esquema; originales preservados en respaldo |
| android/AndroidManifest.xml | Falta cliente móvil → permiso de Internet y actividad de consulta, Android 8+ |
| Ejecutar.ps1 y herramientas/ | Falta ejecución y recuperación reproducible → scripts portables de arranque, pruebas, Android, documentación y puntos de guardado |

## Verificación y conclusión

Consulta VALIDACION.md para la comparación MySQL, clean verify, las 86 comprobaciones CRUD/órdenes y las pruebas HTTP/PDF. GUIA_EJECUCION.md explica cómo arrancar, probar Android y continuar tras una interrupción. Las pruebas visuales de navegador/dispositivo y los commits académicos en fechas distintas siguen siendo tareas manuales, expresamente separadas de las comprobaciones ejecutadas.
