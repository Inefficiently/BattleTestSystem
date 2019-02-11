package com.Inefficiently.engine;

import java.util.ArrayList;

import com.Inefficiently.engine.Spell.SPELLSTATE;

// This Class is the Runic language processor
public class RunicSpellEngine {

	// Allow the spell to influence every aspect of the battle
	private BattleEngine engine;
	public boolean DebugMode = false;

	// Lists all valid functions
	private static final String[] FUNCTIONS = {"MOV","TEQ","TGT",
			"TLT","LBL","JMP", 
			"ADD", "SUB", "MUL", 
			"DIV", "MOD", "RUN", 
			"SLP"};

	// List all outcomes of compiler and runtime errors
	private static final String[] ERRORS = {"No Syntax Errors Found","Syntax-Missing Endline Character ';'", 
			"Syntax-Unrecognized Function", 
			"Syntax-Too many Arguments",
			"Syntax-Invalid Argument",
			"Syntax-Unfinished Line",
			"Syntax-Improper Formating",
			"Syntax-Unable to write to literals",
			"Syntax-Unable to parse argument",
			"Syntax-Insufficient Arguments",
			"Syntax-Too Many Arguments",
			"Syntax-Don't try it Anakin",
			"Syntax-Number is out of valid range (-999 to 999)",
			"Syntax-Label not found",
			"Syntax-Input cannot be less than 1",
			"Syntax-Can't have identical labels"};	

	private static final String[] REGISTERS = {"DAT","ADV", "ACT", "TAR"};

	private static final String[] PINS = {"DIS", "SIG", "DIR", "RDR"};

	public RunicSpellEngine(BattleEngine engine) {
		this.engine = engine;
	}
	
	public RunicSpellEngine() {
		this.engine = new BattleEngine();
	}
	
	public RunicSpellEngine(boolean debug) {
		this.engine = new BattleEngine();
		this.DebugMode = debug;
	}
	
	public RunicSpellEngine(BattleEngine engine, boolean debug) {
		this.engine = engine;
		this.DebugMode = debug;
	}
	
	// Test Compiler for new default spell creation returns various int type error codes and a line number
	// TODO calculate mana cost and other important variables later
	// Compile Codes
	// 0. No Errors
	// Fatal Errors:
	// 1. Check for a single command on a line
	// 2. Check if command exists
	// I2. Line may have one conditional on it at the beginning (+ or -) if done wrong will trigger error 2
	// 3. No line should have more than 3 arguments
	// 4. No line should have an argument larger than 3 characters unless it is negative (-100)
	// 5. Empty or incomplete line all lines must have at least 3 characters
	// 6. Must have proper format no double spaces or space at the end of the line
	// 7. Attempting to write to Read only Data (ROD) like pins or integer literals
	// 8. Integer literals must be  able to be parsed or given a valid integer source (ex. 97 v.s 9p, Registers or pins)
	// 9. Insufficient arguments for function
	// 10. Too many arguments
	// 11. Divide by 0 (Compiler will not allow it however it can happen at runtime in which the spell will immediately detonate lol)
	// 12. Number is outside acceptable bounds (-999 to 999)
	// 13. Check if label exists when called by jump
	// 14. Sleep can't accept a negative
	// 15. Can't have 2 identical labels
	
	// Test run for new default spell creation returns verbose updates
	public void CastSpell(Spell object) {
		if (RunicCompiler(object.SpellCommands)) {
			String[] commands = PreProcessSpell(object.SpellCommands);
			if(DebugMode) {
				System.out.println("Running a Spell");
				System.out.println("Spell payload:" + object.payloadtype.payloadName);
				System.out.println("Spell # of Lines:" + commands.length);
				System.out.println("Spell Sensor:" + object.sensorType.name());
				System.out.println();
			}
			int limitBreaker = 0;
			PollSensor(object);
			while (object.state != SPELLSTATE.SUSPENDED && limitBreaker < 100) {
				if (object.state != SPELLSTATE.DONE && object.pointer < commands.length) {
					ExecuteLine(object, commands[object.pointer]);
					if(DebugMode) {
						if (object.pointer > 0) {
							if (!getFunctionCalled(commands[object.pointer - 1]).equals("JMP"))
								PrintSpellState(object, getFunctionCalled(commands[object.pointer - 1]));
						}
						engine.UpdateSpell(object);
					}
				}
				// spell will update one last time before it dies
				if(object.pointer >= commands.length) {
					engine.UpdateSpell(object);
					object.state = SPELLSTATE.DONE;
				}
				if(object.state == SPELLSTATE.DONE) {
					break;
				}
				limitBreaker++;
				if(DebugMode)
					System.out.println("Lines attempted without suspension: " + limitBreaker);
			}
		}
	}

