import java.util.HashMap;
import java.util.Map.Entry;

public class User {
	
	int userID;
	HashMap<Integer, Integer> ratings = new HashMap<Integer, Integer>(); //<other user, rating>
	
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
		
		float sum = 0;
		
		for(Entry<Integer, Integer> entry : getRatings().entrySet()) {
			
			sum = sum + entry.getValue();
			
		}
		
		sum = sum/getRatings().size();
		
		return sum;
		
	}
	
}
