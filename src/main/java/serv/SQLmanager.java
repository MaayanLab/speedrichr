package serv;
public class SQLmanager {
	
	public String database = "";
	public String user = "";
	public String password = "";
	
	private void loadCredentials(){
		System.out.println("Load environment");
		
		database = System.getenv("dbserver")+":"+System.getenv("dbport")+"/"+System.getenv("dbname");
		user = System.getenv("dbuser");
		password = System.getenv("dbpass");

		System.out.println(database+"\n"+user+"\n"+password);
	}
	
	public SQLmanager() {
		try {
			loadCredentials();
			System.out.println(database);
			
		    // Register database connector to the SQLmanager
			Class.forName("org.mariadb.jdbc.Driver"); 
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
	}
}



