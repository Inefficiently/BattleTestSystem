package com.Inefficiently.clients;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.Inefficiently.startup.InputHandler;
import com.Inefficiently.startup.InputHandler.NUMBERSET;

// Handles the player commands of the host
public class HostClient extends Thread {

	InputHandler scan;

	Socket Connection;

	DataInputStream FromServer;
	DataOutputStream ToServer;
	
	ConnectionMonitor monitor;
	
	String MainMenuPrompt = "What will you do?\n1.Cast Spell\n2.Move\nDo:";
	String PositionOptionsPrompt = "/////\n1 2 3\n4 X 5\n6 7 8\n\\\\\\\\\\\nDirection:";
	String SpellMenuPrompt;
	String[] SpellMenu;

	public HostClient(InputHandler scan, Socket Connection, Socket KConnection) {
		this.scan = scan;
		this.Connection = Connection;
		try {
			FromServer = new DataInputStream(Connection.getInputStream());
			ToServer = new DataOutputStream(Connection.getOutputStream());
			// Populates the spell menu
			this.SpellMenu = FromServer.readUTF().split(",");
			CreateSpellMenuPrompt();
			// Prints first Map data
			System.out.println(FromServer.readUTF());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("\nError creating data streams.");
		}
		this.monitor = new ConnectionMonitor(KConnection);
		this.monitor.start();
	}

	@Override
	public void run() {
		boolean running = true;
		boolean gameOver = false;
		boolean hasInput = false;
		boolean YourTurn = false;
		boolean validInput = false;
		// run while the server is still running and the game hasn't ended
		while(running && !gameOver) {
			try {
				validInput = false;
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
					while(!validInput) {
						ToServer.writeUTF(Menu());
						validInput = FromServer.readBoolean();
						if(!validInput) {
							System.out.println("Position is invalid please try again\n");
						}
					}
					// Check the result of the move
					gameOver = FromServer.readBoolean();
					// Exit loop if game over
					if(gameOver)
						break;
					// Host will remain blind to what happens after their move until everyone else is done
					//System.out.println(FromServer.readUTF());
					FromServer.readUTF();
					//
					System.out.println("\nYour turn has ended. Waiting for opponent to make their move.");
				} else {
					// Run if its not our turn and the server has no input for us
					yield();
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Lost communication from server. Program terminating");
				System.exit(-1);
			}
		}
		if(gameOver) {
			this.monitor.SafeKillSwitch();
			try {
				System.out.println(FromServer.readUTF());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Wait until the other player finishes and disconnects
		try {
			FromServer.readBoolean();
			System.out.println("\nGame is over, the program will now close");
			Connection.close();
			System.exit(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void CreateSpellMenuPrompt() {
		String temp = "Cast which spell:\n";
		for (int i = 0; i < SpellMenu.length; i++) {
			temp += (i+1) + "." + SpellMenu[i] + "\n";
		}
		temp += "Cast Spell:";
		this.SpellMenuPrompt = temp;
	}

	// Returns a command string to be sent to the server for interpretation and execution
		public String Menu() {
			int option = scan.ScannerInt(MainMenuPrompt, NUMBERSET.NATURAL, 2) - 1;
			int position = scan.ScannerInt(PositionOptionsPrompt, NUMBERSET.NATURAL, 8);
			if(option == 0) {
				int SpellID = scan.ScannerInt(SpellMenuPrompt, NUMBERSET.NATURAL, SpellMenu.length) - 1;
				String[] targetOptions = {"10", "11"};
				int Target = scan.ScannerInt("Who will you target:\n1.Host\n2.Player\nTarget:", NUMBERSET.NATURAL, 2) - 1;
				return "Spell " + position + " " + SpellID + " " + targetOptions[Target];
			} else {
				return "Move " + position;
			}
		}
}
