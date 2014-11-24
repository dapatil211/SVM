import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JOptionPane;

import acm.graphics.GLabel;
import acm.graphics.GLine;
import acm.graphics.GOval;
import acm.graphics.GPolygon;
import acm.graphics.GRect;
import acm.program.GraphicsProgram;
import Jama.Matrix;


public class MultiSVM extends GraphicsProgram{

	/**
	 * @param args
	 */
	public static final int APPLICATION_WIDTH = 1000;
	public static final int APPLICATION_HEIGHT = 620;
	public static final double OFFSET = 50;
	public static final double STEP1 = 50;
	public static final double STEP_LABEL = 5;
	public static final double AXIS_LENGTH = 500;
	public static final Integer[] MENU_CHOICES = {1, 2, 3};
	public static final double OBJECT_RADIUS = 5;
	private static final int MAX_PASSES = 10;
	private static final int MAX_RUNS = 15000;
	private static final double TOL = .01;
	private static final int MIN_EXAMPLES = 200;
	private static final String[] KERNEL_CHOICES = {"RADIAL", "LINEAR", "POLYNOMIAL"};
	private static int degree = 0;
	private static int kernel;
	private static double C;
	private static double sig;
	int currentType = 0;
	public static ArrayList<Matrix> xVals = new ArrayList<Matrix>();
	public static ArrayList<Integer> yVals = new ArrayList<Integer>();
	public double b;
	ArrayList<Integer> tempY = new ArrayList<Integer>();
	ArrayList<Matrix> tempX = new ArrayList<Matrix>();
	private int numTypes = 2;
	
	public static void main(String[] args) {
		new MultiSVM().start(args);
//		double[] x = {1, 1, 1, 1};
//		double[] y = {1, 1, 1, 1};
//		System.out.println(kernel(new Matrix(x, 4), new Matrix(y, 4)));
	}
	
	@Override
	public void init() {
		addMouseListeners();
		createAxis(OFFSET, OFFSET, OFFSET, OFFSET + AXIS_LENGTH, STEP1, 5);
		createAxis(OFFSET, OFFSET + AXIS_LENGTH, OFFSET + AXIS_LENGTH, OFFSET + AXIS_LENGTH, STEP1, 5);
		JComboBox<Integer> menu = new JComboBox<Integer>(MENU_CHOICES);
		menu.setActionCommand("MENU");
		menu.addActionListener(this);
		add(menu, WEST);
		JComboBox<String> kernelType = new JComboBox<String>(KERNEL_CHOICES);
		kernelType.setActionCommand("KERNEL");
		kernelType.addActionListener(this);
		add(kernelType, WEST);
		JButton learn = new JButton("LEARN");
		add(learn, WEST);
		learn.addActionListener(this);
		learn.setActionCommand("LEARN");
		addMouseListener(this);
	}
	
	private void createAxis(double x1, double y1, double x2, double y2, double step, int stepLabel) {
		GLine axis1 = new GLine(x1, y1, x2, y2);
		add(axis1);
		if(y1 == y2){
			for(double i = x1 + step; i < x2; i += step){
				add(new GLine(i, y2 - step / 10, i, y2 + step / 10));
				add(new GLabel((stepLabel * (i - x1) / step) + "", i - 3, y2 + step / 3));
			}
			
		}
		else{	// vertical ticks
			for(double i = y1 + step; i < y2; i += step){
				add(new GLine(x2 - step / 10, i, x2 + step / 10, i));
				add(new GLabel((stepLabel * (y2 - i) / step) + "", x2 - 30 , i + 5));
			}
		}
		
	}

