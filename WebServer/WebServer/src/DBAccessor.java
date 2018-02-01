import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

//Would probably look at some sort of queue based dynamic if this was going to be used 
//by multiple users across the web.
public class DBAccessor {
	
	public DBAccessor()
	{
		
	}
	
	public ArrayList<Score> pullFigures()
	{
		ArrayList<Score> figures = new ArrayList<Score>();
		
		String url = "jdbc:sqlite:" + DBAccessor.class.getClassLoader().getResource("scores.db").getPath();
		String query = "select * from Scores ORDER BY Weight DESC";
		
		try {
			Connection c = DriverManager.getConnection(url);
			Statement s = c.createStatement();
			ResultSet rs = s.executeQuery(query);
			while (rs.next()) 
				{ 
				String name = rs.getString("Name");
				double weight = rs.getDouble("Weight");
				figures.add(new Score(name, weight));
				}
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		return figures;
	}

	public void pushFigures(String name, Double weight)
	{
		String url = "jdbc:sqlite:" + DBAccessor.class.getClassLoader().getResource("scores.db").getPath();
		String query = "INSERT INTO Scores VALUES ('" + name + "' , '" + weight + "');";
		
		try {
			Connection c = DriverManager.getConnection(url);
			Statement s = c.createStatement();
			s.execute(query);
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
	}
}