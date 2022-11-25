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
}
