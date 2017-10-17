import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
 
/**
 *
 * @author sqlitetutorial.net
 */
public class SQLiteJDBCDriverConnection {
	
     /**
     * Connect to a sample database
     * @return 
     */
	
	ArrayList<Integer> distinctUsers = new ArrayList<Integer>();
	ArrayList<User> users = new ArrayList<User>();
	User temp;
	
    public static Connection connect() {
    	
        Connection conn = null;
        
        try {
            // db parameters
            String url = "jdbc:sqlite:src/trainingset.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);
            
            System.out.println("Connection to SQLite has been established.");
            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } 
        
		return conn;
        
    }
    
    public void query(Integer user, Connection conn) {
    	
    	String myQuery = "SELECT itemID, rating FROM trainingset WHERE userID = " + user;
    	
    	 try (
    			 
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(myQuery)){
    		 
    		 temp = new User(user);
                
                // loop through the result set
                while (rs.next()) {
                	
                	temp.getRatings().put(rs.getInt(1), rs.getInt(2));
                	
                }          
            
            users.add(temp);
                
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
    	 
    }
    
    public void selectDistinct() {
    		
    	String myQuery = "SELECT DISTINCT userID FROM trainingset" ;
    	
    	 try (
    			 
    		 Connection conn = this.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(myQuery)){
                
                // loop through the result set
                while (rs.next()) {
                        	
                	distinctUsers.add(rs.getInt(1));
                	
                }
                
           	 queryAllUsers(conn);
                
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
    	 

    	 
    }
    
    public void queryAllUsers(Connection conn) {
		
    	for(int i = 0; i < distinctUsers.size(); i++) {
    		
    		query(distinctUsers.get(i), conn);
    		
    	}
    	
    	System.out.println(distinctUsers.toString());
    	 
    }
    
    public float averageRatings(User u1) {
    	
    	float u1Avg = 0;
		
    	for (Map.Entry<Integer, Integer> entry : u1.getRatings().entrySet()) {
    		
    		u1Avg = u1Avg + entry.getValue();
    		
    	}
    	
    	u1Avg = u1Avg / u1.getRatings().size();
    	
    	return u1Avg;
    	
    }
    
    public ArrayList<Integer> getSame(User u1, User u2) {
    	
    	ArrayList<Integer> sameRatings = new ArrayList<Integer>();
    	
    	for (Map.Entry<Integer, Integer> entry : u1.getRatings().entrySet()) {
    		
    		if(u2.getRatings().containsKey(entry.getKey())) {
    			
    			sameRatings.add(entry.getKey());
    			
    		}
    	}
    	
    	return sameRatings;

    }
    
    public float sumMeanDifference(User u1, User u2, float u1Avg, float u2Avg, ArrayList<Integer> sameRatings) {
    	
    	float temp3 = 0;
    	
    	for(int i = 0; i < sameRatings.size(); i++) {
    		
    		float temp1 = u1.getRatings().get(sameRatings.get(i)) - u1Avg;
    		
    		float temp2 = u2.getRatings().get(sameRatings.get(i)) - u2Avg;
    		
    		temp3 = temp3 + (temp1 * temp2);
    		
    	}
    	
    	return temp3;
    	
    }
    
    public float squareRoot(User u1, User u2, float u1Avg, float u2Avg, ArrayList<Integer> sameRatings) {
    	
    	float temp1 = 0;
    	
    	float temp2 = 0;
    	
    	for(int i = 0; i < sameRatings.size(); i++) {
    		
    		temp1 = temp1 + ((u1.getRatings().get(sameRatings.get(i)) - u1Avg) * (u1.getRatings().get(sameRatings.get(i)) - u1Avg));

    	}
    	
    	Math.sqrt(temp1);
    	
    	for(int i = 0; i < sameRatings.size(); i++) {
    		
    		temp2 = temp2 + ((u2.getRatings().get(sameRatings.get(i)) - u2Avg) * (u2.getRatings().get(sameRatings.get(i)) - u2Avg));

    	}
    	
    	Math.sqrt(temp2);
    	
    	return temp1 * temp2;
    	
    }
    
    public float similarityCoefficient(User u1, User u2) {
    	
    	float u1Avg = averageRatings(u1);
    	float u2Avg = averageRatings(u2);
    	
    	ArrayList<Integer> sameRatings = getSame(u1, u2);
    	
    	float similarityValue = (sumMeanDifference(u1, u2, u1Avg, u2Avg, sameRatings) / (squareRoot(u1, u2, u1Avg, u2Avg, sameRatings)));
    	
    	return similarityValue;
    	
    	 
    }

    
    public static void main(String[] args) {

    	SQLiteJDBCDriverConnection myJDBC = new SQLiteJDBCDriverConnection();

    	myJDBC.selectDistinct();
    	
    }
    
}