import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class Test3 {
	
	public static void main(String[] args) {
		
		User u1 = new User(1);
		User u2 = new User(2);
		User u3 = new User(3);
		User u4 = new User(2);
		User u5 = new User(3);
		
		u1.getRatings().put(1, 8);
		u1.getRatings().put(4, 2);
		u1.getRatings().put(5, 7);
		
		u2.getRatings().put(1, 2);
		u2.getRatings().put(3, 5);
		u2.getRatings().put(4, 7);
		u2.getRatings().put(5, 5);
		
		u3.getRatings().put(1, 5);
		u3.getRatings().put(2, 4);
		u3.getRatings().put(3, 7);
		u3.getRatings().put(4, 4);
		u3.getRatings().put(5, 7);
		
		HashMap<Integer, User> users = new HashMap<Integer, User>();
		
		u1.computeAverage();
		u2.computeAverage();
		u3.computeAverage();
		
		users.put(1, u1);
		users.put(2, u2);
		users.put(3, u3);
		users.put(4, u4);
		users.put(5, u5);
		
		System.out.println(prediction(u1, u3, users));
		
	}
	
    public static float prediction(User user, User item, HashMap<Integer, User> users) { // 20-60
    	
    	HashMap<Integer, Float> neighbourhood = new HashMap<Integer, Float>();
    	neighbourhood.put(2, (float) -0.89);
    	neighbourhood.put(3, (float) 0.67);
    	
        float prediction = user.getAverageRating();

        float numerator = 0;

        for (Entry<Integer, Float> entry : neighbourhood.entrySet()) {
            numerator = numerator + (entry.getValue() * (users.get(entry.getKey()).getRatings().get(item.getUserID())
                    - users.get(entry.getKey()).getAverageRating()));
        }
        
        System.out.println(numerator);

        float denominator = 0;

        for (Entry<Integer, Float> entry : neighbourhood.entrySet()) {
            denominator = denominator + entry.getValue();
        }
        
        System.out.println(denominator);

        prediction = prediction + (float) (numerator / denominator);

        if (prediction < 1) {
            prediction = 1;
        } else if (prediction > 10) {
            prediction = 10;
        }

        return prediction;
    }

}
