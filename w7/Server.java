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
 * --- Windows ----
 * If you are using PuTTY, make sure "type" is set to "raw".
 *
 * ---- Mac ----
 * If you are on a Mac, then you can input CR and LF characters by first escaping the command
 * with "CTRL-V". So for example, CTRL+V+CTRL+J will give you a LF character.
 *
 * LF (^J): Ctrl+V, Ctrl+J
 * CR/LF (^M^J): Ctrl+V,Ctrl+M,Ctrl+V,Ctrl+J
 *----

 * On the forums I saw some discussion about not being able to build an echo server without
 * using some of Netty's encoders & decoders. This comment from Prof. Schmidt clarifies this
 * point re: chunks/lines etc:
 * https://class.coursera.org/posa-001/forum/thread?thread_id=687&post_id=3333#comment-2549
 * */
import java.util.concurrent.*;
import java.net.InetSocketAddress;
import org.jboss.netty.bootstrap.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.execution.*;
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
     *
     * To set this up as a HALF SYNC / HALF ASYNC server we provide an ExecutionHandler
     * using an OrderedMemoryAwareThreadPoolExecutor. This provides a context for
     * executing blocking (synchronous) calls. See an overview of the strategy I'm using here:
     * http://netty.io/3.6/guide/#faq.4
     *
     * Requests are accepted asynchronously with
     * the boss/worker threads provided to the NioServerSocketChannelFactory (see this page:
     * http://netty.io/3.5/api/org/jboss/netty/channel/socket/nio/NioServerSocketChannelFactory.html)
     * */
    public static void accept(int port) throws Exception {
        ChannelFactory factory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
        final OrderedMemoryAwareThreadPoolExecutor syncExecutor =
            new OrderedMemoryAwareThreadPoolExecutor(5,100000,10000000, 100, TimeUnit.MILLISECONDS);

        ServerBootstrap bootstrap = new ServerBootstrap(factory);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("sync", new ExecutionHandler(syncExecutor));
                pipeline.addLast("handler", new EchoServerHandler());
                return pipeline;
            }
        });

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
            // simply send the message back
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
