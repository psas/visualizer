package visualizer;

/*
 *      Terrain.java       2001, 2002 Martin Barbisch
 *
 *      Terrain calculation
 *
 *      Studienarbeit at the University of Stuttgart, VIS group
 */

import java.awt.Image;
import java.awt.image.*;
import java.util.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.image.TextureLoader;

public class Terrain extends Shape3D implements GeometryUpdater, MouseBehaviorCallback
{
	private static final float MAX_F = Float.MAX_VALUE; // shortcut

	private static final int FLOATS_PER_VERTEX = 5; // s,t, x,y,z

	private static final int NW = 0, W = 1, SW = 2, S = 3, SE = 4, E = 5, NE = 6, N = 7, C = 8;

	private static final int NUL = 0, NLL = 1, NLR = 2, NUR = 3, FUL = 4, FLL = 5, FLR = 6, FUR = 7;
	private static final int LEFT = 0, RIGHT = 1, TOP = 2, BOTTOM = 3, NEAR = 4, FAR = 5;

	private SimpleUniverse SimpleU; // ref. to Universe

	private Texture2D texture; // the texture

	private TriangleArray TerrainTA; // contains vertices and tex coords

	private Geometry TGeometry; // local geometry
	private Appearance TAppearance; // local appearance

	private float[] Vertices; // positions saved for reference
	private float[] TexCoords; // tex coords saved for reference

	private float[] Interleaved; // working array, size: max #vertices * 5

	private float[] QuadMatrix; // represents quad tree (and contains
					// heights)
	private float[] ErrorMatrix; // contains d2 error values

	private int MinGlobalRes = 12; // min global res., the higher the more
					// vertices
	private float DesiredRes = 22.0f; // desired global resolution
	private float D2K; // factor for neighboring D2-values

	private float VertexSpacing; // distance between two height values
					// (resolution)
	private int Width; // num. of height values in x- and y-direction
	private int Level; // max num of levels
	private float MaxHeight; // height of 'highest' elevation

	private int NumFans; // num of Fans
	private int VNum; // num of Vertices

	private float HeightAboveGround; // height above ground

	private Transform3D GUT3D = new Transform3D();
	private Vector3f EyePos = new Vector3f();

	private boolean CreateNewTA = true; // if array has been resized
	private boolean GeoMorph = true; // use geomorphing?

	private boolean TextureRamp = false; // real texture or generated ramp?

	private Point3d Point = new Point3d();

	private Vector4d[] Planes = new Vector4d[6];
	private Vector3d[] Normals = new Vector3d[6];
	private Vector3d[] Vectors = new Vector3d[2];
	private Point3d[] Points = new Point3d[8];

	private BoundingPolytope ViewFrustum = new BoundingPolytope();
	private BoundingSphere BoundsSphere = new BoundingSphere();

