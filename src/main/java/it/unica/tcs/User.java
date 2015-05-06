package it.unica.tcs;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User {

	private DatabaseInterface db;
	private Integer userID;
	private String firstName;
	private String lastName;
	private String username;
	private String password;
	private Integer credit;
	private Integer reputation;
    private boolean initialized;

	public User() {

		this.db = MainApplication.getDBConnection();
		this.initialized = false;
	}

	public User loadFromUsername(String username) throws SQLException {

		String query = "SELECT * FROM contract WHERE email = '" + userID + "';";
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

		this.initialized = true;

		return this;
	}

	public boolean isInitialized() {

		return this.initialized;
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
}
