import java.util.HashMap;

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
	
}
