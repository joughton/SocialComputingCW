import java.sql.Connection;
import java.util.HashMap;
import java.util.Map.Entry;

public class Test4 {
	
	public static void main(String[] args) {

		User u1 = new User(1);
		
		u1.getRatings().put(11, 8);
		u1.getRatings().put(14, 2);
		u1.getRatings().put(15, 7);
		
		User u2 = new User(2);
		
		u2.getRatings().put(11, 2);
		u2.getRatings().put(13, 5);
		u2.getRatings().put(14, 7);
		u2.getRatings().put(15, 5);
		
		User u3 = new User(3);
		
		u3.getRatings().put(11, 5);
		u3.getRatings().put(12, 4);
		u3.getRatings().put(13, 7);
		u3.getRatings().put(14, 4);
		u3.getRatings().put(15, 7);
		
		User u4 = new User(4);
		
		u4.getRatings().put(11, 7);
		u4.getRatings().put(12, 1);
		u4.getRatings().put(13, 7);
		u4.getRatings().put(14, 3);
		u4.getRatings().put(15, 8);
		
		User u5 = new User(5);
		
		u5.getRatings().put(11, 1);
		u5.getRatings().put(12, 7);
		u5.getRatings().put(13, 4);
		u5.getRatings().put(14, 6);
		u5.getRatings().put(15, 5);
		
		User u6 = new User(6);
		
		u6.getRatings().put(11, 8);
		u6.getRatings().put(12, 3);
		u6.getRatings().put(13, 8);
		u6.getRatings().put(14, 3);
		u6.getRatings().put(15, 7);
		
		u1.computeAverage();
		u2.computeAverage();
		u3.computeAverage();
		u4.computeAverage();
		u5.computeAverage();
		u6.computeAverage();
		
		HashMap<Integer, User> users = new HashMap<Integer, User>();
		
		users.put(1, u1);
		users.put(2, u2);
		users.put(3, u3);
		users.put(4, u4);
		users.put(5, u5);
		users.put(6, u6);
		
		User u11 = new User(11);
		User u12 = new User(12);
		User u13 = new User(13);
		User u14 = new User(14);
		User u15 = new User(15);
		
		users.put(1, u11);
		users.put(2, u12);
		users.put(3, u13);
		users.put(4, u14);
		users.put(5, u15);
		
		System.out.println(slopeOne(u1, u13, users));
		
	}

    public static float slopeOne(User u1, User u2, HashMap<Integer, User> users) {

        float u1Avg = u1.getAverageRating();
        int sum = 0;

        for (Entry<Integer, Integer> entryK : u1.getRatings().entrySet()) {	//for all u1's rated users
            int diff = 0;
            int count = 0;

            for (Entry<Integer, User> entryL : users.entrySet()) {			//for every user
                if (u1.getUserID() != entryL.getKey() && entryL.getValue().getRatings().size() > 0) {

                    if (entryL.getValue().getRatings().containsKey(u2.getUserID())		//rated same user
                            && entryL.getValue().getRatings().containsKey(entryK.getKey())) {

                        diff += entryL.getValue().getRatings().get(u2.getUserID())
                                - entryL.getValue().getRatings().get(entryK.getKey());

                        count++;
                    }
                }
            }
            
            if (count > 0) {
                sum += diff / count;
            }
        }
        return u1Avg + (float) sum / u1.getRatings().size();
    }
	
}
