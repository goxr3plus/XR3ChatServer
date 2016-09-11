package application;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Server extends Application implements Runnable, Initializable {

	@FXML
	private Button startServer;

	@FXML
	private Button stopServer;

	@FXML
	TextArea textArea;

	@FXML
	TextField guestTimeLimit;

	@FXML
	private TextField maximumGuestsField;

	@FXML
	private TextField portField;

	int serverPort = 4444;
	ServerSocket serverSocket;
	Thread thread;
	Socket socket;
	PrintWriter writer;

	// Clients list
	ArrayList<Client> clients;
	// General messages List
	ArrayList<String> roomMessages;
	// maximum Guests in room
	int maximumGuests = 2;

	/**
	 * Pattern used from Server to cut clients and messages
	 */
	private static final String PATTERN = "><:><";

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {

		// BorderPane
		BorderPane borderPane = FXMLLoader.load(getClass().getResource("/fxml/Server.fxml"));

		// Stage+Scene
		stage.setTitle("XR3Chat Server");
		stage.setScene(new Scene(borderPane));
		stage.setX(0);
		stage.setY(0);
		stage.setOnCloseRequest(c -> {
			stopServer();
			System.exit(0);
		});
		stage.show();

	}

	@Override
	public void run() {

		/*********
		 * Accepting all the client connections and create a seperate thread
		 ******/
		while (thread != null) {
			try {

				////////// Accepting the Server Connections ///////
				socket = serverSocket.accept();

				////////// Create a separate Thread for that each client ///////
				new ServerClientCommunication(this, socket);
				Platform.runLater(() -> {
					textArea.appendText("\nNew Client Connected....");
				});

				Thread.sleep(150);
			} catch (InterruptedException | IOException ex) {
				stopServer();
			}
		}
		Platform.runLater(() -> {
			textArea.setText("Server has stopped!");
		});

	}

	/**
	 * Starts the Server
	 */
	private void startServer() {

		////////// Initialize the Server Socket ///////
		try {

			serverSocket = new ServerSocket(serverPort);
			System.out.println(serverSocket.getInetAddress().getCanonicalHostName());
		} catch (IOException e) {
			e.printStackTrace();
			textArea.setText(e.getMessage());
			return;
		}

		////////// Initialize the ArrayLists //////////
		clients = new ArrayList<>();
		roomMessages = new ArrayList<>();

		////////// Initialize the thread //////////
		thread = new Thread(this);
		thread.start();

		////////// Configure the Buttons //////////
		startServer.setDisable(true);
		textArea.setText("Waiting for clients to connect....");
	}

	/**
	 * Stop the Server
	 */
	private void stopServer() {

		if (serverSocket != null) {
			// Disconnect All Clients
			if (clients != null)
				clients.stream().forEach(client -> sendMessageToClient(client.getSocket(), "DISC"));

			thread = null;

			////////// Close Server Socket //////////
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				// serverSocket = null;
				clients = null;
				roomMessages = null;
			}

			////////// Configure the Buttons //////////
			startServer.setDisable(false);
		}
	}

	/**
	 * Add this User to usersList
	 * 
	 * @param userName
	 */
	public boolean addUser(Socket socket, String userName, String password, String genre) {

		// No duplicate names Allowed
		if (clients.stream().anyMatch(client -> client.getUserName().equals(userName))) {
			sendMessageToClient(socket, "DISC" + "Dublicate Name Found!Try Other UserName");
			return false;
		}

		// (Guest+guest.size()>maximumGuest)=DISCONNECT
		if ("GUEST".equals(genre)
				&& clients.stream().filter(client -> "GUEST".equals(client.getGenre())).count() >= maximumGuests) {
			sendMessageToClient(socket, "DISC" + "Maximum Guests allowed:(" + maximumGuests + ")");
			return false;
			// Resident
		} else {
			// ..some code is it is resident
		}

		// clients room empty?
		if (!clients.isEmpty()) {
			StringBuilder builder = new StringBuilder();

			//////// Prepare the ROOM List //////////////
			for (Client client : clients)
				builder.append(client.getUserName() + PATTERN + client.getGenre() + ";");

			textArea.appendText("\nROOMLS" + builder.toString());

			//////// Send a Room List To New Client////////
			sendMessageToClient(socket, "ROOMLS" + builder.toString());

			///////// Send the New Client Detail into All Other Clients
			///////// //////
			clients.stream().forEach(client -> {
				sendMessageToClient(client.getSocket(), "ADD" + userName + PATTERN + genre);
				sendMessageToClient(client.getSocket(), "(" + userName + ")" + "has joined the room...");
			});

		}
		/***** Add a user in to array list ***/
		clients.add(new Client(socket, userName, password, genre));
		return true;

	}

	/**
	 * Lists all Room Clients except the caller
	 * 
	 * @param socket
	 * @param userName
	 */
	public String listUsers(String userName) {
		StringBuilder builder = new StringBuilder();

		//////// Prepare the ROOM List //////////////
		for (Client client : clients)
			if (!client.getUserName().equals(userName))
				builder.append(client.getUserName() + ";");

		return builder.toString();
	}

	String info = null;

	/**
	 * Get client Informations
	 * 
	 * @param userName
	 * @return
	 */
	public String getInfoForClient(String userName) {

		clients.stream().filter(client -> client.getUserName().equals(userName)).forEach(client -> {
			// LocalTime now = LocalTime.now();
			// long z = client.getLastLoggedInHour(). -now;

			info = client.getLastLoggedInDate() + PATTERN + client.getLastLoggedInHour();
		});

		return info;
	}

	/**
	 * Lists all the room messages
	 * 
	 * @return the list with room messages <br>
	 *         Every message has this form userName+"><:><"+message <br>
	 *         and you can get every message doing split by "><++><"
	 */
	public String listMessages() {
		StringBuilder builder = new StringBuilder();

		//////// Prepare the ROOM List //////////////
		for (String roomMessage : roomMessages)
			builder.append(roomMessage + "><++><");

		return builder.toString();
	}

	/**
	 * Removes this User from usersList
	 * 
	 * @param userName
	 */
	public void removeUser(Socket socket, String userName) {

		///////// Remove this Client from All other Clients Details//////
		Iterator<Client> iterator = clients.iterator();
		while (iterator.hasNext()) {
			Client client = iterator.next();
			if (client.getUserName().equals(userName)) {
				iterator.remove();
				break;
			}
		}
		sendMessageToClient(socket, "DISC" + "BYE!");

		///////// Notify all the Clients about Removed Client //////
		clients.stream().forEach(client -> {
			sendMessageToClient(client.getSocket(), "REMOVE" + userName);
			sendMessageToClient(client.getSocket(), "(" + userName + ")" + " left the room...");
		});

		textArea.appendText("\nConnected Users Now are: (" + clients.size() + ")");
	}

	/**
	 * Sends a general Message to the Room
	 * 
	 * @param clientSocker
	 * @param userName
	 */
	public void sentGeneralMessage(String userName, String message) {

		// Add it to Array so It can be seen
		// From Future Clients that log in this room
		roomMessages.add(userName + PATTERN + message);

		textArea.appendText("\nGeneral message:(" + message + ")");

		///////// Notify all the Clients about the Message //////
		for (Client client : clients)
			if (!client.getUserName().equals(userName))
				sendMessageToClient(client.getSocket(), "GMESS" + userName + PATTERN + message);

	}

	/**
	 * Sends a private message to a user
	 * 
	 * @param clientSocket
	 * @param fromClient
	 * @param message
	 * @param toClient
	 */
	public void sentPrivateMessage(String fromClient, String message, String toClient) {

		textArea.appendText(
				"\nPrivate message(from->" + fromClient + " to->" + toClient + ") Message: (" + message + ")");

		///////// Notify all the Clients about the Message //////
		for (Client client : clients)
			if (client.getUserName().equals(toClient))
				sendMessageToClient(client.getSocket(), "PMESS" + fromClient + PATTERN + message + PATTERN + toClient);

	}

	/***** Function To Send a Message to Client **********/
	public void sendMessageToClient(Socket clientSocket, String message) {
		if (clientSocket != null && !clientSocket.isClosed()) {
			try {
				writer = new PrintWriter(clientSocket.getOutputStream(), true);
				writer.println(message);
			} catch (IOException ex) {
				ex.printStackTrace();
				// writer.close();
			}
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		maximumGuestsField.disableProperty().bind(startServer.disabledProperty());
		portField.disableProperty().bind(startServer.disabledProperty());
		guestTimeLimit.disableProperty().bind(startServer.disabledProperty());
		stopServer.disableProperty().bind(startServer.disabledProperty().not());

		// StartServer
		startServer.setOnAction(a -> startServer());

		// StopServer
		stopServer.setOnAction(a -> stopServer());

		// infoArea
		textArea.setPrefWidth(300);
		textArea.setWrapText(true);
		textArea.setEditable(false);
		textArea.setText("Press to start the Server....");

		portField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.matches("\\d"))
				portField.setText(newValue.replaceAll("\\D", ""));
			if (portField.getText().length() > 5)
				portField.setText(newValue.substring(0, 5));

			if (portField.getText().isEmpty())
				portField.setText("4444");

			serverPort = Integer.parseInt(portField.getText());
		});

		maximumGuestsField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.matches("\\d"))
				maximumGuestsField.setText(newValue.replaceAll("\\D", ""));
			if (maximumGuestsField.getText().length() > 5)
				maximumGuestsField.setText(newValue.substring(0, 5));

			if (maximumGuestsField.getText().isEmpty())
				maximumGuestsField.setText("10");
			else if ("0".equals(maximumGuestsField.getText()))
				maximumGuestsField.setText("1");

			maximumGuests = Integer.parseInt(maximumGuestsField.getText());

		});

		guestTimeLimit.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.matches("\\d"))
				guestTimeLimit.setText(newValue.replaceAll("\\D", ""));
			if (guestTimeLimit.getText().length() > 10)
				guestTimeLimit.setText(newValue.substring(0, 10));

			if (guestTimeLimit.getText().isEmpty())
				guestTimeLimit.setText("60");
			else if (guestTimeLimit.getText().equals("0"))
				guestTimeLimit.setText("1");
		});
	}

}
