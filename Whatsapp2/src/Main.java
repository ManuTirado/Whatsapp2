import java.sql.*;
import java.util.Scanner;

public class Main {

    //URL del servidor
    private static final String SERVIDOR = "jdbc:mysql://dns11036.phdns11.es";
    //Nombre usuario
    private static String user = "mtirado";
    //Contraseña
    private static String password = "1234";
    //Nombre de la base de datos del emisor
    private static String baseDatos = "ad2223_mtirado";
    //Nombre de la base de datos del receptor
    private static String baseDatosDestino = "ad2223_acastro";

    private static Connection connection;
    private static Statement st;

    public static void main(String[] args) {
        login();

        try {
            connection = conectar();
            if (connection != null) {
                st = connection.createStatement();

                crearTabla("Contactos", new String[]{"IdContacto int PRIMARY KEY AUTO_INCREMENT", "NombreContacto varchar(30) NOT NULL", "IsBloqueado bit NOT NULL"});
                crearTabla("Mensajes", new String[]{"IdMensaje int PRIMARY KEY auto_increment", "Origen varchar(30) NOT NULL", "Destino varchar(30) NOT NULL",
                        "FechaHora timestamp NOT NULL", "Texto text NOT NULL", "IsLeido bit", "IsRecibido bit", "IdContacto int NOT NULL", "Foreign Key(IdContacto)references Contactos(IdContacto)"});

                int opc;
                do {
                    mostrarMenuPrincipal();
                    mostrarNumeroMensajesSinLeer();
                    opc = Utilidades.validarOpcion(0, 3);
                    switch (opc) {
                        case 1:     // Contactos
                            contactos();
                            break;
                        case 2:     // Añadir Contacto
                            anadirContacto();
                            break;
                        case 3:    // Bloquear/Desbloquear Contacto
                            bloquearDesbloquearContacto();
                            break;
                        case 0:    // Salir
                            System.out.println("Adios!");
                            break;
                    }
                } while (opc != 0);

                st.close();
            }

            if (st != null) {
                connection.close();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Método que mostrará lso contactos que tenemos en nuestro chat
     * Precondición: Ninguna
     * Postcondición: Ninguna
     */
    private static void contactos() {
        try {
            boolean hayContactos = false;
            ResultSet rs = obtenerContactosSinBloquear();
            System.out.println("IdContacto    Nombre");
            while (rs.next()) {
                hayContactos = true;
                System.out.println();
                System.out.println("     " + rs.getString("IdContacto") + "        " + rs.getString("NombreContacto"));
            }
            if (hayContactos) {
                Scanner sc = new Scanner(System.in);
                System.out.println();
                System.out.println("Escoja un contacto por el id (Déjelo en blanco para volver atrás) ");
                System.out.print("==> ");
                String leido = sc.nextLine();
                if (!leido.isBlank() || !leido.isEmpty()) {
                    int id = Integer.parseInt(leido);
                    boolean salirChat = false;
                    while (!salirChat) {
                        salirChat = abrirChat(id);
                    }
                }
            } else {
                System.out.println();
                System.out.println("No tiene ningún contacto, pruebe a añadir alguno");
                System.out.println();
            }

        } catch (SQLException sqlException) {
            System.err.println("Error del sql al intentar mostrar los contactos");
        }
    }

    /**
     * Abre el chat del contacto seleccionado y muestra tu historial de chat con él y permite mandar mensajes
     * @param id id del contacto con el que vamos a chatear
     * @return devuelve si el mensaje que escribimos esta vacio o no
     */
    private static boolean abrirChat(int id) {
        Scanner sc = new Scanner(System.in);
        String nombreContacto = conseguirNombrePorId(id);
        mostrarHistorialMensajes(id);
        System.out.println("No escriba nada para salir");
        System.out.print("==> ");
        String mensaje = sc.nextLine();
        if (!mensaje.isEmpty() || !mensaje.isBlank()) {
            insertarMensajeEnTabla(nombreContacto, mensaje, id);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Este método introduce un mensaje en la tabla de mensajes de los usuarios tanto del receptor como del emisor
     * Precondición: Ninguna
     * Postcondición: Ninguna
     * @param nombreContacto nombre del contacto al que le enviamos el mensaje
     * @param mensaje mensaje el cual queremos enviar
     * @param id id del contacto al que queremos enviar el mensaje
     */
    private static void insertarMensajeEnTabla(String nombreContacto, String mensaje, int id) {
        baseDatosDestino = "ad2223_" + nombreContacto;
        String sql = "INSERT INTO " + baseDatosDestino + ".Mensajes (Origen, Destino,  Texto, IsLeido, IsRecibido, IdContacto) " +
                "VALUES ('" + user + "' , '" + nombreContacto + "' ,  '" + mensaje + "' , 0, 1, " + id + ")";
        try {
            //Actualiza la tabla mensajes del receptor
            st.executeUpdate(sql);

            sql = "INSERT INTO " + baseDatos + ".Mensajes (Origen, Destino,  Texto, IsLeido, IsRecibido, IdContacto) " +
                    "VALUES ('" + user + "' , '" + nombreContacto + "' ,  '" + mensaje + "' , 0, 0, " + id + ")";
            try {
                //Actualiza la tabla de mensajes del emisor
                st.executeUpdate(sql);
            } catch (SQLException e) {
                System.err.println("No se ha podido guardar el mensaje en tu base de datos");
                throw new RuntimeException(e);
            }
        } catch (SQLException sqlException) {
            System.err.println("No se ha podido enviar el mensaje, lo mismo la otra persona no la tiene añadida como contacto");
            throw new RuntimeException(sqlException);
        }
    }

    /**
     * Muestra el chat entero de las dos personas
     * Precondición: Ninguna
     *  Postcondición: Ninguna
     * @param idConatcto id del contacto con el que chateamos
     */
    private static void mostrarHistorialMensajes(int idConatcto) {
        String ANSI_RED = "\u001B[31m";
        String ANSI_RESET = "\u001B[0m";
        String nombreContacto = conseguirNombrePorId(idConatcto);
        baseDatosDestino = "ad2223_" + nombreContacto;
        actualizarLeidos(nombreContacto);
        System.out.println();
        System.out.println("/ / / / / " + nombreContacto + " / / / / /");
        String sql = "SELECT Origen, Destino, Texto, FechaHora, IsRecibido, IsLeido FROM " + baseDatosDestino + ".Mensajes WHERE Destino = '" + user + "' OR Origen = '" + user + "';";
        try {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                String origen = rs.getString("Origen");
                String isLeido = rs.getString("IsLeido");
                String color;
                if (origen.equals(user)) {
                    color = !isLeido.equals("0")? ANSI_RESET:ANSI_RED;
                } else {
                    color = ANSI_RESET;
                }
                System.out.print(color + origen + " -> " + rs.getTimestamp("FechaHora") + "  " + rs.getString("Texto") + "  ");
                if (Integer.parseInt(rs.getString("IsRecibido")) == 1) {
                    System.out.print("✔");
                }
                if (Integer.parseInt(isLeido) == 1) {
                    System.out.print("✔");
                }
                System.out.println(ANSI_RESET);
            }
            System.out.println("/ / / / / / / / / / / / / / /");
        } catch (SQLException sqlException) {
            System.err.println(sqlException.getMessage());
        }
    }

    /**
     * Actualiza la tabla de mensajes cuando el usuario receptor abre el chat y lee el mensaje
     * Precondición: Ninguna
     * Postcondición: Ninguna
     * @param nombreContatco nombre del contacto con el que hemos chateado
     */
    private static void actualizarLeidos(String nombreContatco) {
        String sql = "UPDATE " + baseDatos + ".Mensajes SET IsLeido = 1 WHERE Origen = '" + nombreContatco + "';";
        try {
            st.executeUpdate(sql);
        } catch (SQLException sqlException) {
            System.err.println(sqlException.getMessage());
        }
    }

    /**
     * Con el id conseguimos el nombre del contacto que tenga ese mismo id
     * Precondición: Ninguna
     * Postcondición: Ninguna
     * @param id numero de identificación de cada contacto
     * @return devuelve el nombre del contacto que tiene el mismo id que le pasamos por parámetros
     */
    private static String conseguirNombrePorId(int id) {
        String nombreContacto = "";
        String sql = "SELECT NombreContacto FROM " + baseDatos + ".Contactos WHERE IdContacto = " + id + ";";
        try {
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                nombreContacto = rs.getString("NombreContacto");
            }

        } catch (SQLException sqlException) {
            System.err.println(sqlException.getMessage());
        }


        return nombreContacto;
    }

    /**
     * Muestra una lista con todos los contactos que tenemos agregados en la tabla de contactos ya estén bloqeuados o no
     * Precondición: Ninguna
     * Postcondición: Ninguna
     * @param rs result set para poder mostrar los resultados de la query
     */
    private static void mostrarContactos(ResultSet rs) {
        try {
            ResultSetMetaData rsm = rs.getMetaData();
            for (int i = 1; i <= rsm.getColumnCount(); i++) {
                System.out.print(rsm.getColumnName(i) + "    ");
            }
            System.out.println();
            while (rs.next()) {
                System.out.println();
                for (int i = 1; i <= rsm.getColumnCount(); i++) {
                    System.out.print("     " + rs.getString(i) + "     ");
                }
                System.out.println();
            }
            System.out.println();
        } catch (SQLException sqlException) {
            System.err.println("Error del sql al intentar mostrar los contactos");
        }
    }

    /**
     * Método que obtiene todos los contactos y los guarda en un result set para poder enviarlos
     * Precondición: Ninguna
     * Postcondición: Ninguna
     * @return result set con todos la tabla contactos y todos sus campos
     */
    private static ResultSet obtenerTodosContactos() {
        ResultSet rs = null;
        String sql = "SELECT IdContacto, NombreContacto, IsBloqueado FROM " + baseDatos + ".Contactos";
        try {
            rs = st.executeQuery(sql);
        } catch (SQLException sqlException) {
            System.err.println("Error del sql al intentar obtener los contactos");
            throw new RuntimeException(sqlException);
        }
        return rs;
    }

    /**
     * Obtiene un result set en el que solo se encuentran los contactos sin bloquear
     * Precondición: Ninguna
     * Postcondición: Ninguna
     * @return result set con la tabal contactos y todos sus campos de los contactos que no estén bloqueados
     */
    private static ResultSet obtenerContactosSinBloquear() {
        ResultSet rs = null;
        String sql = "SELECT IdContacto, NombreContacto FROM " + baseDatos + ".Contactos WHERE IsBloqueado = 0";
        try {
            rs = st.executeQuery(sql);
        } catch (SQLException sqlException) {
            //System.err.println("Error del sql al intentar obtener los contactos");
            throw new RuntimeException(sqlException);
        }
        return rs;
    }

    /**
     * Método que nos permite tanto bloquear como desbloquear un contacto. Nos mostrará una lista con todos los contactos
     * tanto los bloqueados como los no bloqueados. Luego elegiremos un contacto, en caso de que este esté bloqueado lo
     * desbloquearemos, en caso contrario lo bloquearemos
     * Precondición: Tener algún contacto agregado
     * Postcondición: Ninguna
     */
    private static void bloquearDesbloquearContacto() {
        Scanner sc = new Scanner(System.in);
        int idContacto, isBloqueado = 0;
        String sql, sqlQuery;

        mostrarContactos(obtenerTodosContactos());

        System.out.println("Diga el id del contacto que quiere bloquear (Déjelo en blanco para volver atrás) ");
        System.out.print("==> ");
        String leido = sc.nextLine();
        if (!leido.isBlank() || !leido.isEmpty()) {
            idContacto = Integer.parseInt(leido);
            sqlQuery = "SELECT IsBloqueado FROM " + baseDatos + ".Contactos WHERE IdContacto= " + idContacto + ";";
            try {
                ResultSet rs = st.executeQuery(sqlQuery);
                if (rs.next()) {
                    isBloqueado = Integer.parseInt(rs.getString("IsBloqueado"));
                }
                if (isBloqueado == 0) {
                    sql = "UPDATE " + baseDatos + ".Contactos SET IsBloqueado = 1 WHERE IdContacto= " + idContacto + ";";
                } else {
                    sql = "UPDATE " + baseDatos + ".Contactos SET IsBloqueado = 0 WHERE IdContacto= " + idContacto + ";";
                }
                st.executeUpdate(sql);
                System.out.println("Se ha actualizado con éxito :D");
            } catch (SQLException e) {
                System.err.println("Ha ocurrido un error muy grave :(");
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Este método nos da la opción de añadir un nuevo contacto a nuestra tabla de contactos
     * No nos permitirá introducir contactos repetidos
     * Precondición: Ninguna
     * Postcondición: Ninguna
     */
    private static void anadirContacto() {
        String nombreContacto;
        Scanner sc = new Scanner(System.in);

        System.out.println("    Nombre del contacto que quiere añadir (Déjelo en blanco para volver atrás)");
        System.out.print("==> ");
        nombreContacto = sc.nextLine();
        if (!nombreContacto.isBlank() || !nombreContacto.isEmpty()) {
            if (!existeContacto(nombreContacto)) {
                String sql = "INSERT INTO " + baseDatos + ".Contactos (NombreContacto, IsBloqueado) values ('" + nombreContacto + "', 0);";
                try {
                    st.executeUpdate(sql);
                } catch (SQLException e) {
                    System.err.println("No se han podido insertar el mensaje en la base de datos");
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("El contacto ya existe");
            }
        }
    }

    /**
     * Este método comprueba si el contacto que queremos añadir ya existe o no.
     * Precondición: Ninguna
     * Postcondición: Ninguna
     * @param nombreContacto nombre del contacto qeu queremos añadir
     * @return devuelve si el contacto que queremos añadir existe o no
     */
    private static boolean existeContacto(String nombreContacto) {
        boolean existe = false;
        ResultSet rs = obtenerTodosContactos();
        try {
            while (rs.next() && !existe) {
                if (rs.getString("NombreContacto").equals(nombreContacto)) {
                    existe = true;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return existe;
    }

    /**
     * Muestra un menu con las opciones que le mostramos al usuario
     * Precondición: Ninguna
     * Postcondición: Ninguna
     */
    private static void mostrarMenuPrincipal() {
        System.out.println("- - - - - - " + user + " - - - - - -");
        System.out.println("1 - Contactos");
        System.out.println("2 - Añadir Contacto");
        System.out.println("3 - Bloquear Contacto");
        System.out.println("0 - Salir");
        System.out.println();
    }

    /**
     * Muestra la cantidad de mensajes que tenemos sin leer en nuestros chats
     * Precondición: Ninguna
     * Postcondición: Ninguna
     */
    private static void mostrarNumeroMensajesSinLeer() {
        int mensajesSinLeer = 0;
        try {
            String sql = "SELECT Origen, Destino, IsLeido FROM " + baseDatos + ".Mensajes WHERE Destino = '" + user + "' AND IsLeido = 0;";
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                mensajesSinLeer++;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Tiene " + mensajesSinLeer + " mensajes sin leer");
    }

    /**
     * Nos pide nuestro usuario y contraseña par aingresar a nuestra abse de datos
     * Precondición: Tener una base de datos
     * Postcondición: Ninguna
     */
    private static void login() {
        Scanner sc = new Scanner(System.in);
        System.out.println("- - - - - - WhatsApp2 - - - - - -");
        System.out.println();
        System.out.println("    Usuario:");
        System.out.print("==> ");
        user = sc.nextLine();
        System.out.println("    Contraseña:");
        System.out.print("==> ");
        password = sc.nextLine();
        baseDatos = "ad2223_" + user;
    }

    /***
     * Función que crea y devuelve una connection al servidor,
     * los datos de acceso los obtiene de las propiedades finales de la clase
     * Precondición: Ninguna
     * Postcondición: Ninguna
     * @return Connection
     */
    public static Connection conectar() {
        Connection connection;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(SERVIDOR, "ad2223_" + user, password);
            System.out.println("Bienvenido " + user + " 👋👋🔥");
        } catch (SQLException e) {
            System.out.println("No se ha podido conectar a la base de datos");
            connection = null;
        } catch (ClassNotFoundException e) {
            System.out.println("Error con el jdbc driver");
            connection = null;
        }
        return connection;
    }


    /***
     * Procedimiento que crea una tabla en la base de datos definida en las propiedades finales de la clase
     * Precondición: Ninguna
     * Postcondición: Ninguna
     * @param tabla Nombre tabla
     * @param campos Nombre campo + tipo de dato + (si procede) Extras como AUTO_INCREMENT...
     */
    private static void crearTabla(String tabla, String[] campos) {
        String sql = "CREATE TABLE IF NOT EXISTS " + baseDatos + "." + tabla + "(";
        sql += campos[0];
        for (int i = 1; i < campos.length; i++) {
            sql += "," + campos[i];
        }
        sql += " )";
        //System.out.println(sql);
        try {
            st.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}