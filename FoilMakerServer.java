
import javax.jws.soap.SOAPBinding;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by Kailu on 11/19/16.
 */
public class FoilMakerServer {
    static ArrayList<String> sug = new ArrayList<>();
    static HashMap<String, User> user = new HashMap<String, User>();//collection of users with their user tokens
    static HashMap<String, ArrayList> suggestion = new HashMap<String, ArrayList>();//collection of suggestions with their game token as an ID
    static HashMap<Integer, Socket> clientSockets = new HashMap<Integer, Socket>(); //collection of sockets to currently connected clients. Stored with client IDs as key

    static String userToken;
    static String gameToken;
    static String question;
    static String answer;
    static User object = new User();



    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = null;
        //Socket clientSocket;
        PrintWriter outToClient;
        BufferedReader inFromClient;
        String clientMessage;
        String[] message;
        String status = null;
        int x;
        int y;
        int z;

        FoilMakerServer fms = new FoilMakerServer();
        fms.user = new HashMap<String, User>();
        User object = new User();
//        fms.hm.put("x",object);

        try {
            serverSocket = new ServerSocket(9999);
            object.clientSocket = serverSocket.accept();
            Thread t1 = new Thread();
            t1.start();
            outToClient = new PrintWriter(object.clientSocket.getOutputStream(), true);
            inFromClient = new BufferedReader(
                    new InputStreamReader(object.clientSocket.getInputStream()));

            while (true) {
                clientMessage = inFromClient.readLine();
                message = clientMessage.split("--");

                BufferedReader br = new BufferedReader(new FileReader("database.txt"));
                String line;
                while (!(((line = br.readLine())) == null)) {
                    object.username = String.valueOf(line.toCharArray()[0]);
                    object.password = String.valueOf(line.toCharArray()[1]);
                    object.setLoggedIn(false);
                    fms.user.put(object.username, object);
                }

                if (clientMessage.contains("CREATENEWUSER")) {
                    if (message.length != 3) {
                        outToClient.println("RESPONSE--CREATENEWUSER--" +
                                "INVALIDMESSAGEFORMAT");
                    }
                    status = fms.RegisterUser(message[1], message[2]);
                    if (status.equals("SUCCESS")) {
                        PrintWriter write = new PrintWriter("database.txt");
                        write.println(message[1] + ":" + message[2] + ":" +
                                0 + ":" + 0 + ":" + 0);
                        outToClient.println("RESPONSE--CREATENEWUSER--" +
                                status + "--");
                        write.close();
                        object.setLoggedIn(false);
                        object.username = message[1];
                        object.password = message[2];
                        object.isGameLeader = false;
                        fms.user.put(message[1], object);
                        System.out.println("Username: " + object.username + "\npass:" + object.password + "\nLogged in: " + object.loggedIn);
                        System.out.println(String.valueOf(fms.user.get(message[1])) + "\n" + message[1]);
                    } else {
                        outToClient.println("RESPONSE--CREATENEWUSER--" +
                                status + "--");
                    }
                } else if (clientMessage.contains("LOGIN")) {
                    if (message.length != 3) {
                        outToClient.println("RESPONSE--LOGIN--" +
                                "INVALIDMESSAGEFORMAT--");
                    }
                    status = fms.LoginUser(message[1], message[2]);
                    if (status.equals("SUCCESS")) {
                        object.setLoggedIn(true);
                        object.username = message[1];
                        object.password = message[2];
                        object.isGameLeader = false;
                        fms.user.put(message[1], object);
                        fms.userToken = createToken(10);
                        String eg = fms.userToken;
                        outToClient.println("RESPONSE--LOGIN--" +
                                "SUCCESS--" + fms.userToken);
                    } else {
                        outToClient.println("RESPONSE--LOGIN--" +
                                status + "--");
                    }

                } else if (clientMessage.contains("STARTNEWGAME")) {
                    String providedToken = message[1];
                    if (!(fms.userToken.equals(providedToken))) {
                        status = "USERNOTLOGGEDIN";
                        outToClient.println("RESPONSE--STARTNEWGAME--" + status);
                    }
                    status = fms.StartNewGame(fms.userToken);
                    if (status.equals("SUCCESS")) {
                        object.isPlaying = true;
                        object.isGameLeader = true;
                        fms.gameToken = createToken(3);
                        outToClient.println("RESPONSE--STARTNEWGAME--SUCCESS--" + fms.gameToken);
                    }
                    //TODO check for internal error in server(failure)
                } else if (clientMessage.contains("JOINGAME")) {
                    String providedUserToken = message[1];
                    String providedGameToken = message[2];
                    if (!(providedUserToken.equals(fms.userToken))) {
                        status = "USERNOTLOGGEDIN";
                        outToClient.println("RESPONSE--JOINGAME--" + status);
                    }
                    if (!(providedGameToken.equals(fms.gameToken))) {
                        status = "GAMEKEYNOTFOUND";
                        outToClient.println("RESPONSE--JOINGAME--" + status);
                    }
                } else if (clientMessage.contains("ALLPARTICIPANTSHAVEJOINED")) {
                    status = aphj(message[1], message[2]);
                    if (status.equals(null)) {
                        BufferedReader read = new BufferedReader(new FileReader("WordleDeck.txt"));
                        String[] qna = read.readLine().split(";");
                        question = qna[0];
                        answer = qna[1];
                        outToClient.println("NEWGAMEWORD--" + question + "--" + answer);
                    } else if (clientMessage.contains("PLAYERSUGGESTION")) {
                        if (message.length != 3) {
                            outToClient.println("RESPONSE--LOGIN--" +
                                    "INVALIDMESSAGEFORMAT--");
                        }
                        status = playerSug(message[1], message[2], message[3]);
                        if (status.equals(null)) {
                            ArrayList<String> sugs = new ArrayList<>();
                            sugs = suggestion.get(message[2]);
                            outToClient.println("ROUNDOPTIONS--" + sugs.get(0) + "--" + sugs.get(1) + answer);

                        } else {
                            outToClient.println("RESPONSE--PLAYERSUGGESTION--" + status);
                        }
                    } else if (clientMessage.contains("PLAYERCHOICE")) {
                        if (message.length != 3) {
                            outToClient.println("RESPONSE--LOGIN--" +
                                    "INVALIDMESSAGEFORMAT--");
                        }
                        status = playerCho(message[1], message[2], message[3]);
                        if (status.equals(null)) {

                            if (message[3].equals(answer)) {
                                object.score += 10;
                            }
                            else{
                                System.out.println("You were fooled by " );
                                object.numtimesfooled++;
                            }
                        } else {
                            outToClient.write("RESPONSE--PLAYERCHOICE--" + status);
                        }
                    }
                    else if(clientMessage.contains("LOGOUT")){
                        if(object.loggedIn) outToClient.write("RESPONSE--LOGOUT--SUCCESS");
                        else{
                            outToClient.write("RESPONSE--LOGOUT--USERNOTLOGGEDIN");
                        }
                    }

                }
            }catch(IOException e){
                e.printStackTrace();
            }catch(Exception e){
                System.out.println("Exception");
            }
        }
    }

    public  String RegisterUser(String username, String password) throws IOException {

        if (username == null) {
            return "INVALIDUSERNAME";
        }
        if (password == null) {
            return "INVALIDPASSWORD";
        }
        if (username.length() > 10) {
            return "INVALIDUSERNAME";
        }
        if (password.length() > 10) {
            return "INVALIDUSERNAME";
        }
        if (!(username.matches("[A-za-z0-9_]+"))) {
            return "INVALIDUSERNAME";
        }
        char pass[] = password.toCharArray();
        if (!(password.matches("[A-za-z0-9#&$*]+"))) {
            return "INVALIDPASSWORD";
        }
        BufferedReader br = new BufferedReader(new FileReader("database.txt"));
        String line;
        while (!(((line = br.readLine())) == null)) {
            if (line.contains(username)) {
                return "USERALREADYEXISTS";
            }
        }
        return "SUCCESS";
    }

    public  String LoginUser(String username, String password) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("database.txt"));
        String line;
        String pass = null;
        int flag = 0;
        while (!(((line = br.readLine())) == null)) {
            if (line.contains(username)) {
                flag = 1;
                pass = line.split(":")[1];
            }
        }
        if (flag == 0) {
            return "UNKNOWNUSER";
        }
        if (!(password.equals(pass))) {
            return "INVALIDUSERPASSWORD";
        }

