/**
 * Compilation instructions:
 *
 * TL;DR: Java 6 + netty 3.5+
 *
 * Download netty 3.6.5.Final (though this should work with many earlier releases) from here:
 * https://oss.sonatype.org/content/repositories/releases/io/netty/netty/3.6.5.Final/netty-3.6.5.Final.jar
 *
 * Place the netty jar file in the same folder as Server.java
 * javac -cp netty-3.6.5.Final.jar Server.java
 *
 * Run the server with:
 * java -cp .:netty-3.6.5.Final.jar Server 8080
 *
 * Connect to the server with telnet:
 * telnet localhost 8080
 *
 * If you are using PuTTY, make sure "type" is set to "raw".
 * */
import java.util.concurrent.*;
import java.net.InetSocketAddress;
import org.jboss.netty.bootstrap.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.*;
import org.jboss.netty.handler.codec.frame.*;
import org.jboss.netty.handler.codec.string.*;
import org.jboss.netty.util.CharsetUtil;

public class Server {

    public static void main(String[] args) {
        try {
            if(args.length > 0) {
                int port = Integer.parseInt(args[0]);
                Server.accept(port);
                return;
            }
        } catch (NumberFormatException nfe) {
            // fall through
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        System.err.println("please provide a port to listen on (ex: 8080) as the only argument.");
        System.err.println("full example: `java -cp .:netty-3.6.5.Final.jar Server 8080`");
    }

    /**
     * Though Netty + Java provide WRAPPER FACADES for all of the underlying
     * functionality of this program, this function is a light wrapper for the
     * Netty boilerplate you can see below. In a different context, we may have
     * had an interface like this:
     *
     * interface IServer {
     *   public static void accept(int port) throws Exception;
     * }
     *
     * This would act as a wrapper facade for different underlying frameworks or
     * customized implementations we may have used.
     *
     * As for this implementation, the code in this funciton ultimately sets up
     * a ServerSocketChannel which accepts incoming TCP/IP connections and acts
     * as our ACCEPTOR.
     * */
    public static void accept(int port) throws Exception {
        ChannelFactory factory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());

        ServerBootstrap bootstrap = new ServerBootstrap(factory);

        // Upstream: from client -> build frames -> decode frame -> handle messages
        // Downstream: from handlers -> encode messages -> to client
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            private final int MAX_FRAME_LENGTH = 8192;

            public ChannelPipeline getPipeline() {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("framer", new DelimiterBasedFrameDecoder(
                        MAX_FRAME_LENGTH, Delimiters.lineDelimiter()));
                pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
                pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
                pipeline.addLast("handler", new EchoServerHandler());
                return pipeline;
            }
        });

        //bootstrap.setOption("child.tcpNoDelay", true);
        //bootstrap.setOption("child.keepAlive", true);

        // start accepting connections on the desired port
        bootstrap.bind(new InetSocketAddress(port));
    }

    /**
     * The EchoServerHandler acts as our abstraction of the REACTOR. At this level the
     * various handlers are exposed. In particular we only need "messageReceived"
     * to handle incoming messages.
     * */
    static class EchoServerHandler extends SimpleChannelHandler {
        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
            Channel ch = e.getChannel();
            ch.write(e.getMessage());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
            e.getCause().printStackTrace();

            Channel ch = e.getChannel();
            ch.close();
        }

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
            System.out.println("client connected");
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
            System.out.println("client disconnected");
        }
    }
}
