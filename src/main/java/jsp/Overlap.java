package jsp;
import java.util.HashSet;

public class Overlap implements Comparable<Overlap>{
	public HashSet<String> overlap;
	public double pval = 0;
	public double pvalc = 0;
	public double odds = 0;
	public int id = 0;
	public String name = "";
	public int gmtlistsize = 0;
	
	public Overlap(int _id, HashSet<String> _overlap, double _pval, double _pvalc, double _odds, int _gmtlistsize) {
		pval = _pval;
		overlap = _overlap;
		odds = _odds;
		pvalc = _pvalc;
		id = _id;
		gmtlistsize = _gmtlistsize;
	}
	
	public int compareTo(Overlap over) {
		if(pval < over.pval) {
			return -1;
		}
		else if(pval > over.pval) {
			return 1;
		}
		else {
			return 0;
		}
	}
}
	