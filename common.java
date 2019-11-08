import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.nio.file.*;

enum ErrorCode{
    OK,
    UnknownCommand,
    CommandMalformed,
    ReadFailed,
    WriteFailed,
    BadUsername,
    BadPassword
}

class Request implements Serializable{
    String command;
    String filename;
    byte[] data;

    public Request(String message){
        message = message.trim();
        String[] arr = message.split(" ");
        this.command = arr[0].toLowerCase();
        this.filename = "";
        this.data = new byte[0];

        switch(this.command){
            case "get":
                processGet(arr);
                break;
            case "upload":
                processUpload(arr);
                break;
            default:
        }
    }

    public void processGet(String[] arr){
        try{
            this.filename = arr[1];
        }
        catch(Exception e){
        }
    }

    public void processUpload(String[] arr){
        try{
            this.filename = arr[1];
        }
        catch(Exception e){
        }

        if(!this.filename.isEmpty()){
            try{
                this.data = Files.readAllBytes(Paths.get(this.filename));
            }
            catch(Exception exception){
                // if file did not exist, send an empty filename
                // so that server detects a malformed command
                this.data = new byte[0];
                this.filename = "";
                System.out.println("File does not exist");
            }
        }
    }

    @Override
    public String toString()
    {
        return "command=" + this.command + "filename=" + this.filename + "dataLen=" + this.data.length;
    }
}

class Response implements Serializable{
    ErrorCode code;
    byte[] data;
    transient Set<String> files;

    public Response(Set<String> files){
        this.files = files;
        this.data = new byte[0];
    }

    void createResponse(Request request){
        switch(request.command){
            case "get":
                readFile(request.filename);
                break;
            case "upload":
                uploadFile(request);
                break;
            case "dir":
                listOfFiles();
                break;
            default:
                this.code = ErrorCode.UnknownCommand;
                this.data = new byte[0];
        }
    }

    void readFile(String filename)
    {
        if(filename.isEmpty()){
            this.code = ErrorCode.CommandMalformed;
            this.data = new byte[0];
        }
        else{
            try{
                this.data = Files.readAllBytes(Paths.get(filename));
                this.code = ErrorCode.OK;
            }
            catch(Exception exception){
                this.data = new byte[0];
                this.code = ErrorCode.ReadFailed;
                System.out.println("Read failed at server");
            }
        }
    }

    void uploadFile(Request request)
    {
        String filename = request.filename;
        if(filename.isEmpty()){
            this.code = ErrorCode.CommandMalformed;
            this.data = new byte[0];
        }
        else{
            try{
                Files.write(Paths.get(request.filename), request.data);
                this.code = ErrorCode.OK;
                this.data = new byte[0];
            }
            catch(Exception exception){
                this.data = new byte[0];
                this.code = ErrorCode.WriteFailed;
                System.out.println("Write failed at server");
            }
        }
    }

    void listOfFiles()
    {
        this.code = ErrorCode.OK;
        this.data = new byte[0];

        Iterator<String> itr = this.files.iterator();
        String fString = "";
        // traversing over HashSet
        while(itr.hasNext()){
           fString += itr.next()+"\n";
        }
        this.data = fString.getBytes();
    }

    @Override
    public String toString()
    {
        return "error=" + this.code.toString() + "dataLen=" + this.data.length; 
    }
}
