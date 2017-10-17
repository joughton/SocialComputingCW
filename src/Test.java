
public class Test {
	
	public static void main(String[] args) {
		
		SQLiteJDBCDriverConnection jdbc = new SQLiteJDBCDriverConnection();
		
		User u1 = new User(1);
		
		User u2 = new User(2);
		
		u1.getRatings().put(1, 8);
		u1.getRatings().put(4, 2);
		u1.getRatings().put(5, 7);
		
		u2.getRatings().put(1, 5);
		u2.getRatings().put(4, 4);
		u2.getRatings().put(5, 7);
		
		System.out.println(jdbc.similarityCoefficient(u1, u2));
		
	}

}
