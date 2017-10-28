
import java.util.HashMap;
import java.util.Map.Entry;

public class User {

    int userID;
    HashMap<Integer, Integer> ratings = new HashMap<Integer, Integer>(); //<other user, rating>
    float average;
    
    public User(int userID) {
        super();
        this.userID = userID;
    }

    public int getUserID() {
        return userID;
    }

    public HashMap<Integer, Integer> getRatings() {
        return ratings;
    }

    //get avg variable
    public float getAverageRating() {
        return average;
    }
    
    public void computeAverage() {
        this.average = 0;
        
        for (Entry<Integer, Integer> entry : getRatings().entrySet()) {
            this.average = this.average + entry.getValue();
        }
        
        this.average = this.average / getRatings().size();
    }

}
