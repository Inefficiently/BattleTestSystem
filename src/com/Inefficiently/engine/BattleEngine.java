package com.Inefficiently.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import com.Inefficiently.engine.Spell.PAYLOADTYPE;
import com.Inefficiently.engine.Spell.SENSORTYPE;
import com.Inefficiently.engine.Spell.SPELLSTATE;
import com.Inefficiently.startup.Execute.CLIENTTYPE;

// Main data storage and access methods and calls to methods that do calculations
public class BattleEngine {

	// Number of Spell update cycles before players can enter their next move
	int CyclesPerTurn = 5;
	int CycleTimer = 0;
	
	// Player Kill Switches
	public Socket HostKillSwitch;
	public Socket PlayerKillSwitch;

	// Notify players that it is time for input
	private boolean ActivatePlayers = true;
	private int TurnCounter = 0;
	
	// Ensure both players are connected before starting
	public boolean HostConnected = false;
	public boolean PlayerConnected = false;
	public boolean begin = false;
	public boolean gameOver = false;

	public Player[] Players = new Player[2];
	public RunicSpellEngine SpellEngine;
	public ArrayList<Spell> Spells = new ArrayList<Spell>();
	// Give each spell a unique spell signature id starting from 100 and up
	public int SpellSignatureID = 103;
	
	// Configurable values with defaults
	public int[][] MapArray;
	public int MapSize = 5;
	public int HealthScale = 100;
	public int ManaScale = 100;
	//
	
	public BattleEngine(int healthScale) {
		// Temporarily use constants that will be later implemented
		this.HealthScale = healthScale;
		if(HealthScale < 1) {
			HealthScale = 100;
		}
		this.ManaScale = HealthScale;
		if(MapSize < 2) {
			MapSize = 2;
		}
		// Initialize map array and players
		this.MapArray = new int[MapSize][MapSize];
		Players[0] = new Player(CLIENTTYPE.HOST, HealthScale, ManaScale, 0, MapSize / 2);
		Players[1] = new Player(CLIENTTYPE.PLAYER, HealthScale, ManaScale, MapSize - 1, MapSize / 2);
		SpellEngine = new RunicSpellEngine(this);
	}
	
	public BattleEngine() {
		HashMap<String, String> Config = EngineUtils.GetConfiguration();
		// Accept only health points above 0
		this.HealthScale = EngineUtils.ErrorCheckHashInteger("Max-Health", Config.get("Max-Health"), 1);
		// TODO add mana scale to config
		this.ManaScale = HealthScale;
		// Accept only sizes above 2 with no cap
		this.MapSize = EngineUtils.ErrorCheckHashInteger("Map-Size", Config.get("Map-Size"), 2);
		// Initialize map array and players
		this.MapArray = new int[MapSize][MapSize];
		Players[0] = new Player(CLIENTTYPE.HOST, HealthScale, ManaScale, 0, MapSize / 2);
		Players[1] = new Player(CLIENTTYPE.PLAYER, HealthScale, ManaScale, MapSize - 1, MapSize / 2);
		SpellEngine = new RunicSpellEngine(this);
	}
	
	public BattleEngine(boolean DebugMode) {
		HashMap<String, String> Config = EngineUtils.GetConfiguration();
		// Accept only health points above 0
		this.HealthScale = EngineUtils.ErrorCheckHashInteger("Max-Health", Config.get("Max-Health"), 1);
		// TODO add mana scale to config
		this.ManaScale = HealthScale;
		// Accept only sizes above 2 with no cap
		this.MapSize = EngineUtils.ErrorCheckHashInteger("Map-Size", Config.get("Map-Size"), 2);
		// Initialize map array and players
		this.MapArray = new int[MapSize][MapSize];
		Players[0] = new Player(CLIENTTYPE.HOST, HealthScale, ManaScale, 0, MapSize / 2);
		Players[1] = new Player(CLIENTTYPE.PLAYER, HealthScale, ManaScale, MapSize - 1, MapSize / 2);
		SpellEngine = new RunicSpellEngine(this, DebugMode);
	}
	
