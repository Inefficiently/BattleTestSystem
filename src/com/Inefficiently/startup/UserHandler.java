package com.Inefficiently.startup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.Inefficiently.engine.BattleEngine;
import com.Inefficiently.engine.Player;
import com.Inefficiently.startup.Execute.CLIENTTYPE;

// Manages Client Input and Output for the Server
public class UserHandler extends Thread {

	Execute exec;
	InputHandler scan;
	BattleEngine engine;

	Socket Client;

	DataInputStream FromClient;
	DataOutputStream ToClient;

	CLIENTTYPE TYPE;

	// Current turn data from engine and previously recorded data
	boolean turnDelta = true;
	boolean recordedTurn = true;

	// status update booleans
	boolean running = true;
	boolean gameover = false;
	boolean hasInput = false;
	boolean isPlayersTurn = false;

	// Player object from battle engine
	Player User;

	// Passes Execute for Input methods, Socket for Communication, ClientType for identification
	public UserHandler(Execute exec, Socket connection, Socket KillSwitch, CLIENTTYPE TYPE) {
		this.exec = exec;
		this.scan = exec.scan;
		this.engine = exec.engine;
		this.Client = connection;
		this.TYPE = TYPE;
		// Attempt to form data streams to connect player to the server
		try {
			FromClient = new DataInputStream(Client.getInputStream());
			ToClient = new DataOutputStream(Client.getOutputStream());
			// Send Client Type to Client as a notification to know what client type to run
			ToClient.writeUTF(TYPE.getTYPENAME());
			if (TYPE != CLIENTTYPE.OBSERVER) {
				ToClient.writeUTF(engine.GetSpellOptionsCSV());
			}
			ToClient.writeUTF(engine.OutputData());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("\nError getting the data streams created. Possible loss in connection");
		}
		if(TYPE == CLIENTTYPE.HOST) {
			engine.AddHostKSocket(KillSwitch);
		} else if(TYPE == CLIENTTYPE.PLAYER) {
			engine.AddPlayerKSocket(KillSwitch);
		}
	}

	@Override
	public void run() {
		getPlayer();
		try {
			if(TYPE.getTYPENAME().equals(CLIENTTYPE.HOST.getTYPENAME())) {
				engine.HostConnected = true;
				engine.updateBegin();
				HostIO();
			} else if(TYPE.getTYPENAME().equals(CLIENTTYPE.PLAYER.getTYPENAME())) {
				engine.PlayerConnected = true;
				engine.updateBegin();
				PlayerIO();
			} else if(TYPE.getTYPENAME().equals(CLIENTTYPE.OBSERVER.getTYPENAME())) {
				ObserverIO();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Handler error");
		}
	}

	// Main run for player thread
	public void PlayerIO() {
		boolean PLAYERTURN = false;
		while(running && !engine.gameOver && !interrupted()) {
			if (engine.begin) {
				hasInput = false;
				try {
					ToClient.writeBoolean(running);
					engine.GameOverCheck();
					ToClient.writeBoolean(engine.gameOver);
					// Exit the loop if game over
					if(engine.gameOver)
						break;
					turnDelta = engine.getActiveTurn();
					isPlayersTurn = turnDelta == PLAYERTURN;
					if (turnDelta != recordedTurn) {
						// Set hasInput flag
						hasInput = true;
						recordedTurn = turnDelta;
					}
					ToClient.writeBoolean(hasInput);
					ToClient.writeBoolean(isPlayersTurn);
					// write the results of opponents turns
					if (hasInput) {
						ToClient.writeUTF(engine.OutputData());
					} else if (isPlayersTurn) {
						engine.HandlePlayerCommand(getValidMove());
						// After player finishes his turn the spells will cycle
						engine.RunSpellCycles();
						engine.EndTurn();
						// TODO fix to be single turn system
						engine.GameOverCheck();
						ToClient.writeBoolean(engine.gameOver);
						if(engine.gameOver)
							break;
						ToClient.writeUTF(engine.OutputData());
					} else {
						yield();
					}
				} catch (IOException e) {
					// Push Kill signal
					e.printStackTrace();
					System.out.println("Socket is dead. Triggering kill switch");
					engine.KillSwitchHost();
					interrupt();
				} 
			} else {
				yield();
			}
		}
		if(engine.gameOver) {
			try {
				ToClient.writeUTF(engine.OutputData());
				ToClient.writeUTF(engine.getGameOverMessage(TYPE));
				engine.PlayerConnected = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	// Main run for Observer thread
	public void ObserverIO() {
		while(running && !gameover) {
			if (engine.begin) {
				try {
					ToClient.writeBoolean(running);
					ToClient.writeBoolean(engine.gameOver);
					// Exit the loop if game over
					if(engine.gameOver)
						break;
					turnDelta = engine.getActiveTurn();
					ToClient.writeBoolean(turnDelta != recordedTurn);
					if (turnDelta != recordedTurn) {
						ToClient.writeUTF(engine.OutputData());
						recordedTurn = turnDelta;
					} else {
						yield();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println("Thread Connection lost");
					interrupt();
				} 
			} else {
				yield();
			}
		}
		if(engine.gameOver) {
			try {
				ToClient.writeUTF(engine.getGameOverMessage(TYPE));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	// Main run for the host thread
	public void HostIO() {
		boolean PLAYERTURN = true;
		while(running && !engine.gameOver && !interrupted()) {
			if (engine.begin) {
				hasInput = false;
				try {
					ToClient.writeBoolean(running);
					ToClient.writeBoolean(engine.gameOver);
					// Exit the loop if game over
					if(engine.gameOver)
						break;
					turnDelta = engine.getActiveTurn();
					isPlayersTurn = turnDelta == PLAYERTURN;
					if (turnDelta != recordedTurn) {
						// Set hasInput flag
						hasInput = true;

						recordedTurn = turnDelta;
					}
					ToClient.writeBoolean(hasInput);
					ToClient.writeBoolean(isPlayersTurn);

					// write the results of opponents turns
					if (hasInput) {
						ToClient.writeUTF(engine.OutputData());
					} else if (isPlayersTurn) {
						engine.HandlePlayerCommand(getValidMove());
						engine.EndTurn();
						ToClient.writeBoolean(engine.gameOver);
						if(engine.gameOver)
							break;
						ToClient.writeUTF(engine.OutputData());
					} else {
						yield();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					// Push kill signal to other user handler
					e.printStackTrace();
					System.out.println("Socket is dead. triggering all kill switches");
					engine.KillSwitchHost();
					engine.KillSwitchPlayer();
					interrupt();
				} 
			} else {
				yield();
			}
		}
		
		if(engine.gameOver) {
			try {
				ToClient.writeUTF(engine.getGameOverMessage(TYPE));
				// wait for player to disconnect then end
				while(engine.PlayerConnected) {}
				ToClient.writeBoolean(engine.PlayerConnected);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void getPlayer() {
		if(TYPE == CLIENTTYPE.HOST) {
			User = engine.Players[0];
		} else if(TYPE == CLIENTTYPE.PLAYER) {
			User = engine.Players[1];
		}
	}
	// Command will be (Spell, Move) (Direction) [(spell directory ID) (target)] 
	public String getValidMove() {
		boolean isValidMove = false;
		String Command = "";
		while(!isValidMove) {
			try {
				Command = FromClient.readUTF();
				String[] parts = Command.split(" ");
				int move = Integer.parseInt(parts[1]);
				isValidMove = engine.ValidPosition(move, User.Iindex, User.Jindex);
				ToClient.writeBoolean(isValidMove);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NumberFormatException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return Command;
	}
}
