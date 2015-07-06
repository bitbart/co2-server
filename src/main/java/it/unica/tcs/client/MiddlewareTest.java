package it.unica.tcs.client;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.Private;
import co2api.TST;

public class MiddlewareTest {
	
	private static final Integer LOG_LEVEL = 1;

	public static void main(String[] args) {
		
		test1();
	}

	private static void test1() {

		/* It executes correctly the contract !hello{x<10}.?great{x<20} */ 
		
		// FIRST USER
		String base = randString();
		String username = base + "@tester.it";
		String password = base;
		
		try {
			CO2ServerConnection.createUser(username, password); // creating the user
			printFinest(1, "new user created with username={ " + username + " } and password={ " + password + " }.");
		
		} catch (ContractException e1) {
			printError(1, "failed when creating user with username={ " + username + " } and password={ " + password + " }. " + e1.getMessage());
			return;
		} 
		
		CO2ServerConnection co2 = null;
		
		try {
			co2 = new CO2ServerConnection(username, password);
			
			printFine(1, "connection created with username={ " + username + " } and password={ " + password + " }.");
		} catch (ContractException e) {
			printError(1, "failed when creating connection with username={ " + username + " } and password={ " + password + " }. " + e.getMessage());
			return;
		}
		
		// SECOND USER
		String base2 = randString();
		String username2 = base2 + "@tester.it";
		String password2 = base2;
		
		try {
			CO2ServerConnection.createUser(username2, password2); // creating the user
			printFinest(1, "new user created with username={ " + username2 + " } and password={ " + password2 + " }.");
		
		} catch (ContractException e1) {
			printError(1, "failed when creating user with username={ " + username2 + " } and password={ " + password2 + " }. " + e1.getMessage());
			return;
		} 
		
		CO2ServerConnection co2bis = null;
		
		try {
			co2bis = new CO2ServerConnection(username2, password2);
			
			printFine(1, "connection created with username={ " + username2 + " } and password={ " + password2 + " }.");
		} catch (ContractException e) {
			printError(1, "failed when creating connection with username={ " + username2 + " } and password={ " + password2 + " }. " + e.getMessage());
			return;
		}
		
		
		// GET REPUTATION
		try {
			printFine(1, "the reputation of the user (1) with username={ " + username + " } is " + co2.getReputation() + ".");
		} catch (ContractException e) {
			printError(1, "failed asking for reputation of user={ " + username + " }. " + e.getMessage());
			return;
		}
		
		try {
			printFine(1, "the reputation of the user (2) with username={ " + username2 + " } is " + co2bis.getReputation() + ".");
		} catch (ContractException e) {
			printError(1, "failed asking for reputation of user={ " + username2 + " }. " + e.getMessage());
			return;
		}
		
		Private<TST> pA = null, pB = null;
		
		try {
			pA = new TST("!hello{x<10}.?great{x<20}").toPrivate(co2);
			printFinest(1, "created the private contract for user (1).");
		} catch (ContractException e) {
			printError(1, "can't create the private for user (1). " + e.getMessage());
			return;
		}
		
		try {
			pB = new TST("?hello{x<10}.!great{x<20}").toPrivate(co2bis);
			printFinest(1, "created the private contract for user (2).");
		} catch (ContractException e) {
			printError(1, "can't create the private for user (2). " + e.getMessage());
			return;
		}
	}
	
	private static void printFinest(Integer n, String msg) {
		
		if (LOG_LEVEL <= 1)
			System.out.println(preface(n) + "finest  | " + msg);
	}
	
	private static void printFine(Integer n, String msg) {
		
		if (LOG_LEVEL <= 2)
			System.out.println(preface(n) + "Fine    | " + msg);
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
		return sdf.format(resultdate) + " | TEST-" + n + " | ";
	}
	
	private static String randString() {
		
		return UUID.randomUUID().toString().replace("-", "").substring(0, 5);
	}
}
