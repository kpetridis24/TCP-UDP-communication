
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.lang.System;
import java.math.BigInteger;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.sound.sampled.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;


public class userApplication {

	
	static int mean;
	static int step;

		
	static double AbsoluteDif(double a, double b) {   
		
		if (a > b) {
			return (a - b);     
		} else return (b - a);
	}
		
		
    public static void FileWrite(double[] b, String FileName) {
    	
        try {
		BufferedWriter response = new BufferedWriter(new FileWriter(
				"C:\\Users\\powerpc.gr\\Desktop\\session02\\" + FileName + ".txt"));
					
	    for (int p = 0; p < b.length; p++) {
		String x = String.valueOf(b[p]);
		response.write(x);
		response.newLine();
	    }
		response.flush();
		response.close();
	} 
        catch (Exception e) {
		e.printStackTrace();
	}
    }
	    
	    

	public static void SaveAndDisplayImage(ArrayList<Byte> imByte) throws IOException { // SAVES IMAGE TO FILE AND DISPLAYS IT
										
		String fname = "C:\\Users\\powerpc.gr\\Pictures\\Camera Roll\\ithaki.JPEG";
		
        JFrame jframe = new JFrame("ITHAKI SPY INFO"); // DISPLAYABLE FRAME CREATION
		jframe.setSize(640, 480);
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JLabel jlabel = new JLabel("Loading image...");
		jframe.add(jlabel, BorderLayout.SOUTH);

		jframe.setVisible(true);

		byte[] image = new byte[imByte.size()]; // SAVING IMAGE DATA TO BYTE ARRAY
		for (int i = 0; i < imByte.size(); i++) {
			image[i] = imByte.get(i);
		}
		InputStream is = new ByteArrayInputStream(image);
		BufferedImage img = ImageIO.read(is);

		JLabel pic = new JLabel(new ImageIcon(img));
		jlabel.setText("image received");
		jframe.add(pic, BorderLayout.CENTER); // ADDING IMAGE TO FRAME

		ImageIO.write(img, "JPEG", new File(fname));  // SAVING IMAGE TO FILE
																											 
		System.out.println("Image saved successfully!\n");					
			
	}
	
	

		
    public static void Telemetry(int FlightLevel, int LRmotor, double DurationInMinutes) throws IOException {
		
	    BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\powerpc.gr\\Desktop\\session02\\FlightData.txt"));

	    int ClientPortNum = 48078;
	    int ServerPortNum = 38048;
	    byte[] hostIP = { (byte) 155, (byte) 207, (byte) 18, (byte) 208 };     // ITHAKI IP ADDRESS
	    InetAddress ia = InetAddress.getByAddress(hostIP);

	    Socket soc = new Socket(ia, ServerPortNum);
	    OutputStream out = soc.getOutputStream();

	    String str = "AUTO FLIGHTLEVEL="+FlightLevel+" LMOTOR="+LRmotor+" RMOTOR="+LRmotor+" PILOT \r\n";   //REQUEST CODE SEND WITH TCP
	    System.out.println("Request sent: " +str);
		out.write(str.getBytes());

		InputStream in = soc.getInputStream();
		InputStream in2 = soc.getInputStream();    

		DatagramSocket socket = new DatagramSocket(ClientPortNum);
		long startTime1 = System.currentTimeMillis();

		for(;System.currentTimeMillis()<startTime1+(DurationInMinutes*1000*60);) {

			byte[] data = new byte[100];
			DatagramPacket packet = new DatagramPacket(data, data.length);      //DATA RECEIVE WITH UDP
			socket.receive(packet);

			String response = new String(data, 0, packet.getLength());
			writer.write(response);
			writer.newLine();
			System.out.println(response);

	    }
	    writer.flush();
	    writer.close();
	}


		

