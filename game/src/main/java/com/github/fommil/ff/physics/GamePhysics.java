/*
 * Copyright Samuel Halliday 2009
 * 
 * This file is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this file.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.fommil.ff.physics;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.ode4j.math.DVector3;
import org.ode4j.ode.DGeom.DNearCallback;

import com.github.fommil.ff.Direction;
import com.github.fommil.ff.Pitch;
import com.github.fommil.ff.PlayerStats;
import com.github.fommil.ff.Tactics;
import com.github.fommil.ff.Tactics.BallZone;
import com.github.fommil.ff.Tactics.PlayerZone;
import com.github.fommil.ff.Team;
import com.github.fommil.ff.physics.Player.PlayerState;
import com.github.fommil.ff.swos.SoundParser;
import com.github.fommil.ff.swos.SoundParser.Fx;

/**
 * The model (M) and controller (C) for game play.
 * The coordinate system is a left-handed system with X = East, Y = North, Z = Sky.
 *
 * @author Samuel Halliday
 */
public class GamePhysics extends Physics {
	public enum GameState {
		Running
		,ThrowIn_PlayerPositioning, ThrowIn_BallPositioning, ThrowIn_BallPickup 
	}
	
	private static final Logger log = Logger.getLogger(GamePhysics.class.getName());

	static final double MIN_SPEED = 0.1;

	static final double MAX_SPEED = 50;

	private final GoalkeeperController goalkeeperController;
	
	private volatile GameState currentState;
	
	
	@Deprecated // DEBUGGING
	private void debugNaNs() {
		ball.getPosition();
		ball.getVelocity();
		for (Player player : getPlayers()) {
			player.getPosition();
			player.getVelocity();
		}
	}

	/**
	 * @param vector
	 * @return the angle relative to NORTH {@code (- PI, + PI]}.
	 */
	static double toAngle(DVector3 vector) {
		return dePhase(Math.PI / 2 - Math.atan2(vector.get1(), vector.get0()));
	}

	/**
	 * @return the angle relative to NORTH {@code (- PI, + PI]}.
	 */
	static double toAngle(DVector3 vector, double fallback) {
		Preconditions.checkNotNull(vector);
		if (vector.length() == 0)
			return fallback;
		return toAngle(vector);
	}

	private static double dePhase(double d) {
		if (d > Math.PI)
			return dePhase(d - 2 * Math.PI);
		if (d <= -Math.PI)
			return dePhase(d + 2 * Math.PI);
		return d;
	}

	private final Team a, b;

	private final Ball ball;

	private final List<Player> as = Lists.newArrayListWithCapacity(11);

	private final List<Player> bs = Lists.newArrayListWithCapacity(11);

	private final Pitch pitch;

	private Player selected;

	private volatile Collection<Action> actions = Collections.emptyList();

	private volatile Collection<Aftertouch> aftertouches = Collections.emptyList();

	private final Map<Player, Double> grounded = Maps.newHashMap();

	private final Collection<Goalpost> goals = Lists.newArrayList();

	/**
	 * @param a
	 * @param b
	 * @param pitch
	 */
	public GamePhysics(Team a, Team b, Pitch pitch) {
		super(9.81);
		this.a = a;
		this.b = b;
		this.pitch = pitch;

		goalkeeperController = new GoalkeeperController(pitch);

		ball = new Ball(world, space);
		Position centre = pitch.getCentre();
		ball.setPosition(centre);

		goals.add(new Goalpost(world, space, pitch, Direction.NORTH));
		goals.add(new Goalpost(world, space, pitch, Direction.SOUTH));

		BallZone bz = ball.getZone(pitch);

		List<PlayerStats> aPlayers = a.getPlayers();
		Tactics tactics = a.getCurrentTactics();
		Goalkeeper goalkeeper = new Goalkeeper(1, a, aPlayers.get(0), world, space);
		goalkeeper.setPosition(pitch.getGoalBottom());
		goalkeeper.setOpponent(Direction.NORTH);
		as.add(goalkeeper);
		for (int i = 2; i <= 11; i++) {
			Position p = tactics.getZone(bz, i, Direction.NORTH).getCentre(pitch);
			Player pma = new Player(i, a, aPlayers.get(i - 1), world, space);
			pma.setPosition(p);
			pma.setOpponent(Direction.NORTH);
			as.add(pma);
		}
		selected = as.get(9);

		// TODO: remove duplication
		List<PlayerStats> bPlayers = b.getPlayers();
		tactics = b.getCurrentTactics();
		goalkeeper = new Goalkeeper(1, b, bPlayers.get(0), world, space);
		goalkeeper.setPosition(pitch.getGoalTop());
		goalkeeper.setOpponent(Direction.SOUTH);
		bs.add(goalkeeper);
//		for (int i = 2; i <= 11; i++) {
//			Position p = tactics.getZone(bz, i, Direction.SOUTH).getCentre(pitch);
//			Player pma = new Player(i, b, bPlayers.get(i - 1), world, space);
//			pma.setPosition(p);
//		    pma.setOpponent(Direction.SOUTH);
//			bs.add(pma);
//		}
		currentState = GameState.Running;
	}

