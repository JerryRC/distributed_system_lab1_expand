package org.rainbow;

import java.io.*;
import java.net.Socket;

public class SocketImplementClient {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8888;
        try (
                Socket socket = new Socket(host, port);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                BufferedReader bi = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("请开始输入查询词：（输入 quit 退出）");
            String res;
            while ((res = bi.readLine()) != null) {
                if (res.equals("quit")) {
                    break;
                }
                // 发送远程方法调用请求
                String methodName = "translate";
                Class<?> parameterTypes = String.class;

                oos.writeUTF(methodName);
                oos.writeObject(parameterTypes);
                oos.writeObject(res);
                oos.flush();

                // 读取服务器返回的结果
                Object result = ois.readObject();

                System.out.println(result != null ? result : "词典中未包含该词。");
            }

        } catch (IOException e) {
            System.err.println("未能找到服务器节点。");
        } catch (ClassNotFoundException e) {
            System.err.println("服务器节点未提供该服务。");
        }
    }
}

