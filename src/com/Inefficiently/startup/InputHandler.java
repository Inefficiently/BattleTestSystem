package com.Inefficiently.startup;

import java.io.IOException;
import java.util.Scanner;

public class InputHandler {

	Scanner scan;

	public InputHandler(Scanner scan) {
		this.scan = scan;
	}

	// Ensure limited input for boolean return types and prompts a dialogue
	public boolean ScannerBoolean(String prompt) {
		// Accepted valid inputs
		String[] validYesAnswers = {"yes", "y", "true", "t", "1"};
		String[] validNoAnswers = {"no", "n", "false", "f", "0"};
		// Input
		while(true) {
			System.out.print(prompt);
			String in = this.scan.nextLine().toLowerCase();
			ScannerFlush();
			// Test if response is False
			for (int i = 0; i < validNoAnswers.length; i++) {
				if(in.equals(validNoAnswers[i])) {
					return false;
				}
			}
			// Test if response is True
			for (int i = 0; i < validYesAnswers.length; i++) {
				if(in.equals(validYesAnswers[i])) {
					return true;
				}
			}
			// If the program reaches this stage no valid input has been found so the loop will reset
			System.out.println("\nInput is invalid. (Yes/No Expected)");
		}
	}

	public int ScannerInt(String prompt, NUMBERSET set) {
		while(true) {
			System.out.print(prompt);
			boolean isInt = scan.hasNextInt();
			ScannerFlush();
			if(isInt) {
				int in = scan.nextInt();
				//scan.nextLine();
				if(in >= set.getMin() && in < set.getMax()) {
					return in;
				} else {
					System.out.println("\nInput is Out of Range. (" + set.getName() + ")\n|Min:" + set.getMin() + " |Max:" + set.getMax());
				}
			} else {
				String in = scan.nextLine();
				ScannerFlush();
				System.out.println("\n\"" + in + "\" is invalid. (" + set.getName() + " expected)");
			}
		}
	}

	public int ScannerInt(String prompt, NUMBERSET set, int MaxCap) {
		while(true) {
			System.out.print(prompt);
			boolean isInt = scan.hasNextInt();
			if(isInt) {
				int in = scan.nextInt();
				if(in >= set.getMin() && in <= MaxCap) {
					return in;
				} else {
					System.out.println("\nInput is Out of Range. (" + set.getName() + ")\n|Min:" + set.getMin() + " |Max:" + MaxCap);
				}
			} else {
				String in = scan.nextLine();
				System.out.println("\n\"" + in + "\" is invalid. (" + set.getName() + " expected)");
			}
			scan.hasNextLine();
		}
	}

	public String ScannerIp() {
		String prompt = "Enter the the Ip of the host you are trying to connect to:";
		// Check to see if there is an input and it has 4 bytes separated by periods
		while(true) {
			System.out.print(prompt);
			String in = scan.nextLine();
			String[] parts = in.split("\\.");
			if(parts == null || parts.length != 4) {

				System.out.println("\nInvalid format for Ip");
			} else {
				boolean error = false;
				for (int i = 0; i < parts.length; i++) {
					int part;
					try {
						part = Integer.parseInt(parts[i]);
					} catch (Exception e) {
						error = true;
						break;
					}
					// Test if it is not a byte
					if(!(part > -1 && part < 256)) {
						error = true;
						break;
					}
				}
				if(error) {
					
					System.out.println("\nThe Ip you entered is not valid. Be sure to check every number is correct");
				} else {
					return in;
				}
			}
		}
	}
	
	public void ScannerClose() {
		this.scan.close();
	}
	
	// read if there is any trailing input from a \n and remove it by reading all avaiable bytes in the input stream
	public void ScannerFlush() {
		try {
			while(System.in.available() > 0) {
				scan.nextByte();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public enum NUMBERSET {
		// Define Expected Number sets
		INTEGER(Integer.MIN_VALUE, Integer.MAX_VALUE, "Integer"),
		WHOLE(0, Integer.MAX_VALUE, "Whole Number"),
		NATURAL(1, Integer.MAX_VALUE, "Natural Number");

		private int min, max;
		private String name;

		private NUMBERSET(int min, int max, String name) {
			this.min = min;
			this.max = max;
			this.name = name;
		}

		public int getMin() {
			return min;
		}

		public int getMax() {
			return max;
		}

		public String getName() {
			return name;
		}
	}
}
