import javax.jws.soap.SOAPBinding;
import java.net.Socket;
import java.util.HashMap;

/**
 * Created by Krishna on 11/18/2016.
 */
public class User {
    String username;
    String password;
    boolean loggedIn;
    boolean isGameLeader;
    boolean isPlaying;
    Socket clientSocket;
    int score;
    int numfooled;
    int numtimesfooled;

    public static void main(String[] args) {

    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setLoggedIn(boolean value) {
        this.loggedIn = value;
    }

    public User(boolean loggedIn, Socket clientSocket) {
        this.setLoggedIn(loggedIn);
        this.clientSocket = clientSocket;
    }

    public User() {
        this.loggedIn=false;
        this.isGameLeader = false;
        this.isPlaying = false;
    }
}