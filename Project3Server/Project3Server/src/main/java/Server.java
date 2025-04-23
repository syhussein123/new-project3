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

	TheServer server;
	private Consumer<String> callback;

	public Server(Consumer<String> callback) {
		this.callback = callback;
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

		public void setSession(GameSession session) {
			this.session = session;
		}

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
						c.out.writeObject(new ArrayList<>(usernames)); //sends out as arrayList
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
					} else if (data.equals("play_request")) {
						boolean alreadyWaiting = false;
						for(ClientThread c : playQueue) {
							if(c.username.equalsIgnoreCase(this.username)) {
								alreadyWaiting = true;
								break;
							}
						}
						if(!alreadyWaiting){
							playQueue.add(this);
							updateClients(username + " wants to play!");
						}
						if (playQueue.size() >= 2) {
							ClientThread p1 = playQueue.remove(0);
							ClientThread p2 = playQueue.remove(0);

							GameSession gs = new GameSession(p1, p2, callback);
							p1.setSession(gs);
							p2.setSession(gs);
							try {
								p1.out.writeObject("game_start:G");
								p2.out.writeObject("game_start:Y");
							} catch (Exception ex) {
								callback.accept("Failed to notify players of game start.");
							}
						}
					} else {
						System.out.println(username + " sent: " + data);
						updateClients(username + ": " + data); //this is to update the clients

						if (session != null) {
							//sedning the chats ONLY to the 2 clients that are talking per thread in the existing session
							session.sendToClientFromServer(this == session.player1 ? session.player2 : session.player1, "CHAT:" + username + ": " + data);
							session.sendToClientFromServer(this, "CHAT:" + username + ": " + data);
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
