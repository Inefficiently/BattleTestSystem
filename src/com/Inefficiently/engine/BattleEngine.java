package com.Inefficiently.engine;

import com.Inefficiently.startup.Execute.CLIENTTYPE;

// Main data storage and access methods and calls to methods that do calculations
public class BattleEngine {
	
	private boolean Player1Turn = true;
	private int TurnCounter = 0;
	public boolean HostConnected = false;
	public boolean PlayerConnected = false;
	public boolean begin = false;
	public boolean gameOver = false;
	
	public int HostHealth;
	public int PlayerHealth;
	
	public BattleEngine(int healthScale) {
		HostHealth = healthScale;
		PlayerHealth = healthScale;
	}
	
	public String OutputData() {
		String output = "Host Health:" + HostHealth + "\nPlayer Health:" + PlayerHealth + "\n";
		return output + "Turn:" + TurnCounter;
	}
	
	public String EndTurn() {
		Player1Turn = !Player1Turn;
		TurnCounter++;
		return OutputData();
	}
	
	// For now a command will consist of a target and an attack/heal
	public void HandlePlayerCommand(String command) {
		String[] parts = command.split(" ");
		int damage = 0;
		switch (parts[1]) {
		case "Water":damage = -5;break;
		case "Fire":damage = 20;break;
		case "Earth":damage = 10;break;
		case "Air":damage = 1;break;
		}
		if(parts[0].equals("Player")) {
			PlayerHealth -= damage;
		} else if (parts[0].equals("Host")) {
			HostHealth -= damage;
		}
		GameOverCheck();
	}
	
	public boolean getPlayerTurn() {
		return Player1Turn;
	}
	
	public void updateBegin() {
		begin = HostConnected && PlayerConnected;
	}
	
	public void GameOverCheck() {
		gameOver = PlayerHealth <= 0 || HostHealth <= 0;
	}
	
	// Creates unique game over message for each player type to be displayed
	public String getGameOverMessage(CLIENTTYPE type) {
		if(type == CLIENTTYPE.PLAYER) {
			if(HostHealth <= 0) {
				return "You Won!!\nYou beat the host player";
			} else if (PlayerHealth <= 0) {
				return "You Lost!!\nThe host player beat you";
			}
		} else if (type == CLIENTTYPE.HOST) {
			if(HostHealth <= 0) {
				return "You Lost!!\nThe player beat you";
			} else if (PlayerHealth <= 0) {
				return "You Won!!\nYou beat the player";
			}
		} else if (type == CLIENTTYPE.OBSERVER) {
			if(HostHealth <= 0) {
				return "The player won!!";
			} else if (PlayerHealth <= 0) {
				return "The Host player won!!";
			}
		}
		return "Error unable to find a loser message";
	}
}
