package org.rainbow;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class SuperServer {
    private static final int SUPER_SERVER_PORT = 8888;

    private List<TaskServer> taskServers;

    public SuperServer() {
        taskServers = new ArrayList<>();
        taskServers.add(new TaskServer("Server1", "127.0.0.1", 9001));
        taskServers.add(new TaskServer("Server2", "127.0.0.1", 9002));
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(SUPER_SERVER_PORT)) {
            System.out.println("超级服务器已启动，等待客户端连接...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端已连接：" + clientSocket.getInetAddress().getHostAddress());

                TaskServer taskServer = selectTaskServer();
                if (taskServer != null) {
                    taskServer.processClientRequest(clientSocket);
                } else {
                    System.out.println("未找到可用的任务服务器。");
                }
            }
        } catch (IOException e) {
            System.err.println("超级服务器初始化失败。");
            e.printStackTrace();
        }
    }

    private TaskServer selectTaskServer() {
        // 简单实现：轮询选择一个任务服务器
        int index = 0;
        if (taskServers.size() > 0) {
            index = (index + 1) % taskServers.size();
            return taskServers.get(index);
        }
        return null;
    }

    public static void main(String[] args) {
        SuperServer superServer = new SuperServer();
        superServer.start();
    }
}

class TaskServer {
    private  RemoteTranslator translator;
    private String name;
    private String host;
    private int port;

    public TaskServer(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
        try {
            this.translator = new RemoteTranslator();
        } catch (RemoteException ignored) {
        }
    }

    public void processClientRequest(Socket clientSocket) {
        try (
                ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())
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
                clientSocket.close();
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
