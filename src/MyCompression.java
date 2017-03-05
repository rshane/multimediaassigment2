import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

import javax.swing.JFrame;
import javax.swing.JPanel;

public class MyCompression {
	int imageWidth = 352;
	int imageHeight = 288;
	
	public class codebookVector{
		int xCoordinate;
		int yCoordinate;
	}
	public class Pixel{
		int color;
		codebookVector nrstCV = null;
	}
	public codebookVector[] intializeCodebook(int n, boolean isGrayScale) {
		codebookVector[] codebooks = new codebookVector[n];
		int boundary, colorDomain;
		for(int i=0; i< codebooks.length; i++) {
			codebooks[i] = new codebookVector();
		}
		if(isGrayScale) {
			colorDomain = (int) Math.pow(2, 8);

		}else{
			colorDomain = (int) Math.pow(2, 24);
		}
		int spaceInterval = (int) (colorDomain/Math.sqrt(n));
		int origXpoint = (int) ((colorDomain/Math.sqrt(n)) * .5), xpoint= origXpoint, ypoint=xpoint;
		int col = 0;
		codebookVector codebook;
		for(int i=0; i< codebooks.length; i++) {
			codebook = codebooks[i];
			codebook.xCoordinate = xpoint;
			codebook.yCoordinate = ypoint + spaceInterval * col;
			if(xpoint + spaceInterval > 255) {
				xpoint = origXpoint;	
				col++;
			} else {
				xpoint += spaceInterval;
			}
		}
		return codebooks;
	}
	
	public Pixel[] file2vectorPxls(String filename) {
		BufferedImage image = file2Img(filename);
		String filetype = filename.substring(filename.length() -3);
		boolean isGrayScale = false;
		if(filetype.equals("raw")) {
			isGrayScale = true;
		}
		Pixel[] imageVP = new Pixel[imageWidth*imageHeight];
		Pixel pxl;
		int i =0;
		if(isGrayScale) {
			for(int y=0; y < imageHeight; y++) {
				for(int x=0; x< imageWidth; x++) {
					pxl = new Pixel();
					pxl.color = 0x000000ff & image.getRGB(x, y);
					imageVP[i] = pxl; 
					i++;
				}
			}
			
		} else {
			for(int y=0; y < imageHeight; y++) {
				for(int x=0; x< imageWidth; x++) {
					pxl = new Pixel();
					pxl.color = 0x00ffffff & image.getRGB(x, y);
					imageVP[i] = pxl; 
					i++;
				}
			}
		}
		return imageVP;
	}
	
	public codebookVector closest(Pixel pxl1, Pixel pxl2, codebookVector[] cvArr) {
		codebookVector cv, minCV=null;
		double minDst = Double.MAX_VALUE;
		for(int i=0; i < cvArr.length; i++) {
			cv = cvArr[i];
			//distance(vector(a,b), codename(x,y)) where a = color of px1 and b= color of px2
			//square-root of (x-a)^2 + (y-b)^2
			double value1 = (cv.xCoordinate - pxl1.color)*(cv.xCoordinate - pxl1.color);
			double value2 = (cv.yCoordinate - pxl2.color)*(cv.yCoordinate - pxl2.color);
			double dst = Math.sqrt(value1+value2);
			if (dst < minDst) {
				minDst = dst;
				minCV = cv;
			}
		}
		return minCV;
	}
	public codebookVector averagingNrstPxls(ArrayList<Pixel> pxlList, codebookVector cv) {
		int x = 0, y=0, avgX, avgY;
		Pixel pxl1, pxl2;
		java.util.Iterator<Pixel> itr = pxlList.iterator();
		while(itr.hasNext()){
			pxl1 = itr.next();
			pxl2 = itr.next();
			x += pxl1.color;
			y += pxl2.color;
		}
		if(pxlList.size() != 0) {
			int pxlListSize = pxlList.size()/2; //each pair of pixels should be counted as 1 pixel vector
			avgX = x/pxlListSize;
			avgY = y/pxlListSize;
			cv.xCoordinate = avgX;
			cv.yCoordinate =avgY;
		}
		return cv;
	}
	public codebookVector[] kMeanClustering(int n, String filename) {
		codebookVector cv;
		Pixel pxl1, pxl2;
		Pixel[] imageVP = file2vectorPxls(filename);
		String filetype = filename.substring(filename.length() -3);
		boolean isGrayScale = false;
		if(filetype.equals("raw")) {
			isGrayScale = true;
		}
		codebookVector[] codebook = intializeCodebook(n, isGrayScale);
		HashMap<codebookVector, ArrayList<Pixel>> clusters = new HashMap<codebookVector, ArrayList<Pixel>>();
		ArrayList<Pixel> pxlList = new ArrayList<Pixel>(), nrstPxls= new ArrayList<Pixel>() ;	
		for(int k = 0; k < 200; k++) {
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
			for(int i=0; i<codebook.length; i++) {
				cv = codebook[i];
				nrstPxls = clusters.get(cv);
				cv = averagingNrstPxls(nrstPxls, cv);
				codebook[i] = cv;
			}
			pxlList.clear();
			nrstPxls.clear();
			clusters.clear();
		}
		
		return codebook;
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
	public void displayImg(BufferedImage inputImg, int width, int height) {
		JFrame frame = new JFrame();
		//when click x button frame closes
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//GridBagLayout places components in a grid of rows and columns
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
		String result = String.format("Video height: %d, width: %d", height, width);
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
		String filetype = filename.substring(filename.length() -3);
		File inputFile = new File(filename);
		byte[] inputBytes = RGBFile2Bytes(inputFile, imageWidth,imageHeight);
		BufferedImage inputImage;
		if(filetype.equals("raw")) {
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
	}	public static void main(String[] args) {
		//image size is 352x288
		MyCompression test = new MyCompression();
		String testFile = "/Users/shane/Documents/workspace/MyCompression/images/image1.raw";
		test.kMeanClustering(16, testFile);
	}
	
}