//        System.out.println("LOOK" + this.hm.get(username).username);
//        if (this.hm.get(username).loggedIn){
//            return "USERALREADYLOGGEDIN";
//        }

        return "SUCCESS";
    }

    public String StartNewGame(String userToken){
        if (object.isPlaying){
            return "FAILURE";
        }
        return "SUCCESS";
    }
    public static String createToken(int length) {
        char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        gameToken = sb.toString();
        return gameToken;
    }
    public static String aphj(String uToken, String gToken){
        if(!(uToken.equals(userToken))) return "USERNOTLOGGEDIN";
        else if(!(gToken.equals(gameToken))) return "INVALIDGAMETOKEN";
        else if(!(object.isGameLeader)) return "USERNOTGAMELEADER";
        else return null;
    }
    public static String playerSug(String uToken, String gToken, String sug){
        if(!(uToken.equals(userToken))) return "USERNOTLOGGEDIN";
        else if(!(gToken.equals(gameToken))) return "INVALIDGAMETOKEN";
        else{
            suggestion.put(gToken,new ArrayList());
            suggestion.get(gToken).add(sug);
            return null;
        }

    }
    public static String playerCho(String uToken, String gToken, String cho){
        if(!(uToken.equals(userToken))) return "USERNOTLOGGEDIN";
        else if(!(gToken.equals(gameToken))) return "INVALIDGAMETOKEN";
            //TODO: unexpected message type
        else return null;
    }

}


