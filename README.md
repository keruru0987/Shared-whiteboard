# COMP90015 Distributed System Assignment 2: Shared White Board

Name: Renzhe Hu

Student ID: 1341712

----

## Problem Context

This project implements a shared whiteboard system. Multiple users can simultaneously draw and interact on a whiteboard, ensuring that the whiteboard remains the same for everyone at all times. To start the whiteboard, an administrator creates a whiteboard, and other users who wish to join need to obtain the administrator's approval. The administrator has the ability to kick users, and users can join or leave at any time. The administrator can create new whiteboards, save the current whiteboard, or open a saved whiteboard. Users should also be able to chat with each other.

## System Architecture

In this project, I continued the implementation of the Client-Server architecture from Assignment 1. I implemented a server using the `thread-per-connection` architecture, where a work thread is created for every connection. This architecture allows for concurrent handling of multiple client connections. The interaction protocol I choose is `TCP` to ensure the reliable connection. In order to facilitate communication between the client and server, the `JSON format` will be used. 

This system mainly consists of two parts: the Manager (Server) and the Client. As shown in the diagram below, if a user wants to perform an operation on the whiteboard, they first send the command message to the Server. The Server then broadcasts the command to all users, ensuring that all whiteboards are updated simultaneously for everyone.

![arch](C:\Users\79336\Desktop\report_ds_a2_md\arch.png)

##  Implementation Details

This section introduces the Server and Client separately.

### Manager（Server）

![server_class](C:\Users\79336\Desktop\report_ds_a2_md\server_class.png)

- ManagerServer

  The entry point for the Server is the `ManagerServer` class. Within `ManagerServer`, it is responsible for maintaining a user list (`userlist`) of currently connected users and recording all commands (`recordcommands`). When the ManagerServer starts, it creates a whiteboard(`ManagerPanel`) for the manager and adds the manager to the user list. Afterwards, the ManagerServer waits for client connections and creates a `connection` for each client to serve them. Each client connection is handled individually to ensure concurrent handling of multiple clients.

- ManagerPanel

  The following image shows the `ManagerPanel`. The left area consists of a user list and a kick button, while the right area is the chat region. The bottom-left section contains various drawing tools, and the bottom-right section contains some administrator functionalities, including opening a new whiteboard, opening a previously saved whiteboard, saving the current whiteboard, saving the current whiteboard as an image, and closing the whiteboard. The largest area in the middle represents the whiteboard.

  ![image-20230526034149834](C:\Users\79336\Desktop\report_ds_a2_md\image-20230526034149834.png)

  In the ManagerPanel, there is also a `userlist` to keep track of users currently connected to the whiteboard. It is synchronized with the `userlist in the ManagerServer` through the `update`method in the class.

  When the `ManagerPanel` performs an operation, it first calls the `draw_managerpanel` method to  apply the operation locally. Then, it calls the synchronized `send` method to add the command to the command list(`recordcommands`) in the` ManagerServer`, which is then broadcasted to other users by the server.

  It's important to note that in the `ManagerPanel`, all the drawing commands for the current displayed whiteboard are stored in the  list `draw_commands`. This list is cleared whenever a new whiteboard is created by the manager. Whenever something new is drawn on the whiteboard, the corresponding command is added to this list. This list is maintained to facilitate the Save and Open operations.

  During the Save process, the `draw_commands` are saved, preserving all the drawing commands. During the Open operation, a new whiteboard is created first, and then the commands in the `draw_commands` list are redrawn on the whiteboard to restore the previous state.

- Connection

  When a client connects, a work thread `Connection` is created. Within the `Connection`, the `agreejoin` method is used to ask the manager whether to allow the client to join. If the manager agrees, an acceptance  response will sent to the client, and the user is added to the userlist.
  
  Subsequently, the `Connection` continuously receives requests from the client, updates the `ManagerPanel`, and adds commands to the `recordcommands` list. Within each Connection, there is also a class called `WriteThread`. This class contains a pointer that ensures all commands in the `recordcommands` list of the ManagerServer are executed.

### Client

![packageclient](C:\Users\79336\Desktop\report_ds_a2_md\packageclient.png)

