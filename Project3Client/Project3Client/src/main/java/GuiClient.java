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
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;

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
	TextArea chatDisplay;
	Label turnLabel = new Label();


	public static void main(String[] args) {
		launch(args);
	}

	private void updatedBoard(int row, int col, String token) {
		Button btn = gridOfButtons[row][col]; //this is so the button that was clicked can actualy get updated with the set token color:>>>
		Circle tokenCircle = (Circle) btn.getGraphic();
		if (tokenCircle == null) {
			tokenCircle = new Circle(25); // game token images
			btn.setGraphic(tokenCircle);
		}
		tokenCircle.setFill(token.equals("G") ? Color.GREEN : Color.GOLD); // setting which color each character should be
	}

	//play game logic method and gui setup
	public void playGameScreen(Stage primaryStage) {
		GridPane boardGrid = new GridPane();
		for (int r = 0; r < 6; r++) {
			for (int c = 0; c < 7; c++) {
				Button space = new Button();
				space.setMinSize(50, 50);
				space.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
				space.setOnMouseEntered(e -> space.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));
				space.setOnMouseExited(e -> space.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));

				Circle circle = new Circle(25);
				circle.setFill(javafx.scene.paint.Color.LIGHTGRAY);
				space.setGraphic(circle);
				space.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				final int colDrop = c;

				space.setOnAction(e -> clientThread.send("MOVE:" + colDrop)); // everytime a space is clicked, it sends the move
				gridOfButtons[r][c] = space; // setting it equal to it
				boardGrid.add(space, c, r); // adding that specific token
			}
		}

		boardGrid.setAlignment(Pos.CENTER);
		boardGrid.setHgap(5);
		boardGrid.setVgap(5);
		boardGrid.setPadding(new Insets(20)); //set to be able to make cuts into a rectangle board of 6by7 circles

		Region boardBg = new Region();
		boardBg.setPrefSize(7 * 70 + 40, 6 * 70 + 40);
		boardBg.setStyle("-fx-background-color: white; -fx-border-radius: 10; -fx-background-radius: 10;"); //this is to set roundness and sie of the circles

		StackPane boardStack = new StackPane(boardBg, boardGrid);
		turnLabel.setFont(Font.font("Courier New", 20));
		turnLabel.setTextFill(Color.BLACK);
		turnLabel.setAlignment(Pos.CENTER);

		VBox boardWithTurn = new VBox(10, turnLabel, boardStack);
		boardWithTurn.setAlignment(Pos.CENTER);

		boardStack.setStyle("-fx-background-color: #c9f;");
		boardStack.setAlignment(Pos.CENTER);

		chatDisplay.setEditable(false);
		chatDisplay.setWrapText(true);
		chatDisplay.setPrefHeight(150);

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

		VBox chatSection = new VBox(10, chatDisplay, input, sendButton);
		chatSection.setPadding(new Insets(20));
		chatSection.setAlignment(Pos.CENTER);

		HBox gameLayout = new HBox(30, boardWithTurn, chatSection);
		gameLayout.setAlignment(Pos.CENTER);
		gameLayout.setPadding(new Insets(20));

		gameLayout.setStyle("-fx-background-color: #c9f;");
		Scene gameScene = new Scene(gameLayout, 600, 600);

		chatDisplay.setPrefWidth(550);
		input.setPrefWidth(450);
		sendButton.setPrefWidth(100);

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
		usernameSelection.setStyle("-fx-background-color: #c9f;");
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

		chatDisplay.setEditable(false); //not an box you can edit but you can edit the actual text
		chatDisplay.setWrapText(true);
		chatDisplay.setPrefHeight(150);

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
		mainLayout.setStyle("-fx-background-color: #c9f;");
		mainScreen = new Scene(mainLayout, 600, 600);

		//sending the request to play when online and switching to loading screen
		playOnline.setOnAction(e -> {
			clientThread.send("play_request");
			loadingScreen((Stage) playOnline.getScene().getWindow(), "matching you to an opponent...");
		});

	}

	//chat layout function
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
		VBox chatSection = new VBox(10, chatDisplay, input, sendButton); //this is the box qwe will use for our chat
	}

	//function for the loading screen
	public void loadingScreen(Stage stage, String message){
		Label loading = new Label(message); // label message
		loading.setFont(Font.font("Impact", 24));
		ProgressIndicator spinner = new ProgressIndicator(); // circle loading indicater
		spinner.setPrefSize(100, 100);
		VBox layout = new VBox(20, loading, spinner);
		layout.setStyle("-fx-background-color: #c9f;");
		layout.setAlignment(Pos.CENTER);
		layout.setPadding(new Insets(30));

		Scene loadingScene = new Scene(layout, 600, 600);
		Platform.runLater(() -> {
			stage.setScene(loadingScene);
			stage.setTitle("Matchmaking...");
		});

		//handles if they are taking too long to get a response
		new Thread(() -> {
			try {
				Thread.sleep(7000); // 5 second timeout
				Platform.runLater(() -> {
					if (stage.getScene() == loadingScene) { // still stuck
						mainScreen();
						stage.setScene(mainScreen);
					}
				});
			} catch (InterruptedException ignored) {}
		}).start();

	}
	//win or lose screen based on the player
	public void winOrLose(boolean isWinner, Stage stage){
		Label resultLabel = new Label();
		// if they are a winner, it displays you won, if not it displays they lost
		if(isWinner){
			resultLabel.setText(username + " won!");
		}
		else{
			resultLabel.setText(username + " lost!");
		}
		resultLabel.setFont(Font.font("Impact", 32));

		// play again and backtomain buttons
		Button playAgain = new Button("play again");
		playAgain.setOnAction(e -> {
			clientThread.send("play_again");
			loadingScreen(stage, "waiting for opponent to accept rematch...");
		});

		Button backToMenu = new Button("Main Menu");
		backToMenu.setOnAction(e -> {
			//clientThread.send("cancel_rematch");
			mainScreen();
			stage.setScene(mainScreen);
		});

		VBox layout = new VBox(20, resultLabel, playAgain, backToMenu);
		layout.setStyle("-fx-background-color: #c9f;");
		layout.setAlignment(Pos.CENTER);
		layout.setPadding(new Insets(30));

		Scene resultScene = new Scene(layout, 600, 600);
		stage.setScene(resultScene);
	}

	//its a tie screen, same as above but set label.
	public void showDrawScreen(Stage stage) {
		Label drawLabel = new Label("It's a draw!");
		drawLabel.setFont(Font.font("Impact", 32));

		Button playAgain = new Button("play again");
		playAgain.setOnAction(e -> {
			clientThread.send("play_again");
			loadingScreen(stage, "Waiting for opponent to accept rematch...");
		});

		Button backToMenu = new Button("Main Menu");
		backToMenu.setOnAction(e -> {
			//clientThread.send("cancel_rematch");
			mainScreen();
			stage.setScene(mainScreen);
		});

		VBox layout = new VBox(20, drawLabel, playAgain, backToMenu);
		layout.setStyle("-fx-background-color: #c9f;");
		layout.setAlignment(Pos.CENTER);
		layout.setPadding(new Insets(30));

		Scene drawScene = new Scene(layout, 600, 600);
		stage.setScene(drawScene);
	}

	@Override
	public void start(Stage primaryStage) {
		clientThread.start();

		chatDisplay = new TextArea();
		chatDisplay.setEditable(false);
		chatDisplay.setWrapText(true);
		chatDisplay.setPrefHeight(150);
		chatDisplay.setPrefWidth(550);

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
			else if (msg.startsWith("CHAT:")) {
				String chatText = msg.substring(5);
				Platform.runLater(() -> {
					if (chatDisplay != null) {
						chatDisplay.appendText(chatText + "\n"); //this is to print out the info from the server onto the corrpsonging thread at hand->so this is chat per 2 clients
					}
				});
			}
			else if (msg.equals("WINNER") || msg.equals("LOSER")) {
				Platform.runLater(() -> winOrLose(msg.equals("WINNER"), (Stage) chatDisplay.getScene().getWindow()));
			}
			else if (msg.equals("DRAW")) {
				Platform.runLater(() -> showDrawScreen((Stage) chatDisplay.getScene().getWindow()));
			}
			else if (msg.equals("Your turn!") || msg.equals("Not your turn yet!")) {
				Platform.runLater(() -> turnLabel.setText(msg));
			}
			else if (msg.startsWith("TURN:")) {
				String playerTurn = msg.substring(5);
				Platform.runLater(() -> {
					if (playerTurn.equals(username)) {
						turnLabel.setText("It's your turn!");
					} else {
						turnLabel.setText("Waiting for " + playerTurn + "...");
					}
				});
			}
			else if (msg.equals("no_rematch")) {
				Platform.runLater(() -> {
					mainScreen();
					((Stage) chatDisplay.getScene().getWindow()).setScene(mainScreen);
				});
			}
			else {
				System.out.println("Server: " + msg);
			}
		});
		promptUsername(primaryStage); //first prompting we have
	}
}