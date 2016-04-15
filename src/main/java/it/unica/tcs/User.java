package it.unica.tcs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unica.tcs.database.DatabaseInterface;

public class User {
	
    private static final Logger logger = LoggerFactory.getLogger(User.class);
    
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
	private Double tv;
	private Double[] ftv;

	private User() {

		this.db = DatabaseInterface.getInstance();
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

        String query = "SELECT * FROM user WHERE user_id = ?";

        try (
                Connection connection = db.getDatasource().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query);
                ) {
            stmt.setInt(1, userID);
            
            ResultSet result = stmt.executeQuery();
            result.next();

            this.userID = result.getInt("user_id");
            this.firstName = result.getString("first_name");
            this.lastName = result.getString("last_name");
            this.username = result.getString("email");
            this.password = result.getString("password");
            this.credit = result.getInt("credit");
            this.reputation = result.getInt("reputation");
            this.tv = result.getDouble("tv");

            this.ftv = db.getFTV(userID + "");
            result.close();
        } catch (SQLException e) {

            logger.error("SQLException in getFTV: " + e.getMessage());
            throw e;
        }
    }

    private void loadFromUsername(String username) throws SQLException {

        String query = "SELECT * FROM user WHERE email = ?";

        try (
                Connection connection = db.getDatasource().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query);
                ) {
            stmt.setString(1, username);
            
            ResultSet result = stmt.executeQuery();
            result.next();

            this.userID = result.getInt("user_id");
            this.firstName = result.getString("first_name");
            this.lastName = result.getString("last_name");
            this.username = result.getString("email");
            this.password = result.getString("password");
            this.credit = result.getInt("credit");
            this.reputation = result.getInt("reputation");
            this.tv = result.getDouble("tv");
            this.ftv = db.getFTV(userID + "");

            result.close();
        }
    }
	
	public void store() throws SQLException {		
		
		db.updateUser(userID, firstName, lastName, username, password, reputation, credit, tv, ftv);
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

	public Integer getLastFeedback() {
		return reputation;
	}

	public void updateReputation() {
		
		Integer MAX_HI 	= 4;				// NUMERO MASSIMO DI INTERVALLI DA MEMORIZZARE
		Integer K 		= 2;				// DIMENSIONE DELL'AGGREGAZIONE
		
		Double currentHistory = 0.;
		Double weightsSum = 0.;
	
		Double Ri, Hi, Di;
		Double alpha = 0.53, beta = 0.49, gamma = .6, ro = 0.8;
		
		
		
		for (int k = 1; k <= this.ftv.length-1; k++) {
			weightsSum += Math.pow(ro, k-1);
		}
		for (int k = 1; k <= this.ftv.length-1; k++) {
			currentHistory += this.ftv[k] * (Math.pow(ro, k-1)/weightsSum);
 		}
		
		Hi 	= currentHistory;
		Ri = new Double(this.reputation);
		
		
		
		
		Double[] tmp = new Double[MAX_HI]; // Temporary array
		tmp[0] = Ri;
		
		int bound = 0;
		if (this.ftv.length == MAX_HI)
			bound = MAX_HI;
		else
			bound = this.ftv.length+1;
		
		
		for (int j=1; j<bound; j++) {
			
			tmp[j] = (this.ftv[j]*(Math.pow(K, j)-1)+this.ftv[j-1]) / (Math.pow(K, j)) ;
		}
		
		this.ftv = tmp;
		
		Di = Ri - Hi;

		// Changes gamma 
		if (Di < 0) 	
			gamma = 0.4;
			
		this.tv = alpha*Ri + beta*Hi + gamma*Di;
	}
	
	public Double getTV() {
		return this.tv;
	}
	
	public Double[] getFTV() {
		return this.ftv;
	}
	
	public void reward() {
		
		this.reputation = REP_SUCCESS;
	}
	
	public void rewardAndStore() throws SQLException {
		
		logger.info("Fatto RAS 1");
		reward();
		logger.info("Fatto RAS 2");
		updateReputation();
		logger.info("Fatto RAS 3");
		store();
		logger.info("Fatto RAS 4");
	}
	
	public void penalize() {
		
		this.reputation = REP_CULPABLE;
	}
	
	public void penalizeAndStore() throws SQLException {
		
		penalize();
		updateReputation();
		store();
	}
	
	public void decrementRep() {
		
		this.reputation = REP_TELL;
	}
	
	public void decrementRepAndStore() throws SQLException {
		
		decrementRep();
		updateReputation();
		store();
	}
}
