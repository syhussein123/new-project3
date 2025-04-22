import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class GuiServer extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		ListView<String> messages = new ListView<>();

		// start server with callback to update GUI
		Server serv = new Server(msg -> {
			Platform.runLater(() -> messages.getItems().add(msg));
		});

		BorderPane layout = new BorderPane();
		layout.setCenter(messages);
		layout.setPrefSize(400, 300);

		primaryStage.setScene(new Scene(layout));
		primaryStage.setTitle("Server");
		primaryStage.show();
	}
}