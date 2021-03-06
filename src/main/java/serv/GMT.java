package serv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;


public class GMT{
	public int id = 1;
	public String name = "";
	public String category = "";
	public String description = "";
	public String text = "";
	public HashMap<Integer, GMTGeneList> genelists;
	public SQLmanager sql;
	public EnrichmentCore core;
	Connection  connection;
	
	public GMT() {
		
	}
	
	public GMT(EnrichmentCore _core) {
		core = _core;
	}
	
	public GMT(int _id, String _name, String _category, String _desc, String _text) {
		
		id = _id;
		name = _name;
		category = _category;
		description = _desc;
		text = _text;
		genelists = new HashMap<Integer, GMTGeneList>();
	}
	
	public void loadGMT(SQLmanager _sql, int _id) {
		id = _id;
		sql = _sql;
		loadGMTInfo();
	}
	
	public void loadGMTInfo() {
		genelists = new HashMap<Integer, GMTGeneList>();
		try { 
			connection = DriverManager.getConnection("jdbc:mysql://"+sql.database, sql.user, sql.password);

			// create the java statement and execute
			String query = "SELECT * FROM gmtinfo WHERE id='"+id+"'";
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			
			while (rs.next()) {
				name = rs.getString("gmtname");
				category = rs.getString("gmtcategory");
				description = rs.getString("gmtdesc");
				text = rs.getString("gmttext");
				System.out.println(name);
			}
			stmt.close();
			
			query = "SELECT listid, geneid FROM gmtgenelist WHERE listid IN (SELECT gmtgenelistid FROM gmt WHERE gmtid = '"+id+"')";
			stmt = connection.createStatement();
			rs = stmt.executeQuery(query);
			
			HashMap<Integer, HashSet<String>> genesets = new HashMap<Integer, HashSet<String>>();
			
			while (rs.next()) {
				int lid = rs.getInt("listid");
				String genesym = core.idToSymbol.get(rs.getInt("geneid"));
				
				if(genesets.containsKey(lid)) {
					genesets.get(lid).add(genesym);
				}
				else {
					HashSet<String> gs = new HashSet<String>();
					gs.add(genesym);
					genesets.put(lid, gs);
				}
			}
			stmt.close();
			
			
			query = "SELECT * FROM gmtgenelistinfo WHERE id IN (SELECT gmtgenelistid FROM gmt WHERE gmtid = '"+id+"')";
			stmt = connection.createStatement();
			rs = stmt.executeQuery(query);
			
			while (rs.next()) {
				GMTGeneList genelist = new GMTGeneList(rs.getInt("id"), rs.getString("listname"), rs.getString("listdesc"), genesets.get(rs.getInt("id")), sql);
				genelists.put(rs.getInt("id"),genelist);
			}
			stmt.close();
			
			connection.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public String toString() {
		
		String text = name+" - "+category+" - "+description+" - size: "+genelists.size();

		for(Integer glid : genelists.keySet()) {
			text += "\n -> " +genelists.get(glid).toString(); 
		}
		
		return text;
	}
	

	public void writeGMT(SQLmanager _sql) {
		sql = _sql;
		
		HashMap<String, Integer> _genemap = core.symbolToId;
		System.out.println("Genemap size: "+_genemap.size());

		Connection  connection;

		try { 
			connection = DriverManager.getConnection("jdbc:mysql://"+sql.database, sql.user, sql.password);
			
			String query = "SELECT COUNT(*) FROM gmtinfo WHERE gmtname='"+name+"'";
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			
			int count = 1;
			while (rs.next()) {
				count = rs.getInt(1);
			}
			
			System.out.println("Count: "+count);
			System.out.println("Genelist count"+genelists.keySet().size());
			System.out.println(toString());
			
			if(count == 0){
				System.out.println("Create new entry");
				query = " INSERT INTO gmtinfo (gmtname, gmtcategory, gmtdesc, gmttext)"
				        + " VALUES (?, ?, ?, ?);";
	
				// create the mysql insert pre-paredstatement
				PreparedStatement preparedStmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
				preparedStmt.setString(1, name);
				preparedStmt.setString(2, category);
				preparedStmt.setString(3, description);
				preparedStmt.setString(4, text);
				preparedStmt.executeQuery();
				
				rs = preparedStmt.getGeneratedKeys();
				
				if (rs.next()) {
				    id = rs.getInt(1);
				}
				
				for(Integer glid : genelists.keySet()) {
					int key = genelists.get(glid).writeGMTGeneList(sql, id, _genemap);
				}
				
				connection.commit();
			}
			else{
				System.out.println("Already exists");
			}
			connection.close();	
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}