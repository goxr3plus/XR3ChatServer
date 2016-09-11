package application;

import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * This class stores information about the client that was connected
 * 
 * @author GOXR3PLUS
 *
 */
public class Client {

	private Socket socket;
	private String userName;
	private String password;
	private String genre;
	private LocalDate lastLoggedInDate;
	private LocalTime lastLoggedInHour;

	/**
	 * RESIDENT
	 * 
	 * @param socket
	 * @param userName
	 * @param password
	 * @param genre
	 */
	public Client(Socket socket, String userName, String password, String genre) {
		setSocket(socket);
		setUserName(userName);
		setPassword(password);
		setGenre(genre);
		setLastLoggedInDate(LocalDate.now());
		setLastLoggedInHour(LocalTime.now());
	}

	/**
	 * GUEST
	 * 
	 * @param socket
	 * @param userName
	 * @param genre
	 */
	public Client(Socket socket, String userName, String genre) {
		setSocket(socket);
		setUserName(userName);
		setGenre(genre);
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	@Override
	public String toString() {
		return userName;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public LocalTime getLastLoggedInHour() {
		return lastLoggedInHour;
	}

	public void setLastLoggedInHour(LocalTime lastLoggedInHour) {
		this.lastLoggedInHour = lastLoggedInHour;
	}

	public LocalDate getLastLoggedInDate() {
		return lastLoggedInDate;
	}

	public void setLastLoggedInDate(LocalDate lastLoggedInDate) {
		this.lastLoggedInDate = lastLoggedInDate;
	}

}
