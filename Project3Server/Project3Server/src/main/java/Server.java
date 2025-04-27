import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;
public class Server {
	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<>(); //stores all clients
	ArrayList<String> usernames = new ArrayList<>(); //stores all usernames
	ArrayList<ClientThread> playQueue = new ArrayList<>(); //stores the player queue of who clicked playOnline
	ArrayList<String> sensoredWords = new ArrayList<>(); // stores the very bad words (please excuse my language)
	ArrayList<ClientThread> spectatorQueue = new ArrayList<>(); // for spectator queue
	GameSession currentActiveGame = null;

	TheServer server;
	private Consumer<String> callback;
	public Server(Consumer<String> callback) {
		this.callback = callback;
		//adding words that should be sensored, please excuse our language.
		sensoredWords.add("fuck");
		sensoredWords.add("shit");
		sensoredWords.add("bitch");
		sensoredWords.add("hoe");
		sensoredWords.add("ass");
		sensoredWords.add("dick");
		sensoredWords.add("cunt");
		sensoredWords.add("whore");
		sensoredWords.add("fatty");
		sensoredWords.add("stupid");
		sensoredWords.add("dumb");
		sensoredWords.add("idiot");
		sensoredWords.add("terrorist");
		server = new TheServer();
		server.start();
	}

	public class TheServer extends Thread {
		public void run() {
			try (ServerSocket mysocket = new ServerSocket(5555)) {
				System.out.println("Server is waiting for a client!");
				System.out.println("hello there");
				while (true) {
					ClientThread c = new ClientThread(mysocket.accept(), count);
					clients.add(c); // add the client to list
					c.start();
					System.out.println("Client Connected");
					count++; // increment the count
				}
			} catch (Exception e) {
				System.err.println("Server did not launch");
			}
		}
	}
	class ClientThread extends Thread {
		Socket connection;
		int count;
		ObjectInputStream in;
		ObjectOutputStream out;
		String username = null;
		private GameSession session;
		ClientThread lastOpponent = null;
		boolean wantsRematch = false;
		public void setSession(GameSession session) {
			this.session = session;
		}
		//constructor
		ClientThread(Socket s, int count) {
			this.connection = s;
			this.count = count;
		}
		//method that checks if the current username already exists
		private boolean isUsernameTaken(String name) {
			//for every client online
			for (ClientThread c : clients) {
				if (c.username != null && c.username.equalsIgnoreCase(name)) {
					return true;
				}
			}
			return false;
		}
		//method that sends the username to all clients in order to ensure they have the most updated list of clients on teh server
		private void broadcastUsernamesToAll() {
			for (ClientThread c : clients) {
				try {
					if (c.out != null) {
						String userListMsg = "USERLIST:" + String.join(",", usernames);
						c.out.writeObject(userListMsg); //this will be the new user list written out tio the client -> so its not static after username is typed in
					}
				} catch (Exception e) {
					System.out.println("Error sending usernames to " + c.username);
				}
			}
		}
		public void updateClients(String message) {
			callback.accept(message);
		}

