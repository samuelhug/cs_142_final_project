import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.EventObject;

import javax.swing.JOptionPane;
import javax.swing.Timer;

import engine.CollisionEngine;
import engine.CollisionEngine.CollisionEvent;
import engine.Engine;

public class PlethoraOfPong extends Engine {

	// The name of the game.
	private static final String NAME = "Plethora of Pong version 1.02";

	// Predefined list of keys to assign to players.
	private static final int[][] PLAYER_KEYS = {
			{ KeyEvent.VK_1, KeyEvent.VK_2 }, { KeyEvent.VK_9, KeyEvent.VK_0 },
			{ KeyEvent.VK_Z, KeyEvent.VK_X }, { KeyEvent.VK_N, KeyEvent.VK_M },
			{ KeyEvent.VK_W, KeyEvent.VK_Q }, { KeyEvent.VK_P, KeyEvent.VK_O },
			{ KeyEvent.VK_V, KeyEvent.VK_C }, { KeyEvent.VK_L, KeyEvent.VK_K },
			{ KeyEvent.VK_4, KeyEvent.VK_3 }, { KeyEvent.VK_8, KeyEvent.VK_7 },
			{ KeyEvent.VK_S, KeyEvent.VK_A }, { KeyEvent.VK_H, KeyEvent.VK_I },
			{ KeyEvent.VK_D, KeyEvent.VK_F }, { KeyEvent.VK_G, KeyEvent.VK_H },
			{ KeyEvent.VK_5, KeyEvent.VK_6 }, { KeyEvent.VK_E, KeyEvent.VK_R },
			{ KeyEvent.VK_U, KeyEvent.VK_I }, { KeyEvent.VK_T, KeyEvent.VK_Y }, };

	/**
	 * The number of seconds to delay the game in between points.
	 */
	private static final int COUNTDOWN_DELAY = 5;

	/**
	 * The number of points that causes a player to lose.
	 */
	private static final int LOSING_SCORE = 3;

	/**
	 * The point on the screen that the ball should start at.
	 */
	private static final Point2D BALL_START_POSITION = new Point2D.Double(0, 0);

	/**
	 * The number of players in the game.
	 */
	private final int nPlayers;

	/**
	 * The number of balls in the game.
	 */
	private final int nBalls;

	public Paddle[] paddles;
	public GoalLine[] goalLines;

	private Ball[] balls;

	// This timer is used to provide a delay at the beginning of the game.
	private Timer countdownTimer;
	private int countdownValue;

	private boolean isGameOver = false;
	private int loserPlayer;

	/**
	 * Constructor
	 */
	public PlethoraOfPong(int nPlayers, int nBalls) {
		// Initialize the game engine with a name.
		super(NAME);

		this.nPlayers = nPlayers;
		this.nBalls = nBalls;

		balls = new Ball[nBalls];

		System.out.println("Starting a " + nPlayers + " player game.");

		// Initialize the game
		initialize();
	}

	/**
	 * Initialize all game objects.
	 */
	public void initialize() {
		super.initialize();

		for (int i = 0; i < nBalls; i++) {
			balls[i] = new Ball(BALL_START_POSITION);

			addObject(balls[i]);
		}

		initializePlayers();
	}

	public void start() {
		super.start();

		reset();
	}

	protected void onUpdate(long tick) {
	}

	protected void onRender(Graphics2D g) {

		// If the count down is in-progress display the countdown value.
		if (countdownValue > 0) {
			g.setColor(Color.GREEN);
			g.scale(1, -1);
			g.translate(-3, 3);
			g.drawString(Integer.toString(countdownValue), 0, 0);
		}

		if (isGameOver) {
			g.setColor(Color.RED);
			g.scale(0.15, -0.15);
			g.translate(-45, 20);
			g.drawString("Player " + loserPlayer + " Loses!", 0, 0);
		}
	}

	public void onPlayerLose(GoalLine goal) {
		for (Ball ball : balls) {
			ball.reset();
		}
		goal.incrementScore();

		if (goal.getScore() == LOSING_SCORE) {
			soundEngine.playSound("death.wav");
			System.out.println("Player " + goal.playerNum + " loses!!");
			isGameOver = true;
			loserPlayer = goal.playerNum;
		} else {
			soundEngine.playSound("oops.wav");
			goal.updateColor();
			reset();
		}
	}

	/**
	 * Splits the circumference of a circle into `2*N_PLAYERS - 1` points to act
	 * as the boundaries for `N_PLAYERS` player paddles, arranged in a polygon
	 * with `N_PLAYERS` sides.
	 */
	private void initializePlayers() {

		final double angleIncrement = 2 * Math.PI / nPlayers;

		paddles = new Paddle[nPlayers];
		goalLines = new GoalLine[nPlayers];

		Point2D a, b;

		for (int i = 0; i < nPlayers; i++) {

			a = new Point2D.Double(WINDOW_SCALE * Math.cos(i * angleIncrement),
					WINDOW_SCALE * Math.sin(i * angleIncrement));
			b = new Point2D.Double(WINDOW_SCALE
					* Math.cos((i + 1) * angleIncrement), WINDOW_SCALE
					* Math.sin((i + 1) * angleIncrement));

			paddles[i] = new Paddle(a, b, PLAYER_KEYS[i][0], PLAYER_KEYS[i][1]);

			goalLines[i] = new GoalLine(i + 1, LOSING_SCORE, a, b);

			addObject(goalLines[i]);
			addObject(paddles[i]);

			for (final Ball ball : balls) {
				collisionEngine.addListener(paddles[i], ball,
						new engine.events.Listener() {

							@Override
							public boolean handle(EventObject e_) {
								CollisionEngine.CollisionEvent e = (CollisionEvent) e_;

								soundEngine.playSound("chirp.wav");
								ball.onCollidePaddle((Paddle) e.getSource());
								return false;
							}
						});

				collisionEngine.addListener(goalLines[i], ball,
						new engine.events.Listener() {

							@Override
							public boolean handle(EventObject e_) {
								CollisionEngine.CollisionEvent e = (CollisionEvent) e_;
								onPlayerLose((GoalLine) e.getSource());
								return false;
							}
						});
			}
		}
	}

	public void reset() {
		countdownValue = COUNTDOWN_DELAY;

		final PlethoraOfPong this_ = this;

		ActionListener timerAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				countdownValue--;

				if (countdownValue == 0) {
					countdownTimer.stop();
					eh.dispatchEvent(new GameStartEvent(this_));
				}
			}
		};

		countdownTimer = new Timer(1000, timerAction);
		countdownTimer.start();
	}

	public static void main(String[] args) {
		String input;
		int nPlayers = 0;
		int nBalls = 0;

		while (nPlayers < 2) {
			input = JOptionPane.showInputDialog(null,
					"Input the number of players:", NAME,
					JOptionPane.PLAIN_MESSAGE);

			try {
				nPlayers = Integer.parseInt(input);
			} catch (NumberFormatException e) {
				return;
			}
		}

		while (nBalls < 1) {
			input = JOptionPane.showInputDialog(null,
					"Input the number of balls:", NAME,
					JOptionPane.PLAIN_MESSAGE);

			try {
				nBalls = Integer.parseInt(input);
			} catch (NumberFormatException e) {
				return;
			}
		}

		new PlethoraOfPong(nPlayers, nBalls).start();
	}

	public static class GameStartEvent extends engine.events.Event {

		public GameStartEvent(Object source) {
			super(source);
		}
	}
}
