package com.github.fommil.ff.physics;

import com.github.fommil.ff.Tactics.BallZone;

public enum GameState {
		Running
		,ThrowIn_PlayerPositioning, ThrowIn_BallPositioning, ThrowIn_BallPickup;
	
	private Position positionWhereTheBallLeftTheField;
	private BallZone ballZoneWhereTheBallLeftTheField;

	// required in order to maintain state using enums
	GameState copyStateFrom(GameState previousGameState){
		positionWhereTheBallLeftTheField = previousGameState.positionWhereTheBallLeftTheField;
		ballZoneWhereTheBallLeftTheField = previousGameState.ballZoneWhereTheBallLeftTheField;
		return this;
	}
	
	void setWhereTheBallLeftTheField(Position pos, BallZone bz)	{
		positionWhereTheBallLeftTheField = pos;
		ballZoneWhereTheBallLeftTheField = bz;
	}
	
	Position getPositionWhereTheBallLeftTheField() {
		return positionWhereTheBallLeftTheField;
	}
	
	BallZone getBallZoneWhereTheBallLeftTheField() {
		return ballZoneWhereTheBallLeftTheField;
	}
}