import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.layout.HBox;
import java.util.ArrayList;
import javafx.scene.layout.GridPane;

public class GuiClient extends Application {
	Client clientThread = new Client();
	TextField usernameInput;
	Button submitUser, playOnline, playWithComputer;
	VBox usernameSelection, mainLayout, buttonBox, onlineBox;
	Label feedbackLabel, welcomeLabel, titleLabel, onlineTitle, onlineList;
	HBox usernameBox, information;
	Scene mainScreen;
	String username;
	Button[][] gridOfButtons = new Button[6][7];
	String currentToken = "G";

	public static void main(String[] args) {
		launch(args);
	}

	private void updatedBoard(int row, int col, String token) {
		Button btn = gridOfButtons[row][col]; //this is so the button that was clicked can actualy get updated with the set token color:>>>
//		btn.setText(" "); //lol i forgot we had a
		btn.setStyle("-fx-background-color: " + (token.equals("G") ? "green" : "gold") + "; " +
				"-fx-border-color: black; -fx-font-weight: bold;");
	}

	//play game logic method ysss
	public void playGameScreen(Stage primaryStage) {
		GridPane boardGrid = new GridPane();

		for (int r = 0; r < 6; r++) {
			for (int c = 0; c < 7; c++) {
				Button space = new Button();
				space.setMinSize(70, 70);
				int colDrop = c;


				space.setOnAction(e -> {
					clientThread.send("MOVE:" + colDrop);
				}); //

//				space.setOnAction(e -> {
//					for (int row = 5; row >= 0; row--) {
//						if (gridOfButtons[row][colDrop].getText().equals(".")) {
//							updatedBoard(row, colDrop, currentToken);  // <-- ⚠ local update BEFORE confirmation
//							currentToken = currentToken.equals("G") ? "Y" : "G";
//							break;
//						}
//					}
//					clientThread.send("MOVE:" + colDrop);
//				});


				gridOfButtons[r][c] = space;
				boardGrid.add(space, c, r);
			}
		}

		TextField input = new TextField();
		input.setPromptText("Type your message here...");
		Button sendButton = new Button("Send");

		sendButton.setOnAction(e -> {
			String msg = input.getText();
			if (!msg.isEmpty()) {
				clientThread.send(msg);
				input.clear();
			}
		});

		VBox chatSection = new VBox(10, input, sendButton);
		chatSection.setPadding(new Insets(20));
		chatSection.setAlignment(Pos.CENTER);

		VBox gameLayout = new VBox(30, boardGrid, chatSection);
		gameLayout.setAlignment(Pos.CENTER);
		gameLayout.setPadding(new Insets(20));

		Scene gameScene = new Scene(gameLayout, 600, 600);
		Platform.runLater(() -> {
			primaryStage.setScene(gameScene);
			primaryStage.setTitle("Connect 4");
		});
	}
	//this method creates the prompt for the username and if a username is valid, it will proceed to the next screen.
	public void promptUsername(Stage primaryStage) {
		usernameInput = new TextField();
		usernameInput.setPromptText("enter your username");
		usernameInput.setPrefWidth(200);
		welcomeLabel = new Label("Welcome to Connect4");
		welcomeLabel.setFont(Font.font("Impact", 32));
		welcomeLabel.setAlignment(Pos.CENTER);//ligns text inside the label
		welcomeLabel.setMaxWidth(Double.MAX_VALUE);
		feedbackLabel = new Label(); //for "username taken" warning
		submitUser = new Button("submit");
		usernameBox = new HBox(10, usernameInput, submitUser);
		usernameBox.setAlignment(Pos.CENTER);

		//event handler for the submit button
		submitUser.setOnAction(e -> {
			username = usernameInput.getText().trim(); //grabbing the text
			System.out.println(username);
			usernameInput.setPromptText("enter your username");
			feedbackLabel.setText("");
			if (!username.isEmpty()) {
				clientThread.send("username:" + username); //sends username input to server
				//check server response in a background thread
				new Thread(() -> {
					try {
						while (clientThread.latestMessage == null ||
								(!clientThread.latestMessage.equals("username_accepted")
										&& !clientThread.latestMessage.equals("username_taken"))) {
						}
						String response = clientThread.latestMessage; //grabbing latest message from server
						clientThread.latestMessage = null; // reset
						//if accepted switch to next scene
						if (response.equals("username_accepted")) {
							//set handler first so we catch updates immediately
							clientThread.setUsernamesHandler(list -> {
								Platform.runLater(() -> {
									ArrayList<String> others = new ArrayList<>(); //list to hold active users
									for (String name : list) { //every name in the list
										if (!name.equalsIgnoreCase(username)) { // removing our current clients name
											others.add(name);
										}
									}
									onlineList.setText(String.join("\n", others)); // display that list on each clients screen
								});
							});
							//this is an application thread that handles the gui better, itll handle the objects on the screen
							//better and update them based on what was read in through the thread.
							Platform.runLater(() -> {
								mainScreen();
								//if already received the list before mainScreen was built..
								//this updates existing clients that are already on that screen
								if (!clientThread.latestUsernames.isEmpty()) {
									ArrayList<String> others = new ArrayList<>();
									for (String name : clientThread.latestUsernames) {
										if (!name.equalsIgnoreCase(username)) {
											others.add(name);
										}
									}
									onlineList.setText(String.join("\n", others));
								}
								primaryStage.setScene(mainScreen);
								primaryStage.setTitle("Connected Chat");
							});
							//anything else means the username is taken, prompt for anotherr
						} else {
							Platform.runLater(() -> feedbackLabel.setText("username is taken, try another. "));
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}).start();
			}
			//in the instance the user tries bypassing the enterUsername screen
			else{
				feedbackLabel.setText("you must enter a username to continue");
			}
		});

		//just setting up the GUI side for them.
		usernameSelection = new VBox(10, welcomeLabel, usernameBox, feedbackLabel);
		usernameSelection.setAlignment(Pos.CENTER);
		usernameSelection.setPadding(new Insets(20));
		Scene usernameScene = new Scene(usernameSelection, 600, 600);
		primaryStage.setScene(usernameScene);
		primaryStage.setTitle("Choose Username");
		primaryStage.show();
	}

	//main screen setup method
	public void mainScreen() {
		titleLabel = new Label("welcome " + username + "! select your game!");
		titleLabel.setFont(Font.font("Impact", 32));
		titleLabel.setAlignment(Pos.CENTER);
		titleLabel.setMaxWidth(Double.MAX_VALUE);

		playOnline = new Button("play online");
		playWithComputer = new Button("play with computer");
		buttonBox = new VBox(10, playOnline, playWithComputer);
		buttonBox.setAlignment(Pos.CENTER);

		onlineTitle = new Label("online players:");
		onlineList = new Label("list of online players");
		onlineBox = new VBox(10, onlineTitle, onlineList);
		onlineBox.setAlignment(Pos.CENTER);

		information = new HBox(100, onlineBox, buttonBox);
		information.setAlignment(Pos.CENTER);

		mainLayout = new VBox(20, titleLabel, information);
		mainLayout.setAlignment(Pos.CENTER);
		mainLayout.setPadding(new Insets(20));
		mainScreen = new Scene(mainLayout, 600, 600);

		playOnline.setOnAction(e -> {
			clientThread.send("play_request");
		});

	}

	public void chatLayout(){
		// Chat layout
		TextField input = new TextField();
		input.setPromptText("Type your message here...");
		Button sendButton = new Button("Send");
		sendButton.setOnAction(e -> {
			String msg = input.getText();
			if (!msg.isEmpty()) {
				clientThread.send(msg);
				input.clear();
			}
		});
		VBox layout = new VBox(10, input, sendButton);
		layout.setPadding(new Insets(20));
		Scene chatScene = new Scene(layout, 1000, 1000);
	}

	@Override
	public void start(Stage primaryStage) {
		clientThread.start();

		clientThread.setMessageHandler(msg -> {
			System.out.println("Received from server: " + msg);
			//game start message
			if (msg.startsWith("game_start:")) {
				Platform.runLater(() -> playGameScreen(primaryStage));
			}
			else if (msg.startsWith("UPDATE:")) {
				String[] parts = msg.substring(7).split(",");
				int row = Integer.parseInt(parts[0]); //getting the rows and columns and the move to be able to detect wins
				int col = Integer.parseInt(parts[1]);
				String token = parts[2];
				System.out.println("UPDATE RECEIVED :O row:" + row + " col:" + col + " token:" + token);
				Platform.runLater(() -> updatedBoard(row, col, token)); //this is so it only syncs to the client side from server ONCE the move HAS been made from EITHER client!!!
			}
			else {
				System.out.println("Server: " + msg);
			}
		});

		promptUsername(primaryStage); //first prompting we have
	}
}