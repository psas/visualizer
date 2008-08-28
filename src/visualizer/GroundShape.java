package visualizer;

import javax.vecmath.*;
import javax.media.j3d.*;
import com.sun.j3d.utils.image.*;

public class GroundShape extends OrientedShape3D
{
	private static final int NUM_VERTS = 4;

	public GroundShape(String fnm)
	{
		this(fnm, 1);
	}

	public GroundShape(String fnm, float screenSize)
	{
		setAlignmentAxis(0.0f, 1.0f, 0.0f);

		createGeometry(screenSize);
		createAppearance(fnm);
	}

	private void createGeometry(float sz)
	{
		QuadArray plane = new QuadArray(
				NUM_VERTS,
				GeometryArray.COORDINATES
						| GeometryArray.TEXTURE_COORDINATE_2);

		Point3f p1 = new Point3f(-sz / 2, 0.0f, 0.0f);
		Point3f p2 = new Point3f(sz / 2, 0.0f, 0.0f);
		Point3f p3 = new Point3f(sz / 2, sz, 0.0f);
		Point3f p4 = new Point3f(-sz / 2, sz, 0.0f);

		plane.setCoordinate(0, p1);
		plane.setCoordinate(1, p2);
		plane.setCoordinate(2, p3);
		plane.setCoordinate(3, p4);

		TexCoord2f q = new TexCoord2f();
		q.set(0.0f, 0.0f);
		plane.setTextureCoordinate(0, 0, q);
		q.set(1.0f, 0.0f);
		plane.setTextureCoordinate(0, 1, q);
		q.set(1.0f, 1.0f);
		plane.setTextureCoordinate(0, 2, q);
		q.set(0.0f, 1.0f);
		plane.setTextureCoordinate(0, 3, q);

		setGeometry(plane);
	}

	private void createAppearance(String fnm)
	{
		Appearance app = new Appearance();

		TransparencyAttributes tra = new TransparencyAttributes();
		tra.setTransparencyMode(TransparencyAttributes.BLENDED);
		app.setTransparencyAttributes(tra);

		Texture2D tex = loadTexture(fnm);
		app.setTexture(tex);
		setAppearance(app);
	}

	private Texture2D loadTexture(String fn)
	{
		TextureLoader texLoader = new TextureLoader(fn, null);
		Texture2D texture = (Texture2D) texLoader.getTexture();
		if(texture == null)
			System.out.println("Cannot load texture from " + fn);
		else
		{
			System.out.println("Loaded texture from " + fn);

			texture.setBoundaryModeS(Texture.CLAMP_TO_EDGE);
			texture.setBoundaryModeT(Texture.CLAMP_TO_EDGE);

			texture.setMinFilter(Texture2D.BASE_LEVEL_LINEAR);
			texture.setMagFilter(Texture2D.BASE_LEVEL_LINEAR);
			texture.setEnable(true);
		}
		return texture;
	}
}
