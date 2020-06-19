import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TimerTask;

public class User_interface extends JFrame{
    // User, and core variables of the program
    private final String user_name;
    private final String IPaddress; private final int port; private boolean is_server;

    // Core variable of the socket connection
    private final Server server; private Client client;
    private Thread server_thread; private Thread client_thread;

    //GUI variables of the program
    private JTable m_table_of_users; private String[][] m_table_data; private String[] m_table_column;
    private int index = 0;
    private HashMap<Integer, String> m_map_of_all_contacts;
    private JTextField m_accept_call; private JButton m_pick_up_call; private JButton m_hang_up;

    //Timer that keeps checking whether the threads should be kept alive
    private final java.util.Timer timer;


    User_interface(String user_name, String IPaddress, int port, boolean is_server){
        super(user_name);
        this.user_name = user_name;
        this.IPaddress = IPaddress;
        this.is_server = is_server;
        this.port = port;
        this.server = new Server(port, user_name, this);
        this.timer = new java.util.Timer();
        display_GUI();
        kill_the_client_keep_server_alive();
    }


    private void display_GUI(){
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setSize(600, 300); this.setVisible(true);
        this.add(new JScrollPane(m_table_of_users));

        m_table_data = new String[10][1];   m_table_column = new String[1];  m_map_of_all_contacts = new HashMap<>();
        m_table_column[0] = "Users";

        m_table_of_users = new JTable(m_table_data, m_table_column);
        m_table_of_users.setRowHeight(60); m_table_of_users.setSize(600, 300);
        m_table_of_users.setFont(new Font("Times", Font.BOLD, 20));
        m_table_of_users.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
        {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
            {
                final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(new Color(30,30,160,50));
                return c;
            }
        });
        set_row_action();
        this.add(m_table_of_users, BorderLayout.CENTER);

        m_accept_call = new JTextField("No calls at the moment.");
        m_accept_call.setEditable(false);
        m_accept_call.setFont(new Font("Times", Font.PLAIN, 25));
        this.add(m_accept_call, BorderLayout.NORTH);

        m_pick_up_call = new JButton("Pick up");

        m_hang_up = new JButton("Hang-up");

        load_users_data();
        reset_UI();

        server_thread = new Thread(server);
        server_thread.start();
    }



    // ready to  pick up or hang up the call flag
    private int is_ready = 0;
    public int is_ready(){ return is_ready;} public void reset_is_ready(boolean reset) {is_ready = reset?0:2;}

    // the server object gets notified when a call was made by the client,
    public void get_notified_of_incoming_client(String client) throws InterruptedException {
        //if(client == null) { return; }
        //server_thread.interrupt();
        is_ready = 0;
        m_accept_call.setText("Incoming call from: " + client);
        this.add(m_pick_up_call, BorderLayout.EAST);
        this.add(m_hang_up, BorderLayout.SOUTH);

        this.validate(); this.repaint();

        m_pick_up_call.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                is_ready = 1;
                //server.display_screen(true);
                m_accept_call.setText("Call Accepted.");
                User_interface.super.remove(m_hang_up); User_interface.super.remove(m_pick_up_call);
                User_interface.this.validate(); User_interface.this.repaint();
                //server_thread.notify();
            }
        });


        m_hang_up.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                is_ready = 2;
                m_accept_call.setText("Call Declined.");
                User_interface.super.remove(m_hang_up); User_interface.super.remove(m_pick_up_call);
                User_interface.this.validate(); User_interface.this.repaint();
                //server_thread.notify();
            }
        });

        this.validate(); this.repaint();

    }


    /* load the users data, from a txt file that contains the IP address, ports, and names of the contacts*/
    public void load_users_data(){
        try{
            File users = new File("/Users/samyallal/Desktop/users.txt");

            Scanner myReader = new Scanner(users);
            while (myReader.hasNext()){
                String str = myReader.nextLine(); String[] data = str.split(" ");
                if(data[0].equals("") || data[0].equals(user_name))
                    continue;

                m_map_of_all_contacts.put(index, str);

                m_table_data[index][0] = data[0];
                index++;
            }
            myReader.close();
        } catch (FileNotFoundException ignored){ }
    }


    /* Sets the action of when a row is clicked, to make a call to chat*/
    public void set_row_action(){
        m_table_of_users.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JTable target = (JTable)e.getSource();
                int row = target.getSelectedRow();

                if(row >= index)
                    return;

                /* Get all information about the person we would like to call from the data table*/
                String other_name =  m_map_of_all_contacts.get(row).split(" ")[0];
                String other_IP = m_map_of_all_contacts.get(row).split(" ")[1];
                int other_port = Integer.parseInt(m_map_of_all_contacts.get(row).split(" ")[2]);

                /* Get Rid of the old thread running as a server, because when you click, a call is made*/
                client = new Client(other_IP,other_port,user_name,User_interface.this);
                client.set_other_name(other_name);
                client.setLocation(User_interface.this.getX() + 630, User_interface.this.getY());

                client_thread = new Thread(client);
                client_thread.start();
            }
        });
    }


    /* When the discussion, the timer catches it and makes sure to call the garbage collector on the client object
    *  It also catches any problems if the server is down, it restarts it
    * */
    public void kill_the_client_keep_server_alive(){
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                if(client != null && client.get_is_ready_to_be_killed()){
                    client = null;
                    client_thread.interrupt(); client_thread = null;
                    is_ready = 0;
                    Runtime.getRuntime().gc();
                }

                if(!server_thread.isAlive()) server_thread.start();

            }
        },10,10);
    }


    /* Simply resets the text field back to the original text*/
    public void reset_UI(){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                User_interface.this.m_accept_call.setText("No Incoming calls for the moment.");
                User_interface.this.validate();
                User_interface.this.repaint();
            }
        });
    }


}
