//Basic object to store names and weights when pulling info from the database.
public class Score 
{
	double weight;
	String name;
	
	public Score(String n, double w)
	{
		name = n;
		weight = w;
	}
	
	public double getWeight() {
		return weight;
	}
	
	public String getName()
	{
		return name;
	}
}
