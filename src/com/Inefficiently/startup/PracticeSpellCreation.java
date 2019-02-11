package com.Inefficiently.startup;

import java.util.Scanner;

import com.Inefficiently.engine.BattleEngine;
import com.Inefficiently.engine.Spell;
import com.Inefficiently.startup.Execute.CLIENTTYPE;
import com.Inefficiently.startup.InputHandler.NUMBERSET;

// THis is meant to practice creating custom spells
public class PracticeSpellCreation {
	public static void main(String[] args) {
		System.out.println("This is a program that will test custom spells written in the Runic language");
		System.out.println("This program will provide a limited test of the runic spell engine with more functionality to be added later");
		InputHandler scan = new InputHandler(new Scanner(System.in));
		BattleEngine engine = new BattleEngine(true);
		engine.UpdateSpellDirectoryFile();
		String SpellCSV = engine.GetSpellOptionsCSV();
		int Directory = SpellMenu(scan, SpellCSV);
		engine.AddSpell(new Spell(engine.SpellEngine, CLIENTTYPE.HOST, engine.getPayloadType(Directory), engine.getSensorType(Directory), engine.GetSpellCommands(Directory), engine.Players[0].Iindex, engine.Players[0].Jindex, GetTarget(scan), GetD(scan), true));
		engine.RunSpellCycles();
		engine.UpdateMap();
		System.out.println("Final Result:");
		System.out.println(engine.OutputData());
	}
	
	public static int SpellMenu(InputHandler scan, String SpellCSV) {
		String[] Spells = SpellCSV.split(",");
		String prompt = "What spell do you want to test:\n";
		for (int i = 0; i < Spells.length; i++) {
			prompt += (i+1) + "." + Spells[i] + "\n";
		}
		prompt += "Test:";
		return scan.ScannerInt(prompt, NUMBERSET.NATURAL, Spells.length) - 1;
	}
	public static int GetD(InputHandler scan) {
		return scan.ScannerInt("What inital direction do you want your spell on\n/////\n1 2 3\n4 X 5\n6 7 8\n\\\\\\\\\\\nDirection:", NUMBERSET.NATURAL, 8);
	}
	public static int GetTarget(InputHandler scan) {
		int[] targets = {10, 11};
		int i = scan.ScannerInt("Who do you want to target?\n1.Host\n2.Player\nTarget:", NUMBERSET.NATURAL) - 1;
		return targets[i];
	}
}
