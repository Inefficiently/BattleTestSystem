package com.Inefficiently.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner; 

// Hold miscellaneous methods that are needed in the Execution of battle engine
public class EngineUtils {
	// Used to create the default config file
	private static String[] ExpectedConfig = {"Max-Health","Map-Size"};
	private static String[] DefaultConfigValues = {"100","5"};
	
	public static String MultiplyString(String in, int multiply) {
		String output = "";
		for (int i = 0; i < multiply; i++) {
			output += in;
		}
		return output;
	}
	
	// return a hash map of values for configuration processing
	public static HashMap<String, String> GetConfiguration() {
		HashMap<String, String> returnValue = new HashMap<String, String>();
		ArrayList<String> FileData = new ArrayList<>();
		File Config = getFileFromResource("prefrences.txt");
		if(!Config.exists()) {
			Config = CreateConfig();
		}
		try {
			Scanner scan = new Scanner(Config);
			while (scan.hasNextLine()) {
				FileData.add(scan.nextLine());
			}
			scan.close();
			// put file data into the hash map
			for (int i = 0; i < FileData.size(); i++) {
				String line = FileData.get(i);
				int found = line.indexOf(":");
				if(found > -1 && line.substring(found).length() > 0) {
					returnValue.put(line.substring(0, found), line.substring(found + 1));
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// Should never be reached
		}
		return returnValue;
	}
	
	// Take a broken or missing file and fill it with the defaults
	public static File CreateConfig() {
		File Config = getFileFromResource("prefrences.txt");
		// Wipe the file in case it got corrupted
		if(Config.exists()) {
			Config.delete();
		}
		PrintWriter writer;
		try {
			Config.createNewFile();
			writer = new PrintWriter(Config);
			// Set default values of the config file
			for (int i = 0; i < ExpectedConfig.length; i++) {
				writer.println(ExpectedConfig[i] + ":" + DefaultConfigValues[i]);
			}
			//
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Config;
	}
	
	// returns a string with the data parsed out in the form key:value
	public static String GetStringValue(String input) {
		int found = input.indexOf(":");
		if(found < 0) {
			found = 0;
		}
		return input.substring(found);
	}
	
	public static String GetDefaultConfigValue(String configProperty) {
		int IndexOfValue = -1;
		for (int i = 0; i < ExpectedConfig.length; i++) {
			if(configProperty.equals(ExpectedConfig[i])) {
				IndexOfValue = i;
				break;
			}
		}
		if(IndexOfValue > -1 && IndexOfValue < DefaultConfigValues.length) {
			return DefaultConfigValues[IndexOfValue];
		} else {
			return "";
		}
	}
	
	// gets a default hash map so that inputs can be parsed out of order
	public static HashMap<String, String> getDefaultHashMapConfig() {
		HashMap<String, String> returnValue = new HashMap<String, String>();
		if (DefaultConfigValues.length > ExpectedConfig.length) {
			for (int i = 0; i < ExpectedConfig.length; i++) {
				returnValue.put(ExpectedConfig[i], DefaultConfigValues[i]);
			}
		} else if (DefaultConfigValues.length <= ExpectedConfig.length) {
			for (int i = 0; i < DefaultConfigValues.length; i++) {
				returnValue.put(ExpectedConfig[i], DefaultConfigValues[i]);
			}
		}
		return returnValue;
	}
	
	// will check the value for an integer and if failed will supply a default value
	public static int ErrorCheckHashInteger(String key, String value) {
		int returnValue = 0;
		try {
			returnValue = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			returnValue = Integer.parseInt(GetDefaultConfigValue(key));
		}
		return returnValue;
	}
	// Checks if below a minimum value and clamps answers to it
	public static int ErrorCheckHashInteger(String key, String value, int min) {
		int returnValue = 0;
		try {
			returnValue = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			returnValue = Integer.parseInt(GetDefaultConfigValue(key));
		}
		if(returnValue < min) {
			returnValue = min;
		}
		return returnValue;
	}
	
	// Clamps integer output down to a certain range
	public static int ErrorCheckHashInteger(String key, String value, int min, int max) {
		int returnValue = 0;
		try {
			returnValue = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			returnValue = Integer.parseInt(GetDefaultConfigValue(key));
		}
		if(returnValue < min) {
			returnValue = min;
		}
		if(returnValue > max) {
			returnValue = max;
		}
		return returnValue;
	}
	
	public static File getFileFromResource(String path) {
		try {
			return new File(DynamicTextFinder(EngineUtils.class.getClassLoader().getResource("res/" + path).toString().substring(5), "%20", " "));
		} catch (Exception e) {
			// TODO: handle exception
			return null;
		}
	}
	
	public static String DynamicTextFinder(String line, String target, String value) {
		int found =	line.indexOf(target);
		if(found != -1) {
			line = line.substring(0, found) + value + line.substring(found + target.length());
		}
		return line;
	}
}
