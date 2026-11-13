import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void subtotalSumaVariosLibros() {
        List<Main.Item> carrito = List.of(
            new Main.Item("A", 2, 100.0),   // 200
            new Main.Item("B", 1, 50.0));   // 50
        assertEquals(250.0, Main.calcularSubtotal(carrito), 0.001);
    }

    @Test
    void subtotalDeCarritoVacioEsCero() {
        assertEquals(0.0, Main.calcularSubtotal(new ArrayList<>()), 0.001);
    }

    @Test
    void totalAgregaIvaDel16() {
        assertEquals(116.0, Main.calcularTotal(100.0), 0.001);
    }

    @Test
    void buscarIgnoraMayusculas() {
        List<Main.Producto> l = List.of(new Main.Producto("El principito", 120, 5));
        assertNotNull(Main.buscar(l, "EL PRINCIPITO"));
    }

    @Test
    void buscarDevuelveNullSiNoExiste() {
        assertNull(Main.buscar(new ArrayList<>(), "X"));
    }

    @Test
    void hayStockValidaCantidades() {
        Main.Producto p = new Main.Producto("L", 10, 3);
        assertTrue(Main.hayStock(p, 3));
        assertFalse(Main.hayStock(p, 4));
        assertFalse(Main.hayStock(p, 0));
        assertFalse(Main.hayStock(p, -1));
        assertFalse(Main.hayStock(null, 1));
    }

    @Test
    void soloAceptaEfectivoOTarjeta() {
        assertTrue(Main.metodoPagoValido("efectivo"));
        assertTrue(Main.metodoPagoValido("tarjeta"));
        assertFalse(Main.metodoPagoValido("cheque"));
    }

    @Test
    void guardarYCargarMantieneDatos(@TempDir Path dir) throws Exception {
        String ruta = dir.resolve("p.txt").toString();
        Main.guardarProductos(List.of(new Main.Producto("Libro", 99.5, 3)), ruta);

        Main.Producto p = Main.cargarProductos(ruta).get(0);
        assertEquals("Libro", p.nombre);
        assertEquals(99.5, p.precio, 0.001);
        assertEquals(3, p.cantidad);
    }

    @Test
    void corteRenombraElArchivoActual(@TempDir Path dir) throws Exception {
        Path actual = dir.resolve("ventas_actual.txt");
        Files.writeString(actual, "venta");

        assertTrue(Main.corte(actual.toString(), dir.toString()));
        assertFalse(Files.exists(actual));   // la próxima venta creará uno nuevo
    }

    @Test
    void corteSinVentasDevuelveFalse(@TempDir Path dir) {
        assertFalse(Main.corte(dir.resolve("no_existe.txt").toString(), dir.toString()));
    }
}