	public Terrain(SimpleUniverse su, int[] hMap, Texture2D tex2D, float cx, float cz, float vertSpacing, float maxHeight)
	{

		SimpleU = su;
		texture = tex2D;
		VertexSpacing = vertSpacing;
		MaxHeight = maxHeight;

		D2K = (float) MinGlobalRes / (2.0f * (MinGlobalRes - 1.0f));

		Width = (int) Math.sqrt((double) hMap.length);
		Level = (int) (Math.log((double) (Width - 1)) / Math.log(2.0d));
		NumFans = (int) Math.pow(4.0, (double) (Level - 1));

		int h = 256;

		if (texture == null)
		{

			TextureRamp = true;

			int[] c = new int[2 * h];
			int index = 0;

			for (int y = h - 1; y >= 0; y--)
			{

				int val = 255 << 24;
				int green = ((y * 135) / (h - 1)) + 120;

				if (y % 10 == 0)
				{
					green -= 20;
				}

				val |= (green << 8);

				c[index++] = val;
				c[index++] = val;
			}

			Canvas3D c3d = SimpleU.getViewer().getCanvas3D();
			Image img = c3d.createImage((ImageProducer) new MemoryImageSource(2, h, c, 0, 2));

			TextureLoader texLoader = new TextureLoader(img, c3d);
			ImageComponent2D ic2d = texLoader.getImage();
			texture = new Texture2D(Texture.BASE_LEVEL, Texture.RGB, ic2d.getWidth(), ic2d.getHeight());

			texture.setImage(0, ic2d);
			texture.setEnable(true);
			texture.setMinFilter(Texture.NICEST);
			texture.setMagFilter(Texture.NICEST);
		}

		// --- allocate memory
		// ---------------------------------------------------------------------

		for (int i = 0; i < Planes.length; i++)
		{
			Planes[i] = new Vector4d();
		}
		for (int i = 0; i < Normals.length; i++)
		{
			Normals[i] = new Vector3d();
		}
		for (int i = 0; i < Vectors.length; i++)
		{
			Vectors[i] = new Vector3d();
		}
		for (int i = 0; i < Points.length; i++)
		{
			Points[i] = new Point3d();
		}

		Vertices = new float[Width * Width * 3];
		TexCoords = new float[Width * Width * 2];

		QuadMatrix = new float[Width * Width];
		ErrorMatrix = new float[Width * Width];

		Interleaved = new float[10002 * FLOATS_PER_VERTEX];

		Arrays.fill(QuadMatrix, MAX_F);

		// --- fill Vertices and TexCoords arrays with values
		// --------------------------------------

		float mid = (float) Width * VertexSpacing / 2.0f;

		float step = 1.0f / (float) Width; // Width steps between
							// [0..1.0] for:
		float s, t = 1.0f; // tex coords
		int vnum = 0; // vertex number

		float heightSteps = MaxHeight / 255.0f;

		for (int z = 0; z < Width; z++, t -= step)
		{

			s = 0.0f;

			for (int x = 0; x < Width; x++, vnum++, s += step)
			{

				setVertex(Vertices, vnum, (x * VertexSpacing - mid + cx), ((float) hMap[vnum] * heightSteps), (z
						* VertexSpacing - mid + cz));

				if (TextureRamp)
				{

					setVertex2(TexCoords, vnum, 0.25f, ((float) hMap[vnum] / 255.0f));
				} else
				{
					setVertex2(TexCoords, vnum, s, t);
				}
			}
		}

		// --- set Geometry and Appearance
		// ---------------------------------------------------------

		setCapability(ALLOW_GEOMETRY_WRITE);
		setCapability(ALLOW_APPEARANCE_WRITE);

		calcD2ErrorMatrix();

		createGeometry();
		TAppearance = createAppearance(true);

		setAppearance(TAppearance); // setGeometry(TGeometry) exec. in
						// createGeometry
	}

	// === Vertex manipulation
	// ==================================================================================

	private void setVertex(float[] array, int vnum, float x, float y, float z)
	{

		vnum *= 3; // vertex number * #values per vertex

		array[vnum] = x;
		array[vnum + 1] = y;
		array[vnum + 2] = z;
	}

	private void setVertex2(float[] array, int vnum, float s, float t)
	{

		vnum *= 2; // vertex number * #values per vertex

		array[vnum] = s;
		array[vnum + 1] = t;
	}

	private void getVertex(int vnum, Point3d point)
	{

		vnum *= 3;

		point.x = Vertices[vnum];
		point.y = Vertices[vnum + 1];
		point.z = Vertices[vnum + 2];
	}

	// === ErrorMatrix calculations
	// =============================================================================

	private void calcD2ErrorMatrix()
	{

		// --- call recursive function for setting initial D2-Values ---

		calcD2ErrorMatrixRec(Width / 2, Width / 2, Width, 1);

		// --- ensure max level difference of 1 ------------------------

		propagateD2Errors();
	}

	private void calcD2ErrorMatrixRec(int centerX, int centerZ, int width, int level)
	{

		if (level <= Level)
		{

			// --- current value
			// -------------------------------------------------------------

			int nodeIndex = centerZ * Width + centerX;

			ErrorMatrix[nodeIndex] = calcD2Value(centerX, centerZ, width);

			// --- descend tree
			// --------------------------------------------------------------

			int w2 = width / 2;
			int w4 = width / 4;

			calcD2ErrorMatrixRec(centerX - w4, centerZ + w4, w2, level + 1); // nw
												// child
			calcD2ErrorMatrixRec(centerX + w4, centerZ + w4, w2, level + 1); // ne
												// child
			calcD2ErrorMatrixRec(centerX + w4, centerZ - w4, w2, level + 1); // se
												// child
			calcD2ErrorMatrixRec(centerX - w4, centerZ - w4, w2, level + 1); // sw
												// child
		}
	}

