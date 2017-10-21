
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jacob & Damyan
 */
public class SQLiteJDBCDriverConnection {

    ArrayList<Integer> distinctUsers = new ArrayList<Integer>();
    HashMap<Integer, User> users = new HashMap<Integer, User>();
    User temp;

    public static Connection connect() {
        Connection conn = null;
        
        try {
            String url = "jdbc:sqlite:C://sqlite/trainingsetdup.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);

            System.out.println("Connection to SQLite has been established.");
        } catch (Exception  e) {
            System.out.println(e.getMessage());
        }

        return conn;
    }

    public void query(Integer user, Connection conn) {
        String myQuery = "SELECT itemID, rating FROM trainingset WHERE userID = " + user;

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(myQuery)) {

            temp = new User(user);

            while (rs.next()) {
                temp.getRatings().put(rs.getInt(1), rs.getInt(2));
            }

            users.put(user, temp);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    public void selectDistinct(Connection conn) {
        String myQuery = "SELECT DISTINCT userID FROM trainingset";

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(myQuery)) {

            while (rs.next()) {
                distinctUsers.add(rs.getInt(1));
            }

            queryAllUsers(conn);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    public void queryAllUsers(Connection conn) {
        for (int i = 0; i < distinctUsers.size(); i++) {
            query(distinctUsers.get(i), conn);
        }

        System.out.println(distinctUsers.size());
    }

    public ArrayList<Integer> getSame(User u1, User u2) {
        ArrayList<Integer> sameRatings = new ArrayList<Integer>();

        for (Map.Entry<Integer, Integer> entry : u1.getRatings().entrySet()) {
            if (u2.getRatings().containsKey(entry.getKey())) {
                sameRatings.add(entry.getKey());
            }
        }

        return sameRatings;
    }

    public float sumMeanDifference(User u1, User u2, float u1Avg, float u2Avg, ArrayList<Integer> sameRatings) {
        float temp3 = 0;

        for (int i = 0; i < sameRatings.size(); i++) {
            float temp1 = u1.getRatings().get(sameRatings.get(i)) - u1Avg;
            float temp2 = u2.getRatings().get(sameRatings.get(i)) - u2Avg;
            temp3 = temp3 + (temp1 * temp2);

        }

        return temp3;
    }

    public float squareRoot(User u1, User u2, float u1Avg, float u2Avg, ArrayList<Integer> sameRatings) {
    	
        float temp1 = 0;
        float temp2 = 0;

        for (int i = 0; i < sameRatings.size(); i++) {
            temp1 = temp1 + ((u1.getRatings().get(sameRatings.get(i)) - u1Avg) * (u1.getRatings().get(sameRatings.get(i)) - u1Avg));
        }

        temp1 = (float) Math.sqrt(temp1);

        for (int i = 0; i < sameRatings.size(); i++) {
            temp2 = temp2 + ((u2.getRatings().get(sameRatings.get(i)) - u2Avg) * (u2.getRatings().get(sameRatings.get(i)) - u2Avg));
        }

        temp2 = (float) Math.sqrt(temp2);

        return temp1 * temp2;
    }

    public float similarityCoefficient(User u1, User u2) {
        float u1Avg = u1.getAverageRating();
        float u2Avg = u2.getAverageRating();

        ArrayList<Integer> sameRatings = getSame(u1, u2);

        if(sameRatings.size() != 0) {
        
	        float similarityValue = (sumMeanDifference(u1, u2, u1Avg, u2Avg, sameRatings) / (squareRoot(u1, u2, u1Avg, u2Avg, sameRatings)));
	
	        return similarityValue;
	        
        } else {
        	
        	return 0;
        	
        }
    }
    
    public void prediction(Connection conn, User user, User item, int threshold) {	//20-60
    	
    	float userAvg = user.getAverageRating();
    	
    	String myQuery = "SELECT colValue FROM simMatrix WHERE rowValue = " + user.getUserID() + "AND similarity > 0.5";
    	
    	User[] neighbourhood = new User[threshold];
    	
    	int count = 0;
    	
        try (Statement stmt = conn.createStatement();
        		  ResultSet rs = stmt.executeQuery(myQuery)) {

            while (rs.next()) {

            	neighbourhood[count] = users.get(rs.getInt(1));
            	count++;
            	
            }
        	
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    	
    }
    
    public void createSimilarityMatrix(Connection conn) {
    	
    	String dropQuery = "DROP TABLE IF EXISTS simMatrix";
    	
        String myQuery = "CREATE TABLE IF NOT EXISTS simMatrix (rowValue integer, colValue integer, similarity float)";
        
        int count = 0;
        
        try (Statement stmt = conn.createStatement()) {

        	stmt.execute(dropQuery);
        	stmt.execute(myQuery);
        	
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        
        for(int i = 0; i < users.size(); i++) {
        	
        	for(int j = 0; j < users.size(); j++) {
        		
        		float simValue = 0;
        		
        		if (users.get(i).getUserID() == users.get(j).getUserID()) {
        			
        			simValue = 1;
        			
        		} else {		
        			
        			 simValue = similarityCoefficient(users.get(i), users.get(j));
        			
        		}
        		
        		String insertQuery = "INSERT INTO simMatrix VALUES (" + 
        				users.get(i).getUserID() + ", " + users.get(j).getUserID() + ", " + simValue + ")";
        		
        		System.out.println("Count:" + count + "(" + 
        				users.get(i).getUserID() + ", " + users.get(j).getUserID() + ", " + simValue + ")");
        		
        		count++;
        		
                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(insertQuery)) {

                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
        		
        	}
        	
        }
 	
    }

    public static void main(String[] args) {
    	
        SQLiteJDBCDriverConnection myJDBC = new SQLiteJDBCDriverConnection();
        
        try (Connection conn = SQLiteJDBCDriverConnection.connect();) {

            myJDBC.selectDistinct(conn);
            
            myJDBC.createSimilarityMatrix(conn);
            
        	
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        


    }
}
