package visualizer;


public class ArcFlightPattern implements FlightPattern
{
	double[] velocity = new double[]
	{
		0, 0.5, 0.5
	};
	double[] position = new double[]
	{
		0, 0, -15
	};
	public double[] getNewCoords(long start)
	{
		double time = (System.currentTimeMillis()-start)/1000;
		velocity[1] += -.00981 * time / 10;
		position[1] += velocity[1] * time / 10;
		position[2] += velocity[2] * time / 10;
		if(position[1] < 0)
			return null;
		return position;
	}
}
