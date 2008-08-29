package visualizer;

import java.util.*;

import javax.media.j3d.*;
import javax.vecmath.*;

public class CheckerFloor
{
	private final static int FLOOR_LEN = 100;

	private final static Color3f blue = new Color3f(0.73f, 0.40f, 0.06f);
	private BranchGroup floorBG;

	public CheckerFloor()
	{
		ArrayList<Point3f> blueCoords = new ArrayList<Point3f>();
		floorBG = new BranchGroup();

		boolean isBlue;
		for(int z = -FLOOR_LEN / 2; z <= (FLOOR_LEN / 2) - 1; z++)
		{
			isBlue = (z % 2 == 0) ? true : false;
			for(int x = -FLOOR_LEN / 2; x <= (FLOOR_LEN / 2) - 1; x++)
			{
				if(isBlue)
					createCoords(x, z, blueCoords);
				else
					createCoords(x, z, blueCoords);
				isBlue = !isBlue;
			}
		}
		floorBG.addChild(new ColouredTiles(blueCoords, blue));
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

	public BranchGroup getBG()
	{
		return floorBG;
	}

}