		public void run() {
			try {
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);
			} catch (Exception e) {
				System.out.println("Streams not open");
				return;
			}
			updateClients("new client on server: client #" + count);
			while (username == null) {
				try {
					String msg = in.readObject().toString(); //reading in from client
					if (msg.startsWith("username:")) { //checking to see if we are reading in the right line by looking for username key word
						String proposed = msg.substring(9).trim(); //parsing the message
						//if the username isn't taken
						if (!isUsernameTaken(proposed)) {
							username = proposed;
							out.writeObject("username_accepted"); //send this prompt
							updateClients("✅ " + username + " has joined."); //print on server side that a new client has joined
							usernames.add(username); // adds username to the username list
							broadcastUsernamesToAll(); //sends out the information to everyone
						} else {
							out.writeObject("username_taken"); // send that the username was taken
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
			//this while statement is used for chatting, very imporatnt for later
			while (true) {
				try {
					String data = in.readObject().toString(); //reading the information
					//handles the player queue logic to pair them into a game
					if (data.startsWith("MOVE:")) {
						if (session != null) {
							int col = Integer.parseInt(data.substring(5));
							session.makeMove(this, col);
						}
					}
					//if there is a play_request coming in
					else if (data.equals("play_request")) {
						if (this.wantsRematch) {
							return;
						}
						boolean alreadyWaiting = false; //if the current user is already waiting for a match
						for(ClientThread c : playQueue) { // for every player in the playqueue
							//handles the case for duplicate clicks
							if(c.username.equalsIgnoreCase(this.username)) {
								alreadyWaiting = true;
								break;
							}
						}
						//if they aren't already in the queueu
						if(!alreadyWaiting){
							playQueue.add(this); //adding to the queue if they are already waiting
							updateClients(username + " wants to play!");
						}
						//when the queue is greater than 2
						if (playQueue.size() >= 2) {
							ClientThread p1 = playQueue.remove(0); // popping off the adjacent two players
							ClientThread p2 = playQueue.remove(0);

							GameSession gs = new GameSession(Server.this, p1, p2, callback); // starting a new game
							currentActiveGame = gs; // this variable used for spectators
							p1.setSession(gs);
							p2.setSession(gs);

							//synchronization wooo woooo, making sure the spectators are syncronized in order to ensure they see the correct thing entire time and they go in order
							synchronized (spectatorQueue) {
								for (ClientThread spectator : spectatorQueue) {
									spectator.setSession(gs);
									gs.addSpectator(spectator);
									try {
										spectator.out.writeObject("spectating");
									} catch (Exception ex) {
										ex.printStackTrace();
									}
								}
								spectatorQueue.clear();
							}

							p1.lastOpponent = p2; // whoever their last oppoenent is for rematch logic
							p2.lastOpponent = p1;
							try {
								p1.out.writeObject("game_start:G");
								p2.out.writeObject("game_start:Y");

								p1.out.writeObject("TURN:" + p1.username);
								p2.out.writeObject("TURN:" + p1.username);//this is so our turns are intitialized the second the board loaded->otherwise, first turn is ambig.
							} catch (Exception ex) {
								callback.accept("Failed to notify players of game start.");
							}
						}
					}
					// user list is usesd for the online players, joins them all together and updates the cliensts
					else if (data.equals("get_user_list")) { //need this so we cn update the clients that are online after a game, otherwise, we stuck bc the server didnt receive or send after BroastingtoAll the first time
						String userListMsg = "USERLIST:" + String.join(",", usernames);
						out.writeObject(userListMsg);
					}
					// this when recieves input that a user wants a rematch, making sure we set the vars to false and if we get it from this and their last opponent sending them to a new game
					else if(data.equals("play_again")) {
						this.wantsRematch = true;
						if (this.lastOpponent != null && this.lastOpponent.wantsRematch) { // checking their opponents rematch status
							this.wantsRematch = false;
							this.lastOpponent.wantsRematch = false;
							GameSession gs = new GameSession(Server.this, this, this.lastOpponent, callback);
							this.setSession(gs); //setting teh session for them and the opponent
							this.lastOpponent.setSession(gs);
							try {
								this.out.writeObject("game_start:G");
								this.lastOpponent.out.writeObject("game_start:Y");
							} catch (Exception ex) {
								callback.accept("Failed to start rematch.");
							}
							broadcastUsernamesToAll();
						}
					}
					//if the person clicked back to main or cancle, showing that they don't want a rematch, basically opposite logic from above
					else if (data.equals("cancel_rematch")) {
						this.wantsRematch = false;
						if (this.lastOpponent != null) {
							try {
								this.lastOpponent.wantsRematch = false;
								this.lastOpponent.lastOpponent = null;
								this.lastOpponent.out.writeObject("no_rematch"); //notify waiting player so they know like hey they don;t wanna play with you dawg
							} catch (Exception ex) {
								callback.accept("Error notifying opponent about rematch cancel.");
							}
						}
						this.lastOpponent = null;
					}
					//spectate logic
					else if (data.equals("spectate_request")) {
						synchronized (spectatorQueue) { // syncrhonized queue
							if (currentActiveGame != null) {
								setSession(currentActiveGame);
								try {
									out.writeObject("spectating");
									out.writeObject("BOARDSTATE:" + currentActiveGame.getBoardStateString()); //sending them the most updated board state
									currentActiveGame.addSpectator(this); // adding them to the current game saved
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							} else {
								spectatorQueue.add(this);
								try {
									out.writeObject("waiting_for_game"); // if no game is active making sure they are waiting
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
						}
					}

					//anything else, bad words but just regular message sending between 2 opponents too...
					else {
						String filteredMessage = data;
						for (String word : sensoredWords) {
							if (filteredMessage.toLowerCase().contains(word)) {
								String stars = "*".repeat(word.length()); //looping through the sentences that contain the bad vocab (word) and changing it with *
								filteredMessage = filteredMessage.replaceAll("(?i)" + word, stars); // (?i) makes it case-insensitive
								System.out.println(filteredMessage);
							}
						}
						System.out.println(username + " sent: " + filteredMessage);
						updateClients(username + ": " + filteredMessage); //this is when the clien ttypes anythign with something thats should be sensored....
						if (session != null) {
							String chatMsg = "CHAT:" + username + ": " + filteredMessage;
							session.sendToClientFromServer(this == session.player1 ? session.player2 : session.player1, chatMsg); //sedning the chats ONLY to the 2 clients that are talking per thread in the existing session
							session.sendToClientFromServer(this, chatMsg);
						}
					}

				} catch (Exception e) {
					//for when the user disconnects from the server
					updateClients("❌ " + username + " disconnected."); //when a user disconnects
					clients.remove(this);//removes them from client list
					usernames.remove(username); //removes their username from the list
					broadcastUsernamesToAll(); // updates the username list
					break;
				}
			}
		}
	}
}






