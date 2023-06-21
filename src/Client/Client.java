package Client;

import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Client {

    private static String address;
    private static int port;
    static String username;
    static PrintStream printStream;
    static JTextArea area;
    static Graphics g;
    static Socket socket;

    static ArrayList<String> current_users = new ArrayList<>();

    public static void main(String[] args) {
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
            username = "user";
            System.out.println("Launch by default settings");
        }

        //WaitPanel wp = new WaitPanel();


        try {
            socket = new Socket(address, port);
            JSONObject json = new JSONObject();
            json.put("command", "ASK_JOIN");
            send(json);
            ClientPanel cp = new ClientPanel(username);
            g = cp.getUI();
            new Read();
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(area, "unknown host, please check the host!", "Fail", JOptionPane.ERROR_MESSAGE);
        }catch (ConnectException e){
            JOptionPane.showMessageDialog(area, "Cannot connect to server", "Fail", JOptionPane.ERROR_MESSAGE);
        } catch (IOException | JSONException e) {
            JOptionPane.showMessageDialog(area, "Unknown error occurred", "Fail", JOptionPane.ERROR_MESSAGE);
        }
    }

    public synchronized static void send(JSONObject json){
        try {
            printStream = new PrintStream(Client.socket.getOutputStream());
            json.put("name", Client.username);
            printStream.println(json);
            printStream.flush();
            ClientPanel.action = 0;
        } catch (IOException | JSONException e) {
            JOptionPane.showMessageDialog(area, "Unknown error occurred", "Fail", JOptionPane.ERROR_MESSAGE);
        }
    }

    static class Read extends Thread{

        BufferedReader bufferedReader;

        public Read() throws JSONException {
            JSONObject cache = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                cache = new JSONObject(bufferedReader.readLine());
            } catch (IOException | JSONException e) {

            }
            assert cache != null;
            if (cache.getBoolean("agree")){
                JOptionPane.showMessageDialog(new JLabel("error"), "Manager has approved your request", "", JOptionPane.INFORMATION_MESSAGE);
                start();
            }else {
                String reason = cache.getString("reason");
                if (reason.equals("Decline")){
                    JOptionPane.showMessageDialog(new JLabel("error"), "Manager denied your request", "Join Wrong", JOptionPane.ERROR_MESSAGE);
                } else if (reason.equals("Duplicate")){
                    JOptionPane.showMessageDialog(new JLabel("error"), "The username is already occupied, please use other name", "Join Wrong", JOptionPane.ERROR_MESSAGE);
                }
                System.exit(0);
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String buffer = bufferedReader.readLine();
                    if (buffer != null) {
                        JSONObject pack = new JSONObject(buffer);
                        String command = pack.getString("command");
                        String name = pack.getString("name"); // who send the command

                        if (command.equals("DRAW")) {
                            String draw_type = pack.getString("draw_type");
                            int x_begin = 0;
                            int y_begin = 0;
                            int x_end = 0;
                            int y_end = 0;
                            try {
                                x_begin = pack.getInt("x_begin");
                                y_begin = pack.getInt("y_begin");
                                x_end = pack.getInt("x_end");
                                y_end = pack.getInt("y_end");
                                Color color = new Color(pack.getInt("r"), pack.getInt("g"), pack.getInt("b"));
                                g.setColor(color);
                            } catch (JSONException ignore) {
                            }
                            switch (draw_type) {
                                case "Line" -> {
                                    g.drawLine(x_begin, y_begin, x_end, y_end);

                                    g.setColor(Color.BLUE);
                                    g.drawString(name, x_end, y_end);
                                }
                                case "Circle" -> {
                                    int max = Math.max(Math.abs(x_begin - x_end), Math.abs(x_begin - x_end));
                                    g.drawOval(Math.min(x_begin, x_end), Math.min(y_begin, y_end), max, max);

                                    g.setColor(Color.BLUE);
                                    g.drawString(name, x_end, y_end);
                                }
                                case "Oval" -> {
                                    g.drawOval(Math.min(x_begin, x_end), Math.min(y_begin, y_end), Math.abs(x_begin - x_end), Math.abs(y_begin - y_end));

                                    g.setColor(Color.BLUE);
                                    g.drawString(name, x_end, y_end);
                                }
                                case "Rect" -> {
                                    g.drawRect(Math.min(x_begin, x_end), Math.min(y_begin, y_end), Math.abs(x_begin - x_end), Math.abs(y_begin - y_end));

                                    g.setColor(Color.BLUE);
                                    g.drawString(name, x_end, y_end);
                                }
                                case "Text" -> {
                                    g.drawString(pack.getString("text"), x_begin, y_begin);

                                    g.setColor(Color.BLUE);
                                    g.drawString(name, x_end, y_end);
                                }
                            }
                        }

                        else if (command.equals("UPDATE_USER_ADD")){
                            String new_user_name = pack.getString("new_user_name");
                            current_users.add(new_user_name);

                            ClientPanel.cur_users = current_users.toArray(new String[0]);
                            ClientPanel.userList.setListData(ClientPanel.cur_users);
                        }

                        else if (command.equals("USER_EXIT")){
                            String exit_user_name = pack.getString("name");
                            current_users.remove(exit_user_name);

                            ClientPanel.cur_users = current_users.toArray(new String[0]);
                            ClientPanel.userList.setListData(ClientPanel.cur_users);
                        }

                        else if (command.equals("USER_KICK")){
                            String kick_user_name = pack.getString("kick_user_name");
                            if (kick_user_name.equals(username)){
                                JOptionPane.showMessageDialog(new JLabel("error"), "Manager has kicked you out", "Kicked", JOptionPane.ERROR_MESSAGE);
                                //socket.close();
                                System.exit(0);
                            }
                            else {
                                current_users.remove(kick_user_name);
                                ClientPanel.cur_users = current_users.toArray(new String[0]);
                                ClientPanel.userList.setListData(ClientPanel.cur_users);
                            }
                        }

                        else if (command.equals("CHAT")) {
                            String chat_text = pack.getString("chattext");

                            String chathistroy = ClientPanel.chatArea.getText();
                            if (chathistroy.equals("")){
                                ClientPanel.chatArea.setText(name + ": " + chat_text);
                            }
                            else {
                                ClientPanel.chatArea.setText(chathistroy + "\n" + name + ": " + chat_text);
                            }
                        }

                        else if (command.equals("NEW")) {
                            g.setColor(Color.white);
                            g.fillRect(0,0, ClientPanel.whiteBoard.getWidth(), ClientPanel.whiteBoard.getHeight());
                        }

                        else if (command.equals("CLOSE")) {
                            ClientPanel.jf.setVisible(false);
                            int result = JOptionPane.showConfirmDialog(new JLabel(), "Manager has closed the whiteboard, Server will close in few seconds", "Close", JOptionPane.YES_NO_OPTION);
                            if(result == JOptionPane.YES_OPTION){
                                System.exit(0);
                            }
                            else {
                                wait(3000);
                                System.exit(0);
                            }
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                JOptionPane.showMessageDialog(area, "Unknown error occurred", "Fail", JOptionPane.ERROR_MESSAGE);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}