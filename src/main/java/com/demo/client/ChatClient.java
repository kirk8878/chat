package com.demo.client;

import javax.swing.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ChatClient {

    private String host;
    private int port;
    private Selector selector;
    private SocketChannel socketChannel;
    private String username;

    public ChatClient(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        selector = Selector.open();
        socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(host, port));
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        username = socketChannel.getLocalAddress().toString().substring(1);
        System.out.println(username + " 连接成功");
    }

    public void sendMsg(String msg) {
        msg = username + "说：" + msg;
        try {
            socketChannel.write(ByteBuffer.wrap(msg.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMsg() {
        try {
            int select = selector.select();
            if (select>0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        channel.read(byteBuffer);
                        String msg = new String(byteBuffer.array());
                        System.out.println(msg);
                    }
                }
                iterator.remove();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        final ChatClient chatClient = new ChatClient("127.0.0.1", 8878);
        new Thread(() -> {
                while (true) {
                    chatClient.readMsg();
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        ).start();

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String msg = scanner.nextLine();
            chatClient.sendMsg(msg);
        }
    }
}
