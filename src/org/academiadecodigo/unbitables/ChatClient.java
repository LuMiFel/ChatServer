package org.academiadecodigo.unbitables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatClient {
    static String hostName = "localhost";
    static int portNumber = 9090;
    static Socket clientSocket;
    static PrintWriter out;

    public static void main(String[] args) throws IOException {

        clientSocket = new Socket(hostName, portNumber);

        out = new PrintWriter(clientSocket.getOutputStream(), true);

        ExecutorService chatPool = Executors.newSingleThreadExecutor();
        chatPool.submit(new ListenChat());
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            out.write(bReader.readLine() + "\n");
            out.flush();
        }
    }

    public static class ListenChat implements Runnable {
        @Override
        public void run() {
            BufferedReader in;
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                while (true) {
                    String inputText = in.readLine();
                    switch (inputText) {
                        case "**shutdown**":
                            in.close();
                            out.close();
                            System.exit(0);
                            break;
                        default:
                            System.out.println(inputText);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
