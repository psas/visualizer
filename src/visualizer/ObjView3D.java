package visualizer;

import javax.swing.*;
import java.awt.*;

public class ObjView3D extends JFrame
{
	public ObjView3D()
	{
		super("ObjView3D");
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		WrapObjView3D w3d = new WrapObjView3D();
		c.add(w3d, BorderLayout.CENTER);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setResizable(false);
		setVisible(true);
		w3d.moveModel();
	}

	public static void main(String[] args)
	{
		new ObjView3D();
	}

}