	private float calcD2Value(int centerX, int centerZ, int width)
	{

		// --- init needed vars
		// -------------------------------------------------------------

		int rx = width / 2; // radius offset in x direction
		int rz = rx * Width; // radius offset in z direction

		int c = centerZ * Width + centerX; // center vertex
		int n = c - rz; // northern vertex
		int w = c - rx; // western
		int s = c + rz; // southern
		int e = c + rx; // eastern
		int nw = n - rx; // ...
		int sw = s - rx;
		int se = s + rx;
		int ne = n + rx;

		float[] v = Vertices; // shortcut

		// --- north, east, south, west errors
		// ----------------------------------------------

		float nErr = Math.abs((float) v[n * 3 + 1] - (float) (v[nw * 3 + 1] + v[ne * 3 + 1]) / 2.0f);
		float eErr = Math.abs((float) v[e * 3 + 1] - (float) (v[ne * 3 + 1] + v[se * 3 + 1]) / 2.0f);
		float sErr = Math.abs((float) v[s * 3 + 1] - (float) (v[se * 3 + 1] + v[sw * 3 + 1]) / 2.0f);
		float wErr = Math.abs((float) v[w * 3 + 1] - (float) (v[sw * 3 + 1] + v[nw * 3 + 1]) / 2.0f);

		// --- 1. and 2. diagonal error
		// -----------------------------------------------------

		float d1Err = Math.abs((float) v[c * 3 + 1] - (float) (v[nw * 3 + 1] + v[se * 3 + 1]) / 2.0f);
		float d2Err = Math.abs((float) v[c * 3 + 1] - (float) (v[ne * 3 + 1] + v[sw * 3 + 1]) / 2.0f);

		// --- determine max of the 6 errors
		// ------------------------------------------------

		float maxErr = Math.max(Math.max(nErr, eErr), Math.max(Math.max(sErr, wErr), Math.max(d1Err, d2Err)));

		return (maxErr / ((float) width * VertexSpacing));
	}

	private void propagateD2Errors()
	{

		// find the highest level

		// --- init vars
		// --------------------------------------------------------------------

		int steps = Width / 2;
		int width = 2;

		int centerX, centerZ;

		int w2;
		int p1, p2, p3; // indices of parents
		int p1x = 0, p1z = 0, p2x = 0, p2z = 0, p3x = 0, p3z = 0;

		float[] em = ErrorMatrix; // shortcut

		// --- iterate through all levels
		// ---------------------------------------------------

		while (steps > 1)
		{

			w2 = width / 2;

			centerX = w2;

			for (int i = 0; i < steps; i++)
			{

				centerZ = w2;

				for (int j = 0; j < steps; j++)
				{

					// --- determine parents' indices
					// ------------------------

					switch ((centerX / width) % 2 + 2 * ((centerZ / width) % 2))
					{

						case 0:
							p1x = centerX + w2;
							p1z = centerZ + w2;
							p2x = centerX - width - w2;
							p2z = centerZ + w2;
							p3x = centerX + w2;
							p3z = centerZ - width - w2;
							break;

						case 1:
							p1x = centerX - w2;
							p1z = centerZ + w2;
							p2x = centerX - w2;
							p2z = centerZ - width - w2;
							p3x = centerX + width + w2;
							p3z = centerZ + w2;
							break;

						case 2:
							p1x = centerX + w2;
							p1z = centerZ - w2;
							p2x = centerX + w2;
							p2z = centerZ + width + w2;
							p3x = centerX - width - w2;
							p3z = centerZ - w2;
							break;

						case 3:
							p1x = centerX - w2;
							p1z = centerZ - w2;
							p2x = centerX + width + w2;
							p2z = centerZ - w2;
							p3x = centerX - w2;
							p3z = centerZ + width + w2;
							break;
					}

					// --- propagate to 3 parents
					// -----------------------------

					float d2K_EMthis = D2K * em[centerZ * Width + centerX];

					// --- to real father
					// -------------------------------------

					p1 = p1z * Width + p1x;
					em[p1] = Math.max(em[p1], d2K_EMthis);

					// --- other 2 "parents"
					// ----------------------------------

					if (p2x >= 0 && p2x < Width && p2z >= 0 && p2z < Width)
					{

						p2 = p2z * Width + p2x;
						em[p2] = Math.max(em[p2], d2K_EMthis);
					}

					if (p3x >= 0 && p3x < Width && p3z >= 0 && p3z < Width)
					{

						p3 = p3z * Width + p3x;
						em[p3] = Math.max(em[p3], d2K_EMthis);
					}

					centerZ += width; // advance zPos
				}
				centerX += width; // advance xPos
			}
			width *= 2;
			steps /= 2;
		}
	}

