package visualizer;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.*;

import javax.imageio.ImageIO;
import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.*;

import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.PlatformGeometry;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class Visualizer extends JFrame
{
	private static final String RESOURCE_DIR = "resource/";
	private static final String HeightFile = RESOURCE_DIR + "Default_HeightMap.png";
	private static final String TextureFile = RESOURCE_DIR + "Default_TexMap.png";
	private static final String BackgroundFile = RESOURCE_DIR + "clouds.jpg";
	private final SimpleUniverse su;

	public Visualizer()
	{
		super("PSAS Rocket Visualizer");

		JPanel rocketPanel = new JPanel();
		rocketPanel.setLayout(new BorderLayout());

		BranchGroup sceneBG = new BranchGroup();
		TransformGroup rocket = createRocket(sceneBG);
		// Begin TeVi code
		GraphicsConfigTemplate3D gctTmpl = new GraphicsConfigTemplate3D();
		GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gDevice = gEnv.getDefaultScreenDevice();
		GraphicsConfiguration gConfig = gDevice.getBestConfiguration(gctTmpl);
		Canvas3D canvas3D = new Canvas3D(gConfig);

		canvas3D.setSize(512, 512);
		canvas3D.setStereoEnable(true);
		rocketPanel.add("Center", canvas3D);

		su = new SimpleUniverse(canvas3D);
		BranchGroup ObjRoot = new BranchGroup();
		BranchGroup NavRoot = new BranchGroup();

		ObjRoot.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		ObjRoot.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		NavRoot.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		NavRoot.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		su.addBranchGraph(ObjRoot);
		su.addBranchGraph(NavRoot);

		BoundingLeaf AlwaysOnBoundingLeaf = new BoundingLeaf(new BoundingSphere(new Point3d(), 100000));
		PlatformGeometry platformGeom = new PlatformGeometry();
		platformGeom.addChild(AlwaysOnBoundingLeaf);
		platformGeom.compile();
		su.getViewingPlatform().setPlatformGeometry(platformGeom);

		BufferedImage HeightImage = null;
		BufferedImage TextureImage = null;
		try
		{
			HeightImage = ImageIO.read(new File(HeightFile));
			TextureImage = ImageIO.read(new File(TextureFile));
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		ImageComponent2D heightIC = new TextureLoader(HeightImage, this).getImage();
		int w = Math.min(heightIC.getWidth(), heightIC.getHeight());
		int possibleW = 3; // 0, 3, 5, 9, 17, 33, 65, 129, 257, ...
		int Width;
		if (w < 3)
			Width = 0;
		while (w > possibleW)
		{
			possibleW = (2 * possibleW) - 1;
		}
		if (w == possibleW)
			Width = possibleW;
		else
			Width = (possibleW + 1) / 2;

		int[] HeightMap = new int[Width * Width];
		try
		{
			new PixelGrabber(HeightImage, 0, 0, Width, Width, HeightMap, 0, Width).grabPixels();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		for (int i = 0; i < HeightMap.length; i++)
		{
			HeightMap[i] &= 0xff;
		}

		ImageComponent2D ic2d = new TextureLoader(TextureImage, this).getImage();
		Texture2D tex = new Texture2D(Texture.BASE_LEVEL, Texture.RGB, ic2d.getWidth(), ic2d.getHeight());
		tex.setImage(0, ic2d);
		tex.setEnable(true);
		tex.setMinFilter(Texture.NICEST);
		tex.setMagFilter(Texture.NICEST);

		BranchGroup terRoot = new BranchGroup();
		Transform3D t3d = new Transform3D();
		t3d.lookAt(new Point3d(15, 15, 15), new Point3d(), new Vector3d(0, 1, 0));
		t3d.invert();
		TransformGroup camera = su.getViewingPlatform().getViewPlatformTransform();
		camera.setTransform(t3d);

		Terrain terrain = new Terrain(su, HeightMap, tex, 0.0f, 0.0f, 30.0f / (float) Width, 3.0f);
		terRoot.addChild(terrain);
		terRoot.addChild(getTheBackground(AlwaysOnBoundingLeaf));
		terRoot.addChild(sceneBG);

		TKeyBehavior keyBeh = new TKeyBehavior(terrain, 0.5f, rocket, camera);
		keyBeh.setSchedulingBoundingLeaf(AlwaysOnBoundingLeaf);
		BranchGroup kbg = new BranchGroup();
		kbg.addChild(keyBeh);
		NavRoot.addChild(kbg);

		TMouseBehavior MouseBeh = new TMouseBehavior();
		MouseBeh.setSchedulingBoundingLeaf(AlwaysOnBoundingLeaf);
		BranchGroup mbg = new BranchGroup();
		mbg.addChild(MouseBeh);
		NavRoot.addChild(mbg);
		MouseBeh.init(camera, 0.008f, false);

		TEachFrameBehavior efBeh = new TEachFrameBehavior(terrain, camera, keyBeh, MouseBeh);
		efBeh.setSchedulingBoundingLeaf(AlwaysOnBoundingLeaf);
		terRoot.addChild(efBeh);

		terRoot.compile();
		ObjRoot.addChild(terRoot);

		// End TeVi code
		add(rocketPanel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}

	public void dispose()
	{
		su.removeAllLocales();
		super.dispose();
		System.exit(0);
	}

	private Background getTheBackground(BoundingLeaf leaf)
	{
		Texture2D texture = (Texture2D) new TextureLoader(BackgroundFile, null).getTexture();
		if (texture == null)
			System.out.println("Cannot load texture from " + BackgroundFile);
		else
			texture.setEnable(true);

		Sphere sphere = new Sphere(1.0f, Sphere.GENERATE_NORMALS_INWARD | Sphere.GENERATE_TEXTURE_COORDS, 8);
		Appearance backApp = sphere.getAppearance();
		backApp.setTexture(texture);

		Background bg = new Background();
		bg.setApplicationBoundingLeaf(leaf);
		BranchGroup backBG = new BranchGroup();
		backBG.addChild(sphere);
		bg.setGeometry(backBG);

		return bg;
	}

	private TransformGroup createRocket(BranchGroup scene)
	{
		Transform3D t3d = new Transform3D();
		t3d.setIdentity();
		t3d.set(new Vector3d(0, 2.4, 0));

		TransformGroup tg4 = new TransformGroup(t3d);
		tg4.addChild(new ModelLoader().getModel(RESOURCE_DIR + "tintin_rocket.obj"));
		TransformGroup rocket = new TransformGroup();
		rocket.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		rocket.addChild(tg4);
		scene.addChild(rocket);

		BoundingSphere bounds = new BoundingSphere(new Point3d(0, 0, 0), 1000);
		Color3f white = new Color3f(1.0f, 1.0f, 1.0f);
		DirectionalLight light1 = new DirectionalLight(white, new Vector3f(-1.0f, -1.0f, -1.0f));
		light1.setInfluencingBounds(bounds);
		scene.addChild(light1);

		DirectionalLight light2 = new DirectionalLight(white, new Vector3f(1.0f, -1.0f, 1.0f));
		light2.setInfluencingBounds(bounds);
		scene.addChild(light2);

		return rocket;
	}

	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				new Visualizer();
			}
		});
	}

}
