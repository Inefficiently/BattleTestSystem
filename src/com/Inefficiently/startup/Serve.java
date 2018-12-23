package com.Inefficiently.startup;

import java.io.IOException;
import java.net.ServerSocket;

import com.Inefficiently.startup.Execute.CLIENTTYPE;

// this class is supposed to constantly give connections to users for the server
public class Serve extends Thread {
	Execute exec;
	ServerSocket SSocket;
	public Serve(Execute exec, ServerSocket SSocket) {
		this.exec = exec;
		try {
			this.SSocket = new ServerSocket(exec.port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Server is set up and will begin serving clients");
	}

	@Override
	public void run() {
		try {
			// Generate a temporary socket and Client type to pass to the UserHandler thread
			// Create Host Player
			
			CLIENTTYPE TempType = CLIENTTYPE.HOST;
			new UserHandler(exec, SSocket.accept(), TempType).start();
			// Change temp to make one Player and N observers
			TempType = CLIENTTYPE.PLAYER;
			//System.out.println("Waiting for connections");
			while(true) {
				new UserHandler(exec, SSocket.accept(), TempType).start();
				//System.out.println("New Connection found!\nThey will be a " + TempType.getTYPENAME());
				//Ensures that after the first loop all other connections are observers
				TempType = CLIENTTYPE.OBSERVER;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error in setting up the server make sure you are not running multiple server instances");
			System.exit(-1);
		}
	}
}
