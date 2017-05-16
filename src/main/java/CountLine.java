import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CountLine {

	public static void main(String[] args) {
		try{
	         //Open input stream for reading the text file MyTextFile.txt
	         InputStream is = new FileInputStream("/home/minhp/Downloads/locations.txt");
	         
	         // create new input stream reader
	         InputStreamReader instrm = new InputStreamReader(is);
	         
	         // Create the object of BufferedReader object
	         BufferedReader br = new BufferedReader(instrm);
	      
	         String strLine;
	         int count = 0;
	         // Read one line at a time 
	         while((strLine = br.readLine()) != null)
	         {    
	            // Print line
	           count++;
	         }
	         System.out.print(count);
	      }catch(Exception e){
	         e.printStackTrace();
	      }
	}

}
