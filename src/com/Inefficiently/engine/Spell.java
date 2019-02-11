package com.Inefficiently.engine;

import com.Inefficiently.startup.Execute.CLIENTTYPE;

// object create to hold spell related behavior
public class Spell {
	// Tracks the owner of the spell for awarding damage and kill counter may be un needed
	public CLIENTTYPE creator;

	// Tracks the state of the program
	public SPELLSTATE state;

	// Tracks the number of cycles left in suspension
	public int sleepTimer;

	RunicSpellEngine SpellEngine;
	
	// holds the source code for spells
	String SpellCommands;
	
	// Tracks the current line the Runic Spell Engine is processing to allow for discontinuous processing
	int pointer = 1;
	
	// Marks position in the array map and the inital direction for dumb spells
	public int i,j,InitialDirection;
	
	// Defines the spell signature for targeting
	public int Signature;
	
	public PAYLOADTYPE payloadtype;
	public SENSORTYPE sensorType;
	
	// Registers
	int Dat = 0; // Internal memory and arithmetic / logic
	int Adv = 0; // will be read to get movement information 1 (8) 2 (80) 3(800) one movement before auto sleep
	int Act = 0; // This will trigger the pay load to be activated any value over 100 passed to it only positive values
	int Tar = 0; // This holds a target data gives a unique identifier for an object 
	
	// Pins
	int Dis = 0; // Sensor polls the nearest object and gives distance result as an integer
	int Dir = 0; // Sensor polls the the rough distance of the target
	int Sig = 0; // Sensor polls the signature given to the object
	int Rdr = 0; // Sensor polls the number of objects in the map
	
	// Used to test whether conditional statements are active
	boolean ConditionalOn;
	boolean ConditionalResult;
	
	public Spell(RunicSpellEngine SpellEngine, CLIENTTYPE creator, PAYLOADTYPE payloadtype, SENSORTYPE sensor,
			String SpellCommands, int i, int j, int TargetSignature, int InitialDirection) {
		this.SpellEngine = SpellEngine;
		this.creator = creator;
		this.payloadtype = payloadtype;
		this.SpellCommands = SpellCommands;
		this.pointer = 0;
		this.InitialDirection = InitialDirection;
		this.Tar = TargetSignature;
		this.Dir = InitialDirection;
		this.state = Spell.SPELLSTATE.ACTIVE;
		this.sensorType = sensor;
		this.i = i;
		this.j = j;
	}
	
	public Spell(RunicSpellEngine SpellEngine, CLIENTTYPE creator, PAYLOADTYPE payloadtype, SENSORTYPE sensor,
			String SpellCommands, int i, int j, int TargetSignature, int InitialDirection, boolean DebugMode) {
		this.SpellEngine = SpellEngine;
		this.creator = creator;
		this.payloadtype = payloadtype;
		this.SpellCommands = SpellCommands;
		this.pointer = 0;
		this.InitialDirection = InitialDirection;
		this.Tar = TargetSignature;
		this.Dir = InitialDirection;
		this.state = Spell.SPELLSTATE.ACTIVE;
		this.sensorType = sensor;
		switch (InitialDirection + "") {
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
			break;
		}
		this.i = i;
		this.j = j;
	}
	
	
	// Active-Code is running/Suspended-Code is not running
	// Armed-Battle Engine will begin damage calculations/Done-Code ceases activation and is marked to be removed
	public enum SPELLSTATE {
		ACTIVE("Active"), SUSPENDED("Suspended"), ARMED("Armed"), DONE("Done");
		public String stateName;
		private SPELLSTATE(String stateName) {
			this.stateName = stateName;
		}
	}
	
	public enum PAYLOADTYPE {
		FIRE("FIRE", 10), BOMB("BOMB", 30), ICE("ICE", 10), 
		KINETIC("KINETIC", 10), HEAL("HEAL", -10), DUD("DUD", 0);
		
		public int Damage;
		public String payloadName;
		private PAYLOADTYPE(String payloadName, int Damage) {
			this.payloadName = payloadName;
			this.Damage = Damage;
		}
	}
	
	public enum SENSORTYPE {
		SIMPLE, ADVANCED;
	}
}
