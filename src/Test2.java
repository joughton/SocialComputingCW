import java.sql.Connection;
import java.util.HashMap;
import java.util.Map.Entry;

public class Test2 {
	
	static HashMap<Integer, User> users = new HashMap<Integer, User>();
	
	public static void main(String[] args) {
		
		User u1 = new User(1);
		User u2 = new User(2);
		User u3 = new User(3);
		User u4 = new User(4);
		User u5 = new User(5);
		
		u1.getRatings().put(3, 3);
		u1.getRatings().put(4, 5);
		
		u2.getRatings().put(3, 4);
		u2.getRatings().put(4, 3);
		u2.getRatings().put(5, 3);
		
		users.put(1, u1);
		users.put(2, u2);
		users.put(3, u3);
		users.put(4, u4);
		users.put(5, u5);
		
		System.out.println("Actual: " + 3.5);
		System.out.println("Calculated: " + slopeOne(u1, u5));
		
	}
	
	public static float slopeOne(User u1, User u2) {
		
		u1.computeAverage();

		float u1Avg = u1.getAverageRating();
		int sum = 0;

		for (Entry<Integer, Integer> entryI : u1.getRatings().entrySet()) {
			
			int diff = 0;
			int count = 0;

			for (Entry<Integer, User> entryJ : users.entrySet()) {
				
				if(u1.getUserID() != entryJ.getKey() && entryJ.getValue().getRatings().size() > 0) {
	
					if (entryJ.getValue().getRatings().containsKey(u2.getUserID())
								&& entryJ.getValue().getRatings().containsKey(entryI.getKey())) {
						
						diff += entryJ.getValue().getRatings().get(u2.getUserID())
								- entryJ.getValue().getRatings().get(entryI.getKey());

						count++;
	
					}
				}

			}

			if (count > 0) {

				sum += diff / count;

			}
		}
		System.out.println((float) -1/2);

		return u1Avg + (float) sum/u1.getRatings().size();
				
	}

}
