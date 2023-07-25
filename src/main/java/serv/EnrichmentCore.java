package serv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jsp.Overlap;

/**
 * Servlet implementation class Test
 */
@WebServlet("/api/*")
@MultipartConfig
public class EnrichmentCore extends HttpServlet {
	private static final long serialVersionUID = 1L;
    public FastFisher f;
	
	public boolean initialized = false;
	
	public HashMap<String, HashMap<String, String>> genemap;
	public HashMap<String, HashMap<String, String>> genemaprev;
	public HashSet<String> humanGenesymbol = new HashSet<String>();
	public HashSet<String> mouseGenesymbol = new HashSet<String>();
	
	public HashSet<GMT> gmts;
	public HashMap<String, GeneBackground> background;
	public HashMap<String, HashSet<String>> backgroundcache = new HashMap<String, HashSet<String>>();
	
	public HashMap<String, Integer> symbolToId = new HashMap<String, Integer>();
	public HashMap<Integer, String> idToSymbol = new HashMap<Integer, String>();
	
	public HashMap<String, String[]> listcache = new HashMap<String, String[]>();
	public ArrayList<String> listcachequeue = new ArrayList<String>();
	public int queuesize = 10000;
	public HashMap<String, String> listcachedesc = new HashMap<String, String>();
	public Connection connection;
	public SQLmanager sql;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public EnrichmentCore() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		
		super.init(config);
		
		// TODO Auto-generated method stub
		f = new FastFisher(40000);
		
