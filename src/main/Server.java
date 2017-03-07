package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Marcin Bala
 */
public class Server {

    public static void main(String[] args ) {
        try {
            ServerSocket socket = new ServerSocket(1234);
            while (true)
            {
                Socket incoming = socket.accept();

                System.out.println("New client is connected");

                Runnable handler = new ThreadedClientHandler(incoming);
                Thread thread = new Thread(handler);
                thread.start();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}