package it.unica.tcs;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User {
	
	public static final Integer REP_TELL = -1;
	public static final Integer REP_SUCCESS = 3;
	public static final Integer REP_CULPABLE = -10;

	private DatabaseInterface db;
	private Integer userID;
	private String firstName;
	private String lastName;
	private String username;
	private String password;
	private Integer credit;
	private Integer reputation;

	private User() {

		this.db = MainApplication.getDBConnection();
	}
	
	public static User build(String username) throws SQLException {

		User tmp = new User();
		tmp.loadFromUsername(username);
		
		return tmp;
	}
	
	public static User build(Integer userID) throws SQLException {

		User tmp = new User();
		tmp.loadFromUserID(userID);
		
		return tmp;
	}
	
	private void loadFromUserID(Integer userID) throws SQLException {
		
			String query = "SELECT * FROM user WHERE user_id = '" + userID + "';";
			ResultSet result;

			result = db.select(query);
			result.next();

			this.userID = result.getInt("user_id");
			this.firstName = result.getString("first_name");
			this.lastName = result.getString("last_name");
			this.username = result.getString("email");
			this.password = result.getString("password");
			this.credit = result.getInt("credit");
			this.reputation = result.getInt("reputation");
	}

	private void loadFromUsername(String username) throws SQLException {
		
		String query = "SELECT * FROM user WHERE email = '" + username + "';";
			ResultSet result;

			result = db.select(query);
			result.next();

			this.userID = result.getInt("user_id");
			this.firstName = result.getString("first_name");
			this.lastName = result.getString("last_name");
			this.username = result.getString("email");
			this.password = result.getString("password");
			this.credit = result.getInt("credit");
			this.reputation = result.getInt("reputation");
	}
	
	public void store() throws SQLException {		
		
		db.updateUser(userID, firstName, lastName, username, password, reputation + "", credit + "");
	}
	
	public Integer getUserID() {

		return this.userID;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Integer getCredit() {
		return credit;
	}

	public void setCredit(Integer credit) {
		this.credit = credit;
	}

	public Integer getReputation() {
		return reputation;
	}

	public void setReputation(Integer reputation) {
		this.reputation = reputation;
	}
	
	public void reward() {
		
		this.reputation += REP_SUCCESS;
	}
	
	public void rewardAndStore() throws SQLException {
		
		reward();
		store();
	}
	
	public void penalize() {
		
		this.reputation += REP_CULPABLE;
	}
	
	public void penalizeAndStore() throws SQLException {
		
		penalize();
		store();
	}
	
	public void decrementRep() {
		
		this.reputation += REP_TELL;
	}
	
	public void decrementRepAndStore() throws SQLException {
		
		decrementRep();
		store();
	}
}
