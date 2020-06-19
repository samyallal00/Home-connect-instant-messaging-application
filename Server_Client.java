//import java.io.*;
//import java.net.*;
//import java.awt.*;
//import java.awt.event.*;
//import javax.swing.*;
//
//public class Server_Client extends JFrame implements Runnable{
//
//    private JTextField user_text;
//    private JTextArea chat_window;
//
//    private ObjectOutputStream output;
//    private ObjectInputStream input;
//
//    private ServerSocket server;
//    private Socket connection;
//
//    private String message = "";
//
//    private String serverIP;
//    private int port;
//    private final String this_name;
//    private String other_name;
//
//    private boolean is_server;
//
//    private final User_interface UI;
//
//
//    // constructor
//    public Server_Client(String host_if_client, int port, String name, boolean is_server, User_interface ui){
//        super(name);
//        this.UI = ui;
//
//        this.serverIP = host_if_client;
//        this.port = port;
//
//        this.this_name = name;
//        this.is_server = is_server;
//
//        user_text = new JTextField();
//        user_text.setEditable(false);
//        user_text.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                send_message("");
//                send_message(e.getActionCommand());
//                user_text.setText("");
//            }
//        });
//        this.add(user_text, BorderLayout.NORTH);
//
//        chat_window = new JTextArea();
//        chat_window.setEditable(false);
//        this.add(new JScrollPane(chat_window));
//        this.setSize(500,300);
//        this.setVisible(true);
//
//    }
//
//
//    // connect to server
//    public void start_running_as_client(){
//        is_server = false;
//        // go on loop attempting to connect to the server
//        show_message("Attempting connection.. \n");
//        while(true) {
//            try {
//                connect_to_server();
//
//                set_up_streams();
//
//                if(while_chatting()) break;
//
//            } catch (EOFException ex) {
//                show_message("\n\nClient terminated connection");
//                break;
//            } catch (IOException | ClassNotFoundException IOex) {
//                //IOex.printStackTrace();
//            }
//        }
//
//        close_crap();
//        show_message("\nDiscussion ended.");
//    }
//
//
//    // connect to server
//    private void connect_to_server() throws IOException, ClassNotFoundException {
//        connection = new Socket(InetAddress.getByName(serverIP), port);
//    }
//
//
//
//    private boolean break_out = false;
//    public void set_break(boolean break_out) {this.break_out = break_out;}
//
//    // set up and run the server
//    public void start_running_as_server(){
//        is_server = true; Boolean resume = null;
//
//        try{
//            if(server != null)
//                server.close();
//            server = new ServerSocket(port, 100);      // 6789 //change these numbers to port
//
//            while (true){
//                try{
//                    //connect and have a conversation
//                    String connection_found = wait_for_connection();
//
//                    set_up_streams();
//
//                    if(connection_found != null) {
//                        if (break_out) { break_out = false; break; }
//                        UI.get_notified_of_incoming_client(connection_found);
//
//                        //while (UI.is_ready() == 0);
//                        //Thread.yield();
//                    }else{ break_out = false; break; }
//
//                    if(while_chatting()) break;
//
//                }catch(EOFException ex){                                    // end of stream exception
//                    show_message("\n\nServer ended the connection! ");  // comes here when conversation is done
//                    break;
//                }
//            }
//
//        }catch (IOException | ClassNotFoundException exception){
//            exception.printStackTrace();
//        }
//
//        show_message("Discussion ended.");
//        this.dispose_screen();
//        close_crap();
//
//    }
//
//
//    // wait for a connection, then once connected, display connection information
//    private String wait_for_connection() throws IOException, ClassNotFoundException {
//        show_message("Waiting for someone to connect.. \n");
//
//        if(break_out) return null;
//
//        connection = server.accept();
//        return connection.getInetAddress().getHostName();
//    }
//
//
//    // get stream to send and receive data
//    private void set_up_streams() throws IOException, ClassNotFoundException {
//        output = new ObjectOutputStream(connection.getOutputStream());
//        output.flush();
//
//        input = new ObjectInputStream(connection.getInputStream());
//        show_message("\nStreams are now set-up \n");
//
//        if(input != null) {
//            send_message(this_name);
//            other_name = ((String) input.readObject()).split(" ")[0];
//            show_message("\nConnected to: " + other_name);
//        }
//    }
//
//
//    // during the chat conversation
//    private boolean while_chatting() throws IOException, ClassNotFoundException {
//        String message = "You are now connected! ";
//        //send_message(message);
//
//        able_to_type(true);
//
//        do{
//            if(UI.is_ready() == 2) send_message("END");
//            // have a conversation
//            try{
//                if(input.readObject() == null)
//                    return true;
//
//                message = (String) input.readObject();
//                show_message("\n" + message);
//
//            }catch (ClassNotFoundException classNotfoundException){
//                show_message("\n I don't know wtf that user sent!");
//            }
//
//        }while (!message.equals(this_name + " - END") && !message.equals(other_name + " - END"));
//
//        return true;
//    }
//
//
//    // close streams and sockets after you are done chatting
//    public void close_crap(){
//        show_message("\n\nClosing connections.. \n");
//        able_to_type(false);
//
//        try{
//            if(output != null)
//                output.close();
//
//            if(input != null)
//                input.close();
//
//            if(connection != null)
//                connection.close();
//
//        } catch (IOException ex){
//            ex.printStackTrace();
//        }
//    }
//
//    // because there is a delay, so message will be sent 2 times, and only the second appears
//    private int message_ratio = 0;
//    // send a message to client
//    private void send_message(String message){
//        if(output == null)
//            return;
//
//        try{
//            output.writeObject(this_name + " - " + message);
//            output.flush();
//
//            if(message_ratio % 2 == 0)
//                show_message("\n" + this_name +" - " + message);
//            message_ratio++;
//
//        } catch (IOException ex){
//            chat_window.append("\n ERROR: I can't send that message.");
//        }
//    }
//
//
//    // updates chat window
//    private void show_message(final String text){
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                chat_window.append(text);
//            }
//        });
//    }
//
//
//    // let the user type stuff into their box
//    private void able_to_type(final boolean is_able){
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                user_text.setEditable(is_able);
//            }
//        });
//    }
//
//
//    @Override
//    public void run() {
//        if(is_server){
//            start_running_as_server();
//        }else
//            start_running_as_client();
//
//        //UI.restart_as_server();
//    }
//
//
//
//
//    // All Helper functions, Mostly called by other classes
//
//    public void display_screen(boolean display){ this.setVisible(display); }
//    public void dispose_screen() {this.dispose();}
//
//    public void set_is_server_true(boolean is_server) {this.is_server = is_server;}
//    public void set_serverIP_host(String host) {this.serverIP = host;}
//    public void set_other_name(String o_name) {this.other_name = o_name;}
//    public void set_port(int port) {this.port = port; this.validate(); this.repaint();}
//
//}
