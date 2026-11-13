import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {

    static final String PRODUCTOS = "productos.txt";       // inventario (editable)
    static final String CARPETA_BD = "bd";                 // "base de datos" local
    static final String VENTAS_ACTUAL = CARPETA_BD + "/ventas_actual.txt";
    static final double IVA = 0.16;

    static Scanner in = new Scanner(System.in);

    // Un producto del inventario: nombre,precio,cantidad
    static class Producto {
        String nombre; double precio; int cantidad;
        Producto(String nombre, double precio, int cantidad) {
            this.nombre = nombre; this.precio = precio; this.cantidad = cantidad;
        }
    }

    // Un renglón de la venta
    static class Item {
        String nombre; int cantidad; double precio;
        Item(String nombre, int cantidad, double precio) {
            this.nombre = nombre; this.cantidad = cantidad; this.precio = precio;
        }
    }

    public static void main(String[] args) throws IOException {
        new File(CARPETA_BD).mkdirs();   // crea la carpeta si no existe

        vender();                        // al abrir, inicia una venta

        while (true) {
            System.out.println("\n1) Nueva venta  2) Corte  3) Salir");
            String op = in.nextLine().trim();
            if (op.equals("1")) vender();
            else if (op.equals("2")) {
                if (corte(VENTAS_ACTUAL, CARPETA_BD)) System.out.println("Corte realizado.");
                else System.out.println("No hay ventas para cortar.");
            }
            else if (op.equals("3")) break;
        }
    }

    // ---------- LÓGICA PURA (fácil de probar) ----------
    static double calcularSubtotal(List<Item> carrito) {
        double s = 0;
        for (Item it : carrito) s += it.precio * it.cantidad;
        return s;
    }

    static double calcularTotal(double subtotal) {
        return subtotal * (1 + IVA);
    }

    static boolean hayStock(Producto p, int cantidad) {
        return p != null && cantidad > 0 && cantidad <= p.cantidad;
    }

    static boolean metodoPagoValido(String pago) {
        return pago.equals("efectivo") || pago.equals("tarjeta");
    }

    // ---------- VENTA ----------
    static void vender() throws IOException {
        List<Producto> productos = cargarProductos(PRODUCTOS);
        List<Item> carrito = new ArrayList<>();

        System.out.println("\n=== NUEVA VENTA ===");
        while (true) {
            System.out.println("\nDisponibles:");
            for (Producto p : productos)
                if (p.cantidad > 0)
                    System.out.printf("  %s - $%.2f (quedan %d)%n", p.nombre, p.precio, p.cantidad);

            System.out.print("Libro a vender (o 'fin'): ");
            String nombre = in.nextLine().trim();
            if (nombre.equalsIgnoreCase("fin")) break;

            Producto p = buscar(productos, nombre);
            if (p == null || p.cantidad <= 0) {
                System.out.println("No existe o no hay existencias.");
                continue;
            }

            System.out.print("Cantidad: ");
            int cant;
            try { cant = Integer.parseInt(in.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Número inválido."); continue; }

            if (!hayStock(p, cant)) {
                System.out.println("Cantidad inválida. Solo hay " + p.cantidad);
                continue;
            }

            p.cantidad -= cant;   // se descuenta en memoria; se guarda al cerrar la venta
            carrito.add(new Item(p.nombre, cant, p.precio));
        }

        if (carrito.isEmpty()) { System.out.println("Venta vacía."); return; }

        // Sumatoria + IVA
        double subtotal = calcularSubtotal(carrito);
        double total = calcularTotal(subtotal);
        double iva = total - subtotal;

        System.out.printf("%nSubtotal: $%.2f%nIVA 16%%: $%.2f%nTOTAL:    $%.2f%n", subtotal, iva, total);

        System.out.print("¿Cerrar venta? (s/n): ");
        if (!in.nextLine().trim().equalsIgnoreCase("s")) {
            System.out.println("Venta cancelada.");
            return;
        }

        // Método de pago
        String pago;
        while (true) {
            System.out.print("Método de pago (efectivo/tarjeta): ");
            pago = in.nextLine().trim().toLowerCase();
            if (metodoPagoValido(pago)) break;
            System.out.println("Solo se acepta efectivo o tarjeta.");
        }

        guardarProductos(productos, PRODUCTOS);   // actualiza inventario

        // Escribe en el archivo de ventas (true = agregar al final)
        try (FileWriter fw = new FileWriter(VENTAS_ACTUAL, true)) {
            for (Item it : carrito)
                fw.write(it.nombre + " x" + it.cantidad + "\n");
            fw.write(String.format("TOTAL: $%.2f | PAGO: %s%n", total, pago.toUpperCase()));
            fw.write("----------\n");
        }
        System.out.println("Venta cerrada.");
    }

    // ---------- CORTE ----------
    // Renombra ventas_actual.txt; la siguiente venta creará uno nuevo.
    // Devuelve false si no había ventas que cortar.
    static boolean corte(String archivoActual, String carpeta) {
        File actual = new File(archivoActual);
        if (!actual.exists()) return false;

        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File cerrado = new File(carpeta + "/corte_" + fecha + ".txt");
        return actual.renameTo(cerrado);
    }

    // ---------- INVENTARIO ----------
    static Producto buscar(List<Producto> lista, String nombre) {
        for (Producto p : lista)
            if (p.nombre.equalsIgnoreCase(nombre)) return p;
        return null;
    }

    static List<Producto> cargarProductos(String ruta) throws IOException {
        List<Producto> lista = new ArrayList<>();
        for (String linea : Files.readAllLines(Paths.get(ruta))) {
            if (linea.trim().isEmpty()) continue;
            String[] partes = linea.split(",");
            lista.add(new Producto(
                partes[0].trim(),
                Double.parseDouble(partes[1].trim()),
                Integer.parseInt(partes[2].trim())));
        }
        return lista;
    }

    static void guardarProductos(List<Producto> lista, String ruta) throws IOException {
        try (PrintWriter pw = new PrintWriter(ruta)) {
            for (Producto p : lista)
                pw.println(p.nombre + "," + String.format(Locale.US, "%.2f", p.precio) + "," + p.cantidad);
        }
    }
}
