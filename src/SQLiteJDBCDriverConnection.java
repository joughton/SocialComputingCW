
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
        } catch (Exception e) {
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
        String myQuery = "SELECT DISTINCT userID FROM trainingset WHERE userID % 13000 = 0";

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

    public float averageRatings(User u1) {
        float u1Avg = 0;

        for (Map.Entry<Integer, Integer> entry : u1.getRatings().entrySet()) {
            u1Avg = u1Avg + entry.getValue();
        }

        u1Avg = u1Avg / u1.getRatings().size();

        return u1Avg;
    }

    public float similarityCoefficient(User u1, User u2) {
        float u1Avg = /*averageRatings(u1);*/ u1.getAverageRating();
        float u2Avg = /*averageRatings(u2);*/ u2.getAverageRating();

        ArrayList<Integer> sameRatings = getSame(u1, u2);

        if (sameRatings.size() != 0) {
            float similarityValue = (sumMeanDifference(u1, u2, u1Avg, u2Avg, sameRatings) / (squareRoot(u1, u2, u1Avg, u2Avg, sameRatings)));

            return similarityValue;
        } else {
            return 0;
        }
    }

    public float prediction(Connection conn, User user, User item, int threshold) {	//20-60
        float prediction = user.getAverageRating();
        
        String indexRowValue = "CREATE INDEX rowIndex ON simMatrix (rowValue)";
        String indexSimilarity = "CREATE INDEX simIndex ON simMatrix (similarity)";
        String myQuery = "SELECT colValue, similarity FROM simMatrix WHERE rowValue = " + user.getUserID() + " ORDER BY similarity LIMIT " + threshold;
        
        HashMap<Integer, Float> neighbourhood = new HashMap<Integer, Float>();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(indexRowValue);
            stmt.execute(indexSimilarity);
            ResultSet rs = stmt.executeQuery(myQuery);

            while (rs.next()) {
                neighbourhood.put(rs.getInt(1), rs.getFloat(2));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        float numerator = 0;

        for (Entry<Integer, Float> entry : neighbourhood.entrySet()) {
            numerator = numerator + (entry.getValue() * (users.get(entry.getKey()).getRatings().get(item.getUserID()) - users.get(entry.getKey()).getAverageRating()));
        }

        float denominator = 0;

        for (Entry<Integer, Float> entry : neighbourhood.entrySet()) {
            denominator = denominator + users.get(entry.getKey()).getRatings().get(item.getUserID());
        }

        prediction = prediction + (numerator / denominator);

        if (prediction < 1) {
            prediction = 1;
        } else if (prediction > 10) {
            prediction = 10;
        }

        System.out.println(prediction);

        return prediction;
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

        /*for(int i = 0; i < users.size(); i++) {
        	
        	for(int j = 0; j < users.size(); j++) {
        		
        		float simValue = 0;
        		
        		if (users.get(i).getUserID() == users.get(j).getUserID()) {
        			
        			simValue = 1;
        			
        		} else {		
        			
        			 simValue = similarityCoefficient(users.get(i), users.get(j));
        			
        		}*/
        for (Entry<Integer, User> entry : users.entrySet()) {
            float simValue = 0;

            for (Entry<Integer, User> entryJ : users.entrySet()) {
                if (entry.getValue().getUserID() == entryJ.getValue().getUserID()) {
                    simValue = 1;
                } else {
                    simValue = similarityCoefficient(entry.getValue(), entryJ.getValue());
                }

                String insertQuery = "INSERT INTO simMatrix VALUES ("
                        + entry.getValue().getUserID() + ", " + entryJ.getValue().getUserID() + ", " + simValue + ")";

                //System.out.println("Count:" + count + "(" + 
                //		entry.getValue().getUserID() + ", " + entryJ.getValue().getUserID() + ", " + simValue + ")");
                count++;

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(insertQuery);
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    public void makePredictions(Connection conn) {
        String myQuery = "SELECT user, item FROM predictions";
        String indexUser = "CREATE INDEX userIndexPred ON predictions (user)";
        String indexItem = "CREATE INDEX itemIndexPred ON predictions (item)";
        String dropIndexUser = "DROP INDEX userIndexPred";
        String dropIndexItem = "DROP INDEX itemIndexPred";

        System.out.println("Here1");

        try (Statement stmt = conn.createStatement()) {
            System.out.println("Here2");
            //stmt.execute(indexUser);
            //stmt.execute(indexItem);
            ResultSet rs = stmt.executeQuery(myQuery);
            //stmt.execute(dropIndexUser);
            //stmt.execute(dropIndexItem);
            while (rs.next()) {
                if (rs.getInt(1) % 13000 == 0) {
                    float prediction = prediction(conn, users.get(rs.getInt(1)), users.get(rs.getInt(2)), 60);

                    String insertQuery = "UPDATE predictions SET prediction = " + prediction + " WHERE user = " + rs.getInt(1) + " AND item = " + rs.getInt(2);

                    stmt.execute(insertQuery);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    public void printNumberOfRatings() {
        for (Entry<Integer, User> entry : users.entrySet()) {
            System.out.println(entry.getValue().getRatings().size());
        }
    }

    public static void main(String[] args) {
        SQLiteJDBCDriverConnection myJDBC = new SQLiteJDBCDriverConnection();

        try (Connection conn = SQLiteJDBCDriverConnection.connect();) {
            myJDBC.selectDistinct(conn);
            myJDBC.createSimilarityMatrix(conn);
            myJDBC.makePredictions(conn);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
