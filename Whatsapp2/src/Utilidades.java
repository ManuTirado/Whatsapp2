import java.sql.*;
import java.util.Scanner;

public class Utilidades {
    /***
     * Valida la entrada de un entero en un rango dado, ambos incluidos
     * @param min Valor mínimo, incluido
     * @param max Valor máximo, incluido
     * @return entero en el rango dado
     */
    public static int validarOpcion(int min, int max) {
        Scanner sc = new Scanner(System.in);
        int opc;
        do {
            System.out.print("==> ");
            opc = sc.nextInt();
        } while (opc < min || opc > max);
        return opc;
    }

    /***
     * Función que valida la entrada de un número entero
     * @return Número entero validado
     */
    public static int validarEntero() {
        Scanner sc = new Scanner(System.in);
        int numero = 0;
        boolean correcto = false;
        do {
            try {
                System.out.print("==> ");
                numero = sc.nextInt();
                correcto = true;
            } catch (Exception e) {
                System.out.println("Valor no válido");
                sc.nextLine();
            }
        } while (!correcto);
        return numero;
    }

    public static void main(String[] args) throws SQLException {
        Connection connection = Main.conectar();

        /*
        DatabaseMetaData dbmd = connection.getMetaData();
        ResultSet rs = dbmd.getTables(null,null, "%", null);

        while (rs.next()) {
            System.out.println(rs.getString(3));
        }
         */
        Statement statement = null;
        statement = connection.createStatement();
        String sql = "SELECT * FROM ad2223_acastro.Mensajes;";
        ResultSet rs = statement.executeQuery(sql);
        ResultSetMetaData rsm = rs.getMetaData();
        while (rs.next()) {
            for (int i = 1; i <= rsm.getColumnCount(); i++) {
                System.out.print(rsm.getColumnName(i) + ": " + rs.getString(i) + "    ");
            }
            System.out.println();
        }
    }
}
