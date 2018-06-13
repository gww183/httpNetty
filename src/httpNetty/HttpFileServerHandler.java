package httpNetty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class HttpFileServerHandler extends
		SimpleChannelInboundHandler<FullHttpRequest> {

	private static final String BAD_REQUEST = "400";
	
	private static final HttpMethod GET = HttpMethod.GET;

	private static final String METHOD_NOT_ALLOWED = "403";

	private static final String FORBIDDEN = "401";

	private static final String NOT_FOUND = "file not find";

	private static final HttpVersion HTTP_1_1 = HttpVersion.HTTP_1_1;
	

	private static final HttpResponseStatus OK = HttpResponseStatus.OK;

	private static final String CONNECTION = "connection";
	
	private String url;

	public HttpFileServerHandler(String url) {
		this.url = url;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request)
			throws Exception {
		System.out.println("channelRead09");
		if(!request.getDecoderResult().isSuccess()) {
			sendError(ctx, BAD_REQUEST);
			return;
		}
		
		if(request.getMethod() != GET) {
			sendError(ctx, METHOD_NOT_ALLOWED);
			return ;
		}
		
		final String uri = request.getUri();
		final String path = sanitizeUri(uri);
		if(path == null) {
			sendError(ctx, FORBIDDEN);
			return;
		}
		
		File file = new File(path);
		if(file.isHidden() || !file.exists()) {
			sendError(ctx, NOT_FOUND);
			return;
		}
		if(!file.isFile()) {
			sendError(ctx, FORBIDDEN);
			return;
		}
		
		RandomAccessFile randomAccessFile = null;
		try{
			randomAccessFile = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException f) {
			sendError(ctx, NOT_FOUND);
			return;
		}
		
		long fileLength = randomAccessFile.length();
		
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
		
		response.headers().set("content-length", fileLength);
		
		response.headers().set("content-type", "text/html;charset=UTF-8");
		
		if(isKeepAlive(request)) {
			response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
		
		ctx.write(response);
		
		ChannelFuture sendFileFuture = 
				ctx.write(new ChunkedFile(randomAccessFile, 0, fileLength, 8192), ctx.newProgressivePromise());
		
		sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
			
			
			
			@Override
			public void operationComplete(ChannelProgressiveFuture arg0)
					throws Exception {
				
			}
			
			@Override
			public void operationProgressed(ChannelProgressiveFuture arg0, long arg1,
					long arg2) throws Exception {
				
			}
		});
	}


	@Override
	public boolean acceptInboundMessage(Object msg) throws Exception {
		return super.acceptInboundMessage(msg);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		super.channelRead(ctx, msg);
	}
	
	
	/**
	 * 
	 * @Title: sanitizeUri 
	 * @Description: 
	 * @param uri
	 * @return
	 * @author gww
	 * 2018年6月13日
	 */
	private String sanitizeUri(String uri) {
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			try {
				uri = URLDecoder.decode(uri, "ISO-8859-1");
			} catch (UnsupportedEncodingException e1) {
				throw new Error();
			}
		}
		return uri;
	}
	

}
