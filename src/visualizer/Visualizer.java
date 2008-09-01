package visualizer;

import javax.swing.*;

public class Visualizer extends JFrame
{
	public Visualizer()
	{
		super("PSAS Rocket Visualizer");
		add(new RocketPanel());

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
		setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
	}

	public static void main(String[] args)
	{
		new Visualizer();
	}

}
