package chat_project;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static String nickname;

    public static void main(String[] args) {
        String hostName = "localhost"; // 서버가 실행 중인 호스트의 이름 또는 IP 주소
        int portNumber = 12345; // 서버와 동일한 포트 번호 사용

        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            socket = new Socket(hostName, portNumber);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner stdIn = new Scanner(System.in);

            System.out.print("Enter your nickname: ");
            nickname = stdIn.nextLine();
            out.println(nickname); // 서버에 닉네임을 전송

            // 서버로부터 메시지를 읽어 화면에 출력하는 별도의 스레드
            Thread readThread = new Thread(new ServerMessageReader(in));
            readThread.start(); // 메시지 읽기 스레드 시작

            // 사용자 입력 처리
            String userInput;
            while (true) {
                userInput = stdIn.nextLine();

                // 명령어 처리
                if (userInput.startsWith("/")) {
                    handleCommand(userInput, out);
                    continue;
                }

                // '/bye'를 입력하면 클라이언트를 종료합니다.
                if ("/bye".equals(userInput)) {
                    out.println(userInput);
                    break;
                }

                // 일반 메시지를 서버로 전송합니다.
                out.println("MESSAGE " + userInput);
            } // while

            // 클라이언트와 서버는 명시적으로 close를 합니다. close를 할 경우 상대방쪽의 readLine()이 null을 반환됩니다. 이 값을 이용하여 접속이 종료된 것을 알 수 있습니다.
            in.close();
            out.close();
            socket.close();

        } catch (IOException e) {
            System.out.println("Exception caught when trying to connect to " + hostName + " on port " + portNumber);
            e.printStackTrace();
        }
    }

    private static void handleCommand(String command, PrintWriter out) {
        String[] parts = command.split(" ");
        String cmd = parts[0];

        switch (cmd) {
            case "/list":
                out.println("LIST"); // 명령어를 서버로 전송
                break;
            case "/create":
                out.println("CREATE"); // 명령어를 서버로 전송
                break;
            case "/join":
                if (parts.length > 1) {
                    out.println("JOIN " + parts[1]); // 명령어를 서버로 전송
                } else {
                    System.out.println("Usage: /join [roomNumber]");
                }
                break;
            case "/exit":
                out.println("EXIT"); // 명령어를 서버로 전송
                break;
            case "/users":
                out.println("USERS"); // 명령어를 서버로 전송
                break;
            case "/roomusers":
                out.println("ROOMUSERS"); // 명령어를 서버로 전송
                break;
            case "/whisper":
                if (parts.length > 2) {
                    // 귓속말 메시지 전송
                    StringBuilder whisperMessage = new StringBuilder();
                    for (int i = 2; i < parts.length; i++) {
                        whisperMessage.append(parts[i]);
                        whisperMessage.append(" ");
                    }
                    out.println("WHISPER " + parts[1] + " " + whisperMessage.toString().trim());
                } else {
                    System.out.println("Usage: /whisper [nickname] [message]");
                }
                break;
            default:
                System.out.println("Unknown command: " + command);
        }
    }
}

class ServerMessageReader implements Runnable {
    private BufferedReader in;

    public ServerMessageReader(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String serverLine;
            while ((serverLine = in.readLine()) != null) {
                System.out.println(serverLine); // 서버로부터 받은 메시지를 출력
            }
        } catch (IOException e) {
            System.out.println("Server connection was closed.");
        }
    }
}

