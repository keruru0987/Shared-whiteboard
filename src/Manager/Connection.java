package Manager;

import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Connection extends Thread{
    Socket clientSocket = null;
    BufferedReader reader;
    PrintStream writer;
    String clientname;

    public Connection(Socket socket) throws IOException, JSONException {
        this.clientSocket = socket;

        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            System.out.println();
        }

        JSONObject request = new JSONObject(reader.readLine());
        agreeJoin(request);

        new WriteThread(clientname, writer);
    }

    public void agreeJoin(JSONObject request) throws JSONException, IOException {
        if (request.getString("command").equals("ASK_JOIN")) {
            clientname = request.getString("name");
            JSONObject response = new JSONObject();
            if (ManagerServer.userlist.containsKey(clientname)) {
                response.put("agree", false);
                response.put("reason", "Duplicate");
                writer.println(response);
                writer.flush();
                clientSocket.close();
            }
            else {
                int result = JOptionPane.showConfirmDialog(new JLabel(), clientname + " want to connect, agree?", "New connection", JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION) {
                    response.put("agree", true);
                    ManagerServer.userlist.put(clientname, clientSocket);
                    ManagerPanel.update();
                    writer.println(response);
                    writer.flush();

                    JSONObject update_user = new JSONObject();
                    update_user.put("command", "UPDATE_USER_ADD");
                    update_user.put("name", ManagerServer.username + "(manager)");
                    update_user.put("new_user_name", clientname);
                    //ManagerServer.recordcommands.add(update_user);
                    update(update_user);
                } else {
                    response.put("agree", false);
                    response.put("reason", "Decline");
                    writer.println(response);
                    writer.flush();
                    clientSocket.close();
                }
            }
        }
    }

    @Override
    public void run() {
        while (ManagerServer.userlist.containsKey(clientname)){

            String buffer = null;
            try {
                buffer = reader.readLine();
            } catch (IOException e) {
                System.out.println("User Disconnected");
            }

            if (buffer != null) {
                JSONObject json = null;
                try {
                    json = new JSONObject(buffer);
                    ManagerPanel.draw_managerpanel(json);
                } catch (JSONException | InterruptedException e) {

                }
                update(json);

                try {
                    if (json.getString("command").equals("USER_EXIT")){
                        String clientname = json.getString("name");
                        clientSocket.close();
                        ManagerServer.userlist.remove(clientname);
                        ManagerPanel.update();
                    }
                } catch (JSONException | IOException e) {

                }
            }


            try {
                sleep(50);
            } catch (InterruptedException e) {

            }
        }
    }

    public synchronized void update(JSONObject pack) {
        ManagerServer.recordcommands.add(pack);
    }

    static class WriteThread extends Thread {

        int pointer = 0;
        PrintStream writer;
        String name;
        Socket clientSocket = null;

        public WriteThread(String name, PrintStream writer) throws JSONException {
            this.name = name;
            this.writer = writer;


            while (pointer < ManagerServer.recordcommands.size()) {
                JSONObject pack = ManagerServer.recordcommands.get(pointer);
                if (pack.getString("command").equals("USER_KICK")){
                    if (pack.getString("kick_user_name").equals(name)){
                        JSONObject json = new JSONObject();
                        try {
                            json.put("command", "USER_EXIT");
                            json.put("name", name);
                        } catch (JSONException e1) {

                        }
                        writer.println(json);
                        writer.flush();
                       pointer ++;
                       continue;
                    }
                }
                writer.println(pack);
                writer.flush();
                pointer++;

            }

            start();
        }

        @Override
        public void run() {
            try {
                //Communicate with client
                while (true) {
                    if (pointer < ManagerServer.recordcommands.size()) {
                        JSONObject pack = ManagerServer.recordcommands.get(pointer);
                        writer.println(pack);
                        writer.flush();
                        pointer++;
                        if (pack.getString("command").equals("USER_KICK")){
                            if (pack.getString("kick_user_name").equals(name)){
                                writer.close();
                                break;
                            }
                        }
                    }
                    sleep(50);
                }
            } catch (InterruptedException e) {
                JOptionPane.showMessageDialog(new JLabel(""), "Unknown error occurred", "Fail", JOptionPane.ERROR_MESSAGE);
            } catch (JSONException e) {

            }
        }
    }


}