	// === Triangulation
	// ========================================================================================

	private void triangulateMeshRec(int c, int width, int level)
	{

		if (level > Level)
		{
			return;
		}

		// --- check view frustum
		// --------------------------------------------------------------

		if (level <= Level - 2)
		{

			getVertex(c, Point);

			BoundsSphere.setCenter(Point);
			BoundsSphere.setRadius((double) (width / 2) * (double) VertexSpacing * 15f);

			if (!ViewFrustum.intersect(BoundsSphere))
			{
				return;
			}
		}

		// --- go on
		// ---------------------------------------------------------------------------

		float subDiv = calcSubDiv(c, width);

		int w2 = width / 2;
		int w4 = width / 4;
		int r = w4 * Width;

		int nw = c - r - w4;
		int ne = c - r + w4;
		int sw = c + r - w4;
		int se = c + r + w4;

		if (subDiv >= 1.0f)
		{

			// --- NOT to be drawn --------------------

			deleteNode(c, width);

			return;
		} else
		{

			// --- leaf or parent? ---------------------

			QuadMatrix[c] = calcBlend(subDiv);

			triangulateMeshRec(nw, w2, level + 1);
			triangulateMeshRec(ne, w2, level + 1);
			triangulateMeshRec(sw, w2, level + 1);
			triangulateMeshRec(se, w2, level + 1);

			return;

		}
	}

	private float calcSubDiv(int nodeIndex, int width)
	{

		// --- calculate the eye distance to each node, using L1-Norm
		// --------------

		float eyeDist = Math.abs(Vertices[nodeIndex * 3] - EyePos.x) + Math.abs(HeightAboveGround)
				+ Math.abs(Vertices[nodeIndex * 3 + 2] - EyePos.z);

		// --- calc subdiv value
		// ---------------------------------------------------

		return eyeDist / ((width * VertexSpacing) * MinGlobalRes * Math.max(DesiredRes * ErrorMatrix[nodeIndex], 1.0f));
	}

	private float calcBlend(float subDiv)
	{

		// --- calc blend factor ----------------------

		float blend = 2 * (1.0f - subDiv);

		if (blend > 1.0f)
		{
			blend = 1.0f;
		}

		return blend;
	}

	private void deleteNode(int index, int width)
	{

		// --- init vars ---------------------------------------

		width /= 2;

		int rx = width / 2;
		int rz = rx * Width;

		// --- delete this node and descend --------------------

		QuadMatrix[index] = MAX_F; // C (this)

		if (width > 2)
		{

			deleteNode(index - rz - rx, width); // NW
			deleteNode(index - rz + rx, width); // NE
			deleteNode(index + rz - rx, width); // SW
			deleteNode(index + rz + rx, width); // SE
		}
	}

	private float getHeightAboveGround()
	{

		// --- init needed vars
		// --------------------------------------------------

		float ex = EyePos.x;
		float ez = EyePos.z;

		float ulx = Vertices[0]; // upper left x coord.
		float ulz = Vertices[2]; // upper left z coord.

		float lrx = Vertices[Vertices.length - 3]; // lower right...
		float lrz = Vertices[Vertices.length - 1]; // ...

		// --- determine height above ground
		// -------------------------------------

		if (ex < ulx)
		{
			ex = ulx;
		} else if (ex > lrx)
		{
			ex = lrx;
		}

		if (ez < ulz)
		{
			ez = ulz;
		} else if (ez > lrz)
		{
			ez = lrz;
		}

		int x = (int) ((ex - ulx) / VertexSpacing);
		int z = (int) ((ez - ulz) / VertexSpacing);

		if (x > Width - 1)
		{
			x = Width - 1;
		}
		if (z > Width - 1)
		{
			z = Width - 1;
		}

		float ground = Vertices[((z * Width) + x) * 3 + 1];

		return EyePos.y - ground;
	}

	// === Render Terrain-Mesh
	// ==================================================================================

