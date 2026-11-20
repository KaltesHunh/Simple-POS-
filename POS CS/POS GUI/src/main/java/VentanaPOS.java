import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Interfaz gráfica del punto de venta. Reutiliza la lógica de Main.
public class VentanaPOS extends JFrame {

    private List<Main.Producto> productos = new ArrayList<>();
    private final List<Main.Item> carrito = new ArrayList<>();

    private final DefaultTableModel modeloProductos = crearModelo("Libro", "Precio", "Existencias");
    private final DefaultTableModel modeloCarrito = crearModelo("Libro", "Cantidad", "Importe");
    private final JTable tablaProductos = new JTable(modeloProductos);

    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JComboBox<String> cbPago = new JComboBox<>(new String[]{"efectivo", "tarjeta"});
    private final JLabel lblSubtotal = new JLabel("Subtotal: $0.00");
    private final JLabel lblIva = new JLabel("IVA 16%: $0.00");
    private final JLabel lblTotal = new JLabel("TOTAL: $0.00");

    public VentanaPOS() {
        super("Punto de Venta - Libros");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // Izquierda: lista de libros + cantidad + botón agregar
        JButton btnAgregar = new JButton("Agregar");
        btnAgregar.addActionListener(e -> agregar());
        JPanel abajoIzq = new JPanel();
        abajoIzq.add(new JLabel("Cantidad:"));
        abajoIzq.add(spCantidad);
        abajoIzq.add(btnAgregar);

        JPanel izquierda = new JPanel(new BorderLayout());
        izquierda.setBorder(BorderFactory.createTitledBorder("Libros disponibles"));
        izquierda.add(new JScrollPane(tablaProductos), BorderLayout.CENTER);
        izquierda.add(abajoIzq, BorderLayout.SOUTH);

        // Derecha: carrito de la venta
        JPanel derecha = new JPanel(new BorderLayout());
        derecha.setBorder(BorderFactory.createTitledBorder("Venta actual"));
        derecha.add(new JScrollPane(new JTable(modeloCarrito)), BorderLayout.CENTER);

        JPanel centro = new JPanel(new GridLayout(1, 2, 8, 8));
        centro.add(izquierda);
        centro.add(derecha);
        add(centro, BorderLayout.CENTER);

        // Abajo: totales, método de pago y botones
        JButton btnCerrar = new JButton("Cerrar venta");
        JButton btnCancelar = new JButton("Cancelar venta");
        JButton btnCorte = new JButton("Corte");
        btnCerrar.addActionListener(e -> cerrarVenta());
        btnCancelar.addActionListener(e -> cancelarVenta());
        btnCorte.addActionListener(e -> hacerCorte());

        JPanel totales = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        totales.add(lblSubtotal);
        totales.add(lblIva);
        totales.add(lblTotal);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        acciones.add(new JLabel("Pago:"));
        acciones.add(cbPago);
        acciones.add(btnCerrar);
        acciones.add(btnCancelar);
        acciones.add(btnCorte);

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(totales, BorderLayout.WEST);
        sur.add(acciones, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);

        cargarInventario();
    }

    // ---------- Acciones de los botones ----------
    private void agregar() {
        int fila = tablaProductos.getSelectedRow();
        if (fila < 0) { aviso("Selecciona un libro de la lista."); return; }

        Main.Producto p = productos.get(fila);
        int cant = (int) spCantidad.getValue();
        if (!Main.hayStock(p, cant)) {
            aviso("Existencias insuficientes. Solo hay " + p.cantidad);
            return;
        }

        p.cantidad -= cant;   // se descuenta en memoria; se guarda al cerrar la venta
        carrito.add(new Main.Item(p.nombre, cant, p.precio));
        refrescar();
    }

    private void cerrarVenta() {
        if (carrito.isEmpty()) { aviso("No hay libros en la venta."); return; }

        double total = Main.calcularTotal(Main.calcularSubtotal(carrito));
        String pago = (String) cbPago.getSelectedItem();

        int r = JOptionPane.showConfirmDialog(this,
            String.format("Total: $%.2f%nPago: %s%n¿Cerrar venta?", total, pago),
            "Cerrar venta", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        try {
            Main.guardarProductos(productos, Main.PRODUCTOS);
            Main.registrarVenta(carrito, total, pago, Main.VENTAS_ACTUAL);
        } catch (IOException e) {
            aviso("Error al guardar: " + e.getMessage());
            return;
        }
        carrito.clear();
        refrescar();
        aviso("Venta cerrada.");
    }

    private void cancelarVenta() {
        carrito.clear();
        cargarInventario();   // vuelve a leer el archivo: restaura las existencias
    }

    private void hacerCorte() {
        if (Main.corte(Main.VENTAS_ACTUAL, Main.CARPETA_BD)) aviso("Corte realizado.");
        else aviso("No hay ventas para cortar.");
    }

    // ---------- Apoyo ----------
    private void cargarInventario() {
        try {
            productos = Main.cargarProductos(Main.PRODUCTOS);
        } catch (IOException | RuntimeException e) {
            productos = new ArrayList<>();
            aviso("No se pudo leer " + Main.PRODUCTOS + ": " + e.getMessage());
        }
        refrescar();
    }

    // Redibuja las tablas y los totales con los datos actuales
    private void refrescar() {
        modeloProductos.setRowCount(0);
        for (Main.Producto p : productos)
            modeloProductos.addRow(new Object[]{p.nombre, String.format("$%.2f", p.precio), p.cantidad});

        modeloCarrito.setRowCount(0);
        for (Main.Item it : carrito)
            modeloCarrito.addRow(new Object[]{it.nombre, it.cantidad, String.format("$%.2f", it.precio * it.cantidad)});

        double subtotal = Main.calcularSubtotal(carrito);
        double total = Main.calcularTotal(subtotal);
        lblSubtotal.setText(String.format("Subtotal: $%.2f", subtotal));
        lblIva.setText(String.format("IVA 16%%: $%.2f", total - subtotal));
        lblTotal.setText(String.format("TOTAL: $%.2f", total));
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje);
    }

    // Tabla de solo lectura (no se puede editar a mano)
    private static DefaultTableModel crearModelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
    }

    public static void main(String[] args) {
        new File(Main.CARPETA_BD).mkdirs();
        SwingUtilities.invokeLater(() -> new VentanaPOS().setVisible(true));
    }
}
