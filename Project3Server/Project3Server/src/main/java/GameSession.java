import java.util.function.Consumer;
import java.util.ArrayList;

public class GameSession {
	Server.ClientThread player1;
	Server.ClientThread player2;
	String[][] gameBoard = new String[6][7];
	Consumer<String> callback;
	String currentToken = "G";  //green token first
	Server.ClientThread currentPlayer;
	private ArrayList<Server.ClientThread> spectators = new ArrayList<>();

	public void addSpectator(Server.ClientThread spectator) {
		spectators.add(spectator);
		for (int r = 0; r < 6; r++) {
			for (int c = 0; c < 7; c++) {
				if (!gameBoard[r][c].equals(".")) {
					sendToClientFromServer(spectator, "UPDATE:" + r + "," + c + "," + gameBoard[r][c]);
					System.out.println("Sending to spectator: " + spectator.username + " -> UPDATE:" + r + "," + c + "," + gameBoard[r][c]);
				}
			}
		}
	}

	public String getBoardStateString() {
		StringBuilder sb = new StringBuilder();
		for (int r = 0; r < 6; r++) {
			for (int c = 0; c < 7; c++) {
				sb.append(gameBoard[r][c]);
				if (c < 6) sb.append(",");
			}
			if (r < 5) sb.append(";");
		}
		return sb.toString();
	}


	public GameSession(Server.ClientThread player1, Server.ClientThread player2, Consumer<String> callback) { //provoked when we have pairs of players only
		this.player1 = player1;
		this.player2 = player2;
		this.callback = callback;
		this.currentPlayer = player1; //default starter
		startBoard();
	}
	//checkks if the board is full
	private boolean isBoardFull() {
		for (int c = 0; c < 7; c++) {
			if (gameBoard[0][c].equals(".")) {
				return false;
			}
		}
		return true;
	}
	//method to initialize board
	private void startBoard() {
		for (int r = 0; r < 6; r++) {
			for (int c = 0; c < 7; c++) {
				gameBoard[r][c] = ".";
			}
		}
	}
	//method for making the move and changing it to the token of the user at turn, checking conditions
	public void makeMove(Server.ClientThread currPlayer, int c) {
		if (currentPlayer != currPlayer) { //case 1: not even your turn
			sendToClientFromServer(currPlayer, "Not your turn yet!");
			return;
		}
		if (c < 0 || c >= 7) { //case 2: invalid column
			sendToClientFromServer(currPlayer, "Column out of bounds!");
			return;
		}
		for (int r = 5; r >= 0; r--) {
			if (gameBoard[r][c].equals(".")) {
				gameBoard[r][c] = currentToken;
				String update = "UPDATE:" + r + "," + c + "," + currentToken;
				sendToClientFromServer(player1, update);
				sendToClientFromServer(player2, update);

				for (Server.ClientThread spectator : spectators) {
					sendToClientFromServer(spectator, "UPDATE:" + r + "," + c + "," + currentToken);
				}

				if (checkWin(gameBoard, currentToken)) {
					// notify winner and loser
					sendToClientFromServer(currPlayer, "WINNER");
					Server.ClientThread loserPlayer = (currPlayer == player1) ? player2 : player1;
					sendToClientFromServer(loserPlayer, "LOSER");

					// tell all spectators who won
					for (Server.ClientThread spectator : spectators) {
						sendToClientFromServer(spectator, "spectator_game_over:" + currPlayer.username + "," + loserPlayer.username);
					}
					return;
				}
				else if (isBoardFull()) {
					// draw case
					sendToClientFromServer(player1, "DRAW");
					sendToClientFromServer(player2, "DRAW");

					for (Server.ClientThread spectator : spectators) {
						sendToClientFromServer(spectator, "spectator_game_over:Draw,Draw");
					}
					return;
				}

				// continue to next turn
				sendToClientFromServer(player1, "Player " + currPlayer.count + " dropped in column " + c);
				sendToClientFromServer(player2, "Player " + currPlayer.count + " dropped in column " + c);
				currentPlayer = (currPlayer == player1) ? player2 : player1;
				currentToken = currentToken.equals("G") ? "Y" : "G";
				sendToClientFromServer(player1, "TURN:" + currentPlayer.username);
				sendToClientFromServer(player2, "TURN:" + currentPlayer.username);
				return;
			}
		}
		//case 3: column full
		sendToClientFromServer(currentPlayer, "Column requested is full. Try again!");
	}

	//method to check the 4 possible wins, being mindeful of bounds associated mainly w diagonal
	private boolean checkWin(String[][] gameBoard, String currentToken) {
		//case 1: horizontal win
		for (int r = 0; r < 6; r++) {
			for (int c = 0; c <= 3; c++) {
				if (gameBoard[r][c].equals(currentToken) && gameBoard[r][c+1].equals(currentToken) && gameBoard[r][c+2].equals(currentToken) && gameBoard[r][c+3].equals(currentToken)) {
					return true;
				}
			}
		}
		//case 2: vertical win
		for (int r = 0; r <= 2; r++) {
			for (int c = 0; c < 7; c++) {
				if (gameBoard[r][c].equals(currentToken) && gameBoard[r+1][c].equals(currentToken) && gameBoard[r+2][c].equals(currentToken) && gameBoard[r+3][c].equals(currentToken)) {
					return true;
				}
			}
		}
		//case 3: diagonal win from lower left to upper right so /
		for (int r = 3; r < 6; r++) { //ONLY  checking when theres 4 spaces to actually check in either diagonal direction
			for (int c = 0; c <= 3; c++) {
				if (gameBoard[r][c].equals(currentToken) && gameBoard[r-1][c+1].equals(currentToken) && gameBoard[r-2][c+2].equals(currentToken) && gameBoard[r-3][c+3].equals(currentToken)) {
					return true;
				}
			}
		}
		//case 4: diagonal win from upper right to lower so \
		for (int r = 0; r <= 2; r++) {
			for (int c = 3; c <7; c++) {
				if (gameBoard[r][c].equals(currentToken) && gameBoard[r+1][c-1].equals(currentToken) && gameBoard[r+2][c-2].equals(currentToken) && gameBoard[r+3][c-3].equals(currentToken)) {
					return true;
				}
			}
		}
		return false; //case 5: no wins yet ..
	}
	public void sendToClientFromServer(Server.ClientThread player, String msg) {
		try {
			player.out.writeObject(msg); //writing the message based on condition to the client from the server's stream object yas
		} catch (Exception e) {
			callback.accept("Failed to send to player #" + player.count); //in case of failure to send back:<
		}
	}
}