	private boolean renderMeshRec(int x, int z, int width, int level, int dirToFather, float rhNW, float rhNE, float rhSW,
			float rhSE)
	{

		// returns true if caller has to draw this corner

		int c = z * Width + x; // center

		if (QuadMatrix[c] == MAX_F)
		{
			return true;
		} // STOP RECURSION

		int w2 = width / 2;
		int w4 = width / 4;

		// --- check view frustum
		// ---------------------------------------------------------------------------

		if (level <= Level - 2)
		{

			getVertex(c, Point);

			BoundsSphere.setCenter(Point);
			BoundsSphere.setRadius((double) w2 * (double) VertexSpacing * 5f);

			if (!ViewFrustum.intersect(BoundsSphere))
			{
				return false;
			}
		}

		// --- init necessary vars
		// --------------------------------------------------------------------------

		int rx = width / 2; // radius offset in x direction
		int rz = rx * Width; // radius offset in z direction

		int n = c - rz; // northern vertex
		int w = c - rx; // western
		int s = c + rz; // southern
		int e = c + rx; // eastern

		// --- get all 9 heights
		// ----------------------------------------------------------------------------

		float blend = QuadMatrix[c];

		float hC = getHeight(c, width, dirToFather, C, blend, rhNW, rhNE, rhSW, rhSE);
		float hN = getHeight(n, width, dirToFather, N, blend, rhNW, rhNE, rhSW, rhSE);
		float hS = getHeight(s, width, dirToFather, S, blend, rhNW, rhNE, rhSW, rhSE);
		float hW = getHeight(w, width, dirToFather, W, blend, rhNW, rhNE, rhSW, rhSE);
		float hE = getHeight(e, width, dirToFather, E, blend, rhNW, rhNE, rhSW, rhSE);

		// --- check if node has children
		// -------------------------------------------------------------------

		boolean[] corners =
		{ true, true, true, true, true, true, true, true };
		boolean isLeaf = false;

		if (level < Level)
		{

			corners[NW] = renderMeshRec(x - w4, z - w4, w2, level + 1, SE, rhNW, hN, hW, hC);
			corners[NE] = renderMeshRec(x + w4, z - w4, w2, level + 1, SW, hN, rhNE, hC, hE);
			corners[SW] = renderMeshRec(x - w4, z + w4, w2, level + 1, NE, hW, hC, rhSW, hS);
			corners[SE] = renderMeshRec(x + w4, z + w4, w2, level + 1, NW, hC, hE, hS, rhSE);
		} else
		{
			isLeaf = true;
		}

		if (corners[NW] || corners[SW] || corners[SE] || corners[NE])
		{

			if (corners[NW] && corners[SW] && corners[SE] && corners[NE])
			{
				isLeaf = true;
			}

			// --- determine 'corners' to be drawn
			// ------------------------------------------------------

			float[] qm = QuadMatrix; // shortcut

			// check array bounds and whether the neighbor exists

			if ((z - width >= 0) && (qm[c - (Width * width)] == MAX_F))
			{
				corners[N] = false;
			}
			if ((x + width < Width) && (qm[c + width] == MAX_F))
			{
				corners[E] = false;
			}
			if ((z + width < Width) && (qm[c + (Width * width)] == MAX_F))
			{
				corners[S] = false;
			}
			if ((x - width >= 0) && (qm[c - width] == MAX_F))
			{
				corners[W] = false;
			}

			createFanAround(x, z, width, corners, isLeaf, hC, hN, hS, hW, hE, rhNW, rhNE, rhSW, rhSE);
		}

		return false;
	}

