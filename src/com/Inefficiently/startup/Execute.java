package com.Inefficiently.startup;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import com.Inefficiently.clients.HostClient;
import com.Inefficiently.clients.ObserverClient;
import com.Inefficiently.clients.PlayerClient;
import com.Inefficiently.engine.BattleEngine;
import com.Inefficiently.engine.EngineUtils;

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
 * V - 0.0.1 - 12/22/2018 # First Release
 * V - 0.1.0 - 01/17/2019
 * V - 0.1.0 - 01/18/2019
 * V - 0.1.0 - 01/19/2019
 * V - 0.1.0 - 01/20/2019
 * V - 0.1.0 - 01/24/2019
 * V - 0.1.0 - 01/28/2019
 * V - 0.1.0 - 01/29/2019
 * V - 0.1.0 - 02/03/2019
 * V - 0.1.0 - 02/04/2019
 * V - 0.1.0 - 02/05/2019
 * V - 0.1.0 - 02/06/2019
 * V - 0.1.0 - 02/09/2019
 * V - 0.1.0 - 02/10/2019
 * 
 * This a command line test of a battle system with multi-player
 * and single player modes
 * 
 *
 */


// This Class is the Starter thread for the game setting up host and client connections
public class Execute {
	public String Version = "0.1.0";
	
	// Variable Initialization
	
	ServerSocket SSocket;
	Socket CSocket;
	Socket KSocket;
	public final int ServerPort = 19982;
	public final int KillPort = ServerPort + 1;
	
	private InetAddress LocalIP;
	public String LocalIPAddress;
	
	public InputHandler scan;
	
	public BattleEngine engine;
	
	private Serve Server;
	
	public Execute() {
		scan = new InputHandler(new Scanner(System.in));
		engine = new BattleEngine();
	}
	
	// Give starting users a description of the game and rules
	public void InfoDump() {
		// TODO Change to read a prepared text file
		File InfoDumpText = EngineUtils.getFileFromResource("InfoDump.txt");
		try {
			Scanner scan = new Scanner(InfoDumpText);
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				line = EngineUtils.DynamicTextFinder(line, "<Version>", Version);
				System.out.println(line);
			}
			scan.close();
		} catch (FileNotFoundException e) {
			System.out.println("Your Information file is missing. Please reinstall to fix this issue");
		}
		
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
		System.out.println("I am currently unable to automatically find your \'external\' Ip and automatically portforward, however if you know it, you can use it (Google what is my ip)");
		System.out.println("You will also have to port forward ports " + ServerPort + " and " + KillPort + " in your router settings to let the game through your fire wall");
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
				CSocket = new Socket(ip, ServerPort);
				KSocket = new Socket(ip, KillPort);
				connected = true;
				String type = new DataInputStream(CSocket.getInputStream()).readUTF();
				System.out.println("You have joined as the type " + type);
				// temporary data stream to read what type of client is being used
				if(type.equals(CLIENTTYPE.PLAYER.getTYPENAME())) {
					PlayerClient thread = new PlayerClient(scan, CSocket, KSocket);
					thread.start();
				} else {
					// Assume anything that isn't player to be an observer
					ObserverClient thread = new ObserverClient(CSocket);
					thread.start();
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
			CSocket = new Socket(LocalIPAddress, ServerPort);
			KSocket = new Socket(LocalIPAddress, KillPort);
			// temporary data stream to read what type of client is being used then left to for GC
			String type = new DataInputStream(CSocket.getInputStream()).readUTF();
			System.out.println("You have joined as the type " + type + "\n");
			HostClient thread = new HostClient(scan, CSocket, KSocket);
			thread.start();
		} catch (UnknownHostException e) {
			System.out.println("\nUnable to find a server with the IP:" + LocalIPAddress);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println();
		} 
	}
	
	// Finds local ip address for use later on
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
