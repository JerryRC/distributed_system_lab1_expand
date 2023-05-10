package org.rainbow;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class SocketImplementServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {

            Translator tr = new RemoteTranslator();
            System.out.println("服务端已启动，等待客户端连接...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("客户端已连接：" + socket.getInetAddress().getHostAddress());

                new ClientHandler(socket, tr).start();
            }
        } catch (IOException e) {
            System.err.println("服务初始化失败。");
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private final Socket socket;
    private final Translator translator;

    public ClientHandler(Socket socket, Translator tr) {
        this.socket = socket;
        this.translator = tr;
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())
        ) {
            while (true) {
                try {
                    // 读取客户端发送的请求
                    String methodName = ois.readUTF();
                    Class<?> parameterTypes = (Class<?>) ois.readObject();
                    Object arguments = ois.readObject();
                    // 执行远程方法调用
                    Object result = invokeMethod(methodName, parameterTypes, arguments);
                    // 将结果发送给客户端
                    oos.writeObject(result);
                } catch (EOFException | SocketException e) {
                    System.out.println("客户端已断开连接。");
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Object invokeMethod(String methodName, Class<?> parameterTypes, Object arguments) {
        // 实现方法调用逻辑
        // 获取方法的反射对象
        Object res = null;
        try {
            Method translateMethod = this.translator.getClass().getMethod(methodName, parameterTypes);
            // 调用方法
            res = translateMethod.invoke(this.translator, arguments);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
        }
        return res;
    }
}

