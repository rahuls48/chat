import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// runnable means this class can be passed to a thread or thread pool and can be executed concurrently along side
// other runnable classes
public class Server implements Runnable {
    private ArrayList<ConnectionHandler> connections; //list of all connections

    private ServerSocket server ;
    private boolean done;
    private ExecutorService pool;
    public Server(){
        connections = new ArrayList<>();
        done =false;
    }
    @Override
    public void run(){
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept(); // when accept a connection we get a socket
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }

    }
    //to broadcast the message to all the connections if not null
    public void broadcast(String message){
        for(ConnectionHandler ch: connections){
            if (ch!=null){
                ch.sendMessage(message);
            }
        }
    }
    public void shutdown(){
        try {
            done = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch:connections){
                ch.shutdown();
            }
        }catch (IOException e){
            //ignore
        }
    }


    // we pass the individual client to this class and its going to handle the connections which we accept in above class Server
    class ConnectionHandler implements Runnable{
        private Socket client;
        private BufferedReader in; // bufferedreader used to get stream from the socket when the client sends something 'in'
        private PrintWriter out; // when we want to send something to the client we use out

        public ConnectionHandler(Socket client){
            this.client = client;
        }
        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                out.println("Enter your name:");
                String name = in.readLine();
                System.out.println(name + " has joined the chat.");
                broadcast(name + " has joined the chat.");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("/quit")) {
                        System.out.println(name + " has left the chat.");
                        broadcast(name + " has left the chat.");
                        break;
                    } else if (inputLine.startsWith("/nick ")) {
                        String[] tokens = inputLine.split("\\s+", 2);
                        if (tokens.length == 2) {
                            String newName = tokens[1];
                            System.out.println(name + " changed their name to " + newName);
                            broadcast(name + " changed their name to " + newName);
                            name = newName;
                        }
                    } else {
                        System.out.println(name + ": " + inputLine);
                        broadcast(name + ": " + inputLine);
                    }
                }

                shutdown();
            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message){
            out.println(message);
        }
        public void shutdown(){
            try {
                in.close();
                out.close();
                if(!client.isClosed()){
                    client.close();
                }
            }catch (IOException e){
                //
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

}