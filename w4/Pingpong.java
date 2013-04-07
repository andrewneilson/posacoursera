import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * You are to design a simple Java program where you create two threads, Ping and Pong, to alternately
 * display “Ping” and “Pong” respectively on the console.
 * */
class Pingpong implements Runnable {
    /**
     * 'Player' is simply an enum that helps us manage the different
     * types of players (ping or pong) and what they 'yell' when their
     * turn is played.
     * */
    public enum Player {
        PING("Ping!"), PONG("Pong!");
        private String name;

        Player(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * Each player needs to know which side they are playing on,
     * how many turns they will play, whose turn it is, and have
     * access to the ball used in the game.
     * */
    public Pingpong(Player name, int turns, AtomicReference<Player> turn, Object ball) {
        this.name = name;
        this.turns = turns;
        this.turn = turn;
        this.ball = ball;
    }

    private Object ball;
    private Player name;
    private AtomicReference<Player> turn;
    private int turns;

    /**
     * In each turn, the ball is only ever heading toward one player
     * at a time. This requires synchronization on the ball.
     *
     * If it is not currently a player's turn, then the player will
     * wait until it is their turn.
     *
     * Once it is the player turn, they will hit the ball and then
     * let the other player know that it is their turn.
     *
     * An atomic reference to the 'turn' is used to manage the reference
     * to whose turn it is. This could also be done without the atomic
     * reference and by making the 'turn' static within the Pingpong class.
     * However I opted for the former option because I wanted the turn
     * to be explicitly stated by whomever was defining the game.
     * */
    public void hitBall() throws InterruptedException {
        synchronized(ball) {
            while(turn.get() != name) {
                ball.wait();
            }

            System.out.println(name);
            if(name == Player.PING)
                turn.set(Player.PONG);
            else
                turn.set(Player.PING);

            ball.notify();
        }
    }

    /**
     * When a player plays their game, they simply attempt to
     * hit the ball once for each of their allotted number of turns.
     * */
    public void run() {
        try {
            for(int i=0; i < turns; i++) {
                hitBall();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start a game of ping pong where the "ping" player
     * has the first turn and each player hits the ball
     * three times.
     */
    public static void main(String[] args) {
        Object ball = new Object();
        AtomicReference<Player> turn = new AtomicReference<Player>(Player.PING);
        Pingpong ping = new Pingpong(Player.PING, 3, turn, ball);
        Pingpong pong = new Pingpong(Player.PONG, 3, turn, ball);
        ExecutorService game = Executors.newFixedThreadPool(2);

        System.out.println("Ready... Set... Go!\n");
        game.execute(ping);
        game.execute(pong);

        try {
            game.shutdown();
            game.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Done!");
        }
    }
}