	public boolean RunicCompiler(String SpellCommands) {
		String[] commands = PreProcessSpell(SpellCommands);
		int Ecode = 0;
		int foundLine = findEndlineCharacters(SpellCommands, commands);
		// #1
		if(foundLine != -1) {
			Ecode = 1;
			PrintCompilerResponse(commands[foundLine], Ecode, foundLine);
			return false;
		}
		for (int i = 0; i < commands.length; i++) {
			String line = commands[i];
			Ecode = ValidSyntax(line);
			if(Ecode == 0) {
				String[] parts = PreProcessLine(RemoveConditionals(line));
				switch (parts[0]) {
		/*MOV*/		case "MOV":
						// #9 MOV must receive a value and it must have register
						if(parts.length == 3) {
							// #7
							if(!InArray(parts[2], REGISTERS)) {
								Ecode = 7;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							}
							// #8
							if(!(InArray(parts[1], REGISTERS) || InArray(parts[1], PINS) || CanParseInt(parts[1]))) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							}
						} else {
							Ecode = 9;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						}
						break;
		/*SLP*/		case "SLP":
						// #9 SLP must have exactly one argument the number of cycles to pause execution
						if(parts.length < 2) {
							Ecode = 9;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else if(parts.length > 2) {
							// #10 SLP must have exactly one argument the number of cycles to pause execution
							Ecode = 10;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else {
							// #8
							if(!CanParseInt(parts[1])) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							} else {
								// #14
								if(Integer.parseInt(parts[1]) < 1) {
									Ecode = 14;
									PrintCompilerResponse(line, Ecode, i);
									return false;
								}
							}
						}
						break;
		/*ADD*/		case "ADD":
						// #9 ADD can only take one argument which will be added to DAT
						if(parts.length < 2) {
							Ecode = 9;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else if(parts.length > 2) {
							// #10 ADD can only take one argument which will be added to DAT
							Ecode = 10;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else {
							// #8
							if(!(InArray(parts[1], REGISTERS) || InArray(parts[1], PINS) || CanParseInt(parts[1]))) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							}
						}
						break;
		/*SUB*/		case "SUB":
						// #9 SUB can only take one argument which will be subtracted from DAT
						if(parts.length < 2) {
							Ecode = 9;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else if(parts.length > 2) {
							// #10 SUB can only take one argument which will be subtracted from DAT
							Ecode = 10;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else {
							// #8
							if(!(InArray(parts[1], REGISTERS) || InArray(parts[1], PINS) || CanParseInt(parts[1]))) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							}
						}
						break;
		/*MUL*/		case "MUL":
						// #9 MUL can only take one argument which will be multiplied to DAT
						if(parts.length < 2) {
							Ecode = 9;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else if(parts.length > 2) {
							// #10 MUL can only take one argument which will be multiplied to DAT
							Ecode = 10;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else {
							// #8
							if(!(InArray(parts[1], REGISTERS) || InArray(parts[1], PINS) || CanParseInt(parts[1]))) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							}
						}
						break;
		/*DIV*/		case "DIV":
						// #9 DIV can only take one argument which will be divided from DAT
						if(parts.length < 2) {
							Ecode = 9;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else if(parts.length > 2) {
							// #10 DIV can only take one argument which will be divided from DAT
							Ecode = 10;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else {
							// #8
							if(!(InArray(parts[1], REGISTERS) || InArray(parts[1], PINS) || CanParseInt(parts[1]))) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							} else {
								// #11 0 / 0 == BOOM!
								if(CanParseInt(parts[1])) {
									if (Integer.parseInt(parts[1]) == 0) {
										Ecode = 11;
										PrintCompilerResponse(line, Ecode, i);
										return false;
									}
								}
							}
						}
						break;
		/*MOD*/		case "MOD":
						// #9 MOD can only take one argument which will be modulo from DAT
						if(parts.length < 2) {
							Ecode = 9;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else if(parts.length > 2) {
							// #10 MOD can only take one argument which will be modulo from DAT
							Ecode = 10;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else {
							// #8
							if(!(InArray(parts[1], REGISTERS) || InArray(parts[1], PINS) || CanParseInt(parts[1]))) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							} else {
								// #11 0 % 0 == BOOM!
								if(Integer.parseInt(parts[1]) == 0) {
									Ecode = 11;
									PrintCompilerResponse(line, Ecode, i);
									return false;
								}
							}
						}
						break;
		/*LBL*/		case "LBL":
						// #9 LBL can only take one argument (int) which will be its identifier
						if(parts.length < 2) {
							Ecode = 9;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else if(parts.length > 2) {
							// #10 LBL can only take one argument (int) which will be its identifier
							Ecode = 10;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else {
							// #8
							if(!CanParseInt(parts[1])) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							}
							// #15
							for (int j = 0; j < commands.length; j++) {
								if(j != i && RemoveConditionals(commands[j]).equals(line)) {
									Ecode = 15;
									PrintCompilerResponse(line, Ecode, i);
									return false;
								}
							}
						}
						break;
		/*JMP*/		case "JMP":
						// #9 JMP can only take one argument (int) which will be its identifier
						if(parts.length < 2) {
							Ecode = 9;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else if(parts.length > 2) {
							// #10 LBL can only take one argument (int) which will be its identifier
							Ecode = 10;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						} else {
							// #8
							if(!CanParseInt(parts[1])) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							} else {
								// #13
								if(!findJumpLabel(SpellCommands, parts[1])) {
									Ecode = 13;
									PrintCompilerResponse(line, Ecode, i);
									return false;
								}
							}
						}
						break;
		/*TEQ*/		case "TEQ":
						// #9 TEQ must receive a 2 arguments
						if(parts.length == 3) {
							// #8
							if(!(InArray(parts[1], REGISTERS) || InArray(parts[1], PINS) || CanParseInt(parts[1]))) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							}
							// #8
							if(!(InArray(parts[2], REGISTERS) || InArray(parts[2], PINS) || CanParseInt(parts[2]))) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							}
						} else {
							Ecode = 9;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						}
						break;
		/*TGT*/		case "TGT":
						// #9 TGT must receive a 2 arguments
						if(parts.length == 3) {
							// #8
							if(!(InArray(parts[1], REGISTERS) || InArray(parts[1], PINS) || CanParseInt(parts[1]))) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							}
							// #8
							if(!(InArray(parts[2], REGISTERS) || InArray(parts[2], PINS) || CanParseInt(parts[2]))) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							}
						} else {
							Ecode = 9;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						}
						break;
		/*TLT*/		case "TLT":
						// #9 TLT must receive a 2 arguments
						if(parts.length == 3) {
							// #8
							if(!(InArray(parts[1], REGISTERS) || InArray(parts[1], PINS) || CanParseInt(parts[1]))) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							}
							// #8
							if(!(InArray(parts[2], REGISTERS) || InArray(parts[2], PINS) || CanParseInt(parts[2]))) {
								Ecode = 8;
								PrintCompilerResponse(line, Ecode, i);
								return false;
							}
						} else {
							Ecode = 9;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						}
						break;
		/*RUN*/		case "RUN":
						// #10 RUN takes no arguments and spits an output onto dat
						if(parts.length != 1) {
							Ecode = 10;
							PrintCompilerResponse(line, Ecode, i);
							return false;
						}
						break;
				}
			} else {
				// Call the error interpreter give line string and line number
				PrintCompilerResponse(line + ";", Ecode, i);
				return false;
			}
		}
		PrintCompilerResponse(null, Ecode, 0);
		return true;
	}
	
	// Executes a single line of code to make it more readable and update pointer
	public void ExecuteLine(Spell spell, String line) {
		boolean ConditionalT = line.charAt(0) == '+';
		boolean ConditionalF = line.charAt(0) == '-';
		if(ConditionalT || ConditionalF) {
			line = line.substring(1);
		} else {
			// Deactivate the conditional functions
			spell.ConditionalOn = false;
		}
		String[] parts = PreProcessLine(line);
		// Runs if the statement isn't dependent on condition
		// Runs if the statement depends on true if dat is 100
		// Runs if the statement is false if dat is 100
		//if (!(ConditionalT || ConditionalF)) {
			// If conditionals are active then run the line that matches the conditional result
			if (!spell.ConditionalOn || (spell.ConditionalOn && (((spell.ConditionalResult == true) && ConditionalT) || (spell.ConditionalResult == false) && ConditionalF))) {
				switch (parts[0]) {
				case "MOV":
					Move(spell, parts[1], parts[2]);
					spell.pointer++;
					break;
				case "SLP":
					Sleep(spell, parts[1]);
					spell.pointer++;
					break;
				case "JMP":
					Jump(spell, parts[1]);
					break;
				case "RUN":
					Run(spell);
					spell.pointer++;
					break;
				case "ADD":
					Add(spell, parts[1]);
					spell.pointer++;
					break;
				case "SUB":
					Subtract(spell, parts[1]);
					spell.pointer++;
					break;
				case "MUL":
					Multiply(spell, parts[1]);
					spell.pointer++;
					break;
				case "DIV":
					Divide(spell, parts[1]);
					spell.pointer++;
					break;
				case "MOD":
					Modulo(spell, parts[1]);
					spell.pointer++;
					break;
				case "TEQ":
					Teq(spell, parts[1], parts[2]);
					spell.pointer++;
					break;
				case "TGT":
					Tgt(spell, parts[1], parts[2]);
					spell.pointer++;
					break;
				case "TLT":
					Tlt(spell, parts[1], parts[2]);
					spell.pointer++;
					break;
				case "LBL":
					spell.pointer++;
					break;
				}
			//}
				} else {
			spell.pointer++;
		}
	}

	public boolean InArray(String input, String[] set) {
		for (int i = 0; i < set.length; i++) {
			if(input.equals(set[i]))
				return true;
		}
		return false;
	}

	public boolean CanParseInt(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	// Clamps any integer to -999 and 999
	public int ClampInt(int in) {
		if(in > 999) {
			return 999;
		}
		if(in < -999) {
			return -999;
		}
		return in;
	}

	// Standardizes spell pre processing ensuring all caps and proper split lines
	public String[] PreProcessSpell(String Spell) {
		Spell = Spell.toUpperCase();
		return Spell.replaceAll("[^\\S ]+", "").split(";");
	}
	
	// Standardized line pre processing ensuring there is no white space on individual parts or empty parts
	public String[] PreProcessLine(String line) {
		String[] partsArray = line.split(" ");
		ArrayList<String> parts = new ArrayList<String>();
		for (int i = 0; i < partsArray.length; i++) {
			partsArray[i] = partsArray[i].replaceAll("[^\\S ]+", "");
			if(partsArray[i].length() > 0) {
				parts.add(partsArray[i]);
			}
		}
		String[] finalArray = new String[parts.size()];
		for (int i = 0; i < finalArray.length; i++) {
			finalArray[i] = parts.get(i);
		}
		return finalArray;
	}
	// Ensure every line has an end line character strictly -1 is good anything else is wrong
	public int findEndlineCharacters(String Spell, String[] lines) {
		Spell = Spell.toUpperCase();
		for (int i = 0; i < lines.length; i++) {
			if(Spell.indexOf(lines[i] + ";") == -1) {
				return i;
			}
		}
		return -1;
	}
	
	// Makes sure if a jump is called there is a label for it to jump to
	public boolean findJumpLabel(String Spell, String label) {
		Spell = Spell.toUpperCase();
		return Spell.indexOf("LBL " + label + ";") != -1;
	}
	// Heuristic Analysis of lines and fist pass to make sure things are in a complete enough form to continue compilation
	public int ValidSyntax(String line) {
		int ValidCommand = 0;
		// #5
		if (line.length() > 2) {
			String[] parts = PreProcessLine(line);
			// #I2
			if (parts[0].contains("+") || parts[0].contains("-")) {
				parts[0] = parts[0].substring(1);
			}
			// #2
			if(!InArray(parts[0], FUNCTIONS)) {
				ValidCommand = 2;
			}
			// #3
			if(parts.length > 3) {
				ValidCommand = 3;
			}
			for (int i = 0; i < parts.length; i++) {
				// #4
				if(!(parts[i].length() < 4)) {
					// #12
					if(CanParseInt(parts[i])) {
						int num = Integer.parseInt(parts[i]);
						if(num > 1000 || num < -1000)
							ValidCommand = 12;
					} else {
						ValidCommand = 4;
					}
				}
				// #6
				if(parts[i].length() == 0 || line.charAt(line.length() - 1) == ' ') {
					ValidCommand = 6;
				}

				// #1
				if(i != 0 && InArray(parts[i], FUNCTIONS)) {
					ValidCommand = 1;
				}
			}

		} else {
			ValidCommand = 5;
		}
		return ValidCommand;
	}
	
	public int getValue(Spell spell, String in) {
		if(CanParseInt(in)) {
			return ClampInt(Integer.parseInt(in));
		} else {
			switch (in) {
			case "DAT":return spell.Dat;
			case "ADV":return spell.Adv;
			case "ACT":return spell.Act;
			case "TAR":return spell.Tar;
			case "DIS":return spell.Dis;
			case "DIR":return spell.Dir;
			case "SIG":return spell.Sig;
			case "RDR":return spell.Rdr;
			}
		}
		// Should never be reached
		return 0;
	}

	public void PrintCompilerResponse(String line, int ErrorCode, int LineNumber) {
		if(ErrorCode >= ERRORS.length || ErrorCode < 0) {
			System.out.println("Un-recognized Compiler Error code");
			System.out.println("This result should never happen");
			System.out.println("Error Code:" + ErrorCode);
		} else if (ErrorCode != 0) {
			// Make error 1 into error 0 for array access
			// Account for the first 2 lines being unaccesible as code
			System.out.println("Compiler Error (" + ErrorCode + "):" + ERRORS[ErrorCode]);
			System.out.println("On Line " + (LineNumber + 3) + ": " + line);
			System.out.println("Unable to Compile");
		} else {
			if (DebugMode) {
				System.out.println(ERRORS[ErrorCode]);
				System.out.println("Compiled Successfully");
			}
		}
	}
	
	public void PrintSpellState(Spell spell, String LineFunction) {
		if (DebugMode) {
			engine.UpdateMap();
			System.out.println("\nMap Data:");
			System.out.println(engine.OutputData() + "\n");
		}
		System.out.println("+-----------------+");
		System.out.println("Line " + spell.pointer + " successfully executed a " + LineFunction + " call");
		System.out.println("Status: " + spell.state.stateName);
		System.out.println("DAT: " + spell.Dat + "|ADV: " + spell.Adv);
		System.out.println("ACT: " + spell.Act + "|TAR: " + spell.Tar);
		if(spell.state == SPELLSTATE.SUSPENDED)
			System.out.println("Sleep Timer: " + spell.sleepTimer);
		System.out.println("Current Position (" + spell.i + ", " + spell.j + ")");
		System.out.println("+-----------------+");
		System.out.println();
	}
	
	// Implement all functions
	public void Move(Spell spell, String value, String location) {
		int change = getValue(spell, value);
		switch (location) {
			case "DAT":spell.Dat = change;engine.UpdateSpell(spell);break;
			case "ADV":spell.Adv = change;engine.UpdateSpell(spell);break;
			case "ACT":spell.Act = change;engine.UpdateSpell(spell);break;
			case "TAR":spell.Tar = change;engine.UpdateSpell(spell);break;
		}
		PollSensor(spell);
	}
	
	public String getFunctionCalled(String line) {
		boolean ConditionalT = line.charAt(0) == '+';
		boolean ConditionalF = line.charAt(0) == '-';
		if(ConditionalT || ConditionalF) {
			line = line.substring(1);
		}
		String[] parts = PreProcessLine(line);
		return parts[0];
	}
	
	public String RemoveConditionals(String line) {
		boolean ConditionalT = line.charAt(0) == '+';
		boolean ConditionalF = line.charAt(0) == '-';
		if(ConditionalT || ConditionalF) {
			line = line.substring(1);
		}
		return line;
	}
	
	public void Sleep(Spell spell, String value) {
		spell.sleepTimer = getValue(spell, value);
		spell.sleepTimer = Math.abs(spell.sleepTimer);
		if(spell.sleepTimer != 0)
			spell.state = Spell.SPELLSTATE.SUSPENDED;
	}
	
	public void Jump(Spell spell, String value) {
		String[] commands = PreProcessSpell(spell.SpellCommands);
		int pointer = -1;
		for (int i = 0; i < commands.length; i++) {
			if(commands[i].equals("LBL " + value)) {
				pointer = i;
			}
		}
		// Fix the assumed change to pointer
		spell.pointer++;
		PrintSpellState(spell, getFunctionCalled(commands[spell.pointer - 1]));
		spell.pointer--;
		// Undo fix
		spell.pointer = pointer;
	}
	
	public void Run(Spell spell) {
		// TODO find a way to get closer to target
		spell.Dat = engine.RunCalculation(spell);
	}
	
	public void Add(Spell spell, String value) {
		spell.Dat = ClampInt(spell.Dat + getValue(spell, value));
	}
	
	public void Subtract(Spell spell, String value) {
		spell.Dat = ClampInt(spell.Dat - getValue(spell, value));
	}
	
	public void Multiply(Spell spell, String value) {
		spell.Dat = ClampInt(spell.Dat * getValue(spell, value));
	}
	
	public void Divide(Spell spell, String value) {
		if(getValue(spell, value) != 0) {
			spell.Dat = ClampInt(spell.Dat / getValue(spell, value));
		} else {
			// Activate the spell if some one tries to divide by 0 at run time
			spell.Act = 100;
			spell.state = SPELLSTATE.ARMED;
		}
	}
	
	public void Modulo(Spell spell, String value) {
		if(getValue(spell, value) != 0) {
			spell.Dat = ClampInt(spell.Dat % getValue(spell, value));
		} else {
			// Activate the spell if some one tries to divide by 0 at run time
			spell.Act = 100;
			spell.state = SPELLSTATE.ARMED;
		}
	}
	
	public void Teq(Spell spell, String value1, String value2) {
		spell.ConditionalOn = true;
		if(getValue(spell, value1) == getValue(spell, value2)) {
			spell.Dat = 100;
			spell.ConditionalResult = true;
		} else {
			spell.Dat = 0;
			spell.ConditionalResult = false;
		}
	}
	
	public void Tgt(Spell spell, String value1, String value2) {
		spell.ConditionalOn = true;
		if(getValue(spell, value1) > getValue(spell, value2)) {
			spell.Dat = 100;
			spell.ConditionalResult = true;
		} else {
			spell.Dat = 0;
			spell.ConditionalResult = false;
		}
	}
	
	public void Tlt(Spell spell, String value1, String value2) {
		spell.ConditionalOn = true;
		if(getValue(spell, value1) < getValue(spell, value2)) {
			spell.Dat = 100;
			spell.ConditionalResult = true;
		} else {
			spell.Dat = 0;
			spell.ConditionalResult = false;
		}
	}
	
	// Update all spell data in battle engine
	public void PollSensor(Spell spell) {
		int[] data = engine.DataBundleSensorInfo(spell);
		spell.Dis = data[0];
		spell.Dir = data[1];
		spell.Sig = data[2];
		spell.Rdr = data[3];
	}
}