	public void AddPlayerKSocket(Socket KillSwitch) {
		this.PlayerKillSwitch = KillSwitch;
	}
	
	public void KillSwitchPlayer() {
		try {
			this.HostKillSwitch.close();
		} catch (IOException e) {
			System.out.println("Failure to activate the kill switch");
		}
	}
	
	public void AddHostKSocket(Socket KillSwitch) {
		this.HostKillSwitch = KillSwitch;
	}
	
	public void KillSwitchHost() {
		try {
			this.PlayerKillSwitch.close();
		} catch (IOException e) {
			System.out.println("Failure to activate the kill switch");
		}
	}

	// Puts all output data in one string to transfer
	public String OutputData() {
		NormalizeHealth();
		String output = GetMapImage();
		output += "Host Health:" + Players[0].health + "\nPlayer Health:" + Players[1].health + "\n";
		return output + "Turn:" + TurnCounter;
	}

	// Prints out the Map as visual output
	public String GetMapImage() {
		UpdateMap();
		String lineBreak = EngineUtils.MultiplyString("+-", MapSize) + "+";
		String output = lineBreak + "\n";
		for (int i = 0; i < MapArray.length; i++) {
			String line = "|";
			for (int j = 0; j < MapArray[i].length; j++) {
				int Space = MapArray[i][j];
				// 0 is empty
				// 1 is Host
				// 2 is Player
				// 3 is a spell
				if(Space == 0) {
					line += " |";
				} else if(Space == 1) {
					line += "H|";
				} else if(Space == 2) {
					line += "P|";
				} else if(Space == 3) {
					line += "*|";
				}
			}
			output += line + "\n";
			output += lineBreak + "\n";
		}
		return output;
	}

	// Places all players on the map based on their position
	public void PlacePlayers() {
		for (int i = 0; i < Players.length; i++) {
			Player player = Players[i];
			if (player.type == CLIENTTYPE.HOST) {
				MapArray[player.Iindex][player.Jindex] = 1;
			} else if (player.type == CLIENTTYPE.PLAYER) {
				MapArray[player.Iindex][player.Jindex] = 2;
			} 
		}
	}
	// if spells overlap with some thing they will be annihilated and removed
	public void DestroySpells(int i, int j) {
		if(SpellEngine.DebugMode) {
			System.out.println("Destroying spells at ( " + i + ", " + j + ")");
		}
		if(MapArray[i][j] != 0) {
			for (int k = 0; k < Spells.size(); k++) {
				if(Spells.get(k).i == i && Spells.get(k).j == j) {
					Spells.get(k).state = SPELLSTATE.DONE;
				}
			}
		}
		UpdateMap();
	}
	// Removes spells from the Spells array list to be garbage collected
	public void SpellRemoval() {
		for (int i = 0; i < Spells.size(); i++) {
			if(Spells.get(i).state == SPELLSTATE.DONE) {
				Spells.remove(i);
				i--;
			}
		}
	}
	
	public void AddSpell(Spell spell) {
		spell.Signature = SpellSignatureID++;
		Spells.add(spell);
	}
	// Places all spell on the map based on their position
	public void PlaceSpells() {
		for (int i = 0; i < Spells.size(); i++) {
			Spell spell = Spells.get(i);
			if(spell.state != SPELLSTATE.DONE) {
				if(MapArray[spell.i][spell.j] != 0) {
					DestroySpells(spell.i, spell.j);
				} else {
					MapArray[spell.i][spell.j] = 3;
				}
			}
		}
	}

	// Wipe the map to begin placing new updated objects on it
	public void WipeMap() {
		for (int i = 0; i < MapArray.length; i++) {
			for (int j = 0; j < MapArray[i].length; j++) {
				MapArray[i][j] = 0;
			}
		}
	}

	// Update the map with all changes made to objects positions and removes dead spellss
	public void UpdateMap() {
		SpellRemoval();
		WipeMap();
		PlacePlayers();
		PlaceSpells();
	}

