package visualizer;

import javax.swing.*;

public class Visualizer extends JFrame
{
	public Visualizer()
	{
		super("PSAS Rocket Visualizer");
		RocketPanel panel = new RocketPanel();
		add(panel);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
		setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
		panel.moveModel();
	}

	public static void main(String[] args)
	{
		new Visualizer();
	}

}
