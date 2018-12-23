package com.Inefficiently.startup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.Inefficiently.engine.BattleEngine;
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
	
	// status update boolean
	boolean running = true;
	boolean gameover = false;
	boolean hasInput = false;
	boolean isPlayersTurn = false;
	
	// Passes Execute for Input methods, Socket for Communication
	public UserHandler(Execute exec, Socket connection, CLIENTTYPE TYPE) {
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
			ToClient.writeUTF(engine.OutputData());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("\nError getting the data streams created. Possible loss in connection");
		}
	}
	
	@Override
	public void run() {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Handler error");
		}
	}
	
	// Main run for player thread
	public void PlayerIO() {
		// TODO Actually Implement
		boolean PLAYERTURN = false;
		while(running && !engine.gameOver) {
			if (engine.begin) {
				hasInput = false;
				try {
					ToClient.writeBoolean(running);
					engine.GameOverCheck();
					ToClient.writeBoolean(engine.gameOver);
					// Exit the loop if game over
					if(engine.gameOver)
						break;
					turnDelta = engine.getPlayerTurn();
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
						String data = FromClient.readUTF();
						engine.HandlePlayerCommand(data);
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
					e.printStackTrace();
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
		// TODO Actually Implement
		while(running && !gameover) {
			if (engine.begin) {
				try {
					ToClient.writeBoolean(running);
					ToClient.writeBoolean(engine.gameOver);
					// Exit the loop if game over
					if(engine.gameOver)
						break;
					turnDelta = engine.getPlayerTurn();
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
		// TODO Actually Implement
		boolean PLAYERTURN = true;
		while(running && !engine.gameOver) {
			if (engine.begin) {
				hasInput = false;
				try {
					ToClient.writeBoolean(running);
					ToClient.writeBoolean(engine.gameOver);
					// Exit the loop if game over
					if(engine.gameOver)
						break;
					turnDelta = engine.getPlayerTurn();
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
						String data = FromClient.readUTF();
						engine.HandlePlayerCommand(data);
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
					e.printStackTrace();
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
}
