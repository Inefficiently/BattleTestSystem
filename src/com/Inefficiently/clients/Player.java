package com.Inefficiently.clients;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.Inefficiently.startup.InputHandler;
import com.Inefficiently.startup.InputHandler.NUMBERSET;

// This Class is meant to handle user input and out put from the server
public class Player extends Thread {
	
	InputHandler scan;
	
	Socket Connection;
	
	DataInputStream FromServer;
	DataOutputStream ToServer;
	
	public Player(InputHandler scan, Socket Connection) {
		this.scan = scan;
		this.Connection = Connection;
		try {
			FromServer = new DataInputStream(Connection.getInputStream());
			ToServer = new DataOutputStream(Connection.getOutputStream());
			System.out.println("\n" + FromServer.readUTF());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("\nError creating data streams. Server connection was most likely lost");
		}
	}
	
	@Override
	public void run() {
		boolean running = true;
		boolean gameOver = false;
		boolean hasInput = false;
		boolean YourTurn = false;
		// run while the server is still running and the game hasn't ended
		System.out.println("Successfully Connected. The game is starting\nWaiting for host player to make their move");
		while(running && !gameOver) {
			try {
				running = FromServer.readBoolean();
				gameOver = FromServer.readBoolean();
				// Exit loop if game over
				if(gameOver)
					break;
				// Check if input has updated else the thread yields its processor time
				hasInput = FromServer.readBoolean();
				YourTurn = FromServer.readBoolean();
				//  Reads the result of the opponents move if available
				if(hasInput) {
					System.out.println(FromServer.readUTF());
				} else if(YourTurn) {
					// It is your turn so hasInput should be false
					System.out.println("It is now your turn. Please select your move");
					ToServer.writeUTF(Menu());
					// Check the result of the move
					gameOver = FromServer.readBoolean();
					// Exit loop if game over
					if(gameOver)
						break;
					System.out.println(FromServer.readUTF());
					System.out.println("\nYour turn has ended. Waiting for opponent to make their move.");
				} else {
					// Run if its not our turn and the server has no input for us
					yield();
				}
			} catch (IOException e) {
				System.out.println("Lost communication from server. Program terminating");
				System.exit(-1);
			}
		}
		if(gameOver) {
			try {
				System.out.println(FromServer.readUTF());
				System.out.println(FromServer.readUTF());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			Connection.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("\nGame is over, the program will now close");
		System.exit(0);
	}
	
	// Returns a command string to be sent to the server for interpretation and execution
	public String Menu() {
		// TODO fill out options
		String prompt = "What move will you use?\n1.Heal\n2.Rock Throw\n3.Light gust\n4.Explosion\nMove:";
		int option = scan.ScannerInt(prompt, NUMBERSET.NATURAL, 4) - 1;
		prompt = "Who will you target?\n1.Host\n2.Player\nTarget:";
		String[] moves = {"Water","Earth","Air","Fire"};
		int target = scan.ScannerInt(prompt, NUMBERSET.NATURAL, 2) - 1;
		String[] targets = {"Host", "Player"};
		return targets[target] + " " + moves[option];
	}
}
