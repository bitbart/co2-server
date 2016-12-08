package it.unica.tcs.client;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;

import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.ContractViolationException;
import co2api.Message;
import co2api.Private;
import co2api.Public;
import co2api.Session;
import co2api.TST;
import co2api.TimeExpiredException;

public class MTester {

	private static final Integer LOG_LEVEL = 1; 

	public static void main(String[] args) {   

		welcome();
		
		pre(1); test1(); post(1);
	} 

	private static void test1() {
		
		final boolean aEnabled = true;
		final boolean bEnabled = true; 
		
		

		// description
		System.out.println("\\\\ ♣ It executes the contract !hello{x<10}.?good{x<20} (without performing 'good', at the moment) ♠\n"); 
		
		Thread pA = new Thread() {
			@Override
            public void run() {
				
				if (!aEnabled)
					return;
				
				String p = "A";

				String base = randString(10);
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
					pA = new TST("!hello{x<10}.?dong{x<20}").toPrivate(co2); 
					printFinest(1, p, "created the private contract for user "+p +".");
				} catch (ContractException e) {
					printError(1, p, "can't create the private for user "+p +". " + e.getMessage());
					return;
				}
				
				Public<TST> puA;
				try {
					puA = pA.tell();
					printInfo(1, p, "the contract of "+p +" has been published online.");
				} catch (ContractException e) {
					printError(1, p, "can't advertise the contract of "+p +" with the TELL. " + e.getMessage());
					return;
				}
				
				/* -- RETRACT (if needed) --
				try {
					
					puA.retract();
					printInfo(1, p, "the contract of "+ p + " has been retracted.");
					
					if(2>1)
					return;
				} catch (ContractException e) {
					
					printError(1, p, "cannot perfom the retract: " + e.getMessage());
					return;
				}*/

				
				Session<TST> sA = null;
				try {
					
					//puA.waitForSession();
					
					if (puA.isFused()) {
						try {
							sA = puA.getSession(); 
							printInfo(1, p, "the contract of "+p +" has been fused, the session is established.");
						}
						catch (ContractException e) {
							printError(1, p, "the contract seems to be fused, but the session can't be getted." + e.getMessage());
						}
					}
					else {
						sA = puA.waitForSession(30000);
						printInfo(1, p, "the contract of "+p +" has been fused, the session is established.");
					}
						
				} catch (ContractException e) {
					printError(1, p, "can't advertise the contract of "+p +" with the TELL. " + e.getMessage());
					return;
				} catch (TimeExpiredException e) {
					printError(1,p, "can't get a session before the deadline of 30secs. " + e.getMessage());
					return;
				}
				
				try {
					Thread.sleep(9000);  // wait for the deadline (one second before)
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				
				try {
					
					if (!sA.amIOnDuty()) {
						
						printError(1,p, "I'm not on duty.");
					}
					else
						printInfo(1,p, "I'm correctly on duty");
					
					if (sA == null)
						throw new ContractException("The session object is null.");
						
					sA.send("hello"); // performs the action
					printInfo(1,p, "action 'hello' correctly performed!");
					
					if (!sA.amIOnDuty()) {
						
						printInfo(1,p, "Now I'm not on duty too.");
					}
					else
						printError(1,p, "Now I can't be on duty, but I am.");
					
				} catch (ContractException e) {
					printError(1,p, "cannot perform the expected action 'hello'. " + e.getMessage());
					return;
				}
			}
		};

		Thread pB = new Thread() {
			@Override
            public void run() {
				
				if (!bEnabled)
					return;
				
				String q = "B";
				
				String base2 = randString(10);
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
					pB = new TST("?hello{x<10}.!dong{x<20}").toPrivate(co2bis);
					printFinest(1, q, "created the private contract for user "+q +".");
				} catch (ContractException e) {
					printError(1, q, "can't create the private for user "+q +". " + e.getMessage());
					return;
				}
				
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				
				Public<TST> puB;
				try {
					puB = pB.tell(30000);
					printInfo(1, q, "the contract of "+q +" has been published online.");
				} catch (ContractException e) {
					printError(1, q, "can't advertise the contract of "+q +" with the TELL. " + e.getMessage());
					return;
				}
				
				Session<TST> sB = null;
				try {
					if (puB.isFused()) {
						
						try {
							sB = puB.getSession(); 
							printInfo(1, q, "the contract of "+q +" has been fused, the session is established.");
						}
						catch (ContractException e) {
							printError(1, q, "the contract seems to be fused, but the session can't be getted." + e.getMessage());
						}
					}
					else {
						sB = puB.waitForSession(30000);
						printInfo(1, q, "the contract of "+q +" has been fused, the session is established.");
					}
						
				} catch (ContractException e) {
					printError(1, q, "can't advertise the contract of "+q +" with the TELL. " + e.getMessage());
					return;
				} catch (TimeExpiredException e) {
					printError(1,q, "can't get a session before the deadline of 30secs. " + e.getMessage());
					return; 
				}
				
				try {
					if (sB == null)
						throw new ContractException("The session object is null.");
						
					Message m = sB.waitForReceive(10500); // waits
					
					switch (m.getLabel()) {
					case "hello": printInfo(1,q, "action 'hello' correctly received by the counterpart"); break;
					default: printError(1,q, "the action label received is not the expected 'hello' (is "+ m.getLabel() + ")!"); return;
					}
					
				} catch (ContractException e) {
					printError(1,q, "cannot receive the expected action 'hello'. " + e.getMessage());
					return;
				} catch (TimeExpiredException e) {
					printError(1,q, "cannot receive the expected action 'hello'. " + e.getMessage());
					return;
				} catch (ContractViolationException e) {
					printError(1,q, "cannot receive the expected action 'hello'. " + e.getMessage());
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

	@SuppressWarnings("unused")
    private static void printFinest(Integer n, String msg) {

		if (LOG_LEVEL <= 1)
			System.out.println(preface(n) + "finest  | " + msg);
	}

	@SuppressWarnings("unused")
    private static void printFine(Integer n, String msg) {

		if (LOG_LEVEL <= 2)
			System.out.println(preface(n) + "fine    | " + msg);
	}

	@SuppressWarnings("unused")
    private static void printInfo(Integer n, String msg) {

		if (LOG_LEVEL <= 3)
			System.out.println(preface(n) + "INFO    | " + msg);
	}

	@SuppressWarnings("unused")
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

	private static String randString(Integer len) {

		return RandomStringUtils.randomAlphabetic(len).toLowerCase();
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
