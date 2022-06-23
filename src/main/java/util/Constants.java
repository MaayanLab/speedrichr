package util;

public class Constants {
	public static String datafolder;
	static {
		datafolder = System.getenv("datafolder");
		if (datafolder == null) {
			datafolder = "/usr/local/tomcat/webapps/speedrichr/WEB-INF/data/";
		}
	}
}
