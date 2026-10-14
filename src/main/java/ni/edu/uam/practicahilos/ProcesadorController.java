
package ni.edu.uam.practicahilos;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class ProcesadorController {

    @FXML private Label lblEstado;
    @FXML private Label lblEstadoClientes;
    @FXML private Label lblEstadoProductos;
    @FXML private Label lblPorcentajeClientes;
    @FXML private Label lblPorcentajeProductos;

    @FXML private ProgressBar barraClientes;
    @FXML private ProgressBar barraProductos;

    @FXML private Button btnIniciarClientes;
    @FXML private Button btnPausarClientes;
    @FXML private Button btnReiniciarClientes;

    @FXML private Button btnIniciarProductos;
    @FXML private Button btnPausarProductos;
    @FXML private Button btnReiniciarProductos;

    private Proceso procesoClientes;
    private Proceso procesoProductos;

    @FXML
    private void initialize() {
        actualizarBotonesClientes();
        actualizarBotonesProductos();
    }

    @FXML
    private void iniciarClientes() {
        iniciarProceso(true);
    }

    @FXML
    private void iniciarProductos() {
        iniciarProceso(false);
    }

    private void iniciarProceso(boolean esCliente) {

        Proceso actual = esCliente
                ? procesoClientes
                : procesoProductos;

        if (actual != null && actual.enCurso) {
            return;
        }

        int total = esCliente ? 100 : 50;
        long espera = esCliente ? 100 : 180;

        Proceso nuevo = new Proceso(esCliente, total, espera);

        if (esCliente) {
            procesoClientes = nuevo;
            barraClientes.setProgress(0);
            lblPorcentajeClientes.setText("0%");
            lblEstadoClientes.setText("Procesando clientes...");
            actualizarBotonesClientes();
        } else {
            procesoProductos = nuevo;
            barraProductos.setProgress(0);
            lblPorcentajeProductos.setText("0%");
            lblEstadoProductos.setText("Procesando productos...");
            actualizarBotonesProductos();
        }

        actualizarEstadoGeneral();

        nuevo.enCurso = true;
        nuevo.hilo = new Thread(
                nuevo,
                esCliente ? "Hilo-Clientes" : "Hilo-Productos"
        );
        nuevo.hilo.setDaemon(true);
        nuevo.hilo.start();

        if (esCliente) {
            actualizarBotonesClientes();
        } else {
            actualizarBotonesProductos();
        }
    }

    @FXML
    private void pausarClientes() {
        cambiarPausa(true);
    }

    @FXML
    private void pausarProductos() {
        cambiarPausa(false);
    }

    private void cambiarPausa(boolean esCliente) {

        Proceso proceso = esCliente
                ? procesoClientes
                : procesoProductos;

        if (proceso == null || !proceso.enCurso) {
            return;
        }

        synchronized (proceso.monitor) {
            if (proceso.pausado) {
                proceso.pausado = false;
                proceso.monitor.notifyAll();
            } else {
                proceso.pausado = true;
            }
        }

        if (esCliente) {
            lblEstadoClientes.setText(
                    proceso.pausado
                            ? "Pausado en " + proceso.progreso + "%"
                            : "Procesando clientes..."
            );
            actualizarBotonesClientes();
        } else {
            lblEstadoProductos.setText(
                    proceso.pausado
                            ? "Pausado en "
                            + proceso.getPorcentaje() + "%"
                            : "Procesando productos..."
            );
            actualizarBotonesProductos();
        }

        actualizarEstadoGeneral();
    }

    @FXML
    private void reiniciarClientes() {
        reiniciarProceso(true);
    }

    @FXML
    private void reiniciarProductos() {
        reiniciarProceso(false);
    }

    private void reiniciarProceso(boolean esCliente) {

        Proceso anterior = esCliente
                ? procesoClientes
                : procesoProductos;

        if (anterior != null) {
            anterior.cancelar();
        }

        if (esCliente) {
            procesoClientes = null;
            barraClientes.setProgress(0);
            lblPorcentajeClientes.setText("0%");
            lblEstadoClientes.setText("Esperando inicio");
            actualizarBotonesClientes();
        } else {
            procesoProductos = null;
            barraProductos.setProgress(0);
            lblPorcentajeProductos.setText("0%");
            lblEstadoProductos.setText("Esperando inicio");
            actualizarBotonesProductos();
        }

        actualizarEstadoGeneral();
    }

    private void actualizarBotonesClientes() {

        boolean activo = procesoClientes != null
                && procesoClientes.enCurso;

        btnIniciarClientes.setDisable(activo);
        btnPausarClientes.setDisable(!activo);
        btnReiniciarClientes.setDisable(procesoClientes == null);

        btnPausarClientes.setText(
                activo && procesoClientes.pausado
                        ? "Reanudar clientes"
                        : "Pausar clientes"
        );
    }

    private void actualizarBotonesProductos() {

        boolean activo = procesoProductos != null
                && procesoProductos.enCurso;

        btnIniciarProductos.setDisable(activo);
        btnPausarProductos.setDisable(!activo);
        btnReiniciarProductos.setDisable(procesoProductos == null);

        btnPausarProductos.setText(
                activo && procesoProductos.pausado
                        ? "Reanudar productos"
                        : "Pausar productos"
        );
    }

    private void actualizarEstadoGeneral() {
        lblEstado.setText(
                "Clientes: " + lblEstadoClientes.getText()
                        + " | Productos: "
                        + lblEstadoProductos.getText()
        );
    }

    public void detenerAlCerrar() {
        if (procesoClientes != null) {
            procesoClientes.cancelar();
        }

        if (procesoProductos != null) {
            procesoProductos.cancelar();
        }
    }

    private class Proceso implements Runnable {

        private final boolean esCliente;
        private final int total;
        private final long espera;

        private final Object monitor = new Object();

        private volatile boolean pausado;
        private volatile boolean cancelado;
        private volatile boolean enCurso;
        private volatile int progreso;

        private Thread hilo;

        private Proceso(
                boolean esCliente,
                int total,
                long espera
        ) {
            this.esCliente = esCliente;
            this.total = total;
            this.espera = espera;
        }

        @Override
        public void run() {

            try {
                while (progreso < total) {

                    synchronized (monitor) {
                        while (pausado && !cancelado) {
                            monitor.wait();
                        }

                        if (cancelado) {
                            return;
                        }
                    }

                    Thread.sleep(espera);

                    int avanceActual;

                    synchronized (monitor) {
                        if (cancelado) {
                            return;
                        }

                        if (pausado) {
                            continue;
                        }

                        progreso++;
                        avanceActual = progreso;
                    }

                    Platform.runLater(() -> mostrarAvance(avanceActual));
                }

                Platform.runLater(() -> finalizarProceso());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                if (!cancelado) {
                    Platform.runLater(() -> mostrarDetenido());
                }

            } finally {
                enCurso = false;
            }
        }

        private void mostrarAvance(int avance) {

            if (!esProcesoActual()) {
                return;
            }

            int porcentaje = avance * 100 / total;

            if (esCliente) {
                barraClientes.setProgress(avance / (double) total);
                lblPorcentajeClientes.setText(porcentaje + "%");
            } else {
                barraProductos.setProgress(avance / (double) total);
                lblPorcentajeProductos.setText(porcentaje + "%");
            }
        }

        private void finalizarProceso() {

            if (!esProcesoActual()) {
                return;
            }

            enCurso = false;
            pausado = false;

            if (esCliente) {
                lblEstadoClientes.setText("Proceso finalizado.");
                actualizarBotonesClientes();
            } else {
                lblEstadoProductos.setText("Proceso finalizado.");
                actualizarBotonesProductos();
            }

            actualizarEstadoGeneral();
        }

        private void mostrarDetenido() {

            if (!esProcesoActual()) {
                return;
            }

            enCurso = false;

            if (esCliente) {
                lblEstadoClientes.setText("Proceso detenido.");
                actualizarBotonesClientes();
            } else {
                lblEstadoProductos.setText("Proceso detenido.");
                actualizarBotonesProductos();
            }

            actualizarEstadoGeneral();
        }

        private int getPorcentaje() {
            return progreso * 100 / total;
        }

        private boolean esProcesoActual() {
            return esCliente
                    ? procesoClientes == this
                    : procesoProductos == this;
        }

        private void cancelar() {

            synchronized (monitor) {
                cancelado = true;
                pausado = false;
                monitor.notifyAll();
            }

            if (hilo != null) {
                hilo.interrupt();
            }

            enCurso = false;
        }
    }
}
