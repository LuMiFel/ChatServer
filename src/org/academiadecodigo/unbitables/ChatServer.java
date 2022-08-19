package org.academiadecodigo.unbitables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static LinkedList<ClientConnection> clientList;

    public static void main(String[] args) {
        try {

            ServerSocket serverSocket = new ServerSocket(9090);
            ExecutorService myPool = Executors.newFixedThreadPool(30);
            clientList = new LinkedList<>();

            while (true) {

                Socket clientSocket = serverSocket.accept();
                ClientConnection newClient = new ClientConnection(clientSocket);
                clientList.add(newClient);
                myPool.submit(newClient);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ClientConnection implements Runnable {

        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private Boolean endChat = false;
        private String userColor = "";
        private final String colorReset = "\u001B[0m";
        private final String chatColor = "\u001B[36m";
        private String userName = "";
        private LinkedList<String> muteList = new LinkedList<>();

        public ClientConnection(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                welcomeMsg();
                setUserName();
                printCommands();

                while (!endChat) {

                    String userText = ignoreEmptyReadyLine();

                    switch (userText) {
                        case ".help":
                            printCommands();
                            break;
                        case ".username":
                            setUserName();
                            break;
                        case ".color":
                            changeUserColor();
                            break;
                        case ".list":
                            showListUsers();
                            break;
                        case ".mute":
                            muteUser();
                            break;
                        case ".quit":
                            quitClientChat();
                            break;
                        default:
                            sendToAll(userText);
                            break;
                    }
                    //Thread.sleep(500);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void sendToAll(String clientText) {
            for (ClientConnection x : clientList) {
                x.receiveMsg(clientText, userColor, userName);
            }
        }
        private void receiveMsg(String text, String color, String name) {
            if (!muteList.contains(name)) {
                out.println(color + name + ": " + text + colorReset);
            }
        }

        private void welcomeMsg() {
            out.println(chatColor
                    + "\n ###############################"
                    + "\n #### Welcome to FelixChat! ####"
                    + "\n ###############################\n"
                    + colorReset);
        }

        private void showListUsers() {
            out.println(chatColor + "\nList of users in chat:" + colorReset);
            for (ClientConnection x : clientList) {
                out.println(">> " + x.getUserName());
            }
        }

        private void quitClientChat() {
            try {
                clientList.remove(this);
                out.println(chatColor + "\n # Goodbye!\n" + colorReset);
                out.println("**shutdown**");
                sendToAll( ">>> Quit chat! <<<");
                endChat = true;
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void printCommands() {
            out.println(chatColor + "-------------------------------------------"
                    + "\n Chat Commands:"
                    + "\n   .help      (Show chat commands)"
                    + "\n   .username  (Change your user name)"
                    + "\n   .color     (Change your user color)"
                    + "\n   .list      (Show list of users in chat)"
                    + "\n   .mute      (Mute user messages)"
                    + "\n   .quit      (Turn off connection)"
                    + "\n-------------------------------------------\n" + colorReset);
        }

        private void changeUserColor() {
            out.println(chatColor + "\nChoose your color:" + "\nBLACK | RED | GREEN | YELLOW | BLUE | PURPLE | CYAN | WHITE" + colorReset);
            String myColor = ignoreEmptyReadyLine();
            switch (myColor) {
                case "BLACK":
                case "black":
                    userColor = "\u001B[30m";
                    break;
                case "RED":
                case "red":
                    userColor = "\u001B[31m";
                    break;
                case "GREEN":
                case "green":
                    userColor = "\u001B[32m";
                    break;
                case "YELLOW":
                case "yellow":
                    userColor = "\u001B[33m";
                    break;
                case "BLUE":
                case "blue":
                    userColor = "\u001B[34m";
                    break;
                case "PURPLE":
                case "purple":
                    userColor = "\u001B[35m";
                    break;
                case "CYAN":
                case "cyan":
                    userColor = "\u001B[36m";
                    break;
                case "WHITE":
                case "white":
                    userColor = "\u001B[37m";
                    break;
                default:
                    out.println(chatColor + "\nSorry! Color not available." + colorReset);
                    return;
            }
            out.println(userColor + "Your color is now: " + myColor.toUpperCase() + colorReset);
        }

        private void muteUser() {

            out.println(chatColor + "\nType username to add/remove mute: " + colorReset);

            String muteName = ignoreEmptyReadyLine();
            String status;

            if (!muteList.contains(muteName)) {
                muteList.add(muteName);
                status = "muted.";
            } else {
                muteList.remove(muteName);
                status = "unmuted.";
            }
            out.println(chatColor + "\nThe user >> " + colorReset + muteName + chatColor + " << is now " + status + colorReset);
        }

        private String getUserName() {
            return userName;
        }

        private void setUserName() {
            out.println(chatColor + " # Choose your Username: " + colorReset);
            String tempName = ignoreEmptyReadyLine();
            while (checkName(tempName)) {
                out.println(chatColor + "\n # Name already exists. Pick another name: " + colorReset);
                tempName = ignoreEmptyReadyLine();
            }
            userName = tempName;
            out.println(chatColor + "\n # Your username is now: >> " + colorReset + userName + chatColor + " <<\n" + colorReset);
        }

        private Boolean checkName(String namePick) {
            boolean nameCheck = false;
            for (ClientConnection x : clientList) {
                if (x.getUserName().equals(namePick)) {
                    nameCheck = true;
                    break;
                }
            }
            return nameCheck;
        }

        private String ignoreEmptyReadyLine() {
            try {
                String newLine = in.readLine();

                while (newLine.isEmpty() || newLine.equals(" ") || newLine.equals("\n")) {
                    newLine = in.readLine();
                }
                return newLine;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}