	private void createFanAround(int x, int z, int width, boolean[] corners, boolean isLeaf, float hC, float hN, float hS,
			float hW, float hE, float hNW, float hNE, float hSW, float hSE)
	{

		// --- init necessary vars
		// -------------------------------------------------

		int rx = width / 2; // radius offset in x direction
		int rz = rx * Width; // radius offset in z direction

		int c = z * Width + x; // center vertex
		int n = c - rz; // northern vertex
		int w = c - rx; // western
		int s = c + rz; // southern
		int e = c + rx; // eastern
		int nw = n - rx;
		int sw = s - rx;
		int se = s + rx;
		int ne = n + rx;

		// --- resize geometry array size (maybe)
		// ----------------------------------

		if ((VNum + 24) * FLOATS_PER_VERTEX > Interleaved.length)
		{

			float[] temp = new float[Interleaved.length * 2];

			System.arraycopy(Interleaved, 0, temp, 0, Interleaved.length);

			Interleaved = temp;
			CreateNewTA = true;
		}

		// --- pre-check corners array
		// ---------------------------------------------

		if (!isLeaf)
		{

			if (!corners[NW] && !corners[SW])
			{
				corners[W] = false;
			}
			if (!corners[SW] && !corners[SE])
			{
				corners[S] = false;
			}
			if (!corners[SE] && !corners[NE])
			{
				corners[E] = false;
			}
			if (!corners[NE] && !corners[NW])
			{
				corners[N] = false;
			}
		}

		// --- check western quarter
		// -----------------------------------------------

		if (corners[NW])
		{

			setInterleaved(nw, hNW);

			if (corners[W])
			{

				setInterleaved(w, hW);

				if (corners[SW])
				{

					setInterleaved(c, hC);
					setInterleaved(w, hW);
					setInterleaved(sw, hSW);
				}
			} else
			{
				if (corners[SW])
				{
					setInterleaved(sw, hSW);
				} else
				{
					setInterleaved(w, hW);
				}
			}

			setInterleaved(c, hC);
		} else if (corners[W] || corners[SW])
		{

			setInterleaved(w, hW);
			setInterleaved(sw, hSW);
			setInterleaved(c, hC);
		}

		// --- check southern quarter
		// ----------------------------------------------

		if (corners[SW])
		{

			setInterleaved(sw, hSW);

			if (corners[S])
			{

				setInterleaved(s, hS);

				if (corners[SE])
				{

					setInterleaved(c, hC);
					setInterleaved(s, hS);
					setInterleaved(se, hSE);
				}
			} else
			{
				if (corners[SE])
				{
					setInterleaved(se, hSE);
				} else
				{
					setInterleaved(s, hS);
				}
			}

			setInterleaved(c, hC);
		} else if (corners[S] || corners[SE])
		{

			setInterleaved(s, hS);
			setInterleaved(se, hSE);
			setInterleaved(c, hC);
		}

		// --- check eastern quarter
		// -----------------------------------------------

		if (corners[SE])
		{

			setInterleaved(se, hSE);

			if (corners[E])
			{

				setInterleaved(e, hE);

				if (corners[NE])
				{

					setInterleaved(c, hC);
					setInterleaved(e, hE);
					setInterleaved(ne, hNE);
				}
			} else
			{
				if (corners[NE])
				{
					setInterleaved(ne, hNE);
				} else
				{
					setInterleaved(e, hE);
				}
			}

			setInterleaved(c, hC);
		} else if (corners[E] || corners[NE])
		{

			setInterleaved(e, hE);
			setInterleaved(ne, hNE);
			setInterleaved(c, hC);
		}

		// --- check northern quarter
		// ----------------------------------------------

		if (corners[NE])
		{

			setInterleaved(ne, hNE);

			if (corners[N])
			{

				setInterleaved(n, hN);

				if (corners[NW])
				{

					setInterleaved(c, hC);
					setInterleaved(n, hN);
					setInterleaved(nw, hNW);
				}
			} else
			{
				if (corners[NW])
				{
					setInterleaved(nw, hNW);
				} else
				{
					setInterleaved(n, hN);
				}
			}

			setInterleaved(c, hC);
		} else if (corners[N] || corners[NW])
		{

			setInterleaved(n, hN);
			setInterleaved(nw, hNW);
			setInterleaved(c, hC);
		}

		NumFans++;
	}

	private float getHeight(int index, int width, int dirToFather, int neswc, float blend, float rhNW, float rhNE,
			float rhSW, float rhSE)
	{

		// --- init vars
		// ------------------------------------------------------------------------

		float height = Vertices[index * 3 + 1];

		// --- determine blend values if this is a leaf
		// -----------------------------------------

		if (GeoMorph)
		{

			// --- init other vars
			// --------------------------------------------------------

			int rx = width / 2; // radius offset in x dir.
			int rz = rx * Width; // radius offset in z dir.

			int z = index / Width; // z coord. of index
			int x = index - (z * Width); // x coord. of index

			// --- determine offset and center indices
			// ------------------------------------

			switch (neswc)
			{

				case C:
					switch (dirToFather)
					{

						case SE:
						case NW:
							height = (1.0f - blend) * (rhNW + rhSE) / 2.0f + blend * height;
							break;
						case NE:
						case SW:
							height = (1.0f - blend) * (rhNE + rhSW) / 2.0f + blend * height;
							break;
					}

					break;

				case N:
					if (z - rx >= 0)
					{

						blend = Math.min(blend, QuadMatrix[index - rz]);
					}

					height = (1.0f - blend) * (rhNW + rhNE) / 2.0f + blend * height;

					break;

				case S:
					if (z + rx < Width)
					{

						blend = Math.min(blend, QuadMatrix[index + rz]);
					}

					height = (1.0f - blend) * (rhSW + rhSE) / 2.0f + blend * height;

					break;

				case W:
					if (x - rx >= 0)
					{

						blend = Math.min(blend, QuadMatrix[index - rx]);
					}

					height = (1.0f - blend) * (rhNW + rhSW) / 2.0f + blend * height;

					break;

				case E:
					if (x + rx < Width)
					{

						blend = Math.min(blend, QuadMatrix[index + rx]);
					}

					height = (1.0f - blend) * (rhNE + rhSE) / 2.0f + blend * height;

					break;
			}
		}

		return height;
	}

