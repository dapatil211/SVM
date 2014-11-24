import acm.graphics.GLine;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.program.GraphicsProgram;
import Jama.Matrix;


public class test extends GraphicsProgram{

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new test().start();

	}
	@Override
	public void init() {
		GPolygon p = new GPolygon(100, 100);
		p.addPolarEdge(25, 120);
		p.addPolarEdge(25, 240);
		p.addPolarEdge(25, 0);
		add(new GLine(100, 100, 25, 25));
		add(p);
	}

}
