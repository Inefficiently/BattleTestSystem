package com.Inefficiently.startup;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import com.Inefficiently.clients.Host;
import com.Inefficiently.clients.Observer;
import com.Inefficiently.clients.Player;
import com.Inefficiently.engine.BattleEngine;

/**
 * 
 * @author Inefficiently
 * 
 * Start - 12/16/2018
 * 
 * Development Tracker/ Days worked
 * V - 0.0.1 - 12/16/2018
 * V - 0.0.1 - 12/17/2018
 * V - 0.0.1 - 12/18/2018
 * V - 0.0.1 - 12/19/2018
 * V - 0.0.1 - 12/22/2018
 * 
 * 
 * This a command line test of a battle system with multi-player
 * and single player modes
 * 
 *
 */


// This Class is the Starter thread for the game setting up host and client connections
public class Execute {
	public String Version = "0.0.1";
	
	// Variable Initialization
	
	ServerSocket SSocket;
	Socket CSocket;
	public final int port = 19982;
	
	private InetAddress LocalIP;
	public String LocalIPAddress;
	
	public InputHandler scan;
	
	public BattleEngine engine;
	
	private Serve Server;
	
	public Execute() {
		scan = new InputHandler(new Scanner(System.in));
		engine = new BattleEngine(10);
	}
	
	// Give starting users a description of the game and rules
	// Provide more information as a sort of Wiki
	public void InfoDump() {
		// TODO Change to read a prepared text file
		System.out.println("------------------------------------------------------------------------------------------");
		System.out.println("This game is an experimental test bed of battling mechanics");
		System.out.println("for a game I am working on. This is Version " + Version + " and is in early development");
		System.out.println("I would very much appreciate feedback and bug reports, if you have any please send them to");
		System.out.println("ineffeciently.github@gmail.com and I will try to respond as quickly as I can");
		System.out.println("------------------------------------------------------------------------------------------");
		System.out.println("\nThis game is a turn-based strategy game in which your player is attempting to defeat an");
		/* Not yet implemented so it will be cut out
		System.out.println("enemy. There are player classes such as Mages, Knights, and Assasins (more may be added)");
		System.out.println("Every Class has strengths and weaknesses which will be presented on request");
		System.out.println("There are Attacks that are limited in Range depending on your class and its level");
		System.out.println("There are traps that can be placed in order to trap and restrict the movement of opponents");
		System.out.println("Each class has a unique movement possibilities that are intrinsic to the player class");
		*/
		System.out.println("enemy. You have 4 moves to choose from for now. A healing water spell, rock throw,");
		System.out.println("weak gust attack, and an magic explosion. Sorry for the lack of depth I wanted to have a");
		System.out.println("working multiplayer game to have something to show. There will hopefully be more added in the");
		System.out.println("next update. Enjoy the Game!");
	}
	
	// Run the game
	public void run() {
		InfoDump();
		StartGameMenu();
	}
	
	// menu for loading the game
	public void StartGameMenu() {
		boolean isHost = scan.ScannerBoolean("Do you want to host the game?");
		if(isHost) {
			ServerMode();
		} else {
			ClientMode();
		}
	}
	
	public void setCSocket(Socket connection) {
		CSocket = connection;
	}
	// Run Server specific commands and handle both client input possibly observer input too.
	public void ServerMode() {
		// Display usable IP
		SetLocalIPAddress();
		System.out.println("\nYour local Ip is " + LocalIPAddress);
		System.out.println("I am currently unable to automatically find your \'external\' Ip, however if you know it, you can use it (Google what is my ip)");
		System.out.println("You will also have to port forward port " + port + " in your router settings to let the game through your fire wall");
		System.out.println("Please make sure your opponent knows the Ip you are using as the host to connect and start the battle");
		// Set up Server Socket
		Server = new Serve(this, SSocket);
		Server.start();
		ServerPlayerMode();
		
	}
	
	// Run Client specific code and handle output from the server
	public void ClientMode() {
		boolean connected = false;
		while (!connected) {
			String ip = scan.ScannerIp();
			try {
				CSocket = new Socket(ip, port);
				connected = true;
				DataInputStream tempFromServer = new DataInputStream(CSocket.getInputStream());
				String type = tempFromServer.readUTF();
				System.out.println("You have joined as the type " + type);
				// temporary data stream to read what type of client is being used
				if(type.equals(CLIENTTYPE.PLAYER.getTYPENAME())) {
					Player thread = new Player(scan, CSocket);
					thread.start();
				} else {
					// Assume anything that isn't player to be an observer
					ObserverMode();
				}
			} catch (UnknownHostException e) {
				System.out.println("\nUnable to find a server with the IP:" + ip);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println();
			} 
		}
	}
	
	// Run a special Client that accepts the server socket
	public void ServerPlayerMode() {
		try {
			CSocket = new Socket(LocalIPAddress, port);
			DataInputStream tempFromServer = new DataInputStream(CSocket.getInputStream());
			String type = tempFromServer.readUTF();
			System.out.println("You have joined as the type " + type + "\n");
			// temporary data stream to read what type of client is being used
			Host Player = new Host(scan, CSocket);
			Player.start();
		} catch (UnknownHostException e) {
			System.out.println("\nUnable to find a server with the IP:" + LocalIPAddress);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println();
		} 
	}
	// Possible Observer mode view Battle progress only
	public void ObserverMode() {
		Observer thread = new Observer(CSocket);
		thread.start();
	}
	
	public void SetLocalIPAddress() {
		try {
			LocalIP = InetAddress.getLocalHost();
			LocalIPAddress = LocalIP.getHostAddress();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("\nUnable to find your local Ip due to the above error.\nCheck your internet connection and try again");
			System.exit(-1);
		}
	}
	
	public enum CLIENTTYPE {
		HOST("Host"), PLAYER("Player"), OBSERVER("Observer");
		
		private String TYPENAME;
		private CLIENTTYPE(String TYPENAME) {
			this.TYPENAME = TYPENAME;
		}
		
		public String getTYPENAME() {
			return TYPENAME;
		}
	}
}