		//sql = new SQLmanager();
		try {
			//connection = DriverManager.getConnection("jdbc:mysql://"+sql.database+"?rewriteBatchedStatements=true", sql.user, sql.password);
			
			System.out.println("Start buffering libraries");
			long time = System.currentTimeMillis();
			//loadGenetranslation();
			//loadGenemapping();
			loadGMT();
			//loadBackground();
			//System.out.println("Background load: "+background.size()+"\nGMTs loaded: "+gmts.size()+"\nElapsed time: "+(System.currentTimeMillis() - time));
			
			//connection.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//response.getWriter().append("My servlet served at: "+fish.getFish()+" : ").append(request.getContextPath());
		
		response.setHeader("Access-Control-Allow-Origin", "*");
		
		String pathInfo = request.getPathInfo();
		System.out.println(pathInfo);
		
		if(pathInfo == null || pathInfo.equals("/index.html") || pathInfo.equals("/")){
			RequestDispatcher rd = getServletContext().getRequestDispatcher("/index.html");
			PrintWriter out = response.getWriter();
			out.write("index.html URL");
			rd.include(request, response);
		}
		else if(pathInfo.matches("^/listcategories")){
			//localhost:8080/EnrichmentAPI/enrichment/listcategories
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			String json = "{ \"categories\": [";
			HashSet<String> categories = new HashSet<String>();
			for(GMT gmt : gmts){
				categories.add(gmt.category);
			}
			
			for(String category : categories){
				json += "\""+category+"\", ";
			}
			
			json += "] }";
			json = json.replace(", ]", "]");
			out.write(json);
		}
		else if(pathInfo.matches("^/datasetStatistics")){
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			StringBuffer sb = new StringBuffer();
			
			try{
				String datafolder = "/usr/local/tomcat/webapps/speedrichr/WEB-INF/data/";

				BufferedReader br = new BufferedReader(new FileReader(new File(datafolder+"datasetStatistics.json")));
				String line = ""; // read header
				
				while((line = br.readLine())!= null){
					sb.append(line);
				}
				br.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}	
			out.write(sb.toString());
		}
		else if(pathInfo.matches("^/listlibs")){
			//localhost:8080/EnrichmentAPI/api/listlibs
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			String json = "{ \"library\": [";
			HashSet<String> gmtNames = new HashSet<String>();
			for(GMT gmt : gmts){
				gmtNames.add(gmt.name);
			}
			
			for(String gmt : gmtNames){
				json += "\""+gmt+"\", ";
			}
			
			json += "] }";
			json = json.replace(", ]", "]");
			out.write(json);
		}
		else if(pathInfo.matches("^/listgenesets/.*")){
			//localhost:8080/EnrichmentAPI/enrichment/listgenesets/KEA
			String libString = pathInfo.replace("/listgenesets/", "");
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			String json = "{ \"geneset_name\": [";
			HashSet<String> genesetNames = new HashSet<String>();
			
			System.out.println("Lib: "+libString);
			
			for(GMT gmt : gmts){
				if(gmt.name.equals(libString)){
					for(Integer i : gmt.genelists.keySet()){
						genesetNames.add(gmt.genelists.get(i).name);
					}
				}
			}
			
			for(String gmt : genesetNames){
				json += "\""+gmt+"\", ";
			}
			
			json += "] }";
			json = json.replace(", ]", "]");
			out.write(json);
		}
		else if(pathInfo.matches("^/translate/.*")){
			//http://localhost:8080/EnrichmentAPI/api/translate/213730_x_at,220184_at,211300_s_at,213721_at
			
			String idlistString = pathInfo.replace("/translate/", "");
			String[] idlist = idlistString.split(",");
			
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			HashMap<String, String[]> res = translateGenes(new HashSet<String>(Arrays.asList(idlist)));
			
			String json = "{";
			json += "\"input\" : [";
			for(int i=0; i<idlist.length; i++){
				json += "\""+idlist[i]+"\"";
				if(i < idlist.length-1){
					json += ", ";
				}
			}
			json += "], ";
			
			for(String key : res.keySet()){
				json += "\""+key+"\" : [";
				for(int i=0; i<res.get(key).length; i++){
					json += "\""+res.get(key)[i]+"\"";
					if(i < res.get(key).length-1){
						json += ", ";
					}
				}
				json += "], ";
			}
			
			json += "}";
			json = json.replace(", }", "}");
			
			out.write(json);
		}
		else if(pathInfo.matches("/view*")){

			int userListId = Integer.parseInt(request.getParameter("userListId"));
			String hvs = Integer.toHexString(userListId);

			String[] genes = listcache.get(hvs);

			StringBuffer sb = new StringBuffer();
			sb.append("{").append("\"genes\": [\"");

			String geneListString = String.join("\",\"", genes);
			sb.append(geneListString);
			sb.append("\"],\"description\": \"").append(listcachedesc.get(hvs)).append("\"}");
			
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			out.write(sb.toString());
		}
		else if(pathInfo.matches("^/enrich")){
			long time = System.currentTimeMillis();
			int genelistid = Integer.parseInt(request.getParameter("userListId"));
			String hvs = Integer.toHexString(genelistid);
			String library = request.getParameter("backgroundType");


			System.out.println(library);

			String[] genes = listcache.get(hvs);
			
		    HashMap<String, HashMap<Integer, Overlap>> enrichment = new HashMap<String, HashMap<Integer, Overlap>>();
		    
		    HashSet<String> gmt_strings = new HashSet<String>();
			gmt_strings.add(library);
			
			for(GMT gmt : gmts){
				if(gmt_strings.contains(gmt.name)){
					enrichment.put(gmt.name, calculateEnrichmentLib(genes, gmt.name));
				}
			}
		    
			StringBuffer sb = new StringBuffer();
			sb.append("{");

			for(String gmtName : enrichment.keySet()){

				Integer[] enrStrings = enrichment.get(gmtName).keySet().toArray(new Integer[0]);
				System.out.println("The number of genesets: "+enrStrings.length);
				double[] pvals = new double[enrStrings.length];
				int count = 0;
				for(int i : enrStrings){
					pvals[count] = enrichment.get(gmtName).get(i).pval;
					count++;
				}

				NameNumber [] zip = new NameNumber[Math.min(enrStrings.length, pvals.length)];
				for(int i = 0; i < zip.length; i++){
					zip[i] = new NameNumber(enrStrings[i], pvals[i]);
				}

				Arrays.sort(zip, new Comparator<NameNumber>() {
					@Override
					public int compare(NameNumber o1, NameNumber o2) {
						return Double.compare(o1.number, o2.number);
					}
				});
				
				sb.append("\"").append(gmtName).append("\" : [");
				int rank = 1;
				
				double[] pvs = new double[zip.length];
				for(int i=0; i<zip.length; i++){
					NameNumber zippair = zip[i];
					int genesetId = zippair.name;
					pvs[i] = enrichment.get(gmtName).get(genesetId).pval;
				}
				FDR fdr = new FDR(pvs);
				fdr.calculate();
				double[] cpv = fdr.getAdjustedPvalues();

				for(int i=0; i<zip.length; i++){
					NameNumber zippair = zip[i];
					int genesetId = zippair.name;
					String genesetName = enrichment.get(gmtName).get(genesetId).name;

					double pval = enrichment.get(gmtName).get(genesetId).pval;
					double pvalCorr = cpv[i];
					double odds = enrichment.get(gmtName).get(genesetId).odds;
					
					HashSet<String> overlap = enrichment.get(gmtName).get(genesetId).overlap;
					String[] overArr = overlap.toArray(new String[0]);
					String geneListString = String.join("\",\"", overArr);
					
					if(pval < 0.05 || overlap.size() > 0){
						sb.append("[").append(rank).append(",");
						sb.append("\"").append(genesetName).append("\",");
						sb.append(pval).append(", ");
						sb.append(odds).append(", ");
						sb.append(-Math.log(pval)*odds).append(", ");
						sb.append("[\"").append(geneListString).append("\"],");
						sb.append(Math.min(1,pvalCorr)).append(", 0, 0 ], ");
						rank++;
					}
				}
				sb.append("], ");
			}
			sb.append("}");

			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			out.write(json);

			System.out.println("Elapsed time: "+(System.currentTimeMillis() - time));
		}
		else if(pathInfo.matches("^/export")){
			long time = System.currentTimeMillis();
			int genelistid = Integer.parseInt(request.getParameter("userListId"));
			String hvs = Integer.toHexString(genelistid);
			String library = request.getParameter("backgroundType");

			System.out.println(library);

			String[] genes = listcache.get(hvs);
			
		    HashMap<String, HashMap<Integer, Overlap>> enrichment = new HashMap<String, HashMap<Integer, Overlap>>();
		    
		    HashSet<String> gmt_strings = new HashSet<String>();
			gmt_strings.add(library);
			
			for(GMT gmt : gmts){
				if(gmt_strings.contains(gmt.name)){
					enrichment.put(gmt.name, calculateEnrichmentLib(genes, gmt.name));
				}
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("Term\tOverlap\tP-value\tAdjusted P-value\tOld P-value\tOld Adjusted P-value\tOdds Ratio\tCombined Score\tGenes\n");


			for(String gmtName : enrichment.keySet()){

				Integer[] enrStrings = enrichment.get(gmtName).keySet().toArray(new Integer[0]);
				System.out.println("The number of genesets: "+enrStrings.length);
				double[] pvals = new double[enrStrings.length];
				int count = 0;
				for(int i : enrStrings){
					pvals[count] = enrichment.get(gmtName).get(i).pval;
					count++;
				}

				NameNumber [] zip = new NameNumber[Math.min(enrStrings.length, pvals.length)];
				for(int i = 0; i < zip.length; i++){
					zip[i] = new NameNumber(enrStrings[i], pvals[i]);
				}

				Arrays.sort(zip, new Comparator<NameNumber>() {
					@Override
					public int compare(NameNumber o1, NameNumber o2) {
						return Double.compare(o1.number, o2.number);
					}
				});
				
				double[] pvs = new double[zip.length];
				for(int i=0; i<zip.length; i++){
					NameNumber zippair = zip[i];
					int genesetId = zippair.name;
					pvs[i] = enrichment.get(gmtName).get(genesetId).pval;
				}
				FDR fdr = new FDR(pvs);
				fdr.calculate();
				double[] cpv = fdr.getAdjustedPvalues();

				int rank = 1;
				for(int i=0; i<zip.length; i++){
					NameNumber zippair = zip[i];
					int genesetId = zippair.name;
					String genesetName = enrichment.get(gmtName).get(genesetId).name;

					double pval = enrichment.get(gmtName).get(genesetId).pval;
					double pvalCorr = cpv[i];
					double odds = enrichment.get(gmtName).get(genesetId).odds;
					
					HashSet<String> overlap = enrichment.get(gmtName).get(genesetId).overlap;
					String[] overArr = overlap.toArray(new String[0]);
					String geneListString = String.join(";", overArr);
					
					
					if(pval < 0.05 || overlap.size() > 0){

						sb.append( genesetName ).append("\t");
						sb.append( overArr.length ).append( "/" ).append( enrichment.get(gmtName).get(genesetId).gmtlistsize ).append("\t");
						sb.append( pval ).append("\t");
						sb.append( Math.min(1,pvalCorr) ).append("\t");
						sb.append( 0 ).append("\t");
						sb.append( 0 ).append("\t");
						sb.append( odds ).append("\t");
						sb.append( -Math.log(pval)*odds ).append("\t");
						sb.append( geneListString ).append("\n");
						rank++;
					}
				}
			}

			String json = sb.toString();
			String filename = "enrichr";
			response.setHeader("Pragma", "public");
			response.setHeader("Expires", "0");
			response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + ".txt\"");
			response.setHeader("Content-Transfer-Encoding", "binary");

			PrintWriter out = response.getWriter();
			out.write(json);

			System.out.println("Elapsed time: "+(System.currentTimeMillis() - time));
		} else if(pathInfo.matches("^/backgroundexport")){
			long time = System.currentTimeMillis();

			int genelistid = Integer.parseInt(request.getParameter("userListId"));
			String hvs = Integer.toHexString(genelistid);

			String backgroundid = request.getParameter("backgroundid");
			String library = request.getParameter("backgroundType");
			
			String[] genes = listcache.get(hvs);
			HashSet<String> backgroundgenes = backgroundcache.get(backgroundid);
			
			HashMap<String, HashMap<Integer, Overlap>> enrichment = new HashMap<String, HashMap<Integer, Overlap>>();
			
			HashSet<String> gmt_strings = new HashSet<String>();
			gmt_strings.add(library);
			
			for(GMT gmt : gmts){
				if(gmt_strings.contains(gmt.name)){
					enrichment.put(gmt.name, calculateEnrichmentLib(genes, gmt.name, backgroundgenes));
				}
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("Term\tOverlap\tP-value\tAdjusted P-value\tOld P-value\tOld Adjusted P-value\tOdds Ratio\tCombined Score\tGenes\n");


			for(String gmtName : enrichment.keySet()){

				Integer[] enrStrings = enrichment.get(gmtName).keySet().toArray(new Integer[0]);
				System.out.println("The number of genesets: "+enrStrings.length);
				double[] pvals = new double[enrStrings.length];
				int count = 0;
				for(int i : enrStrings){
					pvals[count] = enrichment.get(gmtName).get(i).pval;
					count++;
				}

				NameNumber [] zip = new NameNumber[Math.min(enrStrings.length, pvals.length)];
				for(int i = 0; i < zip.length; i++){
					zip[i] = new NameNumber(enrStrings[i], pvals[i]);
				}

				Arrays.sort(zip, new Comparator<NameNumber>() {
					@Override
					public int compare(NameNumber o1, NameNumber o2) {
						return Double.compare(o1.number, o2.number);
					}
				});
				
				double[] pvs = new double[zip.length];
				for(int i=0; i<zip.length; i++){
					NameNumber zippair = zip[i];
					int genesetId = zippair.name;
					pvs[i] = enrichment.get(gmtName).get(genesetId).pval;
				}
				FDR fdr = new FDR(pvs);
				fdr.calculate();
				double[] cpv = fdr.getAdjustedPvalues();

				int rank = 1;
				for(int i=0; i<zip.length; i++){
					NameNumber zippair = zip[i];
					int genesetId = zippair.name;
					String genesetName = enrichment.get(gmtName).get(genesetId).name;

					double pval = enrichment.get(gmtName).get(genesetId).pval;
					double pvalCorr = cpv[i];
					double odds = enrichment.get(gmtName).get(genesetId).odds;
					
					HashSet<String> overlap = enrichment.get(gmtName).get(genesetId).overlap;
					String[] overArr = overlap.toArray(new String[0]);
					String geneListString = String.join(";", overArr);
					
					
					if(pval < 0.05 || overlap.size() > 0){

						sb.append( genesetName ).append("\t");
						sb.append( overArr.length ).append( "/" ).append( enrichment.get(gmtName).get(genesetId).gmtlistsize ).append("\t");
						sb.append( pval ).append("\t");
						sb.append( Math.min(1,pvalCorr) ).append("\t");
						sb.append( 0 ).append("\t");
						sb.append( 0 ).append("\t");
						sb.append( odds ).append("\t");
						sb.append( -Math.log(pval)*odds ).append("\t");
						sb.append( geneListString ).append("\n");
						rank++;
					}
				}
			}

			String json = sb.toString();
			String filename = "enrichr";
			response.setHeader("Pragma", "public");
			response.setHeader("Expires", "0");
			response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + ".txt\"");
			response.setHeader("Content-Transfer-Encoding", "binary");

			PrintWriter out = response.getWriter();
			out.write(json);

			System.out.println("Elapsed time: "+(System.currentTimeMillis() - time));
		}
		else {
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			String json = "{\"error\": \"api endpoint not supported\", \"endpoint:\" : \""+pathInfo+"\"}";
			out.write(json);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		response.setHeader("Access-Control-Allow-Origin", "*");
		String pathInfo = request.getPathInfo();
		System.out.println("POST: |"+pathInfo+"|");
		
		if(pathInfo.equals("/addbackground")){
			String backgroundset = request.getParameter("background");
			
			backgroundset = backgroundset.toUpperCase();
			String[] gene_split = backgroundset.split("\\s*\\r?\\n\\s*");
			Arrays.sort(gene_split);

			if(gene_split.length < 50000){
				int hv = Math.abs(Arrays.hashCode(gene_split));
				String hvs = Integer.toHexString(hv);

				HashSet<String> bgenes = new HashSet<String>();
				for(String s : gene_split){
					bgenes.add(s);
				}

				if(backgroundcache.size() > queuesize){
					String keys[] = backgroundcache.keySet().toArray(new String[0]);
					backgroundcache.remove(keys[1]);
				}

				backgroundcache.put(hvs, bgenes);

				StringBuffer sb = new StringBuffer();
				sb.append("{").append("\"backgroundid\": \"").append(hvs).append("\"}");

				PrintWriter out = response.getWriter();
				response.setHeader("Content-Type", "application/json");
				out.write(sb.toString());
			}
		}
		else if(pathInfo.matches("/enrich.*")){
			//http://localhost:8080/EnrichmentAPI/enrichment/enrich/MDM2,MAPT,CCND1,JAK2,BIRC5,FAS,NOTCH1,MAPK14,MAPK3,ATM,NFE2L2,ITGB1,SIRT1,LRRK2,IGF1R,GSK3B,RELA,CDKN1B,NR3C1,BAX,CASP3,JUN,SP1,RAC1,CAV1,RB1,PARP1,EZH2,RHOA,PGR,SRC,MAPK8,PTK2/KEA
			
			long time = System.currentTimeMillis();

			String libString = request.getParameter("library");
			String geneset = request.getParameter("geneset");
			
			System.out.println(libString+"\n"+geneset);
			geneset = geneset.toUpperCase();

			HashMap<String, HashMap<Integer, Overlap>> enrichment = new HashMap<String, HashMap<Integer, Overlap>>();

		    String[] gene_split = new String[0];
		    HashSet<String> gmt_strings = new HashSet<String>();
		    
		    gene_split = geneset.split("\\s*\\r?\\n\\s*");
			
		    // if our pattern matches the URL extract groups
		    if (libString != null){
		        gmt_strings = new HashSet<String>(Arrays.asList(libString.split(",")));
		    }
		    else{	// enrichment over all geneset libraries
		    	for(GMT gmt : gmts){
		    		gmt_strings.add(gmt.name);
		    	}
		    }
		    
			for(GMT gmt : gmts){
				if(gmt_strings.contains(gmt.name)){
					System.out.println(gmt.name);
					System.out.println(Arrays.toString(gene_split));
					enrichment.put(gmt.name, calculateEnrichmentLib(gene_split, gmt.name));
				}
			}
		    
			StringBuffer sb = new StringBuffer();
			sb.append("{");

			for(String gmtName : enrichment.keySet()){
				sb.append("\"").append(gmtName).append("\" : {");
				for(Integer genesetId : enrichment.get(gmtName).keySet()){
					String genesetName = enrichment.get(gmtName).get(genesetId).name;
					double pval = enrichment.get(gmtName).get(genesetId).pval;
					HashSet<String> overlap = enrichment.get(gmtName).get(genesetId).overlap;
					
					if(pval < 0.05 || overlap.size() > 0){
						
						sb.append("\"").append(genesetName).append("\" : {");
						
						sb.append("\"p-value\" : \"").append(pval).append("\", ");
						
						sb.append("\"overlap\" : [");
						for(String overgene : overlap){
								sb.append("\"").append(overgene).append("\", ");	
						}
						sb.append("]}, ");
					}
				}
				sb.append("}, ");
			}
			sb.append("}");
			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			
			System.out.println(json);

			PrintWriter out = response.getWriter();
			
			response.setHeader("Content-Type", "application/json");
			out.write(json);

			System.out.println("Elapsed time: "+(System.currentTimeMillis() - time));
		}
		else if(pathInfo.equals("/backgroundenrich")){
			
			long time = System.currentTimeMillis();

			int genelistid = Integer.parseInt(request.getParameter("userListId"));
			String hvs = Integer.toHexString(genelistid);

			String backgroundid = request.getParameter("backgroundid");
			String library = request.getParameter("backgroundType");
			
			String[] genes = listcache.get(hvs);
			HashSet<String> backgroundgenes = backgroundcache.get(backgroundid);
			
			HashMap<String, HashMap<Integer, Overlap>> enrichment = new HashMap<String, HashMap<Integer, Overlap>>();
			
			HashSet<String> gmt_strings = new HashSet<String>();
			gmt_strings.add(library);
			
			for(GMT gmt : gmts){
				if(gmt_strings.contains(gmt.name)){
					enrichment.put(gmt.name, calculateEnrichmentLib(genes, gmt.name, backgroundgenes));
				}
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");

			for(String gmtName : enrichment.keySet()){

				Integer[] enrStrings = enrichment.get(gmtName).keySet().toArray(new Integer[0]);
				System.out.println("The number of genesets: "+enrStrings.length);
				double[] pvals = new double[enrStrings.length];
				int count = 0;
				for(int i : enrStrings){
					pvals[count] = enrichment.get(gmtName).get(i).pval;
					count++;
				}

				NameNumber [] zip = new NameNumber[Math.min(enrStrings.length, pvals.length)];
				for(int i = 0; i < zip.length; i++){
					zip[i] = new NameNumber(enrStrings[i], pvals[i]);
				}

				Arrays.sort(zip, new Comparator<NameNumber>() {
					@Override
					public int compare(NameNumber o1, NameNumber o2) {
						return Double.compare(o1.number, o2.number);
					}
				});
				
				sb.append("\"").append(gmtName).append("\" : [");
				int rank = 1;
				
				double[] pvs = new double[zip.length];
				for(int i=0; i<zip.length; i++){
					NameNumber zippair = zip[i];
					int genesetId = zippair.name;
					pvs[i] = enrichment.get(gmtName).get(genesetId).pval;
				}
				FDR fdr = new FDR(pvs);
				fdr.calculate();
				double[] cpv = fdr.getAdjustedPvalues();

				for(int i=0; i<zip.length; i++){
					NameNumber zippair = zip[i];
					int genesetId = zippair.name;
					String genesetName = enrichment.get(gmtName).get(genesetId).name;

					double pval = enrichment.get(gmtName).get(genesetId).pval;
					double pvalCorr = cpv[i];
					double odds = enrichment.get(gmtName).get(genesetId).odds;
					
					HashSet<String> overlap = enrichment.get(gmtName).get(genesetId).overlap;
					String[] overArr = overlap.toArray(new String[0]);
					String geneListString = String.join("\",\"", overArr);
					
					if(pval < 0.05 || overlap.size() > 0){
						sb.append("[").append(rank).append(",");
						sb.append("\"").append(genesetName).append("\",");
						sb.append(pval).append(", ");
						sb.append(odds).append(", ");
						sb.append(-Math.log(pval)*odds).append(", ");
						sb.append("[\"").append(geneListString).append("\"],");
						sb.append(Math.min(1,pvalCorr)).append(", 0, 0 ], ");
						rank++;
					}
				}
				sb.append("], ");
			}
			sb.append("}");

			String json = sb.toString();
			json = json.replace(", }", "}");
			json = json.replace(", ]", "]");
			PrintWriter out = response.getWriter();
			out.write(json);

			System.out.println("Time: "+(System.currentTimeMillis() - time));
		
		}
		else if(pathInfo.matches("/addList")){
			System.out.println("Ok adding list");

			InputStream filecontent = request.getPart("list").getInputStream();
			String geneset = convertStreamToString(filecontent);
			geneset = geneset.toUpperCase();
			String[] gene_split = geneset.split("\\s*\\r?\\n\\s*");

			filecontent = request.getPart("description").getInputStream();
			String desc = convertStreamToString(filecontent);
			
			int hv = Math.abs(Arrays.hashCode(gene_split));
			String hvs = Integer.toHexString(hv);
			listcache.put(hvs, gene_split);
			listcachedesc.put(hvs, desc);
			listcachequeue.add(hvs);

			if(listcachequeue.size() > queuesize){
				String key = listcachequeue.get(0);
				listcache.remove(key);
				listcachedesc.remove(key);
				listcachequeue.remove(0);
			}

			StringBuffer sb = new StringBuffer();
			sb.append("{").append("\"userListId\":").append(hv).append(", \"shortId\": \"").append(hvs).append("\"}");

			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			out.write(sb.toString());
		}
		else {
			PrintWriter out = response.getWriter();
			response.setHeader("Content-Type", "application/json");
			String json = "{\"error\": \"api POST endpoint not supported\", \"endpoint:\" : \""+pathInfo+"\"}";
			out.write(json);
		}
		
	}
	
	public String md5hash(String plaintext) {
		String hashtext = "new";
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(plaintext.getBytes());
			byte[] digest = m.digest();
			BigInteger bigInt = new BigInteger(1,digest);
			hashtext = bigInt.toString(16);
			// Now we need to zero pad it if you actually want the full 32 chars.
			while(hashtext.length() < 32 ){
			  hashtext = "0"+hashtext;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return hashtext;
	}
	
	public void loadGenetranslation() {
		genemap = new HashMap<String, HashMap<String, String>>();
		genemaprev = new HashMap<String, HashMap<String, String>>();
		
		HashMap<String, String> symbolgenemap = new HashMap<String, String>();
		HashMap<String, String> ensemblgenemap = new HashMap<String, String>();
		HashMap<String, String> ensembltransmap = new HashMap<String, String>();
		HashMap<String, String> affymap = new HashMap<String, String>();
		HashMap<String, String> entrezmap = new HashMap<String, String>();
		HashMap<String, String> hgncidmap = new HashMap<String, String>();
		
		HashMap<String, String> symbolgenemaprev = new HashMap<String, String>();
		HashMap<String, String> ensemblgenemaprev = new HashMap<String, String>();
		HashMap<String, String> ensembltransmaprev= new HashMap<String, String>();
		HashMap<String, String> affymaprev = new HashMap<String, String>();
		HashMap<String, String> entrezmaprev = new HashMap<String, String>();
		HashMap<String, String> hgncidmaprev = new HashMap<String, String>();
		
		try{
			String datafolder = "/usr/local/tomcat/webapps/speedrichr/WEB-INF/data/";

			BufferedReader br = new BufferedReader(new FileReader(new File(datafolder+"human_mapping_biomart.tsv")));
			String line = br.readLine(); // read header
			
			while((line = br.readLine())!= null){
				
				String[] sp = line.split("\t");
				
				if(sp.length == 6){
					String ensembl_gene = sp[0];
					String ensembl_transcript = sp[1];
					String affy = sp[2];
					String gsymbol = sp[3];
					String entrezgene = sp[4];
					String hgnc_id = sp[5];
					
					humanGenesymbol.add(gsymbol);
					
					if(gsymbol != ""){
						symbolgenemap.put(gsymbol, sp[3]);
						symbolgenemaprev.put(sp[3], gsymbol);
					}
					if(ensembl_gene != ""){
						ensemblgenemap.put(ensembl_gene, sp[3]);
						ensemblgenemaprev.put(sp[3],ensembl_gene);
					}
					if(ensembl_transcript != ""){
						ensembltransmap.put(ensembl_transcript, sp[3]);
						ensembltransmaprev.put(sp[3], ensembl_transcript);
					}
					if(affy != ""){
						affymap.put(affy, sp[3]);
						affymaprev.put(sp[3], affy);
					}
					if(entrezgene != ""){
						entrezmap.put(entrezgene, sp[3]);
						entrezmaprev.put(sp[3], entrezgene);
					}
					if(hgnc_id != ""){
						hgncidmap.put(hgnc_id, sp[3]);
						hgncidmaprev.put(sp[3], hgnc_id);
					}
				}
			}
			br.close();
			
			genemap.put("gene_symbol", symbolgenemap);
			genemap.put("ensembl_gene", ensemblgenemap);
			genemap.put("ensembl_transcript", ensembltransmap);
			genemap.put("affymetrix_probe_id", affymap);
			genemap.put("entrez_id", entrezmap);
			genemap.put("hgnc_id", hgncidmap);
			
			genemaprev.put("gene_symbol", symbolgenemaprev);
			genemaprev.put("ensembl_gene", ensemblgenemaprev);
			genemaprev.put("ensembl_transcript", ensembltransmaprev);
			genemaprev.put("affymetrix_probe_id", affymaprev);
			genemaprev.put("entrez_id", entrezmaprev);
			genemaprev.put("hgnc_id", hgncidmaprev);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public HashMap<String, String[]> translateGenes(HashSet<String> _geneset){
		
		HashMap<String, HashSet<String>> overlap = new HashMap<String, HashSet<String>>();
		HashMap<String, String[]> result = new HashMap<String, String[]>();
		
		HashSet<String> maxOverlap = new HashSet<String>();
		String maxKey = "";
		
		for(String key : genemap.keySet()){
			HashSet<String> temp = new HashSet<String>(genemap.get(key).keySet());
			
			temp.retainAll(_geneset);
			
			overlap.put(key, temp);
			if(overlap.get(key).size() > maxOverlap.size()){
				maxOverlap = temp;
				maxKey = key;
			}
		}
		
		String[] matchedGenesymbol = new String[maxOverlap.size()];
		int o = 0;
		for(String key : maxOverlap){
			matchedGenesymbol[o] = genemap.get(maxKey).get(key);
			o++;
		}
		
		result.put("gene_symbol", matchedGenesymbol);
		
		for(String key : genemap.keySet()){
			String[] temp = new String[matchedGenesymbol.length];
			for(int i=0; i<matchedGenesymbol.length; i++){
				temp[i] = genemaprev.get(key).get(matchedGenesymbol[i]);
			}
			result.put(key, temp);
		}
		
		return result;
	}
	
	public HashMap<Integer, Overlap> calculateEnrichmentLib(String[] _geneset, String _gmtName) {
		HashSet<String> genesetSet = new HashSet<String>(Arrays.asList(_geneset));
		HashMap<Integer, Overlap> gmtenrichment = new HashMap<Integer, Overlap>();
		
		for(GMT gmt : gmts) {
			if(gmt.name.equals(_gmtName)){
				for(Integer gmtlistid : gmt.genelists.keySet()) {
					GMTGeneList gmtlist = gmt.genelists.get(gmtlistid);
					HashSet<String> overlap = new HashSet<String>();
					if(_geneset.length < gmtlist.genearray.length) {
						for(int i=0; i< _geneset.length; i++) {
							if(gmtlist.genes.contains(_geneset[i])) {
								overlap.add(_geneset[i]);
							}
						}
					}
					else {
						for(int i=0; i< gmtlist.genearray.length; i++) {
							if(genesetSet.contains(gmtlist.genearray[i])) {
								overlap.add(gmtlist.genearray[i]);
							}
						}
					}
					
					int numGenelist = _geneset.length;
	    			int totalBgGenes = 20000;
	    			int gmtListSize =  gmtlist.genearray.length;
	    			int numOverlap = overlap.size();
					
					int a = numOverlap;
					int b = gmtListSize - numOverlap;
					int c = numGenelist - numOverlap;
					int d = totalBgGenes - numGenelist - gmtListSize + numOverlap;

					double pvalue = 1;
					double oddsRatio = 1;
					
					if(a > 0){
						pvalue = f.getRightTailedP(a, b, c, d);
						oddsRatio = (1.0 * a * d) / Math.max(1.0 * b * c, 1);
					}
					
	    			if(numOverlap > 0 || pvalue < 0.05) {
						Overlap over = new Overlap(gmtlist.id, overlap, pvalue, pvalue*gmt.genelists.size(), oddsRatio, gmtListSize);
						over.name = gmtlist.name;
						gmtenrichment.put(gmtlist.id, over);
					}
					
				}
				break;
			}
		}
		
		return gmtenrichment;
	}


	public HashMap<Integer, Overlap> calculateEnrichmentLib(String[] _geneset, String _gmtName, HashSet<String> _background) {
		HashSet<String> genesetSet = new HashSet<String>(Arrays.asList(_geneset));
		genesetSet.retainAll(_background);

		HashMap<Integer, Overlap> gmtenrichment = new HashMap<Integer, Overlap>();
		
		for(GMT gmt : gmts) {
			if(gmt.name.equals(_gmtName)){
				for(Integer gmtlistid : gmt.genelists.keySet()) {
					GMTGeneList gmtlist = gmt.genelists.get(gmtlistid);
					HashSet<String> overlap = new HashSet<String>();
					if(_geneset.length < gmtlist.genearray.length) {
						for(int i=0; i< _geneset.length; i++) {
							if(gmtlist.genes.contains(_geneset[i])) {
								overlap.add(_geneset[i]);
							}
						}
					}
					else {
						for(int i=0; i< gmtlist.genearray.length; i++) {
							if(genesetSet.contains(gmtlist.genearray[i])) {
								overlap.add(gmtlist.genearray[i]);
							}
						}
					}

					int filteredGmtSize = 0;
					for(int i=0; i< gmtlist.genearray.length; i++) {
						if(_background.contains(gmtlist.genearray[i])) {
							filteredGmtSize++;
						}
					}

					int numGenelist = genesetSet.size();
					int totalBgGenes = _background.size();
					int gmtListSize =  filteredGmtSize;
					int numOverlap = overlap.size();
					
					int a = numOverlap;
					int b = gmtListSize - numOverlap;
					int c = numGenelist - numOverlap;
					int d = totalBgGenes - numGenelist - gmtListSize + numOverlap;

					double pvalue = 1;
					double oddsRatio = 1;
					//double oddsRatio = (numOverlap*1.0/(gmtListSize))/(numGenelist*1.0/(totalBgGenes));

					if(a > 0){
						pvalue = f.getRightTailedP(a, b, c, d);
						oddsRatio = (1.0 * a * d) / (1.0 * b * c);
					}
					
	    			if(numOverlap > 0 || pvalue < 0.05) {
						Overlap over = new Overlap(gmtlist.id, overlap, pvalue, pvalue*gmt.genelists.size(), oddsRatio, gmtListSize);
						over.name = gmtlist.name;
						gmtenrichment.put(gmtlist.id, over);
					}
					
				}
				break;
			}
		}
		
		return gmtenrichment;
	}
	
	public HashMap<Integer, HashMap<Integer, Overlap>> calculateEnrichment(UserGeneList _list) {
		
		HashMap<Integer, HashMap<Integer, Overlap>> enrichment = new HashMap<Integer, HashMap<Integer, Overlap>>();
		int counter = 0;
		for(GMT gmt : gmts) {
			HashMap<Integer, Overlap> gmtenrichment = new HashMap<Integer, Overlap>();
			for(Integer gmtlistid : gmt.genelists.keySet()) {
				GMTGeneList gmtlist = gmt.genelists.get(gmtlistid);
				HashSet<String> overlap = new HashSet<String>();
				if(_list.genearray.length < gmtlist.genearray.length) {
					for(int i=0; i< _list.genearray.length; i++) {
						if(gmtlist.genes.contains(_list.genearray[i])) {
							overlap.add(_list.genearray[i]);
						}
					}
				}
				else {
					for(int i=0; i< gmtlist.genearray.length; i++) {
						if(_list.genes.contains(gmtlist.genearray[i])) {
							overlap.add(gmtlist.genearray[i]);
						}
					}
				}
				
				int numGenelist = _list.genearray.length;
	    			int totalBgGenes = 20000;
					int totalInputGenes = _list.genearray.length;
					int gmtListSize =  gmtlist.genearray.length;
	    			int numOverlap = overlap.size();
	    			double oddsRatio = (numOverlap*1.0/(totalInputGenes - numOverlap))/(numGenelist*1.0/(totalBgGenes - numGenelist));
	    			double pvalue = f.getRightTailedP(numOverlap,(gmtListSize - numOverlap), numGenelist, (totalBgGenes - numGenelist));	
	    			
	    			if(pvalue < 0.05) {
	    				counter++;
	    			}
	    			
	    			Overlap over = new Overlap(gmtlist.id, overlap, pvalue, Math.min(1, pvalue*gmt.genelists.size()), oddsRatio, gmtListSize);
	    			gmtenrichment.put(gmtlist.id, over);
			}
			enrichment.put(gmt.id, gmtenrichment);
			
		}
		
		System.out.println("Significant overlaps: "+counter);
		
		return enrichment;
	}
	
	public void loadBackground() {
		background = new HashMap<String, GeneBackground>();
		HashSet<Integer> backgroundids = new HashSet<Integer>();
		
		try { 
			

			// create the java statement and execute
			String query = "SELECT id FROM genebackgroundinfo";
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);

			while (rs.next()) {
				int id = rs.getInt("id");
				backgroundids.add(id);
			}
			stmt.close();
			
			for(Integer i : backgroundids) {
				GeneBackground bg = new GeneBackground();
		        bg.load(sql, (int)i);
		        background.put(bg.name, bg);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public UserGeneList saveUserList(String _user, String _description, String _genetext) {
		
	    UserGeneList list = null;
		try { 
			int id = 0;

			if(_user != null) {
				
				// create the java statement and execute
				String query = "SELECT id FROM userinfo WHERE username='"+_user+"'";
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query);
	
				while(rs.next()) {
					id = rs.getInt("id");
				}
				stmt.close();
			}
			
			String[] lines = _genetext.split("\n");
	        HashSet<String> genes = new HashSet<String>();
	        
	        for(String l : lines) {
	        		String gene = l.toUpperCase().trim();
	        		if(symbolToId.keySet().contains(gene)) {
	        			genes.add(gene);
	        		}
	        }
	        
	        list = new UserGeneList(id, _description, genes);
			list.write(id, null, connection);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	
	public void loadGMT() {
		
		gmts = new HashSet<GMT>();
		
		try { 
			// create new file
			File f = new File("/usr/local/tomcat/webapps/speedrichr/WEB-INF/data/genelibs");
			File[] files = f.listFiles();

			int gmtid = 1;

			for(File tf : files){
				GMT gmt = new GMT(this);
				gmt.name = tf.getName().split("\\.")[0];
				gmt.id = gmtid;
				
				gmt.genelists = new HashMap<Integer, GMTGeneList>();
				BufferedReader br = new BufferedReader(new FileReader(tf));
				String line = "";
				int listcount = 1;
				while((line = br.readLine()) != null){
					HashSet<String> geneset = new HashSet<String>();
					String[] sp = line.split("\t");
					if(sp.length > 2){
						String genesetname = sp[0].replaceAll("\"", "");
						String desc = sp[1].replaceAll("\"", "");
						for(int i=2; i<sp.length; i++){
							geneset.add(sp[i].split(",")[0]);
						}
						GMTGeneList genelist = new GMTGeneList(listcount, genesetname, desc, geneset);
						
						gmt.genelists.put(listcount, genelist);
						listcount++;
					}
				}
				br.close();
				gmtid++;
				gmts.add(gmt);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public void loadGenemapping(){
		
		try {
			String query = "SELECT * FROM genemapping";
			
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			
			while (rs.next()){
			    String gene = rs.getString("genesymbol");
			    int geneid = rs.getInt("geneid");
			    symbolToId.put(gene, geneid);
				idToSymbol.put(geneid, gene);
			}
			stmt.close();
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	static String convertStreamToString(java.io.InputStream is) {
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	class NameNumber {
		public Integer name;
		public double number;

		public NameNumber(Integer name, double n) {
			this.name = name;
			this.number = n;
		}
	}  
}
