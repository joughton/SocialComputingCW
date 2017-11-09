
public class Test {
	
	public static void main(String[] args) {
		
		SQLiteJDBCDriverConnection jdbc = new SQLiteJDBCDriverConnection();
		
		User u1 = new User(1);
		
		User u2 = new User(2);
		
		u1.getRatings().put(1, 2);
		u1.getRatings().put(4, 1);
		u1.getRatings().put(5, 5);
		
		u2.getRatings().put(1, 1);
		u2.getRatings().put(4, 5);
		u2.getRatings().put(5, 8);
		
		u1.computeAverage();
		u2.computeAverage();
		
		System.out.println("Actual: " + 0.661143091);
		System.out.println("Calculated: " + jdbc.similarityCoefficient(u1, u2));
		
	}

}
