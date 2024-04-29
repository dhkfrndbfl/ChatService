package chat_project;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static Map<String, PrintWriter> clients = new ConcurrentHashMap<>();
    private static Map<String, Integer> clientRooms = new ConcurrentHashMap<>();
    private static Map<Integer, List<String>> rooms = new ConcurrentHashMap<>();
    private static int roomCount = 0;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Chat server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                // 클라이언트와 통신할 스레드 생성
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String nickname;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // 닉네임 설정
                nickname = in.readLine();
                System.out.println(nickname + " connected from " + socket.getRemoteSocketAddress());

                // 클라이언트 저장
                clients.put(nickname, out);

                // 클라이언트에게 명령어 목록 전송
                sendCommandList(out);

                // 클라이언트로부터 메시지 수신 및 처리
                String input;
                while ((input = in.readLine()) != null) {
                    if (input.startsWith("/")) {
                        handleCommand(input, out);
                    } else {
                        sendMessageToRoom(nickname, input);
                    }
                }

            } catch (IOException e) {
                System.out.println("Client " + nickname + " disconnected.");
            } finally {
                // 클라이언트 연결 종료 시 처리
                if (nickname != null) {
                    clients.remove(nickname);
                    if (clientRooms.containsKey(nickname)) {
                        leaveRoom(nickname);
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendCommandList(PrintWriter out) {
            out.println("Available commands:");
            out.println("/list - Show available rooms");
            out.println("/create - Create a new room");
            out.println("/join [roomNumber] - Join a room");
            out.println("/exit - Leave the current room or exit the chat");
            out.println("/users - List all users");
            out.println("/roomusers - List users in the current room");
            out.println("/whisper [nickname] [message] - Send a private message to a user");
            out.println("/bye - Disconnect from the chat");
        }

        private void handleCommand(String input, PrintWriter out) {
            String[] parts = input.split(" ");
            String command = parts[0];

            switch (command) {
                case "/list":
                    out.println("Available rooms:");
                    for (int roomNumber : rooms.keySet()) {
                        out.println("Room " + roomNumber);
                    }
                    break;
                case "/create":
                    createRoom(out);
                    break;
                case "/join":
                    if (parts.length > 1) {
                        int roomNumber = Integer.parseInt(parts[1]);
                        joinRoom(nickname, roomNumber, out);
                    } else {
                        out.println("Usage: /join [roomNumber]");
                    }
                    break;
                case "/exit":
                    if (clientRooms.containsKey(nickname)) {
                        leaveRoom(nickname);
                    } else {
                        out.println("You are not in any room.");
                    }
                    break;
                case "/users":
                    out.println("Online users:");
                    for (String user : clients.keySet()) {
                        out.println(user);
                    }
                    break;
                case "/roomusers":
                    if (clientRooms.containsKey(nickname)) {
                        int roomNumber = clientRooms.get(nickname);
                        List<String> usersInRoom = rooms.get(roomNumber);
                        out.println("Users in the current room:");
                        for (String user : usersInRoom) {
                            out.println(user);
                        }
                    } else {
                        out.println("You are not in any room.");
                    }
                    break;
                case "/whisper":
                    if (parts.length > 2) {
                        String receiver = parts[1];
                        StringBuilder message = new StringBuilder();
                        for (int i = 2; i < parts.length; i++) {
                            message.append(parts[i]).append(" ");
                        }
                        sendWhisper(nickname, receiver, message.toString().trim());
                    } else {
                        out.println("Usage: /whisper [nickname] [message]");
                    }
                    break;
                default:
                    out.println("Unknown command: " + command);
            }
        }

        private void createRoom(PrintWriter out) {
            roomCount++;
            rooms.put(roomCount, new ArrayList<>());
            joinRoom(nickname, roomCount, out);
            out.println("Room " + roomCount + " created.");
        }

        private void joinRoom(String user, int roomNumber, PrintWriter out) {
            if (rooms.containsKey(roomNumber)) {
                if (clientRooms.containsKey(user)) {
                    leaveRoom(user);
                }
                rooms.get(roomNumber).add(user);
                clientRooms.put(user, roomNumber);
                out.println("Joined room " + roomNumber);
                sendMessageToRoom(user, user + " joined the room.");
            } else {
                out.println("Room does not exist.");
            }
        }

        private void leaveRoom(String user) {
            int roomNumber = clientRooms.get(user);
            rooms.get(roomNumber).remove(user);
            clientRooms.remove(user);
            sendMessageToRoom(user, user + " left the room.");
        }

        private void sendMessageToRoom(String sender, String message) {
            if (clientRooms.containsKey(sender)) {
                int roomNumber = clientRooms.get(sender);
                List<String> usersInRoom = rooms.get(roomNumber);
                for (String user : usersInRoom) {
                    PrintWriter userOut = clients.get(user);
                    userOut.println(sender + ": " + message);
                }
            }
        }

        private void sendWhisper(String sender, String receiver, String message) {
            if (clients.containsKey(receiver)) {
                PrintWriter receiverOut = clients.get(receiver);
                receiverOut.println("(Whisper from " + sender + "): " + message);
            } else {
                out.println("User " + receiver + " is not online.");
            }
        }
    }
}

