import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.nio.file.*;

public class ftpclient {
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
 	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
	String endP;
	int port;

	public ftpclient(String ep, int port) {
		this.endP = ep;
		this.port = port;
	}

	void run()
	{
		try{
			//create a socket to connect to the server
			requestSocket = new Socket(endP, port);
			System.out.println("Connecting to " + endP + " on Port " + port);
			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			
			//get Input from standard input
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			ErrorCode err = ErrorCode.UnknownCommand;

			do{
				try{
					System.out.println("Enter username : ");
					String username = bufferedReader.readLine();
					System.out.println("Enter password : ");
					String password = bufferedReader.readLine();
					String toSend = username+"\t"+password;
					System.out.println(toSend);
					out.writeObject(toSend);
					Response resp = (Response)in.readObject();
					err = resp.code;
					System.out.println("err=" + err.toString());
				}
				catch(IOException ioException){
					ioException.printStackTrace();
				}
			}while(err != ErrorCode.OK);

			System.out.println("Connected to server");

			while(true)
			{
				System.out.print("Hello, please input a sentence: ");
				//read a sentence from the standard input
				message = bufferedReader.readLine();
				//create a request from the message
				Request request = new Request(message);
				sendMessage(request);
				//Receive the response from the server
				Response response = (Response)in.readObject();
				//show the message to the user
				System.out.println("Receive message: " + response.toString());

				if(request.command.equals("get")){
					if(response.code==ErrorCode.OK){
						Files.write(Paths.get(request.filename), response.data);
					}
				}
				if(request.command.equals("dir")){
					if(response.code==ErrorCode.OK){
						String s = new String(response.data);
						System.out.print(s);
					}
				}
			}
		}
		catch (ConnectException e) {
    			System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch ( ClassNotFoundException e ) {
            		System.err.println("Class not found");
        	} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	//send a message to the output stream
	void sendMessage(Request request)
	{
		try{
			//stream write the message
			out.writeObject(request);
			out.flush();
			System.out.println("Send message: " + request.toString());
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	//main method
	public static void main(String args[])
	{

		if(args.length!=2){
			System.out.println("bad syntax");
			System.out.println("run : java ftpclient <address> <port>");
		}
		else{
			String endP = "";
			int port = 10;
			try{
				endP = args[0];
				port = Integer.parseInt(args[1]);
			}
			catch(Exception e){
				System.out.println("Bad port");
			}
			ftpclient client = new ftpclient(endP, port);
			client.run();
		}
	}

}
