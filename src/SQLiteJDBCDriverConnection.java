
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

/**
 *
 * @author Jacob & Damyan
 */
public class SQLiteJDBCDriverConnection {

    ArrayList<Integer> distinctUsers = new ArrayList<Integer>();
    HashMap<Integer, User> users = new HashMap<Integer, User>();
    HashMap<Integer, HashMap<Integer, Integer>> differences = new HashMap<Integer, HashMap<Integer, Integer>>();

    User user;

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

    public void Pearson(Connection conn) {
        this.selectDistinctUsers(conn);
        this.createSimilarityMatrix(conn);
        this.makePredictions(conn);
    }

    public void slope(Connection conn) {
        this.selectDistinctUsers(conn);
        for (Entry<Integer, User> entryI : users.entrySet()) {
            for (Entry<Integer, User> entryJ : users.entrySet()) {		//for every user-item pair
                if (entryI.getValue().getUserID() != entryJ.getValue().getUserID()) {

                    slopeOne(entryJ.getValue(), entryI.getValue());

                }
            }
        }
    }

    public void selectDistinctUsers(Connection conn) {
        String myQuery = "SELECT DISTINCT userID FROM trainingset";
        String selectUser = null;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(myQuery)) {

            while (rs.next()) {
                distinctUsers.add(rs.getInt(1));
            }

            for (int i = 0; i < distinctUsers.size(); i++) {
                System.out.println(distinctUsers.get(i));
                selectUser = "SELECT itemID, rating FROM trainingset WHERE userID = " + distinctUsers.get(i);

                user = new User(distinctUsers.get(i));

                while (rs.next()) {
                    user.getRatings().put(rs.getInt(1), rs.getInt(2));
                }

                user.computeAverage();
                users.put(distinctUsers.get(i), user);
            }

            System.out.println(distinctUsers.size());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
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
        float denominator = u1MeanDiffSq * u2MeanDiffSq;
        return numerator / denominator;
    }

    public float similarityCoefficient(User u1, User u2, ArrayList<Integer> sameRatings) {
        float u1Avg = u1.getAverageRating();
        float u2Avg = u2.getAverageRating();

        if (sameRatings.size() != 0) {
            float similarityValue = numeratorAndDenominator(u1, u2, u1Avg, u2Avg, sameRatings);

            return similarityValue;
        } else {
            return 0;
        }
    }

