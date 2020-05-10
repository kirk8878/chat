package com.demo.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class ChatServer {

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    public ChatServer(int port) {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        try {
            while (true) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        SocketChannel accept = serverSocketChannel.accept();
                        accept.configureBlocking(false);
                        accept.register(selector, SelectionKey.OP_READ);
                        String msg = accept.getRemoteAddress().toString().substring(1) + " 上线";
                        System.out.println(msg);
                    } else if (key.isReadable()) {
                        readData(key);
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readData(SelectionKey key){
        SocketChannel channel = null;
        try {
            channel = (SocketChannel) key.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            if (channel.read(byteBuffer)>0) {
                String msg = new String(byteBuffer.array());
                System.out.println("收到消息：" + msg);
                sendOthers(msg, channel);
            }
        } catch (IOException e) {
            try {
                key.cancel();
                System.out.println(channel.getRemoteAddress().toString().substring(1) + " 下线");
                channel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void sendOthers(String msg, SocketChannel self)  {
        System.out.println("服务器转发消息");
        for (SelectionKey key : selector.keys()) {
            SelectableChannel channel = key.channel();
            if (channel instanceof SocketChannel && self != channel) {
                try {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes());
                    SocketChannel target = (SocketChannel) channel;
                    target.write(byteBuffer);
                } catch (IOException e) {
                    key.cancel();
                    try {
                        System.out.println(((SocketChannel) channel).getRemoteAddress() + " 下线");
                        channel.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        new ChatServer(8878).listen();
    }
}