	// Mark the end of a players turn
	public String EndTurn() {
		// TODO rewrite to apply new single turn system
		ActivatePlayers = !ActivatePlayers;
		TurnCounter++;
		return OutputData();
	}

	// For now a command will consist of a target and an attack/heal
	public void HandlePlayerCommand(String command) {
		String[] parts = command.split(" ");
		if(parts.length == 2) {
			if(ActivatePlayers) {
				Players[0].movePlayer(parts[1]);
			} else {
				Players[1].movePlayer(parts[1]);
			}
		} else if (parts.length == 4) {
			if(ActivatePlayers) {
				AddSpell(createSpell(Integer.parseInt(parts[2]), Players[0], Integer.parseInt(parts[3]), Integer.parseInt(parts[1])));
			} else {
				AddSpell(createSpell(Integer.parseInt(parts[2]), Players[1], Integer.parseInt(parts[3]), Integer.parseInt(parts[1])));
			}
		}
		GameOverCheck();
	}
	
	public void NormalizeHealth() {
		for (int i = 0; i < Players.length; i++) {
			if(Players[i].health < 0) {
				Players[i].health = 0;
			}
		}
	}

	public boolean getActiveTurn() {
		return ActivatePlayers;
	}

	public void updateBegin() {
		begin = HostConnected && PlayerConnected;
	}

	public void GameOverCheck() {
		gameOver = Players[0].health <= 0 || Players[1].health <= 0;
	}

	// Update a spell by checking the value of its registers and going from there
	public void UpdateSpell(Spell spell) {
		if(spell.state == SPELLSTATE.DONE) {
			Spells.remove(spell);
		} else if(spell.state == SPELLSTATE.ARMED) {
			ActivationDamageCalculations(spell);
		} else if(spell.state == SPELLSTATE.SUSPENDED) {
			if(spell.sleepTimer < 0) {
				spell.state = SPELLSTATE.ACTIVE;
			} else {
				spell.sleepTimer--;
			}
		} else if(spell.state == SPELLSTATE.ACTIVE) {
			if(spell.Adv != 0) {
				String move = spell.Adv + "";
				String[] moves = move.split("");
				for (int j = 0; j < moves.length; j++) {
					switch (moves[j]) {
					case "1":
						spell.i--;
						spell.j--;
						break;
					case "2":
						spell.i--;
						break;
					case "3":
						spell.i--;
						spell.j++;
						break;
					case "4":
						spell.j--;
						break;
					case "5":
						spell.j++;
						break;
					case "6":
						spell.i++;
						spell.j--;
						break;
					case "7":
						spell.i++;
						break;
					case "8":
						spell.i++;
						spell.j++;
						break;
					}
					// Error bounding don't all spells outside the combat area
					if (spell.i < 0) {
						spell.i = 0;
					}
					if (spell.i >= MapSize) {
						spell.i = MapSize - 1;
					}
					if (spell.j < 0) {
						spell.j = 0;
					}
					if (spell.j >= MapSize) {
						spell.j = MapSize - 1;
					}
					// Temp update map
					UpdateMap();
					// Put the spell in a one turn sleep
				}
				spell.Adv = 0;
			}
			// Check 
			if(spell.Act == 100) {
				spell.state = SPELLSTATE.ARMED;
			} else {
				spell.state = SPELLSTATE.ACTIVE;
			}
		}
	}
	
