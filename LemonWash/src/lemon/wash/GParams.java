package lemon.wash;

import java.util.HashMap;

public class GParams {

	public static String phone   = "";
	public static String address = "";
	public static String jAddr   = "";
	public static String[] myCar   = new String[5];
	public static String[] myCarBrend = new String[5];
	public static String[] myCarType  = new String[5];
	public static String[] myCarIdent = new String[5];
	public static int myCarIndex = -1;
	public static String newCarBrend = "";
	public static String newCarType  = "";
	public static String newCarIdent = "";
	
	public static HashMap<String, CharSequence[]> brend2type = new HashMap<String, CharSequence[]>();
	
	static {
		myCarBrend[0] = "Renault";
		myCarType[0] = "Clio";
		myCarIdent[0] = "AE647TH";
		myCar[0] = myCarBrend[0] + " " + myCarType[0] + " - " + myCarIdent[0];
				
		myCarBrend[1] = "Peugeot";
		myCarType[1] = "3008";
		myCarIdent[1] = "FK384KS";
		myCar[1] = myCarBrend[1] + " " + myCarType[1] + " - " + myCarIdent[1];
		
		myCarBrend[2] = "Audi";
		myCarType[2] = "A3";
		myCarIdent[2] = "XZ459ST";
		myCar[2] = myCarBrend[2] + " " + myCarType[2] + " - " + myCarIdent[2];
		
		brend2type.put("Audi", new CharSequence[] {"A3", "A4", "A6", "TT"});
		brend2type.put("Citroën", new CharSequence[] {"C1", "C3", "C5", "C8"});
		brend2type.put("Peugeot", new CharSequence[] {"206", "207", "307", "406", "3008"});
		brend2type.put("", new CharSequence[] {""});
	}
	
	public static final String CMD_WASH  = "command_wash";
	public static final String CMD_MYCAR = "choose_vehicle";
	public static final String CMD_NOTE  = "notation";
	public static final String CMD_CHECK_NOTE  = "check_notation";
}
