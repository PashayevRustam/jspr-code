package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final List<String> VALID_PATHS = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private static final int THREAD_POOL_SIZE = 64;

    public void start() {
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            System.out.println("Сервер запущен на порту 9999");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleConnection(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private void handleConnection(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedOutputStream out = new BufferedOutputStream(clientSocket.getOutputStream())
        ) {
            String requestLine = in.readLine();
            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                return;
            }

            String path = parts[1];
            if (!VALID_PATHS.contains(path)) {
                sendNotFoundResponse(out);
                return;
            }

            Path filePath = Path.of(".", "public", path);
            String mimeType = Files.probeContentType(filePath);

            if (path.equals("/classic.html")) {
                sendClassicResponse(out, filePath, mimeType);
            } else {
                sendFileResponse(out, filePath, mimeType);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendNotFoundResponse(OutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private void sendClassicResponse(OutputStream out, Path filePath, String mimeType) throws IOException {
        try {
            String template = Files.readString(filePath);
            String content = template.replace("{time}", LocalDateTime.now().toString());
            byte[] contentBytes = content.getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + contentBytes.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(contentBytes);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFileResponse(OutputStream out, Path filePath, String mimeType) throws IOException {
        try (InputStream fileInputStream = Files.newInputStream(filePath)) {
            long length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }
}
