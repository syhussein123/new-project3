import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.ArrayList;

public class Client extends Thread {
	Socket socketClient;
	ObjectOutputStream out;
	ObjectInputStream in;
	public volatile String latestMessage = null; // string variable used across multiple threads that needs to be volatile
	public volatile ArrayList<String> latestUsernames = new ArrayList<>(); // arrayList that needs to be volatile because it is used across multiple threads

	//consumer is an interface that represents a function that takes in one argument
	private Consumer<String> messageHandler;
	private Consumer<ArrayList<String>> usernamesHandler;

	// added from saja: constructor for partner’s game message handling
	public Client(Consumer<String> callback) {
		this.messageHandler = callback;
	}

	//default constructor (so my version still works)
	public Client() {}

	//setting the consumer variables based on the input recieved from the server
	public void setMessageHandler(Consumer<String> handler) {
		this.messageHandler = handler;
	}

	public void setUsernamesHandler(Consumer<ArrayList<String>> handler) {
		this.usernamesHandler = handler;
	}

	public void run() {
		try {
			socketClient = new Socket("127.0.0.1", 5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
		} catch (Exception e) {}

		while (true) {
			try {
				Object obj = in.readObject(); // reading in the object that is on the new line
				//if it is a string
				if (obj instanceof String) {
					String message = (String) obj; // saving it to latestMessage variable
					latestMessage = message;
					if (messageHandler != null) {
						messageHandler.accept(message);
					}
					System.out.println(message);
					//if it is an array list, update the usernamesHandler
				} else if (obj instanceof ArrayList) {
					latestUsernames = (ArrayList<String>) obj;
					if (usernamesHandler != null) {
						usernamesHandler.accept(latestUsernames);
					}
					System.out.println("Received usernames: " + latestUsernames);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	//this is to send our own data from the client to the server
	public void send(String data) {
		try {
			out.writeObject(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}