	private void setInterleaved(int index, float height)
	{

		// --- init vars
		// ------------------------------------------------------------

		int ii = VNum * FLOATS_PER_VERTEX;
		int iv = index * 3;
		int it = index * 2;

		// --- set vertex data
		// ------------------------------------------------------

		Interleaved[ii++] = TexCoords[it];
		Interleaved[ii++] = (TextureRamp) ? height / MaxHeight : TexCoords[it + 1];

		Interleaved[ii++] = Vertices[iv];
		Interleaved[ii++] = height;
		Interleaved[ii++] = Vertices[iv + 2];

		VNum++;
	}

	// === Geometry update
	// ======================================================================================

	private void calcViewFrustum()
	{

		// --- get view frustum parameters
		// ---------------------------------------------

		View view = SimpleU.getViewer().getView();

		double fcd = view.getBackClipDistance() * 6;
		double ncd = view.getFrontClipDistance();
		double fovx = view.getFieldOfView();
		double aspr = (double) view.getCanvas3D(0).getWidth() / (double) view.getCanvas3D(0).getHeight();

		// --- calculate the 6 view frustum points
		// -------------------------------------

		double nrx = Math.tan(fovx / 2) * ncd;
		double nry = nrx / aspr;
		double frx = (nrx / ncd) * fcd;
		double fry = frx / aspr;

		Points[NUL].set(-nrx, nry, -ncd);
		Points[NLL].set(-nrx, -nry, -ncd);
		Points[NLR].set(nrx, -nry, -ncd);
		Points[NUR].set(nrx, nry, -ncd);
		Points[FUL].set(-frx, fry, -fcd);
		Points[FLL].set(-frx, -fry, -fcd);
		Points[FLR].set(frx, -fry, -fcd);
		Points[FUR].set(frx, fry, -fcd);

		// --- calculate normals
		// -------------------------------------------------------

		Vector3d[] n = Normals; // shortcut
		Vector3d[] v = Vectors; // shortcut

		v[0].sub(Points[NUL], Points[NLL]);
		v[1].sub(Points[FLL], Points[NLL]);

		n[LEFT].cross(v[0], v[1]);
		n[RIGHT].set(-n[LEFT].x, n[LEFT].y, n[LEFT].z);

		v[0].sub(Points[NUR], Points[NUL]);
		v[1].sub(Points[FUL], Points[NUL]);

		n[TOP].cross(v[0], v[1]);
		n[BOTTOM].set(n[TOP].x, -n[TOP].y, n[TOP].z);

		n[NEAR].set(0.0f, 0.0f, 1.0f);
		n[FAR].set(0.0f, 0.0f, -1.0f);

		for (int i = 0; i < n.length; i++)
		{
			GUT3D.transform(n[i]);
		}

		// --- create view frustum BoundingPolytope
		// ------------------------------------

		GUT3D.transform(Points[NUL]);
		GUT3D.transform(Points[FLR]);

		v[0].set((Tuple3d) Points[NUL]);
		v[1].set((Tuple3d) Points[FLR]);

		Planes[0].set(n[LEFT].x, n[LEFT].y, n[LEFT].z, -n[LEFT].dot(v[0]));
		Planes[1].set(n[RIGHT].x, n[RIGHT].y, n[RIGHT].z, -n[RIGHT].dot(v[1]));
		Planes[2].set(n[TOP].x, n[TOP].y, n[TOP].z, -n[TOP].dot(v[0]));
		Planes[3].set(n[BOTTOM].x, n[BOTTOM].y, n[BOTTOM].z, -n[BOTTOM].dot(v[1]));
		Planes[4].set(n[NEAR].x, n[NEAR].y, n[NEAR].z, -n[NEAR].dot(v[0]));
		Planes[5].set(n[FAR].x, n[FAR].y, n[FAR].z, -n[FAR].dot(v[1]));

		ViewFrustum.setPlanes(Planes);
	}