	@Override
	public void run() {
		
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getX() > OFFSET && e.getX() < OFFSET + AXIS_LENGTH && e.getY() > OFFSET && e.getY() < OFFSET + AXIS_LENGTH){
//			JOptionPane.showMessageDialog(this, "" + e.getX() + ", " + e.getY());
			if(currentType == 0){
				GOval o = new GOval(e.getX() - OBJECT_RADIUS, e.getY() - OBJECT_RADIUS, 2 * OBJECT_RADIUS, 2 * OBJECT_RADIUS);
				o.setFillColor(Color.RED);
				o.setFilled(true);
				add(o);
			}
			else if(currentType == 1){
				GRect r = new GRect(e.getX() - OBJECT_RADIUS, e.getY() - OBJECT_RADIUS, 2 * OBJECT_RADIUS, 2 * OBJECT_RADIUS);
				r.setFillColor(Color.BLUE);
				r.setFilled(true);
				add(r);
			}
			else if(currentType == 2){
				GPolygon p = new GPolygon(e.getX(), e.getY());
				p.addVertex(0, -4 * OBJECT_RADIUS / 3);
				p.addVertex(- Math.sqrt(3) * 2 * OBJECT_RADIUS / 3, 2 * OBJECT_RADIUS / 3);
				p.addVertex(Math.sqrt(3) * 2 * OBJECT_RADIUS / 3, 2 * OBJECT_RADIUS / 3);
				p.setFillColor(Color.GREEN);
				p.setFilled(true);
				add(p);
				numTypes = 3;
			}
			double[] x = {(e.getX() - OFFSET) * STEP_LABEL / STEP1, (AXIS_LENGTH + OFFSET - e.getY()) * STEP_LABEL / STEP1};
			xVals.add(new Matrix(x, x.length));
			yVals.add(currentType);
		}
	}
	
	private static final Color RED_FIELD = Color.RED.brighter().brighter(); 
	private static final Color BLUE_FIELD = Color.BLUE.brighter().brighter();
	private static final Color GREEN_FIELD = Color.GREEN.brighter().brighter();
	private static final double R = 0;
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("MENU")){
			JComboBox cb = (JComboBox)e.getSource();
			currentType = (int) cb.getSelectedItem() - 1;
		}
		else if(e.getActionCommand().equals("LEARN")){
			ArrayList<Matrix> alphaMatrix = new ArrayList<Matrix>();
			ArrayList<Double> CMatrix = new ArrayList<Double>();
			ArrayList<Double> sigMatrix = new ArrayList<Double>();
			for(int i = 0; i < numTypes; i++){
				sigMatrix.add(0.);
			}
			ArrayList<Double> bMatrix = new ArrayList<Double>();
			ArrayList<ArrayList<Integer>> yMatrix = new ArrayList<ArrayList<Integer>>();
			for(int i = 0; i < numTypes ; i++){
				tempY = transform(yVals, i);
				tempX = new ArrayList<Matrix>(xVals);
				scatterShot(tempX, tempY);
				if(kernel != 1){
					sig = jaakkola(tempX, tempY);
				}
				calibrateC(tempX, tempY);
				CMatrix.add(C);
				System.out.println("done CalC");
				if(kernel != 1){
					sig = calibrateSig(tempX, tempY);
					sigMatrix.set(i, sig);
				}
				ArrayList<Integer> ys = transform(yVals, i);
				Matrix alpha = SMO(C, TOL, MAX_PASSES, xVals, ys, sig);
				alphaMatrix.add(alpha);
				bMatrix.add(b);
				yMatrix.add(ys);
			}
//			tempY = transform(yVals, 1);
//			tempX = new ArrayList<Matrix>(xVals);
//			scatterShot(tempX, tempY);
//			if(kernel != 1){
//				sig = jaakkola(tempX, tempY);
//			}
//			calibrateC(tempX, tempY);
//			System.out.println("done CalC");
//			if(kernel != 1){
//				sig = calibrateSig(tempX, tempY);
//			}
//			ArrayList<Integer> ys = transform(yVals, 1);
//			Matrix alpha = SMO(C, TOL, MAX_PASSES, xVals, ys, sig);
			System.out.println("Done Alpha");
			for(double i = OFFSET; i < OFFSET + AXIS_LENGTH; i+= 3){
				int start = 0;
//				Color current = Color.BLUE;
				for(double j = OFFSET; j < OFFSET + AXIS_LENGTH; j+= 3){
//					System.out.println("looping");
					double[] x = {(i - OFFSET) * STEP_LABEL / STEP1, (AXIS_LENGTH + OFFSET - j) * STEP_LABEL / STEP1};
					double vals[] = new double[numTypes];
					for(int k = 0; k < numTypes; k++){
						double val = f(alphaMatrix.get(k), yMatrix.get(k), xVals, new Matrix(x, x.length), bMatrix.get(k), sigMatrix.get(k));
						vals[k] = val;
					}
					int index = getMaxIndex(vals);
					GRect r = new GRect(i, j, 3, 3);
					if(index == 1){
						r.setColor(BLUE_FIELD);
					}
					else if(index == 0){
						r.setColor(RED_FIELD);
					}
					else{
						r.setColor(GREEN_FIELD);
					}
					add(r);
					r.sendToBack();

//					if(val > 0 && !current.equals(Color.BLUE)){
//						GRect r = new GRect(i, start, 1, j - start);
//						r.setColor(current);
//						r.sendToBack();
//						add(r);
//						start = j;
//						current = Color.BLUE;
//					}
//					else if(val < 0 && !current.equals(Color.RED)){
//						GRect r = new GRect(i, start, 1, j - start);
//						r.setColor(current);
//						r.sendToBack();
//						add(r);
//						start = j;
//						current = Color.RED;
//					}
//					else if(val == 0 && !current.equals(Color.BLACK)){
//						GRect r = new GRect(i, start, 1, j - start);
//						r.setColor(current);
//						r.sendToBack();
//						add(r);
//						start = j;
//						current = Color.BLACK;
//					}
				}
			}
			System.out.println("done");
		}
		else if(e.getActionCommand().equals("KERNEL")){
			JComboBox cb = (JComboBox)e.getSource();
			if(cb.getSelectedItem().equals("RADIAL")){
				kernel = 0;
				degree = 0;
			}
			else if(cb.getSelectedItem().equals("LINEAR")){
				kernel = 1;
				degree = 0;
			}
			else{
				kernel = 2;
				degree = Integer.parseInt(JOptionPane.showInputDialog("Select a degree"));
			}		
		}
	}
	
	private int getMaxIndex(double[] vals) {
		int ind = 0;
		double maxVal = vals[0];
		for(int i = 1; i < vals.length; i++){
			if(vals[i] > maxVal){
				maxVal = vals[i];
				ind = i;
			}
		}
		return ind;
	}

	private double calibrateSig(ArrayList<Matrix> calX, ArrayList<Integer> calY) {
		Random rand = new Random();
		ArrayList<Matrix> newCalx = new ArrayList<Matrix>();
		ArrayList<Integer> newCaly = new ArrayList<Integer>();
		int size = calX.size();
		for(int i = 0; i < size; i++){
			int ind = (int) (Math.random() * calX.size());
			newCalx.add(calX.remove(ind));
			newCaly.add(calY.remove(ind));
		}
		tempX = newCalx;
		tempY = newCaly;
		int perExample = newCaly.size() / 10;
		double bestSig = sigify(Math.pow(2, -15));
		double mostAccurate = 0;
		int bestPow = -15;
		double bestScore = 0;
		for(int i = -15; i <= 3; i+=2){
			double sig = sigify(Math.pow(2, i));
			double curScore = 0;
			int curAcc = 0;
			List<Integer> y = newCaly.subList((i + 15) / 2 * perExample, ((i + 15) / 2 + 1) * perExample); 
			List<Matrix> x = newCalx.subList((i + 15) / 2 * perExample, ((i + 15) / 2 + 1) * perExample);
			Matrix alpha = SMO(C, TOL, MAX_PASSES, x, y, sig);
			for(int j = 0; j < perExample; j++){
				double val = f(alpha, y, x, x.get(j), b, sig);
				if(val > 0 && y.get(j) == 1 || val < 0 && y.get(j) == -1){
					curAcc ++;
					curScore += Math.abs(val);
				}
			}
			if(curAcc > mostAccurate){
				mostAccurate = curAcc;
				bestSig = sig;
				bestPow = i;
				bestScore = curScore;
			}
			else if(curAcc == mostAccurate && curScore > bestScore){
				bestScore = curScore;
				bestSig = sig;
				bestPow = i;
			}
		}
		perExample = newCaly.size() / 17;
		for(int i = -8; i <= 8; i++){
			double sig = sigify(Math.pow(2, bestPow + i * .25));
			double curScore = 0;
			int curAcc = 0;
			List<Integer> y = newCaly.subList((i + 8) * perExample, (i + 9) * perExample); 
			List<Matrix> x = newCalx.subList((i + 8) * perExample, (i + 9) * perExample);
			Matrix alpha = SMO(C, TOL, MAX_PASSES, x, y, sig);
			for(int j = 0; j < perExample; j++){
				double val = f(alpha, y, x, x.get(j), b, sig);
				if(val > 0 && y.get(j) == 1 || val < 0 && y.get(j) == -1){
					curAcc ++;
					curScore += Math.abs(val);
				}
			}
			if(curAcc > mostAccurate){
				mostAccurate = curAcc;
				bestSig = sig;
				bestScore = curScore;
			}
			else if(curAcc == mostAccurate && curScore > bestScore){
				bestScore = curScore;
				bestSig = sig;
				bestPow = i;
			}
		}
		return bestSig;
	}

	private double sigify(double gamma) {
		double sigma = 1 / (Math.sqrt(2 * gamma));
		return sigma;
	}

	private void calibrateC(ArrayList<Matrix> calX, ArrayList<Integer> calY) {
		ArrayList<Matrix> newCalx = new ArrayList<Matrix>();
		ArrayList<Integer> newCaly = new ArrayList<Integer>();
		int size = calX.size();
		for(int i = 0; i < size; i++){
			int ind = (int) (Math.random() * calX.size());
			newCalx.add(calX.remove(ind));
			newCaly.add(calY.remove(ind));
		}
		tempX = newCalx;
		tempY = newCaly;
		int perExample = newCaly.size() / 11;
		double bestC = Math.pow(2, -5);
		double mostAccurate = 0;
		int bestPow = -5;
		double bestScore = 0;
		for(int i = -5; i <= 15; i+=2){
			double C = Math.pow(2, i);
			double curScore = 0;
			int curAcc = 0;
			List<Integer> y = newCaly.subList((i + 5) / 2 * perExample, ((i + 5) / 2 + 1) * perExample); 
			List<Matrix> x = newCalx.subList((i + 5) / 2 * perExample, ((i + 5) / 2 + 1) * perExample);
			Matrix alpha = SMO(C, TOL, MAX_PASSES, x, y, sig);
			for(int j = 0; j < perExample; j++){
				double val = f(alpha, y, x, x.get(j), b, sig);
				if(val > 0 && y.get(j) == 1 || val < 0 && y.get(j) == -1){
					curAcc ++;
					curScore += Math.abs(val);
				}
			}
			if(curAcc > mostAccurate){
				mostAccurate = curAcc;
				bestC = C;
				bestPow = i;
				bestScore = curScore;
			}
			else if(curAcc == mostAccurate && curScore > bestScore){
				bestScore = curScore;
				bestC = C;
				bestPow = i;
			}
		}
		perExample = newCaly.size() / 17;
		for(int i = -8; i <= 8; i++){
			double C = Math.pow(2, bestPow + i * .25);
			double curScore = 0;
			int curAcc = 0;
			List<Integer> y = newCaly.subList((i + 8) * perExample, (i + 9) * perExample); 
			List<Matrix> x = newCalx.subList((i + 8) * perExample, (i + 9) * perExample);
			Matrix alpha = SMO(C, TOL, MAX_PASSES, x, y, sig);
			for(int j = 0; j < perExample; j++){
				double val = f(alpha, y, x, x.get(j), b, sig);
				if(val > 0 && y.get(j) == 1 || val < 0 && y.get(j) == -1){
					curAcc ++;
					curScore += Math.abs(val);
				}
			}
			if(curAcc > mostAccurate){
				mostAccurate = curAcc;
				bestC = C;
				bestScore = curScore;
			}
			else if(curAcc == mostAccurate && curScore > bestScore){
				bestScore = curScore;
				bestC = C;
				bestPow = i;
			}
		}
		C = bestC;
	}

	private void scatterShot(ArrayList<Matrix> xVals, ArrayList<Integer> newy) {
		if(newy.size() > MIN_EXAMPLES){
			return;
		}
		int numScatter = MIN_EXAMPLES / newy.size();
		Random rand = new Random();
		int xSize = xVals.size();
		for(int i = 0; i < xSize; i++){	
			for(int j = 0; j < numScatter; j++){
				double[] newX = {rand.nextGaussian() + xVals.get(i).get(0, 0), rand.nextGaussian() + xVals.get(i).get(1, 0)};
				xVals.add(new Matrix(newX, newX.length));
				newy.add(newy.get(i));
//				GRect r = new GRect(newX[0] * STEP1 / STEP_LABEL + OFFSET, newX[1] * STEP1 / STEP_LABEL - AXIS_LENGTH - OFFSET, 10, 10);
//				r.setFillColor(newy.get(i) == 1 ? Color.RED : Color.BLUE);
//				add(r);
			}
		}
	}
	

	private ArrayList<Integer> transform(ArrayList<Integer> y, int val) {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		for(int i : y){
			if(i != val){
				ret.add(-1);
			}
			else{
				ret.add(1);
			}
		}
		return ret;
	}

	private Matrix SMO(double C, double tol, int maxPasses, List<Matrix> x, List<Integer> y, double gamma){
		Matrix alpha = new Matrix(x.size(), 1);
		double b = 0;
		int passes = 0;
		long p1 = 0;
		long p2 = 0; 
		long p3 = 0;
		long p4 = 0;
		long p0 = 0;
		long per1 = 0;
		long per2 = 0;
		long per3 = 0;
		long per4 = 0;
		long start = System.currentTimeMillis();
		int totPasses = 0;
		while(passes < maxPasses){
			int numChanged = 0;
			for(int i = 0; i < x.size(); i++){
				double e = error(i, alpha, x, y, b, gamma);
				if((y.get(i) * e < -tol && alpha.get(i, 0) < C) || (y.get(i) * e > tol && alpha.get(i, 0) > 0)){
//					System.out.println("Pass " + totPasses);
					p0 = System.currentTimeMillis();
					Random rand = new Random();
					int j;
					while((j = rand.nextInt(x.size())) == i);
					p1 = System.currentTimeMillis();
					per1 += p1 - p0;
					double e2 = error(j, alpha, x, y, b, gamma);
					p2 = System.currentTimeMillis();
					per2 += p2 - p1;
					double alpha_i = alpha.get(i, 0);
					double alpha_j = alpha.get(j, 0);
					double L, H;
					if(y.get(i) != y.get(j)){
						L = Math.max(0, alpha_j - alpha_i);
						H = Math.min(C, C + alpha_j - alpha_i);
					}
					else{
						L = Math.max(0, alpha_j + alpha_i - C);
						H = Math.min(C, alpha_i + alpha_j);
					}
					if(L == H){
						continue;
					}
					double eta = compEta(i, j, x, y, gamma);
					p3 = System.currentTimeMillis();
					per3 += p3 - p2;
					if(eta >= 0){
						continue;
					}
					double alphaJPrime = alpha_j - y.get(j) * (e - e2) / eta;
					if(alphaJPrime > H){
						alphaJPrime = H;
					}
					else if(alphaJPrime < L){
						alphaJPrime = L;
					}
					if(Math.abs(alpha_j - alphaJPrime) < 1e-5){
						continue;
					}
					double alphaIPrime = alpha_i + y.get(i) * y.get(j) * (alpha_j - alphaJPrime);
					alpha.set(i, 0, alphaIPrime);
					alpha.set(j, 0, alphaJPrime);
					double b1 = b - e - y.get(i) * (alphaIPrime - alpha_i) * kernel(x.get(i), x.get(i), gamma) - y.get(j) * (alphaJPrime - alpha_j) * kernel(x.get(i), x.get(j), gamma);
					double b2 = b - e2 - y.get(i) * (alphaIPrime - alpha_i) * kernel(x.get(i), x.get(j), gamma) - y.get(j) * (alphaJPrime - alpha_j) * kernel(x.get(j), x.get(j), gamma);
					p4 = System.currentTimeMillis();
					per4 += p4 - p3;
					if(alphaIPrime > 0 && alphaIPrime < C){
						b = b1;
					}
					else if(alphaJPrime > 0 && alphaJPrime < C){
						b = b2;
					}
					else{
						b = (b1 + b2) / 2;
					}
					numChanged++;
					totPasses ++;
				}
			}
			if(totPasses > MAX_RUNS){
				break;
			}
			if(numChanged == 0){
				passes ++;
			}
			else{
				passes = 0;
			}
		}
		System.out.println((System.currentTimeMillis() - start) / 1000.);
		System.out.println(per1/1000.);
		System.out.println(per2/1000.);
		System.out.println(per3/1000.);
		System.out.println(per4/1000.);
		this.b = b;
		return alpha;
	}

	private double compEta(int i, int j, List<Matrix> x, List<Integer> y, double gamma) {
		// TODO Auto-generated method stub
		double eta = 2 * kernel(x.get(i), x.get(j), gamma) - kernel(x.get(i), x.get(i), gamma) - kernel(x.get(j), x.get(j), gamma);
		return eta;
	}

	private double error(int ind, Matrix alpha, List<Matrix> x, List<Integer> y, double b, double gamma) {
		double val = f(alpha, y, x, x.get(ind), b, gamma);
		return val - y.get(ind);
	}
	private double f(Matrix alpha, List<Integer> y, List<Matrix> x2, Matrix x, double b, double gamma){
		double val = 0;
		for(int i = 0; i < alpha.getRowDimension(); i++){
			val += alpha.get(i, 0) * y.get(i) * kernel(x2.get(i), x, gamma);
		}
		return val  + b;
	}
	private static double kernel(Matrix x1, Matrix x2, double gamma) {
		if(kernel == 0){
			double val1 = x1.minus(x2).normF();
			double val2 = (2 * gamma * gamma);
//			double val3 = - Math.pow((x1.minus(x2).normF()), 2);
//			double val4 = 2 * Math.pow(gamma, 2);
			double ret = Math.exp(- val1 * val1 / val2);
//			double ret2 = Math.exp(val3/val4);
			return ret;
		}
		else if(kernel == 1){
			return x2.transpose().times(x1).get(0, 0);
		}
		else{
			return Math.pow(x1.transpose().times(x2).get(0, 0) * gamma + R, degree); 
		}
		
	}
	
	private static double jaakkola(ArrayList<Matrix> x, ArrayList<Integer> y){
		ArrayList<Matrix> set1 = new ArrayList<Matrix>();
		ArrayList<Matrix> set2 = new ArrayList<Matrix>();
		for(int i = 0; i < y.size(); i++){
			if(y.get(i) == 1){
				set1.add(x.get(i));
			}
			else{
				set2.add(x.get(i));
			}
		}
		ArrayList<Double> dist = new ArrayList<Double>();
		for(int i = 0; i < set1.size(); i++){
			for(int j = 0; j < set2.size(); j++){
				dist.add(distance(set1.get(i), set2.get(j)));
			}
		}
		Collections.sort(dist);
		return 1/ (2 * Math.pow(dist.get(dist.size()/2), 2));
	}

	private static double distance(Matrix matrix, Matrix matrix2) {
		double sum = 0;
		for(int i = 0; i < matrix.getRowDimension(); i++){
			sum += Math.pow((matrix.get(i, 0) - matrix2.get(i, 0)), 2);
		}
		return Math.sqrt(sum);
	}
}
