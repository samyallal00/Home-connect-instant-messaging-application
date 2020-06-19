import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Client extends JFrame implements Runnable{

    private JTextField user_text; private JTextArea chat_window;

    private ObjectOutputStream output; private ObjectInputStream input;

    private String message = "";
    private String serverIP; private int port; private Socket connection;

    private String this_name; private String other_name; private boolean kill_switch;
    private User_interface UI;

    // Constructor
    public Client(String IPaddress, int port, String user_name, User_interface UI){
        super(user_name);
        this.serverIP = IPaddress; this.port = port;

        this.this_name = user_name; this.kill_switch = false;
        this.UI = UI;

        this.user_text = new JTextField();
        this.user_text.setEditable(false);
        this.user_text.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                send_message(e.getActionCommand());
                user_text.setText("");
            }
        });
        this.add(user_text, BorderLayout.NORTH);

        this.chat_window = new JTextArea();
        this.chat_window.setEditable(false);
        this.chat_window.setFont(new Font("Times", Font.PLAIN, 14));
        this.add(new JScrollPane(chat_window), BorderLayout.CENTER);
        this.setSize(500,300);
        this.setVisible(true);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(connection ==null) { kill_switch = true; return;}

                send_message("END");
                close_crap();
                kill_switch = true;
            }
        });
    }


    // connect to server
    public void client_start_running(){
        // go on loop attempting to connect to the server
        //show_message("<!--- --- --- ---- -- Attempting connection.. -- ---- --- --- --->\n");
        while(true) {
            try {
                connect_to_server();

                set_up_streams();

                if(while_chatting()) break;

            } catch (EOFException ex) {
                show_message("\n Client terminated connection");
                reset_the_chat(); display_screen(false);
                break;
            } catch (IOException | ClassNotFoundException IOex) {
                //IOex.printStackTrace();
            }
        }

        close_crap();
        show_message("\n\n========> Discussion ended <========");
    }


    // connect to server
    private void connect_to_server() throws IOException{

        connection = new Socket(InetAddress.getByName(serverIP), port);     // 6789
        //show_message("Connected to: " + connection.getInetAddress().getHostName());
    }


    // set up streams to send and receive messages
    private void set_up_streams() throws IOException, ClassNotFoundException {
        output = new ObjectOutputStream(connection.getOutputStream());
        output.flush();

        input = new ObjectInputStream(connection.getInputStream());
        //show_message("\n Your streams are now good to go! \n");

        if(input != null) {
            send_message(this_name);
            other_name = ((String) input.readObject()).split(" ")[0];
            show_message("=========> Waiting for: " + other_name + " <=========\n");
        }
    }


    // while chatting with server
    private boolean while_chatting() throws IOException{
        able_to_type(true);

        boolean first_message = true;
        do{
            try{
                message = (String) input.readObject();

                if(first_message){
                    first_message = false;
                    show_message(message.split("@")[1] + "\n");
                }else
                    show_message("\n" + message);

            } catch (ClassNotFoundException ex){
                show_message("\n I don't know that object type.");
            }

        } while (!message.equals(this_name + " ->   END") && !message.equals(other_name + " ->   END"));

        return true;
    }


    // close the streams and sockets
    private void close_crap(){
        //show_message("\n Closing connection..");
        able_to_type(false);

        try{
            output.close();
            input.close();
            connection.close();

        } catch (IOException ex){
            ex.printStackTrace();
        }
    }


    // send messages to the server
    private void send_message(String message){

        try{
            output.writeObject(this_name +" ->   " + message);
            output.flush();

            if(!message.equals("END") && !message.equals(this_name))
                show_message("\n" + this_name +" ->   " + message);

        } catch (IOException ex){
            chat_window.append("\n Something Went Wrong sending message!");
        }
    }


    // update the chat window
    private void show_message(final String message){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                chat_window.append(message);
            }
        });
    }


    // gives user permission to type crap into the text box
    private void able_to_type(final boolean isAble){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                user_text.setEditable(isAble);
            }
        });
    }



    private void reset_the_chat(){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                user_text.setText(""); user_text.setEditable(false);
                chat_window.setText(""); chat_window.setEditable(false);
            }
        });
    }
    


    public void display_screen(boolean display){ this.setVisible(display); }
    public void dispose_screen() {this.dispose();}
    public void set_serverIP_host(String host) {this.serverIP = host;}
    public void set_other_name(String o_name) {this.other_name = o_name;}
    public void set_port(int port) {this.port = port;}

    public boolean get_is_ready_to_be_killed() {return kill_switch;}

    @Override
    public void run() {
        client_start_running();
        kill_switch = true;
    }
}
