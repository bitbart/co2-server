package it.unica.tcs;

import java.sql.SQLException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/user")
public class UserManagement {

	@POST
	@Path("/create")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public ResponsePacket createUser(QueryPacket postData) {
		
		String username = postData.getUsername();
		String password = postData.getPassword();
		
		if (!isValidEmailAddress(username)) {
			
			Log.message().fine("A user registration has been rejected because the user " + Log.format(username) + " is not considered a valid email address.");
			return new ResponsePacket(-1, "Username was not recognized as a valid email address.");
		}
		
		DatabaseInterface db = MainApplication.getDBConnection();
		
		try {
			db.insertUser(username.split("@")[0], "", username, password);
			
		} catch (SQLException e) {
			
			Log.message().warning("Can't add a new user to the DB. SQL says: " + e.getMessage());
			return new ResponsePacket(-1, "The user registration is failed: maybe the chosen username already exists.");
		}
		
		Log.message().info("Added a new user to the database, with EMAIL=" + Log.format(username) + ".");
		return new ResponsePacket(1, "User successfully created");
	}
	
	public static boolean isValidEmailAddress(String email) {
	   boolean result = true;
	   try {
	      InternetAddress emailAddr = new InternetAddress(email);
	      emailAddr.validate();
	   } catch (AddressException ex) {
	      result = false;
	   }
	   return result;
	}
}
