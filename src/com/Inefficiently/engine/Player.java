package com.Inefficiently.engine;

import com.Inefficiently.startup.Execute.CLIENTTYPE;

// This class will hold player related information to be pulled by the battle engine on command
public class Player {
	// Holds player position in the battle map array
	public int Iindex;
	public int Jindex;
	
	// Holds health and mana for the battle values
	public  int health;
	public int mana;
	
	public int Signature;
	
	// Holds the player identification
	public CLIENTTYPE type;
	
	// Initialized to hold the commands for spells to make player unique effects
	public String[] spells;
	
	public Player(CLIENTTYPE type, int HealthScale, int Mana, int i, int j) {
		this.type = type;
		this.health = HealthScale;
		this.mana = Mana;
		this.Iindex = i;
		this.Jindex = j;
		if(type == CLIENTTYPE.HOST) {
			this.Signature = 10;
		} else {
			this.Signature = 11;
		}
	}
	
	public void movePlayer(String direction) {
		int i = Iindex;
		int j = Jindex;
		switch (direction) {
		case "1":
			i--;
			j--;
			break;
		case "2":
			i--;
			break;
		case "3":
			i--;
			j++;
			break;
		case "4":
			j--;
			break;
		case "5":
			j++;
			break;
		case "6":
			i++;
			j--;
			break;
		case "7":
			i++;
			break;
		case "8":
			i++;
			j++;
		}
		Iindex = i;
		Jindex = j;
	}
}