    public float prediction(Connection conn, User user, User item, int threshold) { // 20-60
        float prediction = user.getAverageRating();

        //String indexRowValue = "CREATE INDEX rowIndex ON simMatrix (rowValue)";
        //String indexSimilarity = "CREATE INDEX simIndex ON simMatrix (similarity)";
        String myQuery = "SELECT colValue, similarity FROM simMatrix WHERE rowValue = " + user.getUserID()
                + " ORDER BY simItems, similarity LIMIT " + threshold;

        HashMap<Integer, Float> neighbourhood = new HashMap<Integer, Float>();

        try (Statement stmt = conn.createStatement()) {
            //stmt.execute(indexRowValue);
            //stmt.execute(indexSimilarity);
            ResultSet rs = stmt.executeQuery(myQuery);

            while (rs.next()) {
                neighbourhood.put(rs.getInt(1), rs.getFloat(2));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        float numerator = 0;

        for (Entry<Integer, Float> entry : neighbourhood.entrySet()) {
            numerator = numerator + (entry.getValue() * (users.get(entry.getKey()).getRatings().get(item.getUserID())
                    - users.get(entry.getKey()).getAverageRating()));
        }

        float denominator = 0;

        for (Entry<Integer, Float> entry : neighbourhood.entrySet()) {
            denominator = denominator + entry.getValue();
        }

        prediction = prediction + (float) (numerator / denominator);

        if (prediction < 1) {
            prediction = 1;
        } else if (prediction > 10) {
            prediction = 10;
        }

        System.out.println(prediction);

        return prediction;
    }

    //if don't appear in the similarity set
    public void createSimilarityMatrix(Connection conn) {
        String dropQuery = "DROP TABLE IF EXISTS simMatrix";
        String myQuery = "CREATE TABLE IF NOT EXISTS simMatrix (rowValue integer, colValue integer, similarity float, simItems integer)";

        int count = 0;
        PreparedStatement insert = null;

        String insertQuery = "INSERT INTO simMatrix VALUES (?,?,?,?)";
        float simValue = 0;
        int similarItems = 0;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(dropQuery);
            stmt.execute(myQuery);

            conn.setAutoCommit(false);
            insert = conn.prepareStatement(insertQuery);

            //print only the user
            for (Entry<Integer, User> entry : users.entrySet()) {
                for (Entry<Integer, User> entryJ : users.entrySet()) {
                    if (entry.getValue().getUserID() <= entryJ.getValue().getUserID()) {
                        continue;
                    } else {
                        ArrayList<Integer> sameRatings = getSame(entry.getValue(), entryJ.getValue());
                        similarItems = sameRatings.size();
                        if (similarityCoefficient(entry.getValue(), entryJ.getValue(), sameRatings) == 0) {
                            continue;
                        }
                    }

                    insert.setInt(1, entry.getValue().getUserID());
                    insert.setInt(2, entryJ.getValue().getUserID());
                    insert.setFloat(3, simValue);
                    insert.setInt(4, similarItems);
                    //insert.addBatch();

                    //check if batch refreshes
                    if (count % 1000 == 0) {
                        //insert.executeBatch();
                        //insert.clearBatch();
                        conn.commit();
                        //System.out.println("Count:" + count + "(" + entry.getValue().getUserID() + ", "
                        // + entryJ.getValue().getUserID() + ", " + simValue + ","+similarItems+")");
                    }

                    count++;
                }
                if (entry.getValue().getUserID() % 1000 == 0) {
                    System.out.println(entry.getValue().getUserID());
                }
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                insert.close();
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void makePredictions(Connection conn) {
        String myQuery = "SELECT user, item FROM predictions";

        System.out.println("Here1");

        try (Statement stmt = conn.createStatement()) {
            System.out.println("Here2");
            ResultSet rs = stmt.executeQuery(myQuery);
            while (rs.next()) {
                // if (rs.getInt(1) % 13000 == 0) {
                float prediction = prediction(conn, users.get(rs.getInt(1)), users.get(rs.getInt(2)), 60);

                String insertQuery = "UPDATE predictions SET prediction = " + prediction + " WHERE user = "
                        + rs.getInt(1) + " AND item = " + rs.getInt(2);

                stmt.execute(insertQuery);
                // }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    public void slopeOne(User u1, User u2) {

        float u1Avg = u1.getAverageRating();
        int sum = 0;

        //move this on top
        for (Entry<Integer, Integer> entryK : u1.getRatings().entrySet()) {
            int diff = 0;
            int count = 0;

            for (Entry<Integer, User> entryL : users.entrySet()) {
                if (u1.getUserID() != u2.getUserID() && u2.getRatings().size() > 0) {

                    if (u2.getRatings().containsKey(u2.getUserID())
                            && u2.getRatings().containsKey(entryK.getKey())) {

                        diff += u2.getRatings().get(u2.getUserID())
                                - u2.getRatings().get(entryK.getKey());

                        count++;
                    }
                }
            }

            if (count > 0) {
                sum += diff / count;
            }
        }
        System.out.println(u1Avg + (float) sum / u1.getRatings().size());
    }

    public void generateDifferences(Connection conn) {
        selectDistinctUsers(conn);

        for (Entry<Integer, User> diffA : users.entrySet()) {
            if (!differences.containsKey(diffA.getKey())) {
                HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
                differences.put(diffA.getKey(), temp);
            }

            for (Entry<Integer, User> diffB : users.entrySet()) {
                if (!differences.containsKey(diffB.getKey())) {
                    HashMap<Integer, Integer> temp2 = new HashMap<Integer, Integer>();
                    differences.put(diffB.getKey(), temp2);
                }

                if (differences.get(diffA.getValue().getUserID()).containsKey(diffB.getValue().getUserID())) {
                    continue;
                }

                for (Entry<Integer, Integer> ratingsA : diffA.getValue().getRatings().entrySet()) {
                    int diff = 0;
                    int count = 0;

                    if (diffB.getValue().getRatings().containsKey(ratingsA.getKey())) {
                        diff += diffA.getValue().getRatings().get(ratingsA.getKey()) - diffB.getValue().getRatings().get(ratingsA.getKey());
                        count++;
                    }

                    if (count == 0) {
                        differences.get(diffA.getKey()).put(diffB.getKey(), 0);
                        differences.get(diffB.getKey()).put(diffA.getKey(), 0);
                    } else {
                        differences.get(diffA.getKey()).put(diffB.getKey(), diff);
                        differences.get(diffB.getKey()).put(diffA.getKey(), -diff);
                    }
                }
            }

            System.out.println(diffA.getKey());
        }
    }

    public static void main(String[] args) {
        SQLiteJDBCDriverConnection myJDBC = new SQLiteJDBCDriverConnection();
        myJDBC.Pearson(SQLiteJDBCDriverConnection.connect());
        //myJDBC.slope(SQLiteJDBCDriverConnection.connect());
        //myJDBC.generateDifferences(SQLiteJDBCDriverConnection.connect());
    }
}
