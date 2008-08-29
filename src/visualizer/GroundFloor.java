package visualizer;

import java.util.*;

import javax.media.j3d.*;
import javax.vecmath.*;

public class GroundFloor extends BranchGroup
{
	private final static int FLOOR_LEN = 100;
	private final static Color3f brown = new Color3f(0.73f, 0.40f, 0.06f);

	public GroundFloor()
	{
		super();
		ArrayList<Point3f> coords = new ArrayList<Point3f>();

		for(int z = -FLOOR_LEN / 2; z <= (FLOOR_LEN / 2) - 1; z++)
		{
			for(int x = -FLOOR_LEN / 2; x <= (FLOOR_LEN / 2) - 1; x++)
			{
				createCoords(x, z, coords);
			}
		}
		final QuadArray plane = new QuadArray(coords.size(), GeometryArray.COORDINATES
				| GeometryArray.COLOR_3);
		final Shape3D tiles = new Shape3D();

		int numPoints = coords.size();

		Point3f[] points = new Point3f[numPoints];
		coords.toArray(points);
		plane.setCoordinates(0, points);

		Color3f cols[] = new Color3f[numPoints];
		for(int i = 0; i < numPoints; i++)
			cols[i] = brown;
		plane.setColors(0, cols);

		tiles.setGeometry(plane);

		Appearance app = new Appearance();

		PolygonAttributes pa = new PolygonAttributes();
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		app.setPolygonAttributes(pa);

		tiles.setAppearance(app);
		addChild(tiles);
	}

	private void createCoords(int x, int z, ArrayList<Point3f> coords)
	{
		Point3f p1 = new Point3f(x, 0.0f, z + 1.0f);
		Point3f p2 = new Point3f(x + 1.0f, 0.0f, z + 1.0f);
		Point3f p3 = new Point3f(x + 1.0f, 0.0f, z);
		Point3f p4 = new Point3f(x, 0.0f, z);
		coords.add(p1);
		coords.add(p2);
		coords.add(p3);
		coords.add(p4);
	}
}
