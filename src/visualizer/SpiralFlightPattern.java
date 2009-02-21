package visualizer;

public class SpiralFlightPattern implements FlightPattern
{
	public double[] getNewCoords(long start)
	{
		int time = (int) start/100;
		double height = 120;
		double radius = 50;
		double segments = 8;
		double xcoord = Math.cos(time * Math.PI / segments) * radius - radius;
		double zcoord = Math.sin(time * Math.PI / segments) * radius + height;
		double ycoord = (time * Math.PI / segments) * 4;
		double[] points = new double[]
		{
			xcoord/30, ycoord/20, zcoord/30
		};
		return points;
	}
}
