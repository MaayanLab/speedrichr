package serv;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// import util.Constants;

public class Enrichment {
	
	int threadCount = 2;
	int stepSize = 100000;
	
	private FastFisher f = new FastFisher(50000);
	
	HashMap<String, short[]> genelists = new HashMap<String, short[]>();
	HashMap<String, Short> dictionary = new HashMap<String, Short>();
	String[] revDictionary = new String[Short.MAX_VALUE*2];
	
	public Enrichment() {
		//genelists = (HashMap<String, short[]>) deserialize("Constants.datafolder + "geneset.so");
		//dictionary = (HashMap<String, Short>) deserialize("Constants.datafolder + "dictionary.so");
		//revDictionary = (String[]) deserialize("Constants.datafolder + "revDictionary.so");
	}
	
	public HashMap<String, Double> calculateEnrichment(short[] _genelist, String[] _uids) {
		
		short stepup = Short.MIN_VALUE;
		HashMap<String, Double> pvals = new HashMap<String, Double>();
		
		boolean[] boolgenelist = new boolean[70000];
		for(int i=0; i<_genelist.length; i++) {
			boolgenelist[_genelist[i]-stepup] = true;
		}
		
		short overlap = 0;
		
		HashSet<String> listfilter = new HashSet<String>(genelists.keySet());
		
		if(_uids == null) {
			HashSet<String> temp = new HashSet<String>();
			for(int i=0; i<_uids.length; i++) {
				temp.add(_uids[i]);
			}
			listfilter.retainAll(temp);
		}
		
		for(String key : listfilter) {
			
			short[] gl = genelists.get(key);
			overlap = 0;
			
			for(int i=0; i< gl.length; i++) {
				if(boolgenelist[gl[i]-Short.MIN_VALUE]) {
					overlap++;
				}
			}
			
			int numGenelist = _genelist.length;
			int totalBgGenes = 20000;
			int gmtListSize =  gl.length;
			int numOverlap = overlap;
			
			double pvalue = f.getRightTailedP(numOverlap,(gmtListSize - numOverlap), numGenelist-numOverlap, (totalBgGenes - numGenelist-gmtListSize+overlap));	
			
			if(numOverlap > 0 || _uids != null) {
				pvals.put(key, pvalue);
			}
			
		}
		
		return pvals;
	}

	public class EnrichmentThread implements Runnable {
		
		boolean[] boolgenelist = null;
		int genelistLength = 0;
		int start = 0;
		
	    public EnrichmentThread(int _i, boolean[] _genelist, int _listLength){
	    	boolgenelist = _genelist;
	    	genelistLength = _listLength;
	    	start = _i;
	    }
	    
	    public void run() {
	    	
			short stepup = Short.MIN_VALUE;
			HashMap<String, Double> pvals = new HashMap<String, Double>();
			
			short overlap = 0;
			int counter = 0;
			
			String[] keys = genelists.keySet().toArray(new String[0]);
			
			for(int i=start*stepSize; i<Math.min(keys.length, (start+1)*stepSize); i++) {
				
				short[] gl = genelists.get(keys[i]);
				overlap = 0;
				
				for(int j=0; j< gl.length; j++) {
					if(boolgenelist[gl[j]-stepup]) {
						overlap++;
					}
				}
				
				int numGenelist = genelistLength;
				int totalBgGenes = 20000;
				int gmtListSize =  gl.length;
				int numOverlap = overlap;
				
				double pvalue = f.getRightTailedP(numOverlap,(gmtListSize - numOverlap), numGenelist, (totalBgGenes - numGenelist));	
				
				if(pvalue < 0.05) {
					pvals.put(keys[i], pvalue);
				}
				
				if(overlap > 0) {
					counter++;
				}
			}
	    }
	}

	public HashMap<String, Double> calculateEnrichmentThreaded(short[] _genelist){

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		HashMap<String, Double> pvals = new HashMap<String, Double>();
		
		short stepup = Short.MIN_VALUE;
		
		int steps = genelists.size()/stepSize;
		
		boolean[] boolgenelist = new boolean[70000];
		for(int i=0; i<_genelist.length; i++) {
			boolgenelist[_genelist[i]-stepup] = true;
		}
		
		for(int i=0; i<=steps; i++) {
			Runnable worker = new EnrichmentThread(i, boolgenelist, _genelist.length);
	        executor.execute(worker);
		}
		
		executor.shutdown();
        while (!executor.isTerminated()) {}
        System.out.println("Finished all threads");
		
		return pvals;
	}
	
	
	public double mannWhitney(short[] _geneset, short[] _rank){
		// smaller rank is better, otherwise return 1-CNDF 
		
		int rankSum = 0;
		
		double n1 =  _geneset.length;
		double n2 = _rank.length - _geneset.length;
		
		double meanRankExpected = (n1*n2)/2;
		
		//System.out.println(n1+" "+n2+" "+meanRankExpected);
		
		// this is true for iia genes (in reality the complexity of the gene input list can be adjusted based on their correlation)
		double sigma = Math.sqrt(n1)*Math.sqrt(n2/12)*Math.sqrt(n1+n2+1);
		
		//System.out.println("n2:"+n2+" - rank: "+_rank.length+" - gs: "+_geneset.length+" - sigma: "+sigma);
		
		for(int i=0; i<_geneset.length; i++) {
			//rankSum += _rank[_geneset[i] + Short.MAX_VALUE];
			rankSum += _rank[_geneset[i]-Short.MIN_VALUE]-Short.MIN_VALUE;
		}
		
		double U = rankSum - n1*(n1+1)/2;
		double z = (U - meanRankExpected)/sigma;
		
		//System.out.println(" z: "+z);
		
		return Math.min(1, Math.min((1-CNDF(z)), CNDF(z))*2);
	}
	
	private static double CNDF(double x){
	    int neg = (x < 0d) ? 1 : 0;
	    if ( neg == 1) {
	        x *= -1d;
	    }
	    double k = (1d / ( 1d + 0.2316419 * x));
	    double y = (((( 1.330274429 * k - 1.821255978) * k + 1.781477937) *
	                   k - 0.356563782) * k + 0.319381530) * k;
	    y = 1.0 - 0.398942280401 * Math.exp(-0.5 * x * x) * y;

	    return (1d - neg) * y + neg * (1d - y);
	}
	

	public Object deserialize(String _file) {
		Object ob = null;
		try{   
            // Reading the object from a file
            FileInputStream file = new FileInputStream(_file);
            ObjectInputStream in = new ObjectInputStream(file);
             
            // Method for deserialization of object
            ob = (Object)in.readObject();
             
            in.close();
            file.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
		
		return ob;
	}
}