	@Override
	protected DNearCallback getCollisionCallback() {
		GameCollisionHandler handler = new GameCollisionHandler();
		return new CollisionCallback(world, joints, handler);
	}

	/**
	 * Controller.
	 *
	 * @param actions
	 * @param aftertouches
	 */
	public void setUserActions(Collection<Action> actions, Collection<Aftertouch> aftertouches) {
		Preconditions.checkNotNull(actions);
		Preconditions.checkNotNull(aftertouches);
		this.actions = Sets.newHashSet(actions);
		this.aftertouches = Sets.newHashSet(aftertouches);
	}

	@Override
	protected void beforeStep() {
		debugNaNs();
		
		for (Goalpost goal : goals) {
			if (goal.isInside(ball)) {
				log.info("GOAL TO " + goal.getFacing());
				SoundParser.play(Fx.CROWD_CHEER);
			}
		}
		
		switch(currentState)
		{
		case Running:
			beforeStep_Running();
			break;
		case ThrowIn_PlayerPositioning:
			beforeStep_ThrowIn_PlayerPositioning();
			break;
		case ThrowIn_BallPickup:
			beforeStep_Pickup();
			break;
		case ThrowIn_BallPositioning:
			beforeStep_BallPositioning();
			break;
		default:
			break;
		
		}
		
		
		ball.setAftertouch(aftertouches);
		ball.setDamping(0);
	}
	
	private void beforeStep_BallPositioning()
	{
		ball.setPosition(positionWhereTheBallLeftTheField);
		ball.setVelocity(new DVector3());
		currentState = GameState.ThrowIn_BallPickup;
	}
	
	private void beforeStep_Pickup()
	{
		if (selected.getVelocity().speed() < MIN_SPEED)
		{
			if (Math.abs(selected.getPosition().x - positionWhereTheBallLeftTheField.x) > 0.6
					|| Math.abs(selected.getPosition().y - positionWhereTheBallLeftTheField.y) > 0.3)
			{
				selected.autoPilot(positionWhereTheBallLeftTheField);
				//System.out.println("x = " + Math.abs(selected.getPosition().x - positionWhereTheBallLeftTheField.x));
				//System.out.println("y = " + Math.abs(selected.getPosition().y - positionWhereTheBallLeftTheField.y));
			}
			else
			{
				selected.setState(Player.PlayerState.THROW);
				currentState = GameState.Running;
				List<Action> auto = Lists.newArrayList();
				auto.add(Action.RIGHT);
				selected.setActions(auto);
			}
		}
	}
	
	private void beforeStep_ThrowIn_PlayerPositioning()
	{
		boolean areStatic = true;
		for (Player p : getPlayers()) {
			if (p.getVelocity().speed() > MIN_SPEED)
				areStatic = false;
		}
		if (areStatic)
		{
			currentState = GameState.ThrowIn_BallPositioning;
			return;
		}
		movePlayersTowardsTheBall(positionWhereTheBallLeftTheField, ballZoneWhereTheBallLeftTheField);
		
	}
	
	private void beforeStep_Running()
	{
		if (selected.getState() != Player.PlayerState.THROW)
			checkIfBallIsOutOfPlay();
		if (actions.contains(Action.CHANGE))
			updateSelected();

		Position bp = ball.getPosition();
		BallZone bz = ball.getZone(pitch);
		movePlayersTowardsTheBall(bp, bz);
		
		// only send actions to the selected player if the game is running
		selected.setActions(actions);
	}
	