	private void createGeometry()
	{

		// --- get T3D and calc view frustum
		// ---------------------------------------------------

		SimpleU.getViewingPlatform().getViewPlatformTransform().getTransform(GUT3D);

		GUT3D.get(EyePos);

		calcViewFrustum();

		VNum = NumFans = 0;

		// --- triangulate Mesh
		// ----------------------------------------------------------------

		HeightAboveGround = getHeightAboveGround();

		triangulateMeshRec(Width * Width / 2, Width - 1, 1);// , C);

		// --- create Terrain Mesh
		// -------------------------------------------------------------

		float hNW = Vertices[0 + 1];
		float hNE = Vertices[(Width - 1) * 3 + 1];
		float hSW = Vertices[(Width - 1) * Width * 3 + 1];
		float hSE = Vertices[((Width - 1) * Width + (Width - 1)) * 3 + 1];

		renderMeshRec(Width / 2, Width / 2, Width - 1, 1, C, hNW, hNE, hSW, hSE);

		if (CreateNewTA)
		{

			TerrainTA = new TriangleArray(Interleaved.length / FLOATS_PER_VERTEX, TriangleFanArray.COORDINATES
					| TriangleFanArray.TEXTURE_COORDINATE_2 | TriangleFanArray.BY_REFERENCE
					| TriangleFanArray.INTERLEAVED);

			TerrainTA.setCapability(TriangleFanArray.ALLOW_REF_DATA_WRITE);
			TerrainTA.setCapability(TriangleFanArray.ALLOW_COUNT_WRITE);
			TerrainTA.setInterleavedVertices(Interleaved);

			TGeometry = TerrainTA;
			CreateNewTA = false;

			setGeometry(TGeometry);
		}

		TerrainTA.setValidVertexCount(VNum);
	}

	// === Appearance settings
	// ==================================================================================

	private Appearance createAppearance(boolean filled)
	{

		// --- create appearance attributes
		// ---------------------------------------

		Appearance app = new Appearance();
		ColoringAttributes ca = new ColoringAttributes();
		PolygonAttributes pa = new PolygonAttributes();

		// --- set settings
		// -------------------------------------------------------

		app.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_READ);

		ca.setShadeModel(ColoringAttributes.SHADE_FLAT);

		pa.setCullFace(PolygonAttributes.CULL_NONE);
		pa.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);

		if (!filled)
		{
			pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		}

		app.setColoringAttributes(ca);
		app.setPolygonAttributes(pa);
		app.setTexture(texture);

		return app;
	}

	public void setFilledPolys(boolean filled)
	{

		PolygonAttributes pa = TAppearance.getPolygonAttributes();

		if (filled)
		{
			pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		} else
		{
			pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		}
	}

	public void toggleGeoMorphing()
	{

		GeoMorph = (GeoMorph) ? false : true;

		updateTerrain();
	}

	public void moreDetail(boolean updateNow)
	{

		DesiredRes += 0.5f;

		if (updateNow)
		{
			updateTerrain();
		}
	}

	public void lessDetail(boolean updateNow)
	{

		if (DesiredRes >= 0.5f)
		{
			DesiredRes -= 0.5f;
		}

		if (updateNow)
		{
			updateTerrain();
		}
	}

	// === MouseBehaviorCallback part
	// ===========================================================================

	public void transformChanged(int type, Transform3D transform)
	{

		updateTerrain();
	}

	// === common update Terrain method
	// =========================================================================

	public void updateTerrain()
	{

		TerrainTA.updateData(this);
	}

	// === GeometryUpdater part
	// =================================================================================

	// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	//
	// THIS WILL BE USED WITH EITHER:
	// TriangleArray where setValidVertexIndex(int) already works OR WITH
	// TriangleFanArray when setValidVertexIndex(int) works (J3D Version
	// 1.3)
	//
	// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

	public void updateData(Geometry geometry)
	{

		createGeometry();
	}
}
