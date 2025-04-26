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
import javafx.animation.PauseTransition;
import javafx.util.Duration;
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
	private Label loadingLabel;
	private Stage loadingStage;
	private boolean isSpectator = false;

	private volatile boolean stillWaiting = false;
	public static void main(String[] args) {
		launch(args);
	}
	private void updatedBoard(int row, int col, String token) {
		Button btn = gridOfButtons[row][col]; //this is so the button that was clicked can actualy get updated with the set token color:>>>
		Circle tokenCircle = new Circle(25); //amking a whoe new token nce it gets the okay from the server
		if (token.equals("G")) {
			tokenCircle.setFill(Color.web("#6CC523"));//dis the color green directly from our figma
		} else {
			tokenCircle.setFill(Color.web("#E5C25A")); //same here
		}
		btn.setGraphic(tokenCircle);
		//this is to clear for the next token->for some reason, this isnt necessary but a good add to avoid the object acting weird
		btn.setDisable(true);
		btn.setOnMouseEntered(null);
		btn.setOnMouseExited(null);
	}

	//play game logic method and gui setup
	public void playGameScreen(Stage primaryStage) {
		GridPane boardGrid = new GridPane();
		for (int r = 0; r < 6; r++) {
			for (int c = 0; c < 7; c++) {
				Button space = new Button();
				space.setMinSize(50, 50);
				space.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
				//this is just filling the tokens for the orginal board --> also updated in game baord : color hex values from figma:o
				space.setOnMouseEntered(e -> {Circle circle = new Circle(25);circle.setFill(currentToken.equals("G") ? Color.web("#FFC6CA") : Color.web("#FFC6CA"));circle.setOpacity(0.5);space.setGraphic(circle);
				});
				space.setOnMouseExited(e -> {Circle circle = new Circle(25);circle.setFill(Color.web("#8471A8"));space.setGraphic(circle); //for the invidisual 6by7 objects
				});
				Circle circle = new Circle(25);
				circle.setFill(Color.web("#8471A8")); //this the shade our purple from our figma
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
		boardStack.setStyle("-fx-background-color: #c9f;");
		boardStack.setAlignment(Pos.CENTER);
		turnLabel.setFont(Font.font("Courier New", 32));
		turnLabel.setTextFill(Color.BLACK);
		turnLabel.setStyle("-fx-effect: dropshadow(gaussian, black, 2, 0.5, 1, 1);"); //this is for the send pbutton so it looks a little more animtated
		turnLabel.setAlignment(Pos.CENTER);
		VBox boardWithTurn = new VBox(10, turnLabel, boardStack);
		boardWithTurn.setAlignment(Pos.CENTER);
		boardStack.setStyle("-fx-background-color: #c9f;");
		boardStack.setAlignment(Pos.CENTER);
		//chat stufff
		chatDisplay.setEditable(false);
		chatDisplay.setWrapText(true);
		chatDisplay.setPrefHeight(150);
		chatDisplay.setPrefWidth(520);
		chatDisplay.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px;");
		TextField input = new TextField();
//		HBox input = new HBox(5, input, sendButton);
		input.setPromptText("Type your message here...");
		input.setPrefWidth(450);
		input.setStyle("-fx-font-family: 'Courier New';");
		Button sendButton = new Button("Send");
		sendButton.setStyle("-fx-background-color: #E5E5E5; -fx-text-fill: black; -fx-font-size: 18px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 4; -fx-padding: 5 15;");
		sendButton.setMinSize(100, 36);

		if (isSpectator) {
			input.setDisable(true);
			sendButton.setDisable(true);
			input.setPromptText("Spectators can't chat.");
		}

		sendButton.setOnAction(e -> {
			String msg = input.getText();
			if (!msg.isEmpty()) {clientThread.send(msg);
				input.clear();
			}
		});

		HBox inputRow = new HBox(5, input, sendButton);
		inputRow.setAlignment(Pos.CENTER);
		VBox chatSection = new VBox(10, chatDisplay, inputRow);
		chatSection.setPadding(new Insets(10));
		chatSection.setAlignment(Pos.CENTER);
		Button quitButton = new Button("quit");
		quitButton.setStyle("-fx-background-color: #E5E5E5; -fx-text-fill: black; -fx-font-size: 18px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 4; -fx-padding: 5 15;");
		quitButton.setMinSize(100, 36);
		quitButton.setOnAction(e -> {
			mainScreen(); //sending the client back to the screen for quitting :- this might causee problems well see
			primaryStage.setScene(mainScreen);
		});
		HBox quitBox = new HBox(quitButton);
		quitBox.setAlignment(Pos.CENTER_RIGHT);
		quitBox.setPadding(new Insets(10, 30, 10, 10));
		VBox chatSectionWrapped = new VBox(chatSection); //wrapping  our plain chatsection to add style-->color, and padding as well as positioning like any other instnace
		chatSectionWrapped.setStyle("-fx-background-color: #bb99ff; -fx-border-color: black; -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");
		chatSectionWrapped.setPadding(new Insets(15));
		chatSectionWrapped.setMaxWidth(580);
		chatSectionWrapped.setAlignment(Pos.CENTER);
		VBox gameLayout = new VBox(20, boardWithTurn, chatSectionWrapped, quitBox); //okay this is the entire layout based on the components i listed above
		gameLayout.setAlignment(Pos.TOP_CENTER); //will be t where its the chat->gameboard order
		gameLayout.setPadding(new Insets(20)); //padding betweeen out componnt s
		gameLayout.setStyle("-fx-background-color: #cab9ec;"); //figma backgrouhnd color
		Scene gameScene = new Scene(gameLayout, 750, 750);
//		chatDisplay.setPrefWidth(550);
//		input.setPrefWidth(450);
//		sendButton.setPrefWidth(100);
		Platform.runLater(() -> {
			primaryStage.setScene(gameScene);
			primaryStage.setTitle("Connect 4");
		});
	}
	//this method creates the prompt for the username and if a username is valid, it will proceed to the next screen.
	public void promptUsername(Stage primaryStage) {
		usernameInput = new TextField();
		usernameInput.setStyle("-fx-font-size: 16px; -fx-font-family: 'Courier New'; -fx-padding: 5; -fx-border-color: black; -fx-border-width: 2; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: white;");
		usernameInput.setPromptText("Enter Your Username");
		usernameInput.setStyle("-fx-font-size: 16px; -fx-font-family: 'Courier New'; -fx-padding: 5;");
		welcomeLabel = new Label("Welcome to Connect4");
		welcomeLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: black;");
		welcomeLabel.setAlignment(Pos.CENTER);//ligns text inside the label
		welcomeLabel.setMaxWidth(Double.MAX_VALUE);
		feedbackLabel = new Label(); //for "username taken" warning
		submitUser = new Button("Submit");
		submitUser.setStyle("-fx-background-color: #E5E5E5; -fx-text-fill: black; -fx-font-size: 18px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 4; -fx-padding: 5 15;");
		submitUser.setMinSize(100, 36);
		usernameBox = new HBox(10, usernameInput, submitUser);
		usernameBox.setAlignment(Pos.CENTER);
		//event handler for the submit button
		submitUser.setOnAction(e -> {
			username = usernameInput.getText().trim(); //grabbing the text
			System.out.println(username);
			usernameInput.setPromptText("Enter Your Username");
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
		Scene usernameScene = new Scene(usernameSelection, 750, 750);
		primaryStage.setScene(usernameScene);
		primaryStage.setTitle("Choose Username");
		primaryStage.show();
	}
	//main screen setup method
	public void mainScreen() {
		titleLabel = new Label("welcome " + username + "! select your game!");
		titleLabel.setFont(Font.font("Courier New", 32));
		titleLabel.setStyle("-fx-effect: dropshadow(gaussian, black, 2, 0.5, 1, 1);");
		titleLabel.setAlignment(Pos.CENTER);
		titleLabel.setTextFill(Color.BLACK);
		titleLabel.setMaxWidth(Double.MAX_VALUE);
		chatDisplay.setEditable(false); //not an box you can edit but you can edit the actual text
		chatDisplay.setWrapText(true);
		chatDisplay.setPrefHeight(150);
		playOnline = new Button("Play Online"); //lol i tried making this exaclty likr the figma....well see how this looks like bruv
		playOnline.setStyle("-fx-background-color: #E5E5E5; -fx-text-fill: black; -fx-font-size: 18px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 4; -fx-padding: 5 15;");
		playOnline.setMinSize(157, 36);
		playWithComputer = new Button("Play with Computer");
		playWithComputer.setStyle("-fx-background-color: #E5E5E5; -fx-text-fill: black; -fx-font-size: 18px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 4; -fx-padding: 5 15;");
		playWithComputer.setMinSize(157, 36);
		Button spectateButton = new Button("Spectate Game");
		spectateButton.setStyle("-fx-background-color: #E5E5E5; -fx-text-fill: black; -fx-font-size: 18px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 4; -fx-padding: 5 15;");
		spectateButton.setMinSize(157, 36);
		spectateButton.setOnAction(e -> {
			clientThread.send("spectate_request");
			loadingScreen((Stage) spectateButton.getScene().getWindow(), "Finding a game to watch...");
		});
		buttonBox = new VBox(10, playOnline, playWithComputer, spectateButton);
		buttonBox.setAlignment(Pos.CENTER);
		onlineTitle = new Label("online players:");
		onlineTitle.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: black;");
		onlineList = new Label("list of online players");
		onlineList.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: black;");
		onlineBox = new VBox(10, onlineTitle, onlineList);
		onlineBox.setAlignment(Pos.CENTER);
		VBox onlineListWrapper = new VBox(10, onlineTitle, onlineList); //wrappign with css based on figma layout and font and rounding of our boxes->will do the same for the other screens
		onlineListWrapper.setStyle("-fx-background-color: #E5E5E5; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 10 15;");
		onlineListWrapper.setPadding(new Insets(15));
		onlineListWrapper.setMaxWidth(200);
		onlineListWrapper.setAlignment(Pos.CENTER);
		information = new HBox(100, onlineListWrapper, buttonBox);
		information.setAlignment(Pos.CENTER);
		mainLayout = new VBox(20, titleLabel, information);
		mainLayout.setAlignment(Pos.CENTER);
		mainLayout.setPadding(new Insets(20));
		mainLayout.setStyle("-fx-background-color: #c9f;");
		mainScreen = new Scene(mainLayout, 750, 750);
		//sending the request to play when online and switching to loading screen
		playOnline.setOnAction(e -> {
			clientThread.send("play_request");
			loadingScreen((Stage) playOnline.getScene().getWindow(), "matching you to an opponent...");
		});
		clientThread.send("get_user_list"); //we NEED her, because she will trigger the NEW list of users in cas epeople jointed mid game-> thi
	}
	//chat layout function
	public void chatLayout(){
		// Chat layout
		TextField input = new TextField();
		input.setPromptText("Type your message here...");
		Button sendButton = new Button("Send");
		sendButton.setStyle("-fx-background-color: #E5E5E5; -fx-text-fill: black; -fx-font-size: 18px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 4; -fx-padding: 5 15;");
		sendButton.setMinSize(100, 36);
		sendButton.setOnAction(e -> {
			String msg = input.getText();
			if (!msg.isEmpty()) {
				clientThread.send(msg);
				input.clear();
			}
		});
		VBox layout = new VBox(10, input, sendButton);
		layout.setPadding(new Insets(20));
		Scene chatScene = new Scene(layout, 750, 750);
		VBox chatSection = new VBox(10, chatDisplay, input, sendButton); //this is the box qwe will use for our chat
	}
	//function for the loading screen
	public void loadingScreen(Stage stage, String message) {
		loadingLabel = new Label(message); // Save globally
		loadingLabel.setStyle("-fx-font-size: 24px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
		ProgressIndicator spinner = new ProgressIndicator();
		spinner.setPrefSize(100, 100);
		VBox layout = new VBox(20, loadingLabel, spinner);
		layout.setStyle("-fx-background-color: #c9f;");
		layout.setAlignment(Pos.CENTER);
		layout.setPadding(new Insets(30));
		Scene loadingScene = new Scene(layout, 750, 750);
		Platform.runLater(() -> {
			stage.setScene(loadingScene);
			stage.setTitle("Matchmaking...");
		});
		loadingStage = stage; // Save globally

		PauseTransition timeout = new PauseTransition(Duration.seconds(7));
		timeout.setOnFinished(event -> {
			if (stillWaiting && stage.getScene() == loadingScene) {
				stillWaiting = false;
				mainScreen();
				stage.setScene(mainScreen);
			}
		});
		timeout.play();
	}

	//function for the loading screen
	public void rematchLoadingScreen(Stage stage, String message) {
		Label loading = new Label(message); // label message
		loading.setStyle("-fx-font-size: 24px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
		ProgressIndicator spinner = new ProgressIndicator(); // circle loading indicater
		spinner.setPrefSize(100, 100);
		//VBox layout = new VBox(20, loading, spinner);
		Button cancel = new Button("Cancel");
		cancel.setStyle("-fx-background-color: #E5E5E5; -fx-text-fill: black; -fx-font-size: 18px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 4; -fx-padding: 5 15;");
		VBox layout = new VBox(20, loading, spinner, cancel);
		layout.setStyle("-fx-background-color: #c9f;");
		layout.setAlignment(Pos.CENTER);
		layout.setPadding(new Insets(30));
		cancel.setOnAction(e -> {
			//clientThread.send("play_again");
			mainScreen();
			stage.setScene(mainScreen);
		});
		Scene loadingScene = new Scene(layout, 750, 750);
		Platform.runLater(() -> {
			stage.setScene(loadingScene);
			stage.setTitle("Matchmaking...");
		});
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
		resultLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 32px;");
		// play again and backtomain buttons
		Button playAgain = new Button("play again");
		playAgain.setOnAction(e -> {
			clientThread.send("play_again");
			rematchLoadingScreen(stage, "waiting for opponent to accept rematch...");
		});
		Button backToMenu = new Button("Main Menu");
		backToMenu.setOnAction(e -> {
			clientThread.send("cancel_rematch");
			mainScreen();
			stage.setScene(mainScreen);
		});
		VBox layout = new VBox(20, resultLabel, playAgain, backToMenu);
		layout.setStyle("-fx-background-color: #c9f;");
		layout.setAlignment(Pos.CENTER);
		layout.setPadding(new Insets(30));
		Scene resultScene = new Scene(layout, 750, 750);
		stage.setScene(resultScene);
	}
	//its a tie screen, same as above but set label.
	public void showDrawScreen(Stage stage) {
		Label drawLabel = new Label("It's a draw!");
		drawLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 32px;");
		Button playAgain = new Button("play again");
		playAgain.setOnAction(e -> {
			clientThread.send("play_again");
			rematchLoadingScreen(stage, "Waiting for opponent to accept rematch...");
		});
		Button backToMenu = new Button("Main Menu");
		backToMenu.setOnAction(e -> {
			clientThread.send("cancel_rematch");
			mainScreen();
			stage.setScene(mainScreen);
		});
		VBox layout = new VBox(20, drawLabel, playAgain, backToMenu);
		layout.setStyle("-fx-background-color: #c9f;");
		layout.setAlignment(Pos.CENTER);
		layout.setPadding(new Insets(30));
		Scene drawScene = new Scene(layout, 750, 750);
		stage.setScene(drawScene);
	}

	public void spectatorResultScreen(Stage stage, String winner, String loser) {
		Label resultLabel = new Label(winner + " won against " + loser + "!");
		resultLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 32px;");
		Button backToMenu = new Button("Return to Main Menu");
		backToMenu.setOnAction(e -> {
			mainScreen();
			stage.setScene(mainScreen);
		});
		backToMenu.setStyle("-fx-background-color: #E5E5E5; -fx-text-fill: black; -fx-font-size: 18px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 4; -fx-padding: 5 15;");

		VBox layout = new VBox(20, resultLabel, backToMenu);
		layout.setAlignment(Pos.CENTER);
		layout.setPadding(new Insets(30));
		layout.setStyle("-fx-background-color: #c9f;");

		Scene spectatorResultScene = new Scene(layout, 750, 750);
		stage.setScene(spectatorResultScene);
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
				stillWaiting = false;
				Platform.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.INFORMATION);
					alert.setTitle("Rematch Cancelled");
					alert.setHeaderText(null);
					alert.setContentText("Your opponent declined the rematch and returned to main menu. Please return to main menu and find another match.");
					alert.showAndWait();
					mainScreen();
					try {
						Scene scene = chatDisplay.getScene();
						if (scene != null && scene.getWindow() != null) {
							Stage stage = (Stage) scene.getWindow();
							stage.setScene(mainScreen);
						} else {
							System.out.println("⚠️ Could not switch to main screen — scene/window is null");
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						System.out.println("❌ Error switching to main screen after no_rematch");
					}
				});
			}
			else if (msg.startsWith("USERLIST:")) {
				String[] allNames = msg.substring(9).split(",");
				ArrayList<String> others = new ArrayList<>();
				for (String name : allNames) { //this is to get the player users btoh before AND during a game
					if (!name.equalsIgnoreCase(username)) {
						others.add(name); //adding them directly from the server as its getting new users joining yas
					}
				}
				clientThread.latestUsernames = others;
				if (clientThread.getUsernamesHandler() != null) {
					clientThread.getUsernamesHandler().accept(others);
				}
			}
			else if (msg.equals("no_rematch")) {
				stillWaiting = false;
				Platform.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.INFORMATION);
					alert.setTitle("Rematch Cancelled");
					alert.setHeaderText(null);
					alert.setContentText("Your opponent declined the rematch. Returning to main menu.");
					alert.showAndWait();
					mainScreen();
					try {
						Scene scene = chatDisplay.getScene();
						if (scene != null && scene.getWindow() != null) {
							Stage stage = (Stage) scene.getWindow();
							stage.setScene(mainScreen);
						} else {
							System.out.println("⚠️ Could not switch to main screen — scene/window is null");
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						System.out.println("❌ Error switching to main screen after no_rematch");
					}
				});
			}
			else if (msg.equals("waiting_for_game")) {
				Platform.runLater(() -> {
					if (loadingLabel != null) {
						loadingLabel.setText("No active game yet... waiting for players! 🕑");
					}
					System.out.println("Still waiting for an active game to spectate...");
				});
			}
			else if (msg.equals("spectating")) {
				isSpectator = true;
				Platform.runLater(() -> playGameScreen(primaryStage));
			}
			else if (msg.startsWith("spectator_game_over:")) {
				String[] parts = msg.substring(20).split(",");
				String winner = parts[0];
				String loser = parts[1];
				Platform.runLater(() -> spectatorResultScreen((Stage) chatDisplay.getScene().getWindow(), winner, loser));
			}
			else {
				System.out.println("Server: " + msg);
			}
		});
		promptUsername(primaryStage); //first prompting we have
	}
}
