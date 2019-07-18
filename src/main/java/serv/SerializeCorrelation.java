package serv;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SerializeCorrelation {

	private double[][] correlation = null;
	private ArrayList<String> genes = null;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		SerializeCorrelation sc = new SerializeCorrelation();
		
//		sc.readCorrelation();
//		long time = System.currentTimeMillis();
//		sc.deserialize();
//		System.out.println(System.currentTimeMillis() - time);
	}
	
	public void serializeGenelists() {
		
		
		
	}
	
	public void readCorrelation() {
		genes = new ArrayList<String>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(new File("/Users/maayanlab/OneDrive/geneshot/correlation.tsv")));
			String line = ""; // read header
			int idx = 0;
			while((line = br.readLine())!= null){
				String[] sp = line.split("\t");
				if(idx == 0) {
					correlation = new double[sp.length-1][sp.length-1];
				}
				genes.add(sp[0]);
				for(int i=1; i<sp.length; i++) {
					correlation[idx][i-1] = Double.parseDouble(sp[i]);
				}
				idx++;
			}
			br.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}	
		
		serialize(correlation);
	}
	

	public void serialize(Object _o) {
		try {
			FileOutputStream file = new FileOutputStream("/Users/maayanlab/OneDrive/geneshot/correlation.so");
	        ObjectOutputStream out = new ObjectOutputStream(file);
	         
	        // Method for serialization of object
	        out.writeObject(_o);
	         
	        out.close();
	        file.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
			
		try {
			FileOutputStream file = new FileOutputStream("/Users/maayanlab/OneDrive/geneshot/genes.so");
	        ObjectOutputStream out = new ObjectOutputStream(file);
	        String[] genes2 = genes.toArray(new String[0]);
	        // Method for serialization of object
	        out.writeObject(genes2);
	         
	        out.close();
	        file.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	public void deserialize() {
		
		try{   
            // Reading the object from a file
            FileInputStream file = new FileInputStream("/Users/maayanlab/OneDrive/geneshot/genes.so");
            ObjectInputStream in = new ObjectInputStream(file);
             
            // Method for deserialization of object
            String[] genes2 = (String[])in.readObject();
            System.out.println(genes2[0]);
            
            in.close();
            file.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
		
		
	}
	
}