	// returns a boolean if gameover
	public boolean RunSpellCycles() {
		int SpellCyclesPerTurn = 1;
		int spellCount = 0;
		if (SpellEngine.DebugMode) {
			System.out.println("Running through " + SpellCyclesPerTurn + " execution Cycles");
			System.out.println("Each execution cycle is limited to reading 100 lines of code");
			System.out.println("The number of cycles tested is not changeable yet");
		}
		for (int i = 0; i < SpellCyclesPerTurn; i++) {
			for (int j = 0; j < Spells.size(); j++) {
				SpellEngine.CastSpell(Spells.get(j));
				spellCount++;
				UpdateMap();
				GameOverCheck();
				if(gameOver) {
					return gameOver;
				}
			}
			if(SpellEngine.DebugMode) {
				System.out.println("Done Executing Cycle " + (i + 1));
				System.out.println("Ran " + spellCount + " spells");
			}
		}
		UpdateMap();
		return gameOver;
	}
	// Creates unique game over message for each player type to be displayed
	public String getGameOverMessage(CLIENTTYPE type) {
		if(type == CLIENTTYPE.PLAYER) {
			if(Players[0].health <= 0) {
				return "You Won!!\nYou beat the host player";
			} else if (Players[1].health <= 0) {
				return "You Lost!!\nThe host player beat you";
			}
		} else if (type == CLIENTTYPE.HOST) {
			if(Players[0].health <= 0) {
				return "You Lost!!\nThe player beat you";
			} else if (Players[1].health <= 0) {
				return "You Won!!\nYou beat the player";
			}
		} else if (type == CLIENTTYPE.OBSERVER) {
			if(Players[0].health <= 0) {
				return "The player won!!";
			} else if (Players[1].health <= 0) {
				return "The Host player won!!";
			}
		}
		return "Error unable to find a loser message";
	}
	
	// Gets all affected strings compared to all positions
	public void ActivationDamageCalculations(Spell spell) {
		String testPositions = getTestPositionString(spell.i, spell.j);
		int affected = 0;
		// Run through players to see if they are in the affected range
		if(SpellEngine.DebugMode) {
			System.out.println("+-----------------+");
			System.out.println("Spell has been armed and activated");
		}
		for (int i = 0; i < Players.length; i++) {
			int in = testPositions.indexOf("( " + Players[i].Iindex + ", " + Players[i].Jindex + ")");
			if(in != -1) {
				Players[i].health -= spell.payloadtype.Damage;
				affected++;
			}
		}
		
		// Run through spells to see if they are in the affected range
		for (int i = 0; i < Spells.size(); i++) {
			int in = testPositions.indexOf("( " + Spells.get(i).i + ", " + Spells.get(i).j + ")");
			if(in != -1) {
				Spells.get(i).state = SPELLSTATE.DONE;
				affected++;
			}
		}
		
		if(SpellEngine.DebugMode) {
			System.out.println("The spell affected " + affected + " object(s) on the map");
			System.out.println("The spell has been destroyed");
			System.out.println("+-----------------+");
			System.out.println();
		}
		
		// Kill the Spell
		spell.state = SPELLSTATE.DONE;
	}
	
	// Get a string containing all 8 surrounding positions to an index
	public String getTestPositionString(int i, int j) {
		String positions = "";
		positions += "( " + (i - 1) + ", " + (j - 1) + ") ";
		positions += "( " + (i - 1) + ", " + (j) + ") ";
		positions += "( " + (i - 1) + ", " + (j + 1) + ") ";
		positions += "( " + (i) + ", " + (j - 1) + ") ";
		positions += "( " + (i) + ", " + (j + 1) + ") ";
		positions += "( " + (i + 1) + ", " + (j - 1) + ") ";
		positions += "( " + (i + 1) + ", " + (j) + ") ";
		positions += "( " + (i + 1) + ", " + (j + 1) + ") ";
		return positions;
	}
	