- Client

  After launching the `Clien`t, it will first requests permission from the Manager to join the shared whiteboard. If the Manager rejects the request or if the username is duplicated from others, an appropriate message is displayed, and the Client exits. If the Manager approves the request, the` ClientPanel` is displayed, and the commands recorded in the ManagerServer are executed to synchronize with the whiteboard of other users.

  The Client implements a synchronized `send` method to send commands to the Server. When sending a command, the sender's name is included. The Client also maintains a `cur_users` list to keep track of the currently connected users. Whenever commands such as a new user joining or a user exiting are received, the `cur_users` list is updated accordingly.

- ClientPanel

  ![image-20230526195411446](C:\Users\79336\Desktop\report_ds_a2_md\image-20230526195411446.png)

  
  
  The image displays the `ClientPanel`, which has a layout similar to the `ManagerPanel`, with the difference being the absence of the kick button. Additionally, only the Exit tool is retained in the bottom-right section. When the client clicks the Exit button, they can exit the whiteboard.

## Communication Protocols and Message Formats


The communication protocol used is TCP. The key to achieving synchronization between all whiteboards lies in the command list( `recordcommands` )maintained in the ManagerServer. Each command in the list includes the command type and the name of the user who posed the command. Through continuous synchronization performed by each Connection, the commands in the `recordcommands` list are executed, ensuring synchronization among all whiteboards.

For example, if a user drags the mouse on the whiteboard to draw a line, a command containing information such as the starting mouse position, ending mouse position, and username is sent to the Server. The Server adds this command to the `recordcommands` list and broadcasts it to all other users. Simultaneously, the Server (Manager) updates its own whiteboard(`ManagerPanel`).

The commands are transmitted in JSON format. Here are the formats for some important commands:

1. DRAW

   - commands: DRAW
   - name: username or manager name
   - draw_type: one of ["Line","Circle", "Oval", "Rect", "Text"]
   - x,y: the position of mouse
   - rgb: color
   - text: put text if the draw_type is TEXT

   When receiving DRAW commands, the whiteboard will draw shape or text and the name will be displayed near the shape or text.

2. USER_EXIT

   - commands: USER_EXIT
   - name: client name

   When receiving USER_EXIT commands, the Server

   and the Client will update the userlist.

3. CHAT

   - commands: CHAT
   - name: username or manager name
   - chattext: the chat information

   When receiving this command, display name + chattext in the chat area.

4.  UPDATE_USER_ADD

   Only manager could send this command. Manager will send this command when a new user is connected.

   - commands: UPDATE_USER_ADD
   - new_user_name: the name of the new user
   - name: manager name

   When receiving this command, Client will update the userlist.

5. USER_KICK

   Only manager could send this command. Manager will send this command when kick a user.

   - commands: USER_KICK
   - kick_user_name: the name of the kicked user
   - name: manager name

   When client received this command, they will check if they were the kicked. If a client finds they are kicked, the client will close the ClientPanel then exit. For other users, they will update the userlist.

6. CLOSE

   Only manager could send this command. Manager will send this command when close the whiteboard and the Server will close after 5s.

   - commands: CLOSE
   - name: manager name

   When client received this command, they will close the ClientPanel and show message: Manager has closed the whiteboard.

## Demo

start server, manager name: jenny

![image-20230526232527657](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526232527657.png)

![image-20230526232538758](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526232538758.png)

a new manager white board is created, and the manager name is in the user list.

![image-20230526233418036](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526233418036.png)

and user could draw on the white board, and send chat in chat window.

now start a new user eric

![image-20230526232909460](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526232909460.png)

![image-20230526232927820](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526232927820.png)

manager will receive the message

![image-20230526232939151](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526232939151.png)

if manager agree, client will get the message, then join the shared whiteboard.

![image-20230526233435525](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526233435525.png)

the whiteboard is same.

manager could save whiteboard as image, or save the current whiteboard as JSON format and restore it using the open button. or open a new whiteboard by new button.

![image-20230526233544737](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526233544737.png)

a new user alice is joined.

![image-20230526233816824](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526233816824.png)

manager could kick the user

![image-20230526233859812](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526233859812.png)

then the kicked user will receive the message.

![image-20230526233911628](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526233911628.png)

when manager close the white board

![image-20230526234048919](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526234048919.png)

other user will close their whiteboard immediately, and show message

![image-20230526234148191](C:\Users\79336\AppData\Roaming\Typora\typora-user-images\image-20230526234148191.png)

I have finished all the basic feature and advanced feature in this project.

## new innovations

Following the implementation of Zoom's whiteboard, after drawing an shape on the whiteboard, the name of the user who drew the shape will be displayed at the edge of the shape.

