import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.nio.file.*;

public class ftpserver {

    // set of file names
    public static Set<String> files = new HashSet<String>(); 
    public static Set<Integer> goodClients = new HashSet<Integer>();
    public static Set<Integer> badClients = new HashSet<Integer>();

    private static int sPort;   //The server will be listening on this port number
    private static String username;
    private static String password;

    public static void main(String[] args) throws Exception {

        if(args.length!=1){
            System.out.println("bad syntax");
            System.out.println("run : java ftpserver <port>");
            return;
        }

        try{
            sPort = Integer.parseInt(args[0]);
        }
        catch(Exception e){
            System.out.println("Bad port");
            return;
        }

        BufferedReader x= new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Set up credentials for server");
        System.out.println("Username : ");
        username = x.readLine();
        System.out.println("Password : ");
        password = x.readLine();
        
        System.out.println(username);
        System.out.println(password);
        System.out.println("Server successfully set up");
        
        ServerSocket listener = new ServerSocket(sPort);

        System.out.println("The server is running."); 
        int clientNum = 1;
        try {
            while(true) {
                new Handler(listener.accept(),clientNum).start();
                System.out.println("Client "  + clientNum + " is attempting to connect!");
                clientNum++;
            }
        } finally {
            listener.close();
        }
    }

    /**
    * A handler thread class.  Handlers are spawned from the listening
    * loop and are responsible for dealing with a single client's requests.
    */
    private static class Handler extends Thread {
        private String message;    //message received from the client
        private Socket connection;
        private ObjectInputStream in;   //stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
        private int no;     //The index number of the client

        public Handler(Socket connection, int no) {
            this.connection = connection;
            this.no = no;
        }

        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());

                try{
                    boolean credMatch = false;
                    do{  
                        String inp = (String)in.readObject();
                        String[] arr = inp.split("\t");
                        Response resp = new Response(ftpserver.files);
                        if(!arr[0].equals(ftpserver.username)){
                            resp.code = ErrorCode.BadUsername;
                        }
                        else if(!arr[1].equals(ftpserver.password)){
                            resp.code = ErrorCode.BadPassword;
                        }
                        else{
                            resp.code = ErrorCode.OK;
                            credMatch = true;
                        }
                        sendMessage(resp);
                    }while(!credMatch);

                    System.out.println("Credentials matched. Client "+ no +" can talk to server");

                    while(true)
                    {
                        //receive the message sent from the client
                        Request request = (Request)in.readObject();
                        //show the message to the user
                        System.out.println("Receive request: " + request.toString() + " from client " + no);
                        // create a response to send to the client
                        synchronized(ftpserver.files)
                        {
                            Response response = new Response(ftpserver.files);
                            response.createResponse(request);
                            if(request.command.equals("upload") && response.code==ErrorCode.OK){
                                ftpserver.files.add(request.filename);
                            }
                            // send response
                            sendMessage(response);
                        }
                        
                    }
                }
                catch(ClassNotFoundException classnot){
                        System.err.println("Data received in unknown format");
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + no);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }

        //send a message to the output stream
        public void sendMessage(Response response)
        {
            try{
                out.writeObject(response);
                out.flush();
                System.out.println("Send message: " + response.toString() + " to Client " + no);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }

    }

}