	// Gets sensor data for a given map position
	public int[] DataBundleSensorInfo(Spell spell) {
		int Distance = 0;
		int Direction = 0;
		int Signature = 0;
		int Radar = 0;
		
		int Oi = spell.i;
		int Oj = spell.j;
		int Ti = 0;
		int Tj = 0;
		// Spells will not ID their creator as a target
		if(spell.creator == CLIENTTYPE.HOST) {
			Radar++;
			Ti = Players[1].Iindex;
			Tj = Players[1].Jindex;
			Distance = DistanceCalculation(Oi, Oj, Ti, Tj);
			Direction = DirectionCalculation(Oi, Oj, Ti, Tj);
			Signature = Players[1].Signature;
			if(SpellEngine.DebugMode) {
				System.out.println(Direction + " is the algorithims direction estimate");
				System.out.println("Distance to target: " + Distance);
			}
		} else if(spell.creator == CLIENTTYPE.PLAYER) {
			Radar++;
			Ti = Players[0].Iindex;
			Tj = Players[0].Jindex;
			Distance = DistanceCalculation(Oi, Oj, Ti, Tj);
			Direction = DirectionCalculation(Oi, Oj, Ti, Tj);
			Signature = Players[0].Signature;
			if(SpellEngine.DebugMode) {
				System.out.println(Direction + " is the algorithims direction estimate");
				System.out.println("Distance to target: " + Distance);
			}
		}
		if (spell.sensorType != SENSORTYPE.SIMPLE) {
			// Run through all spells to test for hostile spells and the nearest target if applicable
			int CompareDistances = 0;
			for (int i = 0; i < Spells.size(); i++) {
				Spell spell2 = Spells.get(i);
				if (spell2.state != SPELLSTATE.DONE && spell2.creator != spell.creator) {
					Radar++;
					Ti = spell2.i;
					Tj = spell2.j;
					CompareDistances = DistanceCalculation(Oi, Oj, Ti, Tj);
					if (CompareDistances < Distance) {
						Distance = CompareDistances;
						Direction = DirectionCalculation(Oi, Oj, Ti, Tj);
						Signature = spell2.Signature;
					}
				}
			} 
		}
		int[] returnArray = new int[4];
		returnArray[0] = Distance;
		if(spell.sensorType == SENSORTYPE.SIMPLE) {
			returnArray[1] = spell.InitialDirection;
		} else if (spell.sensorType == SENSORTYPE.ADVANCED) {
			returnArray[1] = Direction;
		}
		returnArray[2] = Signature;
		returnArray[3] = Radar;
		return returnArray;
	}
	
	// Returns an integer for the approximate distance between 2 array positions
	public int DistanceCalculation(int Oi, int Oj, int Ti, int Tj) {
		return (int) Math.round(Math.sqrt(Math.pow((Oi - Ti), 2) + Math.pow((Oj - Tj), 2)));
	}
	// returns an approximate direction to a target
	//	 I I I
	// J 1 2 3
	// J 4 X 5
	// J 6 7 8
	public int DirectionCalculation(int Oi, int Oj, int Ti, int Tj) {
		if(Ti < Oi && Tj < Oj) {
			return 1;
		} else if(Ti < Oi && Tj == Oj) {
			return 2;
		} else if(Ti < Oi && Tj > Oj) {
			return 3;
		} else if(Ti == Oi && Tj < Oj) {
			return 4;
		} else if(Ti == Oi && Tj > Oj) {
			return 5;
		} else if(Ti > Oi && Tj < Oj) {
			return 6;
		} else if(Ti > Oi && Tj == Oj) {
			return 7;
		} else if(Ti > Oi && Tj > Oj) {
			return 8;
		}
		// Should never return 0 but will signify no movement
		return 0;
	}
	
	// Return a random direction to travel
	// TODO find a nice way to calculate optimal way
	public int RunCalculation(Spell spell) {
		return new Random().nextInt(7) + 1;
	}
	
