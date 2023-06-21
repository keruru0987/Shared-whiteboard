package Manager;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

import static java.lang.Thread.sleep;

public class ManagerPanel {
    static JList<String> userList = new JList<>();
    static String[] cur_users = ManagerServer.userlist.keySet().toArray(new String[0]);
    static Graphics g;

    static JPanel whiteBoard;
    static String managername = "Manager";

    int x_begin;
    int y_begin;
    int x_end;
    int y_end;
    static int action = 0;

    static Color colour = Color.BLACK;
    static String text;

    String[] draw_type_list = {"Line","Circle", "Oval", "Rect", "Text"};

    public static ArrayList<JSONObject> draw_commands = new ArrayList<>();

    static JTextArea chatArea;

    public ManagerPanel(String manager_name) {
        managername = manager_name + "(manager)";
        JFrame jf = new JFrame();
        jf.setTitle("Manager Whiteboard");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setLayout(new BorderLayout());
        jf.setResizable(false);

        // createCanvasPanel();
        whiteBoard = new JPanel();
        whiteBoard.setBackground(Color.white);
        whiteBoard.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                x_begin = e.getX();
                y_begin = e.getY();
            }

            //The coordinates of the point when the mouse is released
            public void mouseReleased(MouseEvent e) {
                x_end = e.getX();
                y_end = e.getY();

                if (x_begin != x_end && action != 0) {
                    JSONObject json = new JSONObject();
                    try {
                        json.put("command", "DRAW");
                        json.put("draw_type", draw_type_list[action-1]);
                        json.put("x_begin", x_begin);
                        json.put("x_end", x_end);
                        json.put("y_begin", y_begin);
                        json.put("y_end", y_end);
                        json.put("r", colour.getRed());
                        json.put("g", colour.getGreen());
                        json.put("b", colour.getBlue());
                        json.put("name", ManagerPanel.managername);
                    } catch (JSONException ex) {
                        //throw new RuntimeException(ex);
                    }
                    if (text != null) {
                        try {
                            json.put("text", text);
                        } catch (JSONException ex) {
                            //throw new RuntimeException(ex);
                        }
                        text = null;
                    }
                    send(json);
                    try {
                        draw_managerpanel(json);
                    } catch (JSONException | InterruptedException ex) {
                        //throw new RuntimeException(ex);
                    }
                    //System.out.println(1);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        g = whiteBoard.getGraphics();
        jf.getContentPane().add(whiteBoard, BorderLayout.CENTER);

        //createToolBar();
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton lineButton = new JButton("Line");
        lineButton.addActionListener(e -> action = 1);

        JButton circleButton = new JButton("Circle");
        circleButton.addActionListener(e -> action = 2);

        JButton ovalButton = new JButton("Oval");
        ovalButton.addActionListener(e -> action = 3);

        JButton rectangleButton = new JButton("Rectangle");
        rectangleButton.addActionListener(e -> action = 4);

        JButton textButton = new JButton("Text");
        textButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String input = JOptionPane.showInputDialog("Please Input Text", "Enter text:");
                if (input != null && !input.isEmpty()){
                    text = input;
                    action = 5;
                }
            }
        });

        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(e -> colour = JColorChooser.showDialog(jf, "select colour", null));

        toolBar.add(lineButton);
        toolBar.add(circleButton);
        toolBar.add(rectangleButton);
        toolBar.add(ovalButton);
        toolBar.add(textButton);
        toolBar.add(colorButton);

        //file menu
        JButton newButton = new JButton("New");
        newButton.setPreferredSize(new Dimension(80, newButton.getPreferredSize().height));
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(new JLabel(), "Are you sure to new whiteboard?", "New", JOptionPane.YES_NO_OPTION);
                if(result == JOptionPane.YES_OPTION){
                    JSONObject json = new JSONObject();
                    try {
                        json.put("command", "NEW");
                        json.put("name", ManagerPanel.managername);
                    } catch (JSONException e1) {
                        //throw new RuntimeException(e1);
                    }
                    send(json);
                    try {
                        draw_managerpanel(json);
                    } catch (JSONException | InterruptedException ex) {
                        //throw new RuntimeException(ex);
                    }
                }
            }
        });

        JButton openButton = new JButton("open");
        openButton.setPreferredSize(new Dimension(80, openButton.getPreferredSize().height));
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String filename = JOptionPane.showInputDialog("Please Input filename you want to open", "save");
                if (!filename.equals("")){
                    // load and update draw commands
                    ArrayList<JSONObject> loaded_drawcommand_List = new ArrayList<>();
                    try{
                        FileReader fileReader = new FileReader(filename + ".json");
                        StringBuilder sb = new StringBuilder();
                        int ch;
                        while ((ch = fileReader.read()) != -1) {
                            sb.append((char) ch);
                        }
                        fileReader.close();

                        JSONArray jsonArray = new JSONArray(sb.toString());
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            loaded_drawcommand_List.add(jsonObject);
                        }
                    } catch (JSONException | IOException ex) {
                        //throw new RuntimeException(ex);
                    }

                    // update all whiteboard
                    try{
                        JSONObject json = new JSONObject();
                        json.put("command", "NEW");
                        json.put("name", ManagerPanel.managername);
                        send(json);
                        draw_managerpanel(json);
                        for (JSONObject pack:loaded_drawcommand_List){
                            send(pack);
                            draw_managerpanel(pack);
                        }
                    } catch (JSONException | InterruptedException ex) {
                        //throw new RuntimeException(ex);
                    }

                }
                else{
                    JOptionPane.showMessageDialog(jf, "no filename entered", "Open", JOptionPane.ERROR_MESSAGE);
                }

            }
        });

        JButton saveButton = new JButton("save");
        saveButton.setPreferredSize(new Dimension(80, saveButton.getPreferredSize().height));
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String jsonString = new JSONArray(draw_commands).toString();
                String filename = JOptionPane.showInputDialog("Please Input filename", "save");
                if (!filename.equals("")){
                    try {
                        FileWriter fileWriter = new FileWriter(filename + ".json");
                        fileWriter.write(jsonString);
                        fileWriter.close();
                        System.out.println("draw_commands saved");
                        JOptionPane.showMessageDialog(jf, "successfully saved " + filename, "Save", JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException e1) {
                        //e1.printStackTrace();
                    }
                }
                else{
                    JOptionPane.showMessageDialog(jf, "no filename entered", "Save", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton saveasButton = new JButton("saveas");
        saveasButton.setPreferredSize(new Dimension(80, saveasButton.getPreferredSize().height));
        saveasButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.showSaveDialog(null);
                File file =chooser.getSelectedFile();
                String path = file.getAbsolutePath();
                if (!(path.endsWith(".png"))){
                    path += ".png";
                }
                try {
                    savePic(path);
                } catch (JSONException ex) {
                    //throw new RuntimeException(ex);
                }
            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.setPreferredSize(new Dimension(80, closeButton.getPreferredSize().height));
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(new JLabel(), "Are you sure to close whiteboard?", "Close", JOptionPane.YES_NO_OPTION);
                if(result == JOptionPane.YES_OPTION){
                    JSONObject json = new JSONObject();
                    try {
                        json.put("command", "CLOSE");
                        json.put("name", ManagerPanel.managername);
                    } catch (JSONException e1) {
                        //throw new RuntimeException(e1);
                    }
                    send(json);
                    JOptionPane.showMessageDialog(jf, "Server will quit in 5s", "Close", JOptionPane.WARNING_MESSAGE);
                    try {
                        sleep(5000);
                    } catch (InterruptedException ex) {
                        //throw new RuntimeException(ex);
                    }
                    ManagerServer.quit();
                }
            }
        });


        toolBar.add(Box.createHorizontalGlue()); // Add horizontal glue to push button to the right
        toolBar.add(newButton);
        toolBar.add(openButton);
        toolBar.add(saveButton);
        toolBar.add(saveasButton);
        toolBar.add(closeButton);

        jf.getContentPane().add(toolBar, BorderLayout.SOUTH);


        //createUserListPanel();
        JPanel userListPanel = new JPanel();
        userListPanel.setLayout(new BorderLayout());

        JLabel userListLabel = new JLabel("User List");
        userListPanel.add(userListLabel, BorderLayout.NORTH);

        userList = new JList<>(cur_users);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(userList);
        scrollPane.setPreferredSize(new Dimension(150, scrollPane.getPreferredSize().height)); // Set preferred size for the scroll pane
        userListPanel.add(scrollPane, BorderLayout.CENTER);

        JButton kickButton = new JButton("Kick");
        kickButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int i = userList.getSelectedIndex();
                if (i>=0){
                    String kick_name = cur_users[i];
                    if (kick_name.contains("(Manager)")){
                        JOptionPane.showMessageDialog(jf, "You cannot kick yourself", "Kick Fail", JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                        int result = JOptionPane.showConfirmDialog(new JLabel(), "Are you sure to kick " + kick_name, "Exit", JOptionPane.YES_NO_OPTION);
                        if(result == JOptionPane.YES_OPTION){
                            JSONObject json = new JSONObject();
                            try {
                                json.put("command", "USER_KICK");
                                json.put("kick_user_name", kick_name);
                                json.put("name", ManagerPanel.managername);
                            } catch (JSONException e1) {
                                //throw new RuntimeException(e1);
                            }
                            send(json);
                            ManagerServer.userlist.remove(kick_name);
                            update();
                        }
                    }
                }
            }
        });
        userListPanel.add(kickButton, BorderLayout.SOUTH);

        jf.getContentPane().add(userListPanel, BorderLayout.WEST);


        //createChatPanel();
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());

        JLabel chatLabel = new JLabel("Chat");
        chatPanel.add(chatLabel, BorderLayout.NORTH);

        //JTextArea chatArea = new JTextArea();
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPanechat = new JScrollPane(chatArea);
        chatPanel.add(scrollPanechat, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        JTextField inputField = new JTextField();
        inputField.setPreferredSize(new Dimension(200, 30)); // Set preferred size for input field

        inputPanel.add(inputField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = inputField.getText();
                inputField.setText("");
                if(!text.equals("")){
                    JSONObject json = new JSONObject();
                    try {
                        json.put("command", "CHAT");
                        json.put("chattext", text);
                        json.put("name", ManagerPanel.managername);
                    } catch (JSONException ex) {
                        //throw new RuntimeException(ex);
                    }
                    send(json);
                    try {
                        draw_managerpanel(json);
                    } catch (JSONException | InterruptedException ex) {
                        //throw new RuntimeException(ex);
                    }
                }
            }
        });
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        jf.getContentPane().add(chatPanel, BorderLayout.EAST);

        jf.setPreferredSize(new Dimension(1200, 700));

        jf.pack();
        jf.setVisible(true);
    }

    static void update() {
        cur_users = ManagerServer.userlist.keySet().toArray(new String[0]);
        userList.setListData(cur_users);
    }

    static void draw_managerpanel(JSONObject pack) throws JSONException, InterruptedException {
        g = whiteBoard.getGraphics();
        String command = pack.getString("command");

        String name = pack.getString("name");

        if (command.equals("DRAW")){
            draw_commands.add(pack);
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
                //case 6 -> charHistory.setText(charHistory.getText() + "\n" + name + ": " + pack.getString("text"));
            }
        } else if (command.equals("CHAT")) {
            String chat_text = pack.getString("chattext");
            String chathistroy = chatArea.getText();
            if (chathistroy.equals("")){
                chatArea.setText(name + ": " + chat_text);
            }
            else {
                chatArea.setText(chathistroy + "\n" + name + ": " + chat_text);
            }
        } else if (command.equals("NEW")) {
            draw_commands.clear();
            g.setColor(Color.WHITE);
            g.fillRect(0,0, whiteBoard.getWidth(), whiteBoard.getHeight());
        }
    }

    public static synchronized void send(JSONObject json) {
        ManagerServer.recordcommands.add(json);
    }

    private void savePic(String path) throws JSONException {
        BufferedImage pic = new BufferedImage(whiteBoard.getWidth(), whiteBoard.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics graphics = pic.getGraphics();
        graphics.fillRect(0, 0, whiteBoard.getWidth(), whiteBoard.getHeight());
        for (JSONObject pack: draw_commands) {
            String command = pack.getString("command");
            String name = pack.getString("name");
            String draw_type = pack.getString("draw_type");
            assert command.equals("DRAW");
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
                graphics.setColor(color);

            } catch (JSONException ignore) {
            }

            switch (draw_type) {
                case "Line" -> {
                    graphics.drawLine(x_begin, y_begin, x_end, y_end);
                    graphics.setColor(Color.BLUE);
                    graphics.drawString(name, x_end, y_end);
                }
                case "Circle" -> {
                    int max = Math.max(Math.abs(x_begin - x_end), Math.abs(x_begin - x_end));
                    graphics.drawOval(Math.min(x_begin, x_end), Math.min(y_begin, y_end), max, max);

                    graphics.setColor(Color.BLUE);
                    graphics.drawString(name, x_end, y_end);
                }
                case "Oval" -> {
                    graphics.drawOval(Math.min(x_begin, x_end), Math.min(y_begin, y_end), Math.abs(x_begin - x_end), Math.abs(y_begin - y_end));
                    graphics.setColor(Color.BLUE);
                    graphics.drawString(name, x_end, y_end);
                }
                case "Rect" -> {
                    graphics.drawRect(Math.min(x_begin, x_end), Math.min(y_begin, y_end), Math.abs(x_begin - x_end), Math.abs(y_begin - y_end));
                    graphics.setColor(Color.BLUE);
                    graphics.drawString(name, x_end, y_end);
                }
                case "Text" -> {
                    graphics.drawString(pack.getString("text"), x_begin, y_begin);
                    graphics.setColor(Color.BLUE);
                    graphics.drawString(name, x_end, y_end);
                }
            }
        }
        try {
            ImageIO.write(pic, "PNG", new File(path));
            JOptionPane.showMessageDialog(new JLabel(), "Picture successfully saved!", "saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(new JLabel(), "save picture failed", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }



    public static void main(String[] args) {
        new ManagerPanel("jenny");
    }


}
