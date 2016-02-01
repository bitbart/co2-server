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
		
		DatabaseInterface db = DatabaseInterface.getInstance();
		
		try {
			db.insertUser(username.split("@")[0], "", username, password);
			
		} catch (SQLException e) {
			
			Log.message().warning("Can't add a new user to the DB. SQL says: " + e.getMessage());
			return new ResponsePacket(-1, "The user registration is failed: maybe the chosen username already exists.");
		}
		
		Log.message().info("Added a new user to the database, with EMAIL=" + Log.format(username) + ".");
		return new ResponsePacket(1, "User successfully created");
	}
	
	@POST
	@Path("/getReputation")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public ResponsePacket getReputation(QueryPacket postData) {
		
		String username = postData.getUsername();
		String password = postData.getPassword();
		
		if (Tools.isNotValid(username, Tools.USERNAME_REGEX) || Tools.isNotValid(password, Tools.PASSWORD_REGEX)) {// TODO: to be completed
            
            Log.message().warning("The getReputation() was called with wrong parameters.");
            return new ResponsePacket(-1, "The GET_REPUTATION api was called with wrong parameters.");
        }

        // 1) Connecting to db

        try {
            // 2) Checking for valid auth data
            if (!DatabaseInterface.getInstance().authenticate(username, password)) {
                Log.message().warning(
                        "Authentication error. Cannot accept USERNAME=" + Log.format(username) + " and hashed PASSWORD="
                                + Log.format(Tools.hash256(password)) + "");
    
                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }
        }catch (SQLException e) {

            Log.message().severe("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }
        
        User currentUser;
		try {
			currentUser = User.build(username);
		} catch (SQLException e) {
			
			Log.message().severe("Thrown SQL exception while trying to get the reputation of an user: " + e.getMessage());
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
		}
		
		Log.message().finest("User with EMAIL=" + Log.format(username) + " has asked for his rep. Result is: " + currentUser.getTV());
		
		return new ResponsePacket(1, currentUser.getTV() + "");
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
	
	@POST
	@Path("/changePassword")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public ResponsePacket changePassword(QueryPacket postData) {
		
		String username = postData.getUsername();
		String oldPassword = postData.getPassword();
		String newPassword = postData.getNewPassword();
		
		
		if (Tools.isNotValid(username, Tools.USERNAME_REGEX) || Tools.isNotValid(oldPassword, Tools.PASSWORD_REGEX)
				|| Tools.isNotValid(newPassword, Tools.PASSWORD_REGEX)) {// TODO: to be completed
            
            Log.message().warning("The changePassword() was called with wrong parameters.");
            return new ResponsePacket(-1, "The CHANGE_PASSWORD api was called with wrong parameters.");
        }

        // 1) Connecting to db
        DatabaseInterface db = DatabaseInterface.getInstance();

        try {
            // 2) Checking for valid auth data
            if (!DatabaseInterface.getInstance().authenticate(username, oldPassword)) {
                Log.message().warning(
                        "Authentication error. Cannot accept USERNAME=" + Log.format(username) + " and hashed PASSWORD="
                                + Log.format(Tools.hash256(oldPassword)) + "");
    
                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }
        }catch (SQLException e) {

            Log.message().severe("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }
        
        // 3) update the password
		try {
			 db.updatePassword(username, newPassword);
		} catch (SQLException e) {
			
			Log.message().severe("Thrown SQL exception while trying update the password of an user: " + e.getMessage());
            return new ResponsePacket(-1, Messages.DB_INSERT_FAILED);
		}
		
		Log.message().finest("Updated password for user with EMAIL=" + Log.format(username));
		
		return new ResponsePacket(1, "Password update sucessfully");
	}
}
