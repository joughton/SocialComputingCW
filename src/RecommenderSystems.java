
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author Jacob & Damyan
 */
public class RecommenderSystems {

    /*
      ArrayList of users IDs
      HashMap of user IDs linked to users
     */
    List<Integer> distinctUsers = new ArrayList<Integer>();
    Map<Integer, User> users = new HashMap<Integer, User>();
    Set<User> toTestUsers = new HashSet<User>();
    User user;

    //Creates connection to the database
    public static Connection connect() {
        Connection conn = null;

        try {
            String url = "jdbc:sqlite:D://new.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);

            System.out.println("Connection to SQLite has been established.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return conn;
    }

    //function which executes the flow for the user-based recommender system
    public void userBased(Connection conn) {
        this.selectDistinctUsers(conn);
        this.createSimilarityMatrix(conn);
        this.makePredictions(conn);
    }

    /*
      Function which reads all the users from the database and puts them
      in the arraylist and hashmap
      ARGS: Connection
     */
    public void selectDistinctUsers(Connection conn) {
        String selectUsers = "SELECT DISTINCT userID FROM trainingset";
        String selectUser = null;
        String selectTestUsers = null;
        ResultSet userRatings = null;
        ResultSet testUsers = null;

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(selectUsers);) {
            //populate the userIDs
            while (rs.next()) {
                distinctUsers.add(rs.getInt(1));
            }

            //populate the hashmaps of the users and the hashmap of test users
            for (int i = 0; i < distinctUsers.size(); i++) {
                selectUser = "SELECT itemID, rating FROM trainingset WHERE userID = " + distinctUsers.get(i);

                userRatings = stmt.executeQuery(selectUser);
                user = new User(distinctUsers.get(i));

                while (userRatings.next()) {
                    user.getRatings().put(userRatings.getInt(1), userRatings.getInt(2));
                }

                user.computeAverage();
                users.put(distinctUsers.get(i), user);
            }

            System.out.println(users.size());

            selectTestUsers = "SELECT DISTINCT user FROM predictions";
            testUsers = stmt.executeQuery(selectTestUsers);

            //get users in test set
            while (testUsers.next()) {
                toTestUsers.add(users.get(testUsers.getInt(1)));
            }

            System.out.println(toTestUsers.size());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
      Function which creates the similarity matrix
      ARGS: Connection
     */
    public void createSimilarityMatrix(Connection conn) {
        String createTable = "CREATE TABLE IF NOT EXISTS simMatrix (rowValue integer, colValue integer, similarity float, simItems integer)";
        String getLastID = "SELECT rowValue FROM simMatrix ORDER BY rowValue DESC LIMIT 1";
        String removeLast = "DELETE FROM simMatrix WHERE rowValue IN (SELECT rowValue FROM simMatrix ORDER BY rowValue DESC LIMIT 1)";

        int commitCounter = 0;
        float simValue = 0;
        int similarItems = 0;
        PreparedStatement insert = null;
        List<Integer> sameRatings = null;
        int lastID = 0;

        String insertQuery = "INSERT INTO simMatrix VALUES (?,?,?,?)";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            //see if it has been interrupted, delete last entry if so
            try {
                ResultSet r = stmt.executeQuery(getLastID);
                lastID = r.getInt(1);
                stmt.execute(removeLast);
            } catch (SQLException e) {
                System.out.println("No entries");
            }

            conn.setAutoCommit(false);
            insert = conn.prepareStatement(insertQuery);
            for (Entry<Integer, User> entry : users.entrySet()) {
                //continue matrix if it has been interrupted
                if (entry.getValue().getUserID() >= lastID && toTestUsers.contains(entry.getValue())) {
                    for (Entry<Integer, User> entryJ : users.entrySet()) {
                        sameRatings = getSameItems(entry.getValue(), entryJ.getValue());
                        similarItems = sameRatings.size();
                        if (similarItems == 1) {
                            continue;
                        } else {
                            simValue = similarityCoefficient(entry.getValue(), entryJ.getValue(), sameRatings);
                        }

                        if (simValue == 0) {
                            continue;
                        }

                        commitCounter++;

                        insert.setInt(1, entry.getValue().getUserID());
                        insert.setInt(2, entryJ.getValue().getUserID());
                        insert.setFloat(3, simValue);
                        insert.setInt(4, similarItems);
                        insert.executeUpdate();

                        if (commitCounter % 1000 == 0) {
                            conn.commit();
                        }
                    }

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

    /*
      Function which gets items that both users have rated
      ARGS: Two users
     */
    public List<Integer> getSameItems(User u1, User u2) {
        List<Integer> sameItems = new ArrayList<Integer>();

        if (u1.getRatings().size() < u2.getRatings().size()) {
            for (Map.Entry<Integer, Integer> entry : u1.getRatings().entrySet()) {
                if (u2.getRatings().containsKey(entry.getKey())) {
                    sameItems.add(entry.getKey());
                }
            }
        } else {
            for (Map.Entry<Integer, Integer> entry : u2.getRatings().entrySet()) {
                if (u1.getRatings().containsKey(entry.getKey())) {
                    sameItems.add(entry.getKey());
                }
            }
        }

        return sameItems;
    }

    /*
      Function which computes the similarity coefficient
      ARGS: Two users and List of similar items
     */
    public float similarityCoefficient(User u1, User u2, List<Integer> sameRatings) {
        float u1Avg = u1.getAverageRating();
        float u2Avg = u2.getAverageRating();

        float numerator = 0;
        float u1MeanDiffSq = 0;
        float u2MeanDiffSq = 0;
        float u1MeanDiff = 0;
        float u2MeanDiff = 0;

        if (sameRatings.isEmpty()) {
            return 0;
        } else {

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
    }

    /*
      Function which makes the predictions
      ARGS: Connection
     */
    public void makePredictions(Connection conn) {
        String myQuery = "SELECT user, item FROM predictions";
        String insertQuery = "UPDATE predictions SET prediction = ? WHERE user = ? AND item = ?";
        PreparedStatement insert = null;
        try (Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            insert = conn.prepareStatement(insertQuery);
            ResultSet rs = stmt.executeQuery(myQuery);
            while (rs.next()) {
                float prediction = prediction(conn, users.get(rs.getInt(1)), users.get(rs.getInt(2)), 2000);

                insert.setFloat(1, prediction);
                insert.setInt(2, users.get(rs.getInt(1)).getUserID());
                insert.setInt(3, users.get(rs.getInt(2)).getUserID());
                insert.executeUpdate();
            }
            conn.commit();
            System.out.println(countA+" "+countN+" "+countC);
            int coverage = countN/countA;
            System.out.println(coverage);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
      Function which creates the predictions and populates the DB
      ARGS: Connection, the user, the item and a threshold 
     */
    int countN = 0;
    int countC = 0;
    int countA = 0;
    public float prediction(Connection conn, User user, User item, int threshold) { // 20-60
        countA++;
        float prediction = user.getAverageRating();
        String myQuery = null;
        myQuery = "SELECT * FROM (select colValue, similarity from simMatrix WHERE rowValue = " + user.getUserID()
                + " ORDER BY simItems DESC LIMIT " + threshold + ") ORDER BY similarity desc";

        HashMap<Integer, Float> neighbourhood = new HashMap<Integer, Float>();

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(myQuery);

            while (rs.next()) {
                //checking if he has rated the product
                if (users.get(rs.getInt(1)).getRatings().containsKey(item.getUserID()) && rs.getFloat(2) > 0 && neighbourhood.size() < 50) {
                    neighbourhood.put(rs.getInt(1), rs.getFloat(2));
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        float numerator = 0;
        int count = 0;
        float sum = 0;
        
        if (neighbourhood.size() < 10 || user.getRatings().size() < 5) {
            countN++;
            for (Entry<Integer, User> entry : users.entrySet()) {
                if (entry.getValue().getRatings().containsKey(item.getUserID())) {
                    count++;
                    sum += (users.get(entry.getKey()).getRatings().get(item.getUserID()) - users.get(entry.getKey()).getAverageRating());
                }
            }
            sum = sum / count;
            prediction += sum;
        } else {
            countC++;
            for (Entry<Integer, Float> entry : neighbourhood.entrySet()) {
                numerator = numerator + users.get(entry.getKey()).getRatings().size() * (entry.getValue() * ((users.get(entry.getKey()).getRatings().get(item.getUserID()) - users.get(entry.getKey()).getAverageRating()) / users.get(entry.getKey()).getRatings().size()));
            }

            float denominator = 0;

            for (Entry<Integer, Float> entry : neighbourhood.entrySet()) {
                denominator = denominator + entry.getValue();
            }

            prediction = prediction + (float) (numerator / denominator);
        }

        if (prediction < 1) {
            prediction = 1;
        } else if (prediction > 10) {
            prediction = 10;
        }

        System.out.println(user.getUserID() + " " + neighbourhood.size() + " " + prediction + " " + user.getAverageRating());

        return prediction;
    }
    
    public static void main(String[] args) {
        RecommenderSystems myJDBC = new RecommenderSystems();
        myJDBC.userBased(RecommenderSystems.connect());
    }
}
