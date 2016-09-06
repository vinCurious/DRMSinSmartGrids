
/* 
 * PeakTest.java 
 *  
 */

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * This PeakTest class extract averages of peak values per station.
 * 
 * @author Vinay Vasant More
 * @author Pratik Shirish Kulkarni
 *
 */

public class PeakTest {

	/**
	 * The main program.
	 *
	 * @param args
	 *            command line arguments (ignored)
	 */
	public static void main(String args[]) throws IOException {
		String str;
		String tempString1 = "";
		String tempString2 = "";
		String tempString3 = "";
		HashMap<String, String> hp = new HashMap<>();

		// modify paths to match file locations
		FileInputStream fs = new FileInputStream("C:\\Users\\vin\\Desktop\\DS\\project\\STN_103_DATA.csv");
		DataInputStream in = new DataInputStream(fs);
		PrintWriter pw = new PrintWriter(new FileWriter("C:\\Users\\vin\\Desktop\\DS\\project\\STN_103.txt"));
		str = in.readLine();
		str = in.readLine();
		while (str != null) {

			tempString1 = str.substring(str.indexOf(',') + 1);
			tempString2 = tempString1.substring(tempString1.indexOf(',') + 1);
			tempString3 = tempString2.substring(tempString2.indexOf(',') + 1, tempString2.length() - 8);
			// System.out.print(tempString3+": ");
			// System.out.println(tempString2.substring(tempString2.lastIndexOf(',')+1));
			if (hp.get(tempString3) == null)
				hp.put(tempString3, "");
			hp.put(tempString3, hp.get(tempString3) + tempString2.substring(tempString2.lastIndexOf(',') + 1) + ",");
			str = in.readLine();
		}

		for (String key : hp.keySet()) {
			System.out.println(key + ": " + hp.get(key));
			System.out.println(key + ":" + calculateAvg(hp.get(key)));
			pw.println(key + ":" + calculateAvg(hp.get(key)));
		}
		fs.close();
		pw.close();
	}

	/**
	 * calculateAvg function.
	 *
	 * @param str
	 *            String with power consumption logs
	 * 
	 * 
	 * @return Integer value which represents the average of peak values
	 * 
	 */
	static int calculateAvg(String str) {
		int count = countOccurrences(str, ',');
		int sum = 0;
		int index = -1;
		for (int i = 0; i < count; i++) {
			// System.out.println(str.substring(index+1,str.indexOf(',',
			// index+1)));
			sum = sum + Integer.parseInt(str.substring(index + 1, str.indexOf(',', index + 1)));
			index = str.indexOf(',', index + 1);
		}
		return (int) sum / count;
	}

	/**
	 * countOccurrences function.
	 *
	 * @param str
	 *            String with power consumption logs
	 * @param ch
	 *            character whose count is to be found out
	 * 
	 * 
	 * @return Integer value represents the count of mentioned character
	 * 
	 */
	static int countOccurrences(String str, char ch) {
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == ch) {
				count++;
			}
		}
		return count;
	}
}