	public String ReadSpellFromFile(File f) {
		Scanner scan;
		String spell = "";
		try {
			scan = new Scanner(f);
			while(scan.hasNextLine()) {
				spell += scan.nextLine();
			}
			scan.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return spell;
	}
	
	// Gets the options for spells that the server recognizes
	// From a directory file
	public String GetSpellOptionsCSV() {
		String SpellsCSV = "";
		String SpellName;
		try {
			Scanner scan = new Scanner(EngineUtils.getFileFromResource("SpellDirectory.txt"));
			while(scan.hasNextLine()) {
				SpellName = scan.nextLine();
				SpellsCSV += SpellName.substring(0, SpellName.indexOf(".")) + ",";
			}
			scan.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return SpellsCSV;
	}
	
	// Updates the directory file to make a menu of
	// Creates files if deleted or not found
	public void UpdateSpellDirectoryFile() {
		File directory = EngineUtils.getFileFromResource("Spells");
		directory.mkdir();
		File SpellList = EngineUtils.getFileFromResource("SpellDirectory.txt");
		String text = "";
		try {
			SpellList.createNewFile();
			FileWriter write = new FileWriter(SpellList);
			if(directory.isDirectory()) {
				for (String spellFile : directory.list()) {
					text += spellFile + "\n";
				}
				write.write(text);
			}
			write.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String GetSpellCommands(int DirectoryIndex) {
		
		File directory = EngineUtils.getFileFromResource("Spells");
		String SpellFile = directory.list()[DirectoryIndex];
		File SpellText = EngineUtils.getFileFromResource("Spells" + "/" + SpellFile);
		String Commands = "";
		try {
			Scanner scan = new Scanner(SpellText);
			// First line is the payload type second line is sensor type
			scan.nextLine();
			scan.nextLine();
			// TODO implement more sensors
			while(scan.hasNextLine()) {
				Commands += scan.nextLine();
			}
			scan.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Commands;
	}
	public PAYLOADTYPE getPayloadType(int DirectoryIndex) {
		File directory = EngineUtils.getFileFromResource("Spells");
		String SpellFile = directory.list()[DirectoryIndex];
		File SpellText = EngineUtils.getFileFromResource("Spells" + "/" + SpellFile);
		PAYLOADTYPE type = PAYLOADTYPE.DUD;
		try {
			Scanner scan = new Scanner(SpellText);
			// First line is the payload type second line is sensor type
			String name = scan.nextLine();
			PAYLOADTYPE[] types = PAYLOADTYPE.values();
			for (int i = 0; i < types.length; i++) {
				if(types[i].payloadName.equals(name)) {
					type = types[i];
				}
			}
			scan.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return type;
	}
	
	public SENSORTYPE getSensorType(int DirectoryIndex) {
		File directory = EngineUtils.getFileFromResource("Spells");
		String SpellFile = directory.list()[DirectoryIndex];
		File SpellText = EngineUtils.getFileFromResource("Spells" + "/" + SpellFile);
		SENSORTYPE type = SENSORTYPE.SIMPLE;
		try {
			Scanner scan = new Scanner(SpellText);
			// First line is the payload type second line is sensor type
			scan.nextLine();
			String name = scan.nextLine();
			SENSORTYPE[] types = SENSORTYPE.values();
			for (int i = 0; i < types.length; i++) {
				if(types[i].name().equals(name)) {
					type = types[i];
				}
			}
			scan.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return type;
	}
	
	public Spell createSpell(int SpellID, Player creator, int Target, int InitialDirection) {
		PAYLOADTYPE type = getPayloadType(SpellID);
		SENSORTYPE sensor = getSensorType(SpellID);
		String Commands = GetSpellCommands(SpellID);
		int i = creator.Iindex;
		int j = creator.Jindex;
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
		}
		return new Spell(SpellEngine, creator.type, type, sensor, Commands, i, j, Target, InitialDirection);
	}
	
	public boolean ValidPosition(int move, int i, int j) {
		if(move == 1) {
			return (i - 1 >= 0) && (j - 1 >= 0);
		} else if(move == 2) {
			return (i - 1 >= 0);
		} else if(move == 3) {
			return (i - 1 >= 0) && (j + 1 < MapSize);
		} else if(move == 4) {
			return (j - 1 > 0);
		} else if(move == 5) {
			return (j + 1 < MapSize);
		} else if(move == 6) {
			return (i + 1 < MapSize) && (j - 1 >= 0);
		} else if(move == 7) {
			return (i + 1 < MapSize);
		} else if(move == 8) {
			return (i + 1 < MapSize) && (j + 1 < MapSize);
		}
		return false;
	}
}
