package httpNetty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpFileServer {
	
	public static void main(String[] arg) {
		int port = 8080;
		new HttpFileServer().bind(port);
	}
		
	public void bind(int port) {
		// 定义接受连接group
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		// 定义处理连接的group
		EventLoopGroup workGroup = new NioEventLoopGroup();

		try {
			// 定义server
			ServerBootstrap bootStrap = new ServerBootstrap();
			// 设置server处理group
			bootStrap.group(bossGroup, workGroup);
			// 设置NIOSocket
			bootStrap.channel(NioServerSocketChannel.class);
			// 设置日志大小
			bootStrap.option(ChannelOption.SO_BACKLOG, 1024);
			// 设置日志级别
			bootStrap.handler(new LoggingHandler(LogLevel.INFO));
			//
			bootStrap.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel socketChannel)
						throws Exception {
					// 设置http请求解码器
					socketChannel.pipeline().addLast("http-decoder", new HttpRequestDecoder());
					// 设置聚合器
					socketChannel.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
					// 设置http响应编码器
					socketChannel.pipeline().addLast("http-encoder", new HttpResponseEncoder());
					socketChannel.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
					// 设置文件服务处理类
					socketChannel.pipeline().addLast("fileServerHandler", new HttpFileServerHandler());
				}
			});
			ChannelFuture future = bootStrap.bind(port).sync();
			future.channel().closeFuture().sync();
		} catch (InterruptedException e) {

		} finally {
			workGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}

	}

}
