import java.net.*;
import java.io.*;
import java.util.*;
import java.time.*;
import static java.time.temporal.ChronoUnit.*;

public class myHttpServer implements Runnable
{
    // The config file values are read and assigned to the data members
    public static int port;
    public static String htmlFolder="";
    public static int timeOut;
    public static boolean persistent;
    public Socket client;
    public LocalTime threadStartTime;

    public myHttpServer(Socket s)
    {
        //Assigning the accepted socket into the data member for closing it later
        client=s;
    }

    public static void main(String[] args)
    {
        try
        {
            //reading from the config file by initializing the Properties object, opening and loading the config file label values
            Properties config=new Properties();
            FileInputStream configInput= new FileInputStream("configuration.properties");
            config.load(configInput);

            System.out.println("Running at port : "+ config.getProperty("port"));

            //assigning the label values from config file into data members
            port=Integer.parseInt(config.getProperty("port"));
            htmlFolder=config.getProperty("htmlFolder");
            persistent=Boolean.parseBoolean(config.getProperty("persistent"));
            timeOut=Integer.parseInt(config.getProperty("timeOut"));
        }
        catch(Exception e)
        {
            System.out.println("Error in the port and directory assigning : "+e.getMessage());
        }

        try
        {
            //instantiating the ServerSocket by passing the port number into the constructor
            ServerSocket server=new ServerSocket(port);
            while(true)
            {
                //accept incoming requests from clients, start new thread associating with each client
                myHttpServer obj=new myHttpServer(server.accept());
                Thread t=new Thread(obj);
                //calling start which internally calls run function which is overriden below
                t.start();
            }
        }
        catch(Exception e)
        {
            System.out.println("Connection error : "+e.getMessage());
        }
    }
    
/* 
    public void start()
    {
        threadStartTime= LocalTime.now();
        System.out.println("thread started at : "+threadStartTime);
    } */

