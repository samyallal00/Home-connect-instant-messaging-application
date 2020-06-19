import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Server implements Runnable{

    /*
    *                                          The Brain of the Server
    *                        Main Server Class dealing with back-end part of the project
    *                Sets up the sockets, ports and info. Starts up a new UI window if chat is accepted
    * */

    private ObjectOutputStream output; private ObjectInputStream input;

    private ServerSocket server; private Socket connection;

    private int port; private String user_name; private String other_name;
    private final User_interface UI;

    /* constructor, sets up the ports only */
    public Server(int port, String name, User_interface UI){
        this.user_name = name;
        this.port = port;
        this.UI = UI;
    }

    /* set up and run the server */
    public void server_start_running(){

        try{
            /* First, create the server with the given port number*/
            server = new ServerSocket(port, 100);

            /* Loop until end of program*/
            while (true){
                try{
                    connection = null;

                    /* Wait for a connection first, for a client to reach out to connect*/
                    wait_for_connection();

                    /* Once a client has been found, set up connections, and notify the UI to see which actions to take*/
                    set_up_streams();
                    UI.get_notified_of_incoming_client(other_name);

                    /* is_ready will be different than 0 only if the user responds to the UI, pickup or hangup*/
                    while (UI.is_ready() == 0);

                    /* If user hangs up, send a message to END the conversation, reset everything, close connections and loop again*/
                    if(UI.is_ready() == 2)
                    { send_one_way_message("END"); UI.reset_is_ready(true); close_connection(); continue; }


                    /* Otherwise, if user picks up. Run a new Thread with the GUI for the chat window*/
                    Thread client_connected = new Client_handler(connection, input, output);
                    client_connected.start();

                    /* reset the UI is ready flag back to 0*/
                    UI.reset_is_ready(true);

                }catch(EOFException ex){                                    // end of stream exception
                    //show_message("\n======> Server ended the connection <====== ");  // comes here when conversation is done
                    close_connection();
                } catch (ClassNotFoundException e) {
                    System.out.println("Wait problem");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //close_crap();
            //show_message("Discussion ended.");

        }catch (IOException exception){
            exception.printStackTrace();
        }
    }


    /* wait for a connection, until one found*/
    private void wait_for_connection() throws IOException, ClassNotFoundException {
        connection = server.accept();
    }


    /* get stream to send and receive data */
    private void set_up_streams() throws IOException, ClassNotFoundException {
        output = new ObjectOutputStream(connection.getOutputStream());
        output.flush();

        input = new ObjectInputStream(connection.getInputStream());

        /* Send a one way message for the client to get the name and read the client's name*/
        if(input != null) {
            send_one_way_message(user_name);
            other_name = ((String) input.readObject()).split(" ")[0];
        }
    }

    /* send a one way message to client, without showing it */
    private void send_one_way_message(String message){

        try{
            output.writeObject(user_name +" ->   " + message);
            output.flush();

        } catch (IOException ex){
            //chat_window.append("\n ERROR: I can't send that message.");
        }
    }


    /* close streams and sockets after you are done chatting */
    private void close_connection(){
        //show_message("\n Closing connections.. \n");
        //able_to_type(false);

        try{
            output.close();
            input.close();
            connection.close();

        } catch (IOException ex){
            ex.printStackTrace();
        }
    }


    /*
    *                   The Server's Thread starts here
    *               Runs the Server's back-end look for connections
    * */

    @Override
    public void run() {
        server_start_running();
    }


    /*
    *                   Helper Functions
    * */

    public void set_other_name(String o_name) {this.other_name = o_name;}
    public void set_port(int port) {this.port = port;}


    /* ************************************************************************************************************** */

    /*
    *               The Graphical User Interface for the discussion window
    *           The Client handler client is created running on a new thread each time
    *        a user wants to connect, to allow multiple connections going at the same time
    * */


    private class Client_handler extends Thread{

        private ObjectOutputStream output; private ObjectInputStream input;
        private Socket connection;

        private JFrame frame; private JTextField user_text; private JTextArea chat_window;


        /* Constructor, builds up the sockets and the UI */
        Client_handler(Socket connection, ObjectInputStream input, ObjectOutputStream output){
            this.connection = connection;
            this.input = input;
            this.output = output;

            frame = new JFrame(user_name);

            user_text = new JTextField();
            user_text.setEditable(false);
            user_text.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    send_message(e.getActionCommand());
                    user_text.setText("");
                }
            });
            frame.add(user_text, BorderLayout.NORTH);

            chat_window = new JTextArea();
            chat_window.setEditable(false);
            chat_window.setFont(new Font("Times", Font.PLAIN,  14));
            frame.add(new JScrollPane(chat_window));
            frame.setSize(500,300);
            frame.setVisible(true);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    UI.reset_is_ready(false);
                }
            });
        }



        /* during the chat conversation */
        private void while_chatting() throws IOException{
            /* Sends to the client who it is connected to first*/
            String message = "@Connected to: " + user_name;
            send_message(message);

            /* Shows the message in the server's window*/
            show_message("========> Now connected to: " + other_name + " <========\n\n");

            able_to_type(true);

            /* while the connection is still on, meaning the client did not yet END the discussion*/
            do{
                if(UI.is_ready() == 2) break;

                // have a conversation
                try{
                    message = (String) input.readObject();

                    show_message("\n" + message);

                }catch (ClassNotFoundException classNotfoundException){
                    show_message("\n I don't know wtf that user sent!");
                }

            }while (!message.equals(user_name + " ->   END") && !message.equals(other_name + " ->   END"));

            /* Once the connection is over, close the connection and reset the chat*/
            close_crap(); reset_the_chat();
            UI.reset_UI();
        }


        /* send a message to client */
        private void send_message(String message){

            try{
                output.writeObject(user_name +" ->   " + message);
                output.flush();

                if(!message.equals("END") && !message.equals("@Connected to: " + user_name))
                    show_message("\n" + user_name +" ->   " + message);

            } catch (IOException ex){
                chat_window.append("\n ERROR: I can't send that message.");
            }
        }


        /* updates chat window */
        private void show_message(final String text){
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    chat_window.append(text);
                }
            });
        }


        /* let the user type stuff into their box */
        private void able_to_type(final boolean is_able){
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    user_text.setEditable(is_able);
                }
            });
        }


        /* Resets the window chat back to displaying nothing*/
        private void reset_the_chat(){
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    user_text.setText(""); user_text.setEditable(false);
                    chat_window.setText(""); chat_window.setEditable(false);
                }
            });
        }

        /* close streams and sockets after you are done chatting */
        private void close_crap(){
            //show_message("\n Closing connections.. \n");
            able_to_type(false);

            try{
                output.close();
                input.close();
                connection.close();

            } catch (IOException ex){
                ex.printStackTrace();
            }
        }



        /* The thread starts running here, if the thread starts it means connections have been set up already*/
        @Override
        public void run() {
            try {

                while_chatting();
                
            } catch (IOException ex) {
                show_message("\n======> Server ended the connection <====== ");  // comes here when conversation is done
                reset_the_chat(); this.display_screen(false);
                close_crap(); UI.reset_UI();
            }
        }


        /*
        *               Helper Functions for the GUI
        * */

        public void display_screen(boolean display){ frame.setVisible(display); }
        public void dispose_screen() {frame.dispose();}
    }





}
