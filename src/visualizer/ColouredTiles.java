package visualizer;

import javax.media.j3d.*;
import javax.vecmath.*;
import java.util.ArrayList;

public class ColouredTiles extends Shape3D
{
	private QuadArray plane;

	public ColouredTiles(ArrayList<Point3f> coords, Color3f col)
	{
		plane = new QuadArray(coords.size(), GeometryArray.COORDINATES
				| GeometryArray.COLOR_3);
		createGeometry(coords, col);
		createAppearance();
	}

	private void createGeometry(ArrayList<Point3f> coords, Color3f col)
	{
		int numPoints = coords.size();

		Point3f[] points = new Point3f[numPoints];
		coords.toArray(points);
		plane.setCoordinates(0, points);

		Color3f cols[] = new Color3f[numPoints];
		for(int i = 0; i < numPoints; i++)
			cols[i] = col;
		plane.setColors(0, cols);

		setGeometry(plane);
	}

	private void createAppearance()
	{
		Appearance app = new Appearance();

		PolygonAttributes pa = new PolygonAttributes();
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		app.setPolygonAttributes(pa);

		setAppearance(app);
	}
}
