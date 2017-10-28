
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

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

            temp.computeAverage();
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

        if (u1.getRatings().size() < u2.getRatings().size()) {
            for (Map.Entry<Integer, Integer> entry : u1.getRatings().entrySet()) {
                if (u2.getRatings().containsKey(entry.getKey())) {
                    sameRatings.add(entry.getKey());
                }
            }
        } else {
            for (Map.Entry<Integer, Integer> entry : u2.getRatings().entrySet()) {
                if (u1.getRatings().containsKey(entry.getKey())) {
                    sameRatings.add(entry.getKey());
                }
            }
        }

        return sameRatings;
    }

    public float numeratorAndDenominator(User u1, User u2, float u1Avg, float u2Avg, ArrayList<Integer> sameRatings) {

        float numerator = 0;
        float u1MeanDiffSq = 0;
        float u2MeanDiffSq = 0;
         float u1MeanDiff = 0;
         float u2MeanDiff = 0;
         
        for (int i = 0; i < sameRatings.size(); i++) {
            u1MeanDiff = u1.getRatings().get(sameRatings.get(i)) - u1Avg;
            u2MeanDiff = u2.getRatings().get(sameRatings.get(i)) - u2Avg;
            numerator += u1MeanDiff * u2MeanDiff;

            u1MeanDiffSq += u1MeanDiff * u1MeanDiff;
            u2MeanDiffSq += u2MeanDiff * u2MeanDiff;
        }

        u1MeanDiffSq = (float) Math.sqrt(u1MeanDiffSq);
        u2MeanDiffSq = (float) Math.sqrt(u2MeanDiffSq);
        float deonominator = u1MeanDiffSq * u2MeanDiffSq;

        return numerator / deonominator;
    }

    public float squareRoot(User u1, User u2, float u1Avg, float u2Avg, ArrayList<Integer> sameRatings) {
        float temp4 = 0;
        float temp5 = 0;

        for (int i = 0; i < sameRatings.size(); i++) {
            temp4 = temp4 + ((u1.getRatings().get(sameRatings.get(i)) - u1Avg) * (u1.getRatings().get(sameRatings.get(i)) - u1Avg));
        }

        temp4 = (float) Math.sqrt(temp4);

        for (int i = 0; i < sameRatings.size(); i++) {
            temp5 = temp5 + ((u2.getRatings().get(sameRatings.get(i)) - u2Avg) * (u2.getRatings().get(sameRatings.get(i)) - u2Avg));
        }

        temp5 = (float) Math.sqrt(temp5);

        return temp4 * temp5;
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
        float u1Avg = u1.getAverageRating();
        float u2Avg = u2.getAverageRating();

        ArrayList<Integer> sameRatings = getSame(u1, u2);

        if (sameRatings.size() != 0) {
            float similarityValue = numeratorAndDenominator(u1, u2, u1Avg, u2Avg, sameRatings);

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

        PreparedStatement insert = null;
        String begin = "BEGIN;";
        String commit = "COMMIT;";

        String insertQuery = "INSERT INTO simMatrix VALUES (?,?,?)";
        float simValue = 0;
        try {
            conn.setAutoCommit(false);
            insert = conn.prepareStatement(insertQuery);

            for (Entry<Integer, User> entry : users.entrySet()) { 
                for (Entry<Integer, User> entryJ : users.entrySet()) {
                    if (entry.getValue().getUserID() == entryJ.getValue().getUserID()) {
                        simValue = 1;
                    } else {
                        simValue = similarityCoefficient(entry.getValue(), entryJ.getValue());
                    }

                    insert.setInt(1, entry.getValue().getUserID());
                    insert.setInt(2, entryJ.getValue().getUserID());
                    insert.setFloat(3, simValue);
                    insert.addBatch();

                    if (count % 1000 == 0) {
                        insert.executeBatch();
                        conn.commit();
                        System.out.println("Count:" + count + "("
                                + entry.getValue().getUserID() + ", " + entryJ.getValue().getUserID() + ", " + simValue + ")");
                    }

                    count++;
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                insert.close();
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                Logger.getLogger(SQLiteJDBCDriverConnection.class.getName()).log(Level.SEVERE, null, ex);
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
                //if (rs.getInt(1) % 13000 == 0) {
                float prediction = prediction(conn, users.get(rs.getInt(1)), users.get(rs.getInt(2)), 60);

                String insertQuery = "UPDATE predictions SET prediction = " + prediction + " WHERE user = " + rs.getInt(1) + " AND item = " + rs.getInt(2);

                stmt.execute(insertQuery);
                // }
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
