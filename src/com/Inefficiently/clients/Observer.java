package com.Inefficiently.clients;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

// This Class is only meant to observe the output from the players moves
public class Observer extends Thread {
	
	Socket Connection;
	
	DataInputStream FromServer;
	
	public Observer(Socket Connection) {
		this.Connection = Connection;
		try {
			FromServer = new DataInputStream(Connection.getInputStream());
			System.out.println("\n" + FromServer.readUTF());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("\nError creating input stream. Server connection was most likely lost");
		}
	}
	
	
	@Override
	public void run() {
		boolean running = true;
		boolean gameOver = false;
		boolean hasInput = false;
		// run while the server is still running and the game hasn't ended
		while(running && !gameOver) {
			try {
				running = FromServer.readBoolean();
				gameOver = FromServer.readBoolean();
				// Exit the loop if game over
				if(gameOver)
					break;
				// Check if input has updated else the thread yields its processor time
				hasInput = FromServer.readBoolean();
				if(hasInput) {
					System.out.println(FromServer.readUTF());
				} else
					yield();
			} catch (IOException e) {
				System.out.println("Lost communication from server. Program terminating");
				System.exit(-1);
			}
		}
		if(gameOver) {
			try {
				System.out.println(FromServer.readUTF());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("\nGame is over the program will now close");
		try {
			Connection.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}
}
