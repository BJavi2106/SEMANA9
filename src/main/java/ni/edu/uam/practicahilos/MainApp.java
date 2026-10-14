
package ni.edu.uam.practicahilos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp {

    public static void main(String[] args) {
        Application.launch(VentanaPrincipal.class, args);
    }

    public static class VentanaPrincipal extends Application {

        @Override
        public void start(Stage stage) throws Exception {

            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("procesador-view.fxml")
            );

            Scene scene = new Scene(loader.load());

            ProcesadorController controller = loader.getController();

            stage.setTitle("Monitor de procesamiento - JavaFX");
            stage.setScene(scene);
            stage.setMinWidth(700);
            stage.setMinHeight(600);

            stage.setOnCloseRequest(event ->
                    controller.detenerAlCerrar()
            );

            stage.show();
        }
    }
}
