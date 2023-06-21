package Manager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class ManagerServer {
    static String address = "localhost";
    static int port = 8888;
    static String username = "Manager";
    static ServerSocket listeningSocket = null;
    public static ArrayList<JSONObject> recordcommands = new ArrayList<>();
    public static HashMap<String, Socket> userlist = new HashMap<>();


    public static void main(String[] args) throws JSONException {
        if (args.length >= 3) {
            try {
                address = args[0];
                port = Integer.parseInt(args[1]);
                username = args[2];
            } catch (Exception e){
                System.out.println("Input error");
                System.exit(1);
            }
        } else {
            address = "localhost";
            port = 8888;
            username = "Manager";
            System.out.println("Launch by default settings");
        }

        try {
            listeningSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("create serverSocket fail");
        }

        userlist.put(username + "(Manager)", new Socket());

        JSONObject update_user = new JSONObject();
        update_user.put("command", "UPDATE_USER_ADD");
        update_user.put("name", username + "(Manager)");
        update_user.put("new_user_name", username + "(Manager)");
        ManagerServer.recordcommands.add(update_user);

        new ManagerPanel(username);

        Socket clientSocket = null;

        while(true){
            System.out.println("Server listening on port for a connection");
            try {
                clientSocket = listeningSocket.accept();
                System.out.println("connect client succ");
                // connection set, create worker thread
                Connection clientConnection = new Connection(clientSocket);
                clientConnection.start();
            } catch (IOException e) {
                System.out.println("server stop listening");
                break;
            } catch (JSONException e) {
                //throw new RuntimeException(e);
            }
        }
    }

    static void quit(){
        System.exit(0);
    }


}
