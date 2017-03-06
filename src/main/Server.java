package main;

import java.io.*;
import java.net.*;

/**
 * @author Marcin Bala
 */
public class Server {

    public static void main(String[] args ) {
        try {
            int i = 1;
            ServerSocket socket = new ServerSocket(1234);
            while (true)
            {
                Socket incoming = socket.accept();

                System.out.println("New client: " + i);

                Runnable handler = new ThreadedClientHandler(incoming);
                Thread thread = new Thread(handler);
                thread.start();
                i++;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}