    public void run()
    {
        //if the start of the execution time of the current object is not set yet, set it to the current time

        if(threadStartTime==null)
        {
            threadStartTime= LocalTime.now();
            //System.out.println("thread started at : "+threadStartTime);
        }

        //System.out.println("in run - thread started at : "+threadStartTime);

        //declaring all the request readers and response writers below
        BufferedReader clientIn=null;
        PrintWriter headerOut=null;
        BufferedOutputStream clientout=null;
        String path=null;

        //pathsplit will hold the URL split by / to make it easier to look for file names in the static html folder and to check for valid path and return a 404 page
        String[] pathsplit=new String[3];
        //updateCookie will only be set to true if a valid path is visited by the client
        boolean updateCookie=false;
        //cookieCount keeps tracks and cookie value sent by the client, if nothing is sent i.e the client is visiting for the first time, it is considered a first visit
        int cookieCount=1;
        //if the visited path is not valid, the send_404 flag is set to true based on which later, the response is generated
        boolean send_404=false;

        //the persistent data member holds the value from the config file and the below code hadles the non-persistent connections, the else block later deals with persistent connections
        if(!persistent)
        {
        try
        {
            //initializing the previously declared request readers and response writers fro client socket
            clientIn=new BufferedReader(new InputStreamReader(client.getInputStream()));
            headerOut=new PrintWriter(client.getOutputStream());
            clientout=new BufferedOutputStream(client.getOutputStream());

            //req will hold all the request headers
            String req="";
            //reading all the request headers line by line until the header is blank which happens at the end of a request
            while (!(req = clientIn.readLine()).equals(""))
            {
            
                System.out.println(req);
                //The first ever line in the request header will always hold the method followed by the path and the http version 
                if(req.indexOf("GET")!=-1)
                {
                    //parsing the get request by using StringTokenizer class
                    StringTokenizer reqHeader=new StringTokenizer(req);

                    //I'm not using method anywhere else later, only to parse to the next element in the current req
                    String method=reqHeader.nextToken().toUpperCase();
                    
                    //Fetching the path here
                    path=reqHeader.nextToken().toLowerCase();
                    //System.out.println("method = "+method+" path = "+path);

                    //pathsplit will hold the URL split by / to make it easier to look for file names in the static html folder and to check for valid path and return a 404 page
                    pathsplit=path.split("/");

                    //System.out.println("pathsplit[1] "+pathsplit[1]);
                    //System.out.println("pathsplit[2] "+pathsplit[2]);
                    
                    //checking if valid URL is visited else the send_404 flag is set to true
                    if(pathsplit[1].equals("txr177"))
                    {
                        //if the /txr177/ path is right, checking for the file requested updateCookie flag is updated accordingly
                        switch(pathsplit[2])
                        {
                            case "visits.html":
                                                updateCookie=true;
                                                break;
                            case "test1.html":
                                                updateCookie=true;
                                                break;
                            case "test2.html":
                                                updateCookie=true;
                                                break;
                            default:
                                                send_404=true;
                                                break;
                        }
                    }
                    else
                    {
                        send_404=true;
                    }
                }

                //Checking for Cookie header in the request sent by the client 
                //Note: The get and teh Cokkie headers are never in the same line of the request
                else if(req.indexOf("Cookie")!=-1) 
                {
                    //System.out.println("Well 14?? == "+req);
                    //System.out.println("updateCookie flag set to = "+updateCookie);


                    //parsing the cookie line and updating the cookieCount value of a Cookie header is sent else the value of 1 is considered
                    //Cookie value is updated only when the request is made to a valid URL
                    //Note: I'm adding EOC at the end of the cookie value to make it easier to parse, this will not affect the performance of the server in any way

                    //When the code was put on the EECS lab 10 server, there were other cookies and it was harder to parse the cookie value although I have a unique name for the cookie. So, adding "EOC" to the end of the cookie value seemed like a better and easy approah (; as a seperator does not always work for instance when a single cookie is sent in the request a ; is not added in the end) Therefore, I went with my way of identifying the cookie value

                    String prev_hits="0";
                    if(updateCookie==true && req.indexOf("txr177_count_hits")!=-1)
                    {
                            //header was sent by the browser So paring and attaching it in the response header
                            StringTokenizer reqHeader=new StringTokenizer(req);
                            String txr177_count_hits_cookie=reqHeader.nextToken();
                            txr177_count_hits_cookie=reqHeader.nextToken();
                            //finding the value stored in the cookie
                            int prev_hits_index=txr177_count_hits_cookie.indexOf("txr177_count_hits=");
                            //System.out.println("prev_hits index at : "+prev_hits_index);

                            //extracting the cookie value from the string between my delimiter and the name of the cookie I've assigned 
                            prev_hits=txr177_count_hits_cookie.substring(prev_hits_index+18,txr177_count_hits_cookie.indexOf("EOC"));

                            //System.out.println("this is where we are : "+txr177_count_hits_cookie);
                            //System.out.println("prev_hits : "+prev_hits);

                            //parsing the cookie count into an integer
                            cookieCount=Integer.parseInt(prev_hits)+1;                        
                    }
                }
            }

            //after all the request lines are read, checking for the send_404 and updateCookie flags and sending appropriate reponses

            if(send_404)
            {

            //sending response for a wrongly visited URL
            String htmlContent="<h2>404 Not Found</h2>";
            byte[] htmlByteContent=htmlContent.getBytes();

            headerOut.println("HTTP/1.1 404 Implemented");
			headerOut.println("Date: " + new Date());
			headerOut.println("Content-type: text/html");
			headerOut.println("Content-length: " + htmlByteContent.length);
			headerOut.println(); // to differentiate the body of the response from the header
			headerOut.flush();
            //writing the html content onto the response body
            clientout.write(htmlByteContent,0,htmlByteContent.length);
            clientout.flush();
            }

            else if(updateCookie)
            {

                //if updateCookie was set to true, that indicates a valid URL was requested and the response body is generated based on the file requested for
                switch(pathsplit[2])
                        {
                            case "visits.html":
                                                String htmlContent="<title>txr177</title><h4> Hello, <br/> You have visited the valid URLs on this site for a total of "+cookieCount+" times </h4> (Inclusive of the current visit) <br/> (Delete the cookie stored in the browser to reset the count)";
                                                byte[] htmlByteContent=htmlContent.getBytes();

                                                //setting necessary headers for the browser to be able to read the content sent
                                                headerOut.println("HTTP/1.1 200 Implemented");
                                                headerOut.println("Date: " + new Date());
                                                //sending the updated cookie value here
                                                headerOut.println("Set-Cookie: txr177_count_hits="+cookieCount+"EOC; Path=/txr177/");
                                                headerOut.println("Content-type: text/html");
                                                //content lenght= the byte array length that holds the html content
                                                headerOut.println("Content-length: " + htmlByteContent.length);
                                                //this part of the code is for the non-persistent part hence I'm sending the connection close header right away for the browser/client to be aware that the server is closing the connection
                                                headerOut.println("Connection: close");
                                                headerOut.println();
                                                headerOut.flush();

                                                //writing the html content into the reponse body
                                                clientout.write(htmlByteContent,0,htmlByteContent.length);
                                                clientout.flush();

                                                break;

                            case "test1.html":  //call the same function so don't matter to display the html content
                            case "test2.html":  //so adding one common break
                                                //reading from the test1.html and test2.html based on the request path
                                                String resBodyContent="";
                                                try 
                                                {
                                                    //the folderpath is read from the config file
                                                        File fileObj = new File(htmlFolder+pathsplit[2]);
                                                        Scanner htmlFileReader = new Scanner(fileObj);
                                                        //reading the content in the html files and appending it into response body variable
                                                        while (htmlFileReader.hasNextLine()) 
                                                        {
                                                            resBodyContent+=htmlFileReader.nextLine();
                                                        }
                                                        //System.out.println("html content = "+resBodyContent);
                                                        htmlFileReader.close();
                                                        
                                                    } catch (Exception e) 
                                                    {
                                                        System.out.println("Error while reading the html files : "+e.getMessage());
                                                    }
                                                    finally
                                                    {
                                                        byte[] resBodyByte=resBodyContent.getBytes();
                                                        //setting headers
                                                        headerOut.println("HTTP/1.1 200 Implemented");
                                                        headerOut.println("Date: " + new Date());
                                                        headerOut.println("X-Frame-Options: ALLOWALL");
                                                        headerOut.println("set :protection, :except => :frame_options");
                                                        //set :protection, :except => :frame_options
                                                        //sending updated cookie value here
                                                        headerOut.println("Set-Cookie: txr177_count_hits="+cookieCount+"EOC; Path=/txr177/");
                                                        headerOut.println("Content-type: text/html");
                                                        //content lenght= the byte array length that holds the html content
                                                        headerOut.println("Content-length: " + resBodyByte.length);
                                                        //this part of the code is for the non-persistent part hence I'm sending the connection close header right away for the browser/client to be aware that the server is closing the connection
                                                        headerOut.println("Connection: close");
                                                        headerOut.println();
                                                        headerOut.flush();
                                                        //write the html content into the response body
                                                        clientout.write(resBodyByte,0,resBodyByte.length);
                                                        clientout.flush();
                                                    }
                                                break;
                        }
            }
        }
        catch(Exception e)
        {
            if(e.getMessage().equals("null"))
            {
                System.out.println("End of Request");
            }
            System.out.println("error "+e.getMessage());
        }
        finally
        {
            try
            {
                //closing the response writer and request reader objects
                clientIn.close();
                headerOut.close();
                clientout.close();
            }
            catch(Exception e)
            {
                System.out.println("Error while closing response writer objects : "+e.getMessage());
            }
            finally
            {
                try
                {
                    //closing the client socket
                    client.close();
                    //t.stop();
                    System.out.println("Closed all connections successfully!");
                }
                catch(Exception e)
                {
                    System.out.println("Exception while closing the client socket : "+e.getMessage());
                }
            }
        }
        }
        else
        {
            //handling the persistant connection
            try
            {
                //initializing the previously declared request readers and response writers fro client socket
                clientIn=new BufferedReader(new InputStreamReader(client.getInputStream()));
                headerOut=new PrintWriter(client.getOutputStream());
                clientout=new BufferedOutputStream(client.getOutputStream());
                //req will hold all the request headers
                String req="";
                
                //until the difference between the time the thread started running and the current time is lesser than the timeout seconds value set in the config file, the below statement hold true
                
                while(SECONDS.between(threadStartTime, LocalTime.now())<=timeOut)
                {
                            
                        System.out.println("Listening to client's request...");
                        System.out.println("Current connection has been up for "+SECONDS.between(threadStartTime,LocalTime.now())+" seconds");

                        //reading all the request headers line by line until the header is blank which happens at the end of a request
                                    while (!(req = clientIn.readLine()).equals(""))
                                    {
                                        //The first ever line in the request header will always hold the method followed by the path and the http version 
                                        if(req.indexOf("GET")!=-1)
                                        {
                                            //parsing the get request by using StringTokenizer class
                                            StringTokenizer reqHeader=new StringTokenizer(req);

                                            //I'm not using method anywhere else later, only to parse to the next element in the current req
                                            String method=reqHeader.nextToken().toUpperCase();

                                            //Fetching the path here
                                            path=reqHeader.nextToken().toLowerCase();
                                            System.out.println("Get request made by the client either for a fevicon or a regular get request");
                                            //System.out.println("method = "+method+" path = "+path);

                                            //pathsplit will hold the URL split by / to make it easier to look for file names in the static html folder and to check for valid path and return a 404 page
                                            pathsplit=path.split("/");

                                            //System.out.println("pathsplit[1] "+pathsplit[1]);
                                            //System.out.println("pathsplit[2] "+pathsplit[2]);

                                            //checking if valid URL is visited else the send_404 flag is set to true
                                            if(pathsplit[1].equals("txr177"))
                                            {
                                                //if the /txr177/ path is right, checking for the file requested updateCookie flag is updated accordingly

                                                //changing the flag values to keep the communication going after the first request as the current if block handles the persistent connection

                                                switch(pathsplit[2])
                                                {
                                                    case "visits.html":
                                                                        send_404=false;
                                                                        updateCookie=true;
                                                                        break;
                                                    case "test1.html":
                                                                        send_404=false;
                                                                        updateCookie=true;
                                                                        break;
                                                    case "test2.html":
                                                                        send_404=false;
                                                                        updateCookie=true;
                                                                        break;
                                                    default:            
                                                                        updateCookie=false;
                                                                        send_404=true;
                                                                        break;
                                                }
                                            }
                                            else
                                            {
                                                send_404=true;
                                            }
                                        }

                                        //Checking for Cookie header in the request sent by the client 
                                        //Note: The get and teh Cokkie headers are never in the same line of the request

                                        else if(req.indexOf("Cookie")!=-1) //if checking for cookie
                                        {
                                            //System.out.println("Well 14?? == "+req);
                                            //System.out.println("updateCookie flag set to = "+updateCookie);


                                            //parsing the cookie line and updating the cookieCount value of a Cookie header is sent else the value of 1 is considered
                                            //Cookie value is updated only when the request is made to a valid URL
                                            //Note: I'm adding EOC at the end of the cookie value to make it easier to parse, this will not affect the performance of the server in any way

                                            //When the code was put on the EECS lab 10 server, there were other cookies and it was harder to parse the cookie value although I have a unique name for the cookie. So, adding "EOC" to the end of the cookie value seemed like a better and easy approah (; as a seperator does not always work for instance when a single cookie is sent in the request a ; is not added in the end) Therefore, I went with my way of identifying the cookie value
                                            String prev_hits="0";
                                            if(updateCookie==true && req.indexOf("txr177_count_hits")!=-1)
                                            {
                                                    //header was sent by the browser So paring and attaching it in the response header
                                                    StringTokenizer reqHeader=new StringTokenizer(req);
                                                    String txr177_count_hits_cookie=reqHeader.nextToken();
                                                    txr177_count_hits_cookie=reqHeader.nextToken();
                                                    //finding the value stored in the cookie
                                                    int prev_hits_index=txr177_count_hits_cookie.indexOf("txr177_count_hits=");
                                                    //System.out.println("prev_hits index at : "+prev_hits_index);

                                                    //extracting the cookie value from the string between my delimiter and the name of the cookie I've assigned 
                                                    prev_hits=txr177_count_hits_cookie.substring(prev_hits_index+18,txr177_count_hits_cookie.indexOf("EOC"));
                                                    //System.out.println("this is where we are : "+txr177_count_hits_cookie);
                                                    //System.out.println("prev_hits : "+prev_hits);

                                                    //parsing the cookie count into an integer
                                                    cookieCount=Integer.parseInt(prev_hits)+1;                        
                                            }
                                        }
                                        
                                    }

                                   //after all the request lines are read, checking for the send_404 and updateCookie flags and sending appropriate reponses

                                    if(send_404)
                                    {

                                    //sending response for a wrongly visited URL
                                    String htmlContent="<h2>404 Not Found</h2>";
                                    byte[] htmlByteContent=htmlContent.getBytes();

                                    headerOut.println("HTTP/1.1 404 Implemented");
                                    headerOut.println("Date: " + new Date());
                                    //headerOut.println("Set-Cookie: txr177_count_hits=20; Path=/txr177/");
                                    headerOut.println("Content-type: text/html");
                                    headerOut.println("Content-length: " + htmlByteContent.length);
                                    headerOut.println(); // to differentiate the body of the response
                                    headerOut.flush();

                                    //writing the html content onto the response body
                                    clientout.write(htmlByteContent,0,htmlByteContent.length);
                                    clientout.flush();
                                    }

                                    else if(updateCookie)
                                    {
                                        //if updateCookie was set to true, that indicates a valid URL was requested and the response body is generated based on the file requested for
                                        switch(pathsplit[2])
                                                {
                                                    case "visits.html":
                                                                        String htmlContent="<title>txr177</title><h4> Hello, <br/> You have visited the valid URLs on this site for a total of "+cookieCount+" times </h4> (Inclusive of the current visit) <br/> (Delete the cookie stored in the browser to reset the count)";
                                                                        byte[] htmlByteContent=htmlContent.getBytes();

                                                                        //setting necessary headers for the browser to be able to read the content sent

                                                                        headerOut.println("HTTP/1.1 200 Implemented");
                                                                        headerOut.println("Date: " + new Date());
                                                                        //sending the updated cookie value here
                                                                        headerOut.println("Set-Cookie: txr177_count_hits="+cookieCount+"EOC; Path=/txr177/");
                                                                        headerOut.println("Content-type: text/html");
                                                                        //content lenght= the byte array length that holds the html content
                                                                        headerOut.println("Content-length: " + htmlByteContent.length);

                                                                        //this part of the code is for the persistent part hence I'm sending the connection header as keep-alive as the connection will remain open for further communication
                                                
                                                                        headerOut.println("Connection: keep-alive");
                                                                        headerOut.println();
                                                                        headerOut.flush();

                                                                        clientout.write(htmlByteContent,0,htmlByteContent.length);
                                                                        clientout.flush();

                                                                        break;
                                                    case "test1.html":  //call the same function so don't matter to display the html content
                                                    case "test2.html":  //so adding one common break
                                                                        //reading from the test1.html and test2.html based on the request path
                                                                        String resBodyContent="";
                                                                        try {

                                                                                //the folderpath is read from the config file
                                                                                File fileObj = new File(htmlFolder+pathsplit[2]);
                                                                                Scanner htmlFileReader = new Scanner(fileObj);
                                                                                //reading the content in the html files and appending it into response body variable
                                                                                while (htmlFileReader.hasNextLine()) 
                                                                                {
                                                                                    resBodyContent+=htmlFileReader.nextLine();
                                                                                }
                                                                                //System.out.println("html content = "+resBodyContent);
                                                                                htmlFileReader.close();
                                                                                
                                                                            } catch (Exception e) 
                                                                            {
                                                                                System.out.println("Error while reading the html files : "+e.getMessage());
                                                                            }
                                                                            finally
                                                                            {
                                                                                byte[] resBodyByte=resBodyContent.getBytes();
                                                                                //setting headers
                                                                                headerOut.println("HTTP/1.1 200 Implemented");
                                                                                headerOut.println("Date: " + new Date());
                                                                                headerOut.println("X-Frame-Options: ALLOWALL");
                                                                                headerOut.println("set :protection, :except => :frame_options");
                                                                                //set :protection, :except => :frame_options
                                                                                //sending updated cookie value here
                                                                                headerOut.println("Set-Cookie: txr177_count_hits="+cookieCount+"EOC; Path=/txr177/");
                                                                                headerOut.println("Content-type: text/html");
                                                                                //content lenght= the byte array length that holds the html content
                                                                                headerOut.println("Content-length: " + resBodyByte.length);
                                                                                //this part of the code is for the persistent part hence I'm sending the connection header as keep-alive as the connection will remain open for further communication
                                                                                headerOut.println("Connection: keep-alive");
                                                                                headerOut.println();
                                                                                headerOut.flush();
                                                                                //write the html content into the response body
                                                                                clientout.write(resBodyByte,0,resBodyByte.length);
                                                                                clientout.flush();
                                                                            }
                                                                        break;
                                                }
                                    }
                                //Thread.sleep(10000);
                }
            }
            catch(Exception e)
                                {
                                    if(e.getMessage().equals("null"))
                                    {
                                        System.out.println("End of Request");
                                    }
                                    System.out.println("error "+e.getMessage());
                                }
            finally
                            {
                                try
                                    {
                                        System.out.println("Time out. Cannot wait for the client anymore..");
                                        //setting connection header to close to let the client know. 
                                        headerOut.println("Connection: close");
                                        //closing the response writer and request reader objects
                                        clientIn.close();
                                        headerOut.close();
                                        clientout.close();
                                        System.out.println("Closed all writer and reader objects successfully!");
                                    }
                                catch(Exception e)
                                    {
                                        System.out.println("Error while closing response writer objects : "+e.getMessage());
                                    }
                                finally
                                    {
                                        try
                                        {
                                            //closing the client socket
                                            client.close();
                                            //t.stop();
                                            System.out.println("Closed connection with the client completely");
                                        }
                                        catch(Exception e)
                                        {
                                            System.out.println("Exception while closing the client socket : "+e.getMessage());
                                        }
                                    }
                            }
        }
    }
}