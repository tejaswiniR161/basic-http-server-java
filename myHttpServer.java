import java.net.*;
import java.io.*;
import java.util.*;

public class myHttpServer implements Runnable
{
    public static int port=50062;
    public Socket client;

    public myHttpServer(Socket s)
    {
        client=s;
    }

    public static void main(String[] args)
    {
        //
        try
        {
            ServerSocket server=new ServerSocket(port);
            while(true)
            {
                myHttpServer obj=new myHttpServer(server.accept());
                Thread t=new Thread(obj);
                t.start();
            }
        }
        catch(Exception e)
        {
            System.out.println("Connection error : "+e.getMessage());
        }
    }

    //@override
    public void run()
    {
        BufferedReader clientin=null;
        PrintWriter headerOut=null;
        BufferedOutputStream clientout=null;
        String path=null;
        String[] pathsplit=new String[3];
        boolean updateCookie=false;
        int cookie_count=0;
        boolean send_404=false;

        try
        {
            clientin=new BufferedReader(new InputStreamReader(client.getInputStream()));
            headerOut=new PrintWriter(client.getOutputStream());
            clientout=new BufferedOutputStream(client.getOutputStream());

            String req="Tejaswini";
            int i=0;
            while (!(req = clientin.readLine()).equals(""))
            {
            
                System.out.println(req);
                if(req.indexOf("GET")!=-1)
                {
                    StringTokenizer reqHeader=new StringTokenizer(req);
                    String method=reqHeader.nextToken().toUpperCase();
                    path=reqHeader.nextToken().toLowerCase();
                    System.out.println("method = "+method+" path = "+path);

                    pathsplit=path.split("/");

                    System.out.println("pathsplit[1] "+pathsplit[1]);
                    System.out.println("pathsplit[2] "+pathsplit[2]);

                    if(pathsplit[1].equals("txr177"))
                    {
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

                else if(req.indexOf("Cookie")!=-1) //if checking for cookie
                {
                    System.out.println("Well 14?? == "+req);
                    System.out.println("updateCookie flag set to = "+updateCookie);
                    String prev_hits="0";
                    if(updateCookie==true)
                    {
                            //header was sent by the browser So paring and attaching it in the response header
                            StringTokenizer reqHeader=new StringTokenizer(req);
                            String txr177_count_hits_cookie=reqHeader.nextToken();
                            txr177_count_hits_cookie=reqHeader.nextToken();
                            //finding the value stored in the cookie
                            int prev_hits_index=txr177_count_hits_cookie.indexOf("=");
                            prev_hits=txr177_count_hits_cookie.substring(prev_hits_index+1,txr177_count_hits_cookie.length());
                            System.out.println("this is where we are : "+txr177_count_hits_cookie);
                            System.out.println("prev_hits : "+prev_hits);
                            cookie_count=Integer.parseInt(prev_hits)+1;                        
                    }
                }
                
                i++;
                //System.out.println("i = "+i);
            }

            //read all the req headers

            if(send_404)
            {
            String htmlContent="<h2>404 Not Found</h2>";
            byte[] htmlByteContent=htmlContent.getBytes();

            headerOut.println("HTTP/1.1 404 Implemented");
			headerOut.println("Date: " + new Date());
            //headerOut.println("Set-Cookie: txr177_count_hits=20; Path=/txr177/");
			headerOut.println("Content-type: text/html");
			headerOut.println("Content-length: " + htmlByteContent.length);
			headerOut.println(); // to differentiate the body of the response
			headerOut.flush();
            clientout.write(htmlByteContent,0,htmlByteContent.length);
            clientout.flush();
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
    }
}