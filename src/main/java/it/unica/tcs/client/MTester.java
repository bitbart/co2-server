package it.unica.tcs.client;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.Private;
import co2api.TST;

public class MTester {

	private static final Integer LOG_LEVEL = 1;

	public static void main(String[] args) {

		welcome();
		
		pre(1); test1(); post(1);
	}

	private static void test1() {

		/* It executes correctly the contract !hello{x<10}.?great{x<20} */
		Thread pA = new Thread() {
			public void run() {
				
				String p = "A";

				String base = randString();
				String username = base + "@tester.it";
				String password = base;

				try {
					CO2ServerConnection.createUser(username, password); // creating user
					printFinest(1, p, "new user created with username={ " + username + " } and password={ " + password + " }.");

				} catch (ContractException e1) {
					printError(1, p, "failed when creating user with username={ " + username + " } and password={ " + password + " }. " + e1.getMessage());
					return;
				}

				CO2ServerConnection co2 = null;

				try {
					co2 = new CO2ServerConnection(username, password);

					printFine(1, p, "connection created with username={ " + username + " } and password={ " + password + " }.");
				} catch (ContractException e) {
					printError(1, p, "failed when creating connection with username={ " + username + " } and password={ " + password + " }. " + e.getMessage());
					return;
				}

				try {
					printFine(1, p, "the reputation of the user "+p +" with username={ " + username + " } is " + co2.getReputation() + ".");
				} catch (ContractException e) {
					printError(1, p, "failed asking for reputation of user={ " + username + " }. " + e.getMessage());
					return;
				}

				Private<TST> pA;
				try {
					pA = new TST("!hello{x<10}.?great{x<20}").toPrivate(co2);
					printFinest(1, p, "created the private contract for user "+p +".");
				} catch (ContractException e) {
					printError(1, p, "can't create the private for user "+p +". " + e.getMessage());
					return;
				}
			}
		};

		Thread pB = new Thread() {
			public void run() {
				
				String q = "B";
				
				String base2 = randString();
				String username2 = base2 + "@tester.it";
				String password2 = base2;

				try {
					CO2ServerConnection.createUser(username2, password2); // creating
																			// the
																			// user
					printFinest(1, q, "new user created with username={ " + username2 + " } and password={ " + password2 + " }.");

				} catch (ContractException e1) {
					printError(1, q, "failed when creating user with username={ " + username2 + " } and password={ " + password2 + " }. " + e1.getMessage());
					return;
				}

				CO2ServerConnection co2bis = null;

				try {
					co2bis = new CO2ServerConnection(username2, password2);

					printFine(1, q, "connection created with username={ " + username2 + " } and password={ " + password2 + " }.");
				} catch (ContractException e) {
					printError(1, q, "failed when creating connection with username={ " + username2 + " } and password={ " + password2 + " }. " + e.getMessage());
					return;
				}

				// GET REPUTATION

				try {
					printFine(1, q, "the reputation of the user "+q +" with username={ " + username2 + " } is " + co2bis.getReputation() + ".");
				} catch (ContractException e) {
					printError(1, q, "failed asking for reputation of user={ " + username2 + " }. " + e.getMessage());
					return;
				}
				Private<TST> pB = null;

				try {
					pB = new TST("?hello{x<10}.!great{x<20}").toPrivate(co2bis);
					printFinest(1, q, "created the private contract for user "+q +".");
				} catch (ContractException e) {
					printError(1, q, "can't create the private for user "+q +". " + e.getMessage());
					return;
				}
			}
		};
		
		pA.start();
		pB.start();
		
		try {
			pA.join();
			pB.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void printFinest(Integer n, String p, String msg) {

		if (LOG_LEVEL <= 1)
			System.out.println(preface(n,p) + "finest  | " + msg);
	}

	private static void printFine(Integer n, String p, String msg) {

		if (LOG_LEVEL <= 2)
			System.out.println(preface(n,p) + "fine    | " + msg);
	}

	private static void printInfo(Integer n, String p, String msg) {

		if (LOG_LEVEL <= 3)
			System.out.println(preface(n,p) + "INFO    | " + msg);
	}

	private static void printError(Integer n, String p, String msg) {
		System.out.println(preface(n,p) + "*ERROR* | " + msg);
	}

	private static void printFinest(Integer n, String msg) {

		if (LOG_LEVEL <= 1)
			System.out.println(preface(n) + "finest  | " + msg);
	}

	private static void printFine(Integer n, String msg) {

		if (LOG_LEVEL <= 2)
			System.out.println(preface(n) + "fine    | " + msg);
	}

	private static void printInfo(Integer n, String msg) {

		if (LOG_LEVEL <= 3)
			System.out.println(preface(n) + "INFO    | " + msg);
	}

	private static void printError(Integer n, String msg) {
		System.out.println(preface(n) + "*ERROR* | " + msg);
	}

	private static String preface(Integer n) {

		long yourmilliseconds = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		Date resultdate = new Date(yourmilliseconds);
		return sdf.format(resultdate) + " | TEST-" + n + "   | ";
	}
	
	private static String preface(Integer n, String p) {

		long yourmilliseconds = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		Date resultdate = new Date(yourmilliseconds);
		return sdf.format(resultdate) + " | TEST-" + n + "-"+p +" | ";
	}

	private static String randString() {

		return UUID.randomUUID().toString().replace("-", "").substring(0, 5);
	}
	
	private static void pre(Integer n) {

		System.out.println("                                       ╔════════════════════╗");
		System.out.println("═══════════════════════════════════════╣  test " + n + " started ♫  ╠════════════════════════════════════════");
		System.out.println("                                       ╚════════════════════╝");
	}
	
	private static void post(Integer n) {

		System.out.println("                                       ╔════════════════════╗");
		System.out.println("═══════════════════════════════════════╣ test " + n + " completed ☼ ╠════════════════════════════════════════");
		System.out.println("                                       ╚════════════════════╝\n");
	}

	private static void welcome() {
		
		System.out.println("     ___                   ___          ___                   ___          ___     \n" +
                           "    /  /\\     ___         /  /\\        /  /\\     ___         /  /\\        /  /\\\n" +    
                           "   /  /::|   /__/\\       /  /::\\      /  /::\\   /__/\\       /  /::\\      /  /::\\\n" +  
                           "  /  /:|:|   \\  \\:\\     /  /:/\\:\\    /__/:/\\:\\  \\  \\:\\     /  /:/\\:\\    /  /:/\\:\\\n" + 
                           " /  /:/|:|__  \\__\\:\\   /  /::\\ \\:\\  _\\_ \\:\\ \\:\\  \\__\\:\\   /  /::\\ \\:\\  /  /::\\ \\:\\\n" +
                           "/__/:/_|::::\\ /  /::\\ /__/:/\\:\\ \\:\\/__/\\ \\:\\ \\:\\ /  /::\\ /__/:/\\:\\ \\:\\/__/:/\\:\\_\\:\\\n"+
                           "\\__\\/  /~~/://  /:/\\:\\\\  \\:\\ \\:\\_\\/\\  \\:\\ \\:\\_\\//  /:/\\:\\\\  \\:\\ \\:\\_\\/\\__\\/~|::\\/:/\n"+
                           "      /  /://  /:/__\\/ \\  \\:\\ \\:\\   \\  \\:\\_\\:\\ /  /:/__\\/ \\  \\:\\ \\:\\     |  |:|::/\n" +
                           "     /  /://__/:/       \\  \\:\\_\\/    \\  \\:\\/://__/:/       \\  \\:\\_\\/     |  |:|\\/\n"  +
                           "    /__/:/ \\__\\/         \\  \\:\\       \\  \\::/ \\__\\/         \\  \\:\\       |__|:|~\n"   +
						   "    \\__\\/                 \\__\\/        \\__\\/                 \\__\\/        \\__\\|  ");
	}
}