	private void movePlayersTowardsTheBall(Position bp, BallZone bz)
	{
		for (Player p : getPlayers()) {
			transition(p);
			if (p == selected)
				continue;
			Position target = bp;
			if (p instanceof Goalkeeper) {
				goalkeeperController.autoPilot((Goalkeeper) p, ball);
				continue;
			}
			double near = Math.min(10, bp.distance(selected.getPosition()));
			if (bp.distance(p.getPosition()) > near) {
				Team team = p.getTeam();
				Tactics tactics = team.getCurrentTactics();
				PlayerZone pz;
				if (team == a) {
					pz = tactics.getZone(bz, p.getShirt(), Direction.NORTH);
				} else {
					pz = tactics.getZone(bz, p.getShirt(), Direction.SOUTH);
				}
				target = pz.getCentre(pitch);
			}
			p.autoPilot(target);
		}
	}
	
	private Position positionWhereTheBallLeftTheField;
	private BallZone ballZoneWhereTheBallLeftTheField;
	private void checkIfBallIsOutOfPlay()
	{
		Position bp = ball.getPosition();
		//check if there is a throw-in
		if(bp.x < pitch.getPitchLowerLeft().x || bp.x > pitch.getPitchUpperRight().x)
		{
			currentState = GameState.ThrowIn_PlayerPositioning;
			// TODO: Improve the calculation of this point
			double y = bp.y;
			double z = bp.z;
			double x = bp.x < pitch.getPitchLowerLeft().x ? pitch.getPitchLowerLeft().x : pitch.getPitchUpperRight().x;
			
			positionWhereTheBallLeftTheField = new Position(x, y, z);
			ballZoneWhereTheBallLeftTheField = new BallZone(positionWhereTheBallLeftTheField, getPitch());
		}
	}
	
	@Override
	protected void afterStep() {
		double ballSpeed = ball.getVelocity().speed();
//		if (Double.isNaN(ballSpeed)) {
//			log.warning("ball had NaN speed");
//			ball.setVelocity(new DVector3());
//		}
		if (ballSpeed < MIN_SPEED)// stops small movements
			ball.setVelocity(new DVector3());
//		if (ballSpeed > MAX_SPEED) { // stops really weird rounding errors
//			log.warning("ball was going " + ballSpeed);
//			DVector3 ballVelocity = ball.getVelocity().toDVector();
//			ballVelocity.normalize();
//			ballVelocity.scale(MAX_SPEED);
//		}

		switch (selected.getState()) {
			case KICK:
				selected.kick(ball);
				break;
			case THROWING:
				selected.throwIn(ball);
		}
	}

	private void updateSelected() {
		assert selected != null;
		Player closest = selected;
		double distance = Double.MAX_VALUE;
		for (Player model : as) {
			switch (model.getState()) {
				case GROUND:
				case INJURED:
				case HEAD_START:
				case HEAD_MID:
				case HEAD_END:
				case TACKLE:
					continue;
			}
			double ds2 = model.getPosition().distance(ball.getPosition());
			if (ds2 < distance) {
				distance = ds2;
				closest = model;
			}
		}
		selected = closest;
	}

	@SuppressWarnings("fallthrough")
	private void transition(Player p) {
		// TODO: should be in the Player class
		switch (p.getState()) {
			case TACKLE:
				if (p.getVelocity().speed() > MIN_SPEED)
					break;
			case GROUND:
				if (!grounded.containsKey(p)) {
					grounded.put(p, time);
				} else if ((time - grounded.get(p)) > 2) {
					if (p.getState() == PlayerState.GROUND && new Random().nextBoolean()) {
						p.setState(PlayerState.INJURED);
						return;
					}
					p.setState(PlayerState.RUN);
					grounded.remove(p);
					break;
				}
			case INJURED:
				if (!grounded.containsKey(p)) {
					log.warning("WASN'T LISTED IN GROUNDED");
					grounded.put(p, time);
				} else if ((time - grounded.get(p)) > 5) {
					p.setState(PlayerState.RUN);
					grounded.remove(p);
				}
		}
	}

	// <editor-fold defaultstate="collapsed" desc="BOILERPLATE GETTERS/SETTERS">
	public Ball getBall() {
		return ball;
	}

	public Iterable<Player> getPlayers() {
		return Iterables.concat(as, bs);
	}

	public Player getSelected() {
		return selected;
	}

	public double getTimestamp() {
		return time;
	}

	public Team getTeamA() {
		return a;
	}

	public Team getTeamB() {
		return b;
	}

	public Pitch getPitch() {
		return pitch;
	}
	// </editor-fold>
}
