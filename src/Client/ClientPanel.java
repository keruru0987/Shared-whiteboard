package Client;


import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class ClientPanel {


    static Graphics g;
    static JPanel whiteBoard;

    int x_begin;
    int y_begin;
    int x_end;
    int y_end;
    static int action = 0;

    static Color colour = Color.BLACK;
    static String text;

    static JList<String> userList = new JList<>();
    static String[] cur_users = {} ;

    String[] draw_type_list = {"Line","Circle", "Oval", "Rect", "Text"};

    static JTextArea chatArea;
    static JFrame jf;

    public ClientPanel(String clientname) {
        jf = new JFrame();
        jf.setTitle("Client Whiteboard: " + clientname);
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
                    } catch (JSONException ex) {

                    }
                    if (text != null) {
                        try {
                            json.put("text", text);
                        } catch (JSONException ex) {

                        }
                        text = null;
                    }
                    Client.send(json);

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
        toolBar.add(ovalButton);
        toolBar.add(rectangleButton);
        toolBar.add(textButton);
        toolBar.add(colorButton);

        JButton exitButton = new JButton("Exit"); // Add Exit button
        exitButton.setPreferredSize(new Dimension(80, exitButton.getPreferredSize().height));
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(new JLabel(), "Are you sure to exit?", "Exit", JOptionPane.YES_NO_OPTION);
                if(result == JOptionPane.YES_OPTION){
                    JSONObject json = new JSONObject();
                    try {
                        json.put("command", "USER_EXIT");
                    } catch (JSONException e1) {

                    }
                    Client.send(json);
                    System.exit(0);
                }
            }
        });


        toolBar.add(Box.createHorizontalGlue()); // Add horizontal glue to push Exit button to the right
        toolBar.add(exitButton); // Add Exit button

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
                    } catch (JSONException ex) {

                    }
                    Client.send(json);
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

    public Graphics getUI(){
        return whiteBoard.getGraphics();
    }

    public static void main(String[] args) {
        new ClientPanel("jenny");
    }


}


