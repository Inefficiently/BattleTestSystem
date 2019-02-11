package com.Inefficiently.clients;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


// This is a kill switch that will end the program arbitrarily if connection
// With the other player is lost
public class ConnectionMonitor extends Thread {
	private DataOutputStream inputs;
	boolean safed = false;

	// This is the connection that monitors the other player if it dies that means
	// that the player has disconnected
	public ConnectionMonitor(Socket connection) {
		try {
			inputs = new DataOutputStream(connection.getOutputStream());
		} catch (IOException e) {
			System.out.println("Error creating Connection Monitor");
		}
	}

	// Continuously writes to a socket to check for disconnetions
	public void run() {
		boolean active = true;
		while (active) {
			try {
				inputs.writeUTF("");
				yield();
			} catch (IOException e) {
				active = false;
			}
		}
		if (!safed) {
			System.out.println("The other player has unexpectedly disconnected");
			System.out.println("The game will be terminated. Thank you for playing");
			System.exit(0);
		}
	}

	// if the game is over call this to stop connection monitor
	public void SafeKillSwitch() {
		safed = false;
		System.out.println("Safed the kill switch");
		interrupt();
	}
}
