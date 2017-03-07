import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;



import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class MyCompression {
	int imageWidth = 352;
	int imageHeight = 288;
	BufferedImage origImage = null;
	boolean isGrayScale = false;
	Pixel[] redPxls = null, greenPxls = null, bluePxls =null;
	
	public class codebookVector{
		int xCoordinate;
		int yCoordinate;
	}
	public class codebookVectorRGB{
		int[][] colors = new int[3][2];
		//[(R1,G1,B1),(R2,G2,B2)]
		public int getPxl1(String color, int value) {
			if(color == "r") {
				return colors[0][0];
			} else if(color == "g") {
				return colors[0][1];
			} else {
				return colors[0][2];
			}
		}
		public int getPxl2(String color, int value) {
			if(color == "r") {
				return colors[1][0];
			} else if(color == "g") {
				return colors[1][1];
			} else {
				return colors[1][2];
			}
		}
		public void setPxl1(String color, int value) {
			if(color == "r") {
				colors[0][0] = value;
			} else if(color == "g") {
				colors[0][1] = value;
			} else {
				colors[0][2] = value;
			}
		}
		public void setPxl2(String color, int value) {
			if(color == "r") {
				colors[1][0] = value;
			} else if(color == "g") {
				colors[1][1] = value;
			} else {
				colors[1][2] = value;
			}
		}
	}
	public class Pixel{
		protected int color;
		codebookVector nrstCV = null;
		public void setColor(boolean isGrayScale, int value) {
			if(isGrayScale){
				color = 0x000000ff & value;
			} else {
				color = 0x00ffffff & (0x00000000 | value);
			}
		}
		}
	
	public codebookVector[] intializeCodebook(int n, boolean isGrayScale) {
		codebookVector[] codebooks = new codebookVector[n];
		int boundary, colorDomain=0;
		for(int i=0; i< codebooks.length; i++) {
			codebooks[i] = new codebookVector();
		}
		colorDomain = (int) Math.pow(2, 8) - 1;
		int spaceInterval = (int) (colorDomain/Math.sqrt(n));
		int origXpoint = (int) ((colorDomain/Math.sqrt(n)) * .5), xpoint= origXpoint, ypoint=xpoint;
		int col = 0;
		codebookVector codebook;
		for(int i=0; i< codebooks.length; i++) {
			codebook = codebooks[i];
			codebook.xCoordinate = xpoint;
			codebook.yCoordinate = ypoint + spaceInterval * col;
			if(xpoint + spaceInterval > colorDomain) {
				xpoint = origXpoint;	
				col++;
			} else {
				xpoint += spaceInterval;
			}
		}
		return codebooks;
	}
	
	public codebookVectorRGB[] intializeCodebookRGB(int n, Pixel[] imageVP) {
		codebookVectorRGB[] codebooks = new codebookVectorRGB[n];
		int randomNum,r,g,b;
		int pxl1, pxl2;
		for(int i=0; i< codebooks.length; i++) {
			randomNum = ThreadLocalRandom.current().nextInt(imageVP.length);
			if((randomNum % 2) != 0) {
				if(randomNum + 1 == (imageWidth*imageHeight)) {
					randomNum = randomNum - 1;
				}else {
					randomNum =randomNum + 1;
					}
			}
			codebookVectorRGB cv = new codebookVectorRGB();
			pxl1 = imageVP[randomNum].color;
			pxl2 = imageVP[randomNum + 1].color;
			
			r = pxl1 & 0x00ff0000 >> 16;
			cv.setPxl1("r", r);
			g = pxl1 & 0x0000ff00 >> 8;
			cv.setPxl1("g", g);
			b = pxl1 & 0x000000ff;
			cv.setPxl1("b", b);
			
			r = pxl2 & 0x00ff0000 >> 16;
			cv.setPxl2("r", r);
			g = pxl2 & 0x0000ff00 >> 8;
			cv.setPxl2("g", g);
			b = pxl2 & 0x000000ff;
			cv.setPxl2("b", b);
			codebooks[i] = cv;
		}
		return codebooks;
	}
	
	public Pixel[] file2vectorPxls(String filename) {
		BufferedImage image = file2Img(filename);
		origImage = image;
		Pixel[] imageVP = new Pixel[imageWidth*imageHeight];
		Pixel pxl;
		int i =0;
			for(int y=0; y < imageHeight; y++) {
				for(int x=0; x< imageWidth; x++) {
					pxl = new Pixel();
					pxl.setColor(isGrayScale, image.getRGB(x, y));
					imageVP[i] = pxl; 
					i++;
				}
			}
		return imageVP;
	}
	
	public double distance(int x1, int x2, int y1, int y2){
		//square-root of (x-a)^2 + (y-b)^2
		double value1 = Math.pow(x1 - x2, 2);
		double value2 = Math.pow(y1 - y2, 2);
		double dst = Math.sqrt(value1+value2);
		return dst;
	}
	
	public codebookVector closest(Pixel pxl1, Pixel pxl2, codebookVector[] cvArr) {
		codebookVector cv, minCV=null;
		double minDst = Double.MAX_VALUE;
		for(int i=0; i < cvArr.length; i++) {
			cv = cvArr[i];
			//distance(vector(a,b), codename(x,y)) where a = color of px1 and b= color of px2
			//square-root of (x-a)^2 + (y-b)^2
			double value1 = Math.pow(cv.xCoordinate - pxl1.color, 2);
			double value2 = Math.pow(cv.yCoordinate - pxl2.color, 2);
			double dst = Math.sqrt(value1+value2);
			if (dst < minDst) {
				minDst = dst;
				minCV = cv;
			}
		}
		return minCV;
	}
	public codebookVectorRGB closestRGB(Pixel pxl1, Pixel pxl2, codebookVectorRGB[] cvArr) {
		codebookVectorRGB cv, minCV=null;
		int rCV, gCV, bCV;
		double minDst = Double.MAX_VALUE;
		for(int i=0; i < cvArr.length; i++) {
			cv = cvArr[i];
			//distance(vector(a,b), codename(x,y)) where a = color of px1 and b= color of px2
			//square-root of (x-a)^2 + (y-b)^2
		}
		return minCV;
	}
	
	public codebookVector averagingNrstPxls(ArrayList<Pixel> pxlList, codebookVector cv) {
		int avgX, avgY;
		double x =0, y=0;
		Pixel pxl1, pxl2;
		java.util.Iterator<Pixel> itr = pxlList.iterator();
		int pxlListSize = pxlList.size()/2; //each pair of pixels should be counted as 1 pixel vector
		while(itr.hasNext()){
			pxl1 = itr.next();
			pxl2 = itr.next();
			x += pxl1.color;
			y += pxl2.color;
		}
		if(pxlList.size() != 0) {
			avgX = (int) x/pxlListSize;
			avgY = (int) y/pxlListSize;
			cv.xCoordinate = avgX;
			cv.yCoordinate =avgY;
		}
		return cv;
	}
	
	public codebookVector[] kMeanClustering(Pixel[] imageVP, int n) {
		codebookVector cv, newCV, oldCV = new codebookVector();
		Pixel pxl1, pxl2;
		codebookVector[] codebook = intializeCodebook(n, isGrayScale);
		double error = 2;
		HashMap<codebookVector, ArrayList<Pixel>> clusters = new HashMap<codebookVector, ArrayList<Pixel>>();
  		ArrayList<Pixel> pxlList = new ArrayList<Pixel>(), nrstPxls= new ArrayList<Pixel>() ;	
		while(error >= 1){
			for(int i =0; i< codebook.length; i++) {
				pxlList = new ArrayList<Pixel>();
				clusters.put(codebook[i], pxlList);
			}
			for(int i = 0; i<imageVP.length; i=i+2) {
				pxl1 = imageVP[i];
				pxl2 = imageVP[i+1];
				cv = closest(pxl1, pxl2, codebook);
				nrstPxls = clusters.get(cv);
				pxl1.nrstCV = cv;
				pxl2.nrstCV = cv;
				nrstPxls.add(pxl1);
				nrstPxls.add(pxl2);
				clusters.put(cv, nrstPxls);
			}
			error = 0;
			for(int i=0; i<codebook.length; i++) {
				cv = codebook[i];
				oldCV.xCoordinate = cv.xCoordinate;
				oldCV.yCoordinate = cv.yCoordinate;
				nrstPxls = clusters.get(cv);
				newCV = averagingNrstPxls(nrstPxls, cv);
				error += distance(oldCV.xCoordinate, newCV.xCoordinate, oldCV.yCoordinate, newCV.yCoordinate);
				codebook[i] = newCV;
	
			}
			pxlList.clear();
			nrstPxls.clear();
			clusters.clear();
		}
		return codebook;
	}
	public int grayscale2RGB(int value) {
		return 0x00000000 | ((value & 0xff) << 16) | ((value & 0xff) << 8) | (value & 0xff);
	}
	public Pixel[] rgbBytes2imageVP(String filename) {
		int width = imageWidth, height =imageHeight;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		File inputFile = new File(filename);

		Pixel[] imageVP = new Pixel[imageWidth*imageHeight*3];
		byte[] inputBytes = RGBFile2Bytes(inputFile, imageWidth, imageHeight);
		int ind = width*height, i =0;
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){	
				byte r = inputBytes[x + y*width];
				byte g = inputBytes[(x + y*width)+height*width];
				byte b = inputBytes[(x + y*width)+height*width*2]; 
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				//int pix = ((a << 24) + (r << 16) + (g << 8) + b); bit shifting
				img.setRGB(x,y,pix);
				Pixel pxlR = new Pixel(), pxlG = new Pixel(), pxlB = new Pixel() ;
				pxlR.color = r & 0xff;
				pxlG.color = g & 0xff;
				pxlB.color = b & 0xff;
				imageVP[x + y*width] = pxlR;
				imageVP[(x + y*width) + height*width] = pxlG;
				imageVP[(x + y*width) + 2*height*width] = pxlB;
				//redPxls[i] = pxlR;
				//greenPxls[i] = pxlG;
				//bluePxls[i] = pxlB;
			}
		}
		origImage = img;
		return imageVP;
	}
	public void compress(String filename, int n) {
		String filetype = filename.substring(filename.length() -3);
		Pixel[] imageVP = null;
		if(filetype.equals("raw")) {
			isGrayScale = true;
		}
		if(isGrayScale) {
			imageVP= file2vectorPxls(filename);
		}else {
			imageVP= rgbBytes2imageVP(filename);
		}
		codebookVector[] codebook = kMeanClustering(imageVP, n);
		BufferedImage decompressedImg = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);;
		Pixel pxl1 = new Pixel(), pxl2 = new Pixel();
		Pixel pxl1R = new Pixel(), pxl1G = new Pixel(), pxl1B = new Pixel();
		Pixel pxl2R = new Pixel(), pxl2G = new Pixel(), pxl2B = new Pixel();
		int color1, color2, r1, r2, g1, g2, b1, b2;
		codebookVector cv, cvR, cvG, cvB;
		if(isGrayScale) {
			for(int y =0; y < imageHeight; y++) {
				for (int x=0; x < imageWidth; x=x+2) {
					pxl1.setColor(isGrayScale, origImage.getRGB(x, y));
					pxl2.setColor(isGrayScale, origImage.getRGB(x + 1, y));
					cv = closest(pxl1, pxl2, codebook);
					color1 = grayscale2RGB(cv.xCoordinate);
					color2 = grayscale2RGB(cv.yCoordinate);
					decompressedImg.setRGB(x, y, color1);
					decompressedImg.setRGB(x+1, y, color2);
				}
			}
		}else {
			for(int y =0; y < imageHeight; y++) {
				for (int x=0; x < imageWidth; x=x+2) {
					pxl1.setColor(isGrayScale, origImage.getRGB(x, y));
					pxl2.setColor(isGrayScale, origImage.getRGB(x + 1, y));
					pxl1R.setColor(isGrayScale, (pxl1.color & 0x00ff0000) >> 16);
					pxl2R.setColor(isGrayScale, (pxl2.color & 0x00ff0000) >> 16);
					pxl1G.setColor(isGrayScale, (pxl1.color & 0x0000ff00) >> 8);
					pxl2G.setColor(isGrayScale, (pxl2.color & 0x0000ff00) >> 8);
					pxl1B.setColor(isGrayScale, (pxl1.color & 0x000000ff));
					pxl2B.setColor(isGrayScale, (pxl2.color & 0x000000ff));
					cvR = closest(pxl1R, pxl2R, codebook);
					cvG = closest(pxl1G, pxl2G, codebook);
					cvB = closest(pxl1B, pxl2B, codebook);
					color1 = 0xff000000 | (cvR.xCoordinate << 16) | (cvG.xCoordinate <<8) | cvB.xCoordinate;
					color2 = 0xff000000 | (cvR.yCoordinate << 16) | (cvG.yCoordinate <<8) | cvB.yCoordinate;
					decompressedImg.setRGB(x, y, color1);
					decompressedImg.setRGB(x+1, y, color2);
				}
			}
			}
		displayImg(origImage, imageWidth, imageHeight, false);
		displayImg(decompressedImg, imageWidth, imageHeight, true);
		
	}

	public byte[] RGBFile2Bytes(File file, int width, int height) {
		byte[] bytes = null;
		try {
			//file only contains RGB no alpha
			InputStream is = new FileInputStream(file);
			long len = file.length();
			bytes = new byte[(int)len];

			//read the whole file into  to temp buffer called bytes
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
				//is(byte[], off, len) reads up to len bytes from is, attempt to read len bytes but smaller amount may be read
				//return number of bytes read as int, offset tells b[off] through b[off+k-1] where k is amount read
				offset += numRead;
			}
			is.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bytes;
	}
	public BufferedImage bytes2IMG(boolean isRaw, int width, int height, long totalFrames, byte[] bytes) {
		BufferedImage[] allFramesAsImages = new BufferedImage[(int)totalFrames];
		if(isRaw) {
			for(int frameIndex = 0; frameIndex < totalFrames; frameIndex++){
				BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				//ind contains where frameNumber is located in bytes array	
				int ind = width*height*frameIndex;
				for(int y = 0; y < height; y++){
					for(int x = 0; x < width; x++){			
						byte a = 0;
						byte r = bytes[ind];
						int pix = 0x00000000 | ((r & 0xff) << 16) | ((r & 0xff) << 8) | (r & 0xff);
						//int pix = ((a << 24) + (r << 16) + (g << 8) + b); bit shifting
						//since only black and white either FFFFFF (white) or 000000 (black)
						img.setRGB(x,y,pix);
						ind++;
					}
				}
				allFramesAsImages[frameIndex] = img;
				img.flush();
			}
		} else {
			for(int frameIndex = 0; frameIndex < totalFrames; frameIndex++){
				BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				//ind contains where frameNumber is located in bytes array	
				int ind = width*height*frameIndex*3;
				for(int y = 0; y < height; y++){
					for(int x = 0; x < width; x++){			
						byte a = 0;
						byte r = bytes[ind];
						byte g = bytes[ind+height*width];
						byte b = bytes[ind+height*width*2]; 
						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						//int pix = ((a << 24) + (r << 16) + (g << 8) + b); bit shifting
						img.setRGB(x,y,pix);
						ind++;
						}
					}
				allFramesAsImages[frameIndex] = img;
				img.flush();
			}
		}
		return allFramesAsImages[0];

	}
	public void displayImg(BufferedImage inputImg, int width, int height, boolean isSideBySide) {
		JFrame frame = new JFrame();
		String result;
		//frame.setLocation(dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);
		if(isSideBySide) {
			frame.setLocation(frame.getSize().width + imageWidth+10, frame.getSize().height);
			 result = String.format("Output Image");
		} else {
			frame.setLocation(frame.getSize().width+5, frame.getSize().height);
			 result = String.format("Original Image");
		}
		//when click x button frame closes
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//GridBagLayout places components in a grid of rows and columns
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
		
		JLabel lbText1 = new JLabel(result);
		lbText1.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel lbIm1 = new JLabel();
		
		GridBagConstraints c = new GridBagConstraints();
		//Stretches frame horizontally
		c.fill = GridBagConstraints.HORIZONTAL; //Resize the component horizontally but not vertically
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5; //Specifies how to distribute extra horizontal space
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(lbText1, c);
		lbIm1.setIcon(new ImageIcon(inputImg));
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);
			
		frame.pack();

		frame.setVisible(true);
		inputImg.flush();		
	}
	public BufferedImage file2Img(String filename){
		File inputFile = new File(filename);
		byte[] inputBytes = RGBFile2Bytes(inputFile, imageWidth,imageHeight);
		BufferedImage inputImage;
		if(isGrayScale) {
			inputImage = bytes2IMG(true, imageWidth, imageHeight, 1, inputBytes); 
		} else{
			inputImage = bytes2IMG(false, imageWidth, imageHeight, 1, inputBytes);
		}
		return inputImage;
	}
	public int[] img2PxlVectors(BufferedImage image){
		int[] vectorPair = new int[imageWidth*imageHeight];
		int rows = 0;
		for(int y=0; y<imageHeight; y++) {
			for(int x=0; x<imageWidth; x++){
				vectorPair[x+y*imageWidth]=image.getRGB(x, y);
			}
		}
		return vectorPair;
	}
	public static void main(String[] args) {
		//image size is 352x288
		
		MyCompression comp = new MyCompression();
		String inputFile = args[0];
		String number = args[1];
		int n = Integer.parseInt(number);
		comp.compress(inputFile, n);
	}
	
}
