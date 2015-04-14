package view;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;

import model.Body;
import model.Collision;

public class CollisionGUI extends JFrame {

	private JPanel drawPanel;
	private Collision collision;
	private Body[] bodies;
	private CircleBody[] circles;
	public OptionGUI option;
	
	protected int ANIMATIONTIME = 100;
	private int SIZE = 750;
	
	public CollisionGUI( int numBodies, Collision col, String[] args) {
		this.collision = col;
		layoutGUI();
		option = new OptionGUI(this);
	}
	
	private void layoutGUI() {
		setTitle("2D Collisions");
	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    setBackground(Color.BLUE);
	    
	    drawPanel = new JPanel();
	    
	    drawPanel.setLayout(null);
	    
	    bodies = collision.getBodies();
	    
	    double xCoord = 100;
        double yCoord = 100;

        circles = new CircleBody[bodies.length];
        
	    for(int i = 0; i < bodies.length; i++)
	    {
	    	xCoord = bodies[i].getXPos()*10 + SIZE/2;
	    	yCoord = bodies[i].getYPos()*10 + SIZE/2;
	    	circles[i] = new CircleBody(bodies[i].getRadius() * 10);
	        circles[i].setLocation((int)xCoord,(int)yCoord);
	        circles[i].setSize(circles[i].getPreferredSize());
	        drawPanel.add(circles[i]);
	    }

        drawPanel.repaint();

        this.add(drawPanel);
		setMinimumSize(new Dimension(SIZE, SIZE));
		setVisible(true);
	}
	
	public void updateCircles() {
		if(ANIMATIONTIME > 0)
		{
			try {
				Thread.sleep(ANIMATIONTIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	    double xCoord = 100;
        double yCoord = 100;
        
        drawPanel.removeAll();
        drawPanel.revalidate();
        
	    for(int i = 0; i < bodies.length; i++)
	    {
	    	xCoord = bodies[i].getXPos()*10 + SIZE/2;
	    	yCoord = bodies[i].getYPos()*10 + SIZE/2;
	    	circles[i] = new CircleBody(bodies[i].getRadius() * 22);
	        circles[i].setLocation((int)xCoord,(int)yCoord);
	        circles[i].setSize(circles[i].getPreferredSize());
	        drawPanel.add(circles[i]);
	    }

        drawPanel.repaint();
	}
	
	public void updateAnimation( int time ) {
		ANIMATIONTIME = time;
	}
	
}