	public static void AudioRequest_AQ_DPCM(int ClientPortNum, int ServerPortNum, String requestCodeAQYXXX) throws IOException, LineUnavailableException {

		DatagramSocket soc1 = new DatagramSocket();
		
		ArrayList<Long> meanList = new ArrayList<>();
		ArrayList<Long> stepList = new ArrayList<>();      // SAVING AUDIO DATA INTO LIST
		ArrayList<Integer> difList = new ArrayList<>();
		ArrayList<Byte> audioList = new ArrayList<>(); 

		byte[] hostIP = { (byte) 155, (byte) 207, (byte) 18, (byte) 208 };  // ITHAKI IP ADDRESS
		InetAddress ia = InetAddress.getByAddress(hostIP);

		byte[] requestBytes = requestCodeAQYXXX.getBytes();  // SENDING REQUEST CODE
		DatagramPacket pkt1 = new DatagramPacket(requestBytes, requestBytes.length, ia, ServerPortNum);

		soc1.send(pkt1);
		soc1.close();
		System.out.println("Requesting audio...\n\n");

		DatagramSocket soc2 = new DatagramSocket(ClientPortNum);
		soc2.setSoTimeout(8000);

		byte[] recBuffer = new byte[132];
		DatagramPacket pkt2 = new DatagramPacket(recBuffer, recBuffer.length, ia, ServerPortNum);
		
		for(;;) {
			try {

				soc2.receive(pkt2);

				mean = (int) (256 * (recBuffer[0] & 0x0000FF00) + (recBuffer[1] & 0x000000FF));
				meanList.add((long) mean);
				step = (int) (256 * (recBuffer[2] & 0x0000FF00) + (recBuffer[3] & 0x000000FF));
				stepList.add((long) step);

				for (int j = 4; j < 132; j++) {

					int Nibble1 = (int) (recBuffer[j] & 0x0000000F);
					int Nibble2 = (int) ((recBuffer[j] & 0x000000F0) >> 4);

					int Difference1 = Nibble2 - 8;
					int Difference2 = Nibble1 - 8;

					difList.add(Difference1);
					difList.add(Difference2);

					int Sample1 = step * Difference1 + mean; // FIRST DEMODULATED SAMPLE (16 bits)

					int Sample11 = (Sample1 & 0xFF);
					int Sample12 = 256 * (Sample1 & 0x0000FF00);

					audioList.add((byte) Sample12);
					audioList.add((byte) Sample11);

					int Sample2 = step * Difference2 + mean; // SECOND DEMODULATED SAMPLE (16 bits)
					int Sample21 = (Sample2 & 0xFF);
					int Sample22 = 256 * (Sample2 & 0x0000FF00);

					audioList.add((byte) Sample22);
					audioList.add((byte) Sample21);
				}
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}

			double[] Differences = new double[difList.size()];
			for (int j = 0; j < difList.size(); j++) {
				Differences[j] = difList.get(j); // SAVING DIFFERENCE VALUES TO TXT FILE
			}
			FileWrite(Differences, "s02_AQdif_G13");
			
		}
		
		double[] Means = new double[meanList.size()];
		double[] Steps = new double[stepList.size()];

		for (int i = 0; i < meanList.size(); i++) 
			Means[i] = meanList.get(i);
		

		for (int j = 0; j < stepList.size(); j++) 
			Steps[j] = stepList.get(j); // SAVING MEAN/STEP VALUES TO TXT FILE
		

		FileWrite(Means, "s02_MEAN_G15");
		FileWrite(Steps, "s02_STEP_G16");

		AudioFormat PCM = new AudioFormat(8000, 16, 1, true, false); // DECODING AUDIO

		AudioFormat PCM2 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 40000, 16, 2, 4, 40000, false); // FOR
																											// RECORDING
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, PCM2);

		TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(info); // READY TO RECORD
		targetLine.open();

		SourceDataLine out = AudioSystem.getSourceDataLine(PCM);

		double[] Samples = new double[audioList.size()];
	
		for (int i = 0; i < audioList.size(); i++) 
			Samples[i] = audioList.get(i); 
		
		FileWrite(Samples, "s02_AQsam_G14");  // SAVING SAMPLES TO TXT FILE

		byte[] audioBytes = new byte[audioList.size()];
		for (int j = 0; j < audioList.size(); j++) {
			audioBytes[j] = audioList.get(j);
		}

		targetLine.start();  //START RECORDING
		out.open(PCM, audioBytes.length);

		Thread thread = new Thread() {
			@Override
			public void run() {

				AudioInputStream stream = new AudioInputStream(targetLine);
				File audioFile = new File("sess02_AQ-DPCM.wav");
				try {
					AudioSystem.write(stream, AudioFileFormat.Type.WAVE, audioFile);
				} catch (IOException e) {

					e.printStackTrace();
				}
				System.out.println("Stopped recording...");
			}
		};

		try {
			thread.start();

			out.start();
			Thread.sleep(20000); // RECORD FOR 20 SECONDS, AUDIO REQUESTED HAS TO BE LONG ENOUGH TO RECORD

			out.write(audioBytes, 0, audioBytes.length); // PLAYING THE AUDIO

			out.stop();
			out.close();
			System.out.println("DPCM audio decoded successfully!");

			targetLine.stop();
			targetLine.close();
		} catch (InterruptedException e) {

			e.printStackTrace();
		}
	}
		
		
		
		
	public static void AudioRequest_DPCM(int ClientPortNum, int ServerPortNum, String requestCodeYXXX) throws IOException, LineUnavailableException {

		DatagramSocket soc1 = new DatagramSocket();
		
		ArrayList<Byte> audioList = new ArrayList<>(); // SAVING AUDIO DATA INTO LIST
		ArrayList<Integer> difList = new ArrayList<>();

		byte[] hostIP = { (byte) 155, (byte) 207, (byte) 18, (byte) 208 }; // ITHAKI IP ADDRESS
		InetAddress ia = InetAddress.getByAddress(hostIP);

		byte[] requestBytes = requestCodeYXXX.getBytes(); // SENDING REQUEST CODE
		DatagramPacket pkt1 = new DatagramPacket(requestBytes, requestBytes.length, ia, ServerPortNum);

		soc1.send(pkt1);
		soc1.close();
		System.out.println("Requesting audio...\n\n");

		DatagramSocket soc2 = new DatagramSocket(ClientPortNum);
		soc2.setSoTimeout(8000);

		byte[] recBuffer = new byte[300];
		DatagramPacket pkt2 = new DatagramPacket(recBuffer, recBuffer.length, ia, ServerPortNum);

		int a = 15;
		int b = 240;
		int Sample2 = 0;
		int Sample1;

		System.out.println("Loading audio from server...\n");

		for (;;) {

			try {

				soc2.receive(pkt2);

				for (int i = 0; i < pkt2.getLength(); i++) {

					int q = recBuffer[i];
					int Nibble1 = (a & q); // FIRST NIBBLE
					int Nibble2 = ((b & q) >> 4); // SECOND NIBBLE

					int beta = 3;
					int difference1 = (Nibble1 - 8) * beta;
					int difference2 = (Nibble2 - 8) * beta; // !!!DECODING THE AUDIO PACKETS!!!

					difList.add(difference1);
					difList.add(difference2);

					Sample1 = Sample2 + difference2;
					Sample2 = Sample1 + difference1;

					audioList.add((byte) Sample1);
					audioList.add((byte) Sample2);
				}
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}

		}

		double[] Differences = new double[difList.size()];
		for (int i = 0; i < difList.size(); i++) {
			Differences[i] = difList.get(i); // SAVING DIFFERENCES TO TXT FILE
		}
		FileWrite(Differences, "s02_DPCMdif_G11");

		soc2.close();
		System.out.println("AUDIO ACQUIRED! decoding data...");

		AudioFormat PCM = new AudioFormat(8000, 8, 1, true, false); // DECODING AUDIO

		AudioFormat PCM2 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 40000, 16, 2, 4, 40000, false); // FOR
																											// RECORDING
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, PCM2);
        TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(info); // READY TO RECORD
		targetLine.open();

		SourceDataLine out = AudioSystem.getSourceDataLine(PCM);

		double[] Samples = new double[audioList.size()];
		for (int z = 0; z < audioList.size(); z++) {
			Samples[z] = audioList.get(z);       // SAVING DPCM SAMPLES TO TXT FILE
		}
		FileWrite(Samples, "s02_DPCMsam_G12");

		byte[] audioBytes = new byte[audioList.size()];
		for (int j = 0; j < audioList.size(); j++) {
			audioBytes[j] = audioList.get(j);
		}

		targetLine.start(); // START RECORDING
		out.open(PCM, audioBytes.length);

		Thread thread = new Thread() {
			@Override
			public void run() {

				AudioInputStream stream = new AudioInputStream(targetLine);
				File audioFile = new File("sess02_DPCM.wav");               //SAVE RECORD TO FILE
				try {
					AudioSystem.write(stream, AudioFileFormat.Type.WAVE, audioFile);
				} catch (IOException e) {

					e.printStackTrace();
				}
				System.out.println("Stopped recording...");
			}};

		try {
			thread.start();

			out.start();
			Thread.sleep(20000); // RECORD FOR 20 SECONDS, AUDIO REQUESTED HAS TO BE LONG ENOUGH TO RECORD

			out.write(audioBytes, 0, audioBytes.length); // PLAYING THE AUDIO
            out.stop();
			out.close();
			System.out.println("DPCM audio decoded successfully!");

			targetLine.stop();
			targetLine.close();
		} catch (InterruptedException e) {

			e.printStackTrace();
		}
	}
		
		
		
		
	public static void ImageRequest(int ClientPortNum, int ServerPortNum, String requestCode) throws IOException {

		DatagramSocket soc1 = new DatagramSocket();
		
		ArrayList<Byte> imageList = new ArrayList<>(); // SAVING THE IMAGE DATA INTO LIST

		byte[] hostIP = { (byte) 155, (byte) 207, (byte) 18, (byte) 208 }; // ITHAKI IP ADDRESS
		InetAddress ia = InetAddress.getByAddress(hostIP);

		byte[] requestBytes = requestCode.getBytes(); // SENDING REQUEST CODE
		DatagramPacket pkt1 = new DatagramPacket(requestBytes, requestBytes.length, ia, ServerPortNum);

		soc1.send(pkt1);
		soc1.close();
		System.out.println("Requesting image...\n\n");

		DatagramSocket soc2 = new DatagramSocket(ClientPortNum);
		soc2.setSoTimeout(8000);
		byte[] recBuffer = new byte[1024];

		DatagramPacket pkt2 = new DatagramPacket(recBuffer, recBuffer.length, ia, ServerPortNum);

		for (;;) {

			try {
				soc2.receive(pkt2);
				for (int i = 0; i < 1024; i++) {
					imageList.add(recBuffer[i]);
				}

			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
		
		soc2.close();
		System.out.println("IMAGE ACQUIRED! Downloading...");

		try {
			SaveAndDisplayImage(imageList); // CALLING SAVEIMAGE FUNCTION
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
		
			

	public static long EchoRequest(int ClientPortNum, int ServerPortNum, String requestCode) throws IOException {

			DatagramSocket soc1 = new DatagramSocket();

			byte[] hostIP = { (byte) 155, (byte) 207, (byte) 18, (byte) 208 }; // ITHAKI IP ADDRESS
			InetAddress ia = InetAddress.getByAddress(hostIP);

			byte[] requestBytes = requestCode.getBytes(); // SENDING REQUEST CODE
			DatagramPacket pkt1 = new DatagramPacket(requestBytes, requestBytes.length, ia, ServerPortNum);

			soc1.send(pkt1);
			soc1.close();
		    // System.out.println("Requesting echo...\n\n");

			DatagramSocket soc2 = new DatagramSocket(ClientPortNum);
			soc2.setSoTimeout(8000);

			byte[] recBytes = new byte[2048];
			DatagramPacket pkt2 = new DatagramPacket(recBytes, recBytes.length); // RECEIVING ECHO
			long t1 = System.currentTimeMillis();
			soc2.receive(pkt2);
			long t2 = System.currentTimeMillis();
			soc2.close();
			// System.out.println("ECHO ACQUIRED!\n");
			// System.out.println("Server response time: " +(t2 - t1));

			String echo = new String(recBytes, 0, pkt2.getLength());
			return (t2 - t1);
			//return echo;
	     }
		
				
		
		
	public static void RemoteControl(int ClientPortNum, int ServerPortNum, String RequestCode, double DurationInMinutes) throws IOException {     
			
        DatagramSocket soc1 = new DatagramSocket(); 
        DatagramSocket soc2 = new DatagramSocket(ClientPortNum);
        
        ArrayList<Integer> list0 = new ArrayList<>();
        ArrayList<Integer> list1 = new ArrayList<>();
        ArrayList<Integer> list2 = new ArrayList<>();
        ArrayList<Integer> list3 = new ArrayList<>();    //LIST FOR DATA SAVING
        ArrayList<Integer> list4 = new ArrayList<>();
        ArrayList<Integer> list5 = new ArrayList<>();
        
		byte[] hostIP = {(byte) 155, (byte) 207, (byte) 18, (byte) 208};     // ITHAKI IP ADDRESS              
	    InetAddress ia = InetAddress.getByAddress(hostIP);
	    
	    String []requests = {"OBD=01 1F\r", "OBD=01 0F\r", "OBD=01 05\r", "OBD=01 11\r", "OBD=01 0C\r", "OBD=01 0D\r"};
	    long tin = System.currentTimeMillis();
	    
	    for(;System.currentTimeMillis()<tin+(DurationInMinutes*1000*60);) {
	    	
	    for(int i=0; i<6; i++) {
	    	
	    	String finalCode = RequestCode + requests[i];
	    	
			byte[] requestBytes = finalCode.getBytes();                       
			DatagramPacket pkt1 = new DatagramPacket(requestBytes, requestBytes.length, ia, ServerPortNum);
			
			soc1.send(pkt1);          //REQUESTING DATA
			soc2.setSoTimeout(8000);
			
			byte[] recBuffer = new byte[15];
			DatagramPacket pkt2 = new DatagramPacket(recBuffer, recBuffer.length);      //RECEIVING DATA
			
			soc2.receive(pkt2);
			
			switch(i) {
			
			case 0: 
				
				String str1, str2, str3;
				str1 = new String(recBuffer);
				
				str2 = str1.substring(6,8);
				BigInteger hexint = new BigInteger(str2, 16);
				
				str3 = str1.substring(9, 11);                                //RUN TIME
				BigInteger hexint2 = new BigInteger(str3, 16);
				
				int result = 256 * hexint.intValue() + hexint2.intValue();
				
				System.out.println("ENGINE RUN TIME: "+result+ " sec");
				list0.add(result);
				break;
				
			case 1:
				
				String str7, str8;
				str7 = new String(recBuffer);
				
				str8 = str7.substring(6,8);
				BigInteger hexint5 = new BigInteger(str8, 16);               //INTAKE AIR
				
				int result1 = hexint5.intValue()-40;
				
				System.out.println("INTAKE AIR TEMPERATURE "+result1+"C");
				list1.add(result1);
				break;
				
			case 2:
				
                String str13, str14;
				str13 = new String(recBuffer);
				
				str14 = str13.substring(6,8);
				BigInteger hexint8 = new BigInteger(str14, 16);                //COOLANT
				                                                       
				int result2 = hexint8.intValue() * 100 / 255;
				
				System.out.println("COOLANT TEMPERATURE "+result2+"C");
				list2.add(result2);
				break;
				
			case 3:
				
				String str9, str10;
				str9 = new String(recBuffer);
				
				str10 = str9.substring(6,8);
				BigInteger hexint6 = new BigInteger(str10, 16);                //THROTTLE
				                                                       
				int result3 = hexint6.intValue() * 100 / 255;
				
				System.out.println("THROTTLE POSSITION "+result3+"%");
				list3.add(result3);
				break;
				
			case 4:
				
				String str4, str5, str6;
				str4 = new String(recBuffer);
				
				str5 = str4.substring(6,8);
				BigInteger hexint3 = new BigInteger(str5, 16);
				
				str6 = str4.substring(9, 11);                                //RPM
				BigInteger hexint4 = new BigInteger(str6, 16);
				
				int result4 = (256 * hexint3.intValue() + hexint4.intValue()) / 4;   
				
				System.out.println("ENGINE RPM: "+result4+ " rpm");
				list4.add(result4);
				break;
				
			case 5:
				
				String str11, str12;
				str11 = new String(recBuffer);
				
				str12 = str11.substring(6,8);
				BigInteger hexint7 = new BigInteger(str12, 16);                //SPEED
				                                                       
				int result5 = hexint7.intValue();
				
				System.out.println("VEHICLE SPEED "+result5+" km/h");
				list5.add(result5);
				break;
				}
			}
	    }
	    
	    double[] data0 = new double[list0.size()];
	    double[] data1 = new double[list1.size()];
	    double[] data2 = new double[list2.size()];
	    double[] data3 = new double[list3.size()];
	    double[] data4 = new double[list4.size()];
	    double[] data5 = new double[list5.size()];
	    
	    for(int j=0; j<list0.size(); j++) {
	    	data0[j] = list0.get(j);
	    	data1[j] = list1.get(j);
	    	data2[j] = list2.get(j);
	    	data3[j] = list3.get(j);
	    	data4[j] = list4.get(j);
	    	data5[j] = list5.get(j);
	    }
	    FileWrite(data0, "RunTime");
	    FileWrite(data1, "IntakeAir");
        FileWrite(data2, "Coolant");       //SAVING DATA TO TXT FILES
        FileWrite(data3, "ThrottlePos");
        FileWrite(data4, "EngineRpm");
        FileWrite(data5, "Speed");
        
        soc1.close();
        soc2.close();
        
	    }
					
		
		
		
	public static void Measurements(int ClientPortNum, int ServerPortNum, String EchoCode, double DurationInMinutes) throws IOException {
			
		  double[] RTT = new double[6000];
		  ArrayList <Long> timerList = new ArrayList<>();
		  ArrayList <Double> bpsList = new ArrayList<>(); 
		  
		  int lengthCounter = 0;
		  int i = 0;
		  
		  long startTime1 = System.currentTimeMillis();
		  
		  long t0 = System.currentTimeMillis();
		  
		  for(;System.currentTimeMillis()<startTime1+(DurationInMinutes*1000*60); i++, lengthCounter++){
		  
		  
		  RTT[i] = (int)EchoRequest(ClientPortNum, ServerPortNum, EchoCode);    //RTT HAS RESPONSE TIME VALUES
		  
		  long t2 = System.currentTimeMillis();
		  
		  
		  timerList.add(t2 - t0);
		  System.out.println(RTT[i]);
		  System.out.println("a = " + (t2 - t0));
		  }
		  System.out.println("LIST SIZE: "+timerList.size());
		  
		  
		  double[] timerArray = new double[timerList.size()];
		  
		  for(int p=0; p<timerList.size()-1; p++) {   
			  timerArray[p] = timerList.get(p);       //RECEIVE TIME FOR EACH PACKET IN LIST
		  }
		
		  FileWrite(RTT, "ResponceTime");   //SAVING RESPONCE TIME VALUES TO FILE
		  
		  for(int u=0, j=8000; j<timerList.get(timerList.size()-1); j+=1000, u++) {
			  
			  int pc = 0;
			  for(int z=0; timerList.get(z)<j; z++) {
				  if(j - timerList.get(z) < 8000) {
					  pc++;
				  }
			  }
			  
			  bpsList.add(((double) pc * 256.0) / 8.0);
			  System.out.println("bps = " +bpsList.get(u));
			  }
		      double[] BPS = new double[bpsList.size()];
		      for(int g=0; g<bpsList.size(); g++) {
		    	  BPS[g] = bpsList.get(g);
		      }
		  
		  FileWrite(BPS, "BPSvalues");
		  
          //SRTT/RTO CALCULATION
		  
		  double alpha = 0.5; 
		  double beta = 0.6;       //PARAMETERS
		  double G = 2; 
		  
		  double [] SRTT = new double[RTT.length];
		  double [] RTTVAR = new double[RTT.length];
		  double [] RTO = new double[RTT.length];
		  
		  SRTT[0] = (1-alpha) * RTT[0];  
		  RTTVAR[0] = (1-beta) * AbsoluteDif(SRTT[0], RTT[0]);     //INITIALIZATION OF VALUES DURING THE FIRST COMPUTATION
		  RTO[0] = SRTT[0] + RTTVAR[0] * G;
		  
		  for(int z=1; z<lengthCounter; z++) {
		  
	      SRTT[z] = (1 - alpha) * RTT[z] + alpha * SRTT[z-1];   //SRTT CALCULATION
	      SRTT[z] = Math.round(SRTT[z] * 100.0) / 100.0;
	      
		  RTTVAR[z] = (beta * RTTVAR[z-1] + (1-beta) * AbsoluteDif(SRTT[z], RTT[z]));  //RTT VARIANCE 
		  RTTVAR[z] = Math.round(RTTVAR[z] * 100.0) / 100.0;
		  
		  RTO[z] = SRTT[z] + G * RTTVAR[z] ;    //RTO CALCULATION 
		  RTO[z] = Math.round(RTO[z] * 100.0) / 100.0;
		  }
		  
		  FileWrite(SRTT, "SRTTvalues");
		  FileWrite(RTO, "RTOvalues");    //SAVING DATA TO TXT FILES
		  FileWrite(RTTVAR, "RTTVariance");
	}
	
	
	
		
	public static void main(String args[]) throws IOException, LineUnavailableException {
		
		// Measurements(48002, 38002, "E0000\r", 4);

		// long str =  EchoRequest(48007, 38007, "E0000T00\r");   //ECHO RETURN TYPE HAS TO BE STRING FOR THIS TO WORK!!!
		// System.out.println("Echo from server:\t" +str);

		// ImageRequest(48002, 38002, "M7160CAM=PTZDIR=RUDP=1024\r");

		// AudioRequest_DPCM(48002, 38002, "A2039F650");

		// AudioRequest_AQ_DPCM(48002, 38002, "A2039AQF650");

       	 	// RemoteControl(48035, 38035, "V6602", 2);
	
	    	// Telemetry(180, 180, 1);

	}
}
