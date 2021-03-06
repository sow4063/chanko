import java.io.IOException;
import java.text.SimpleDateFormat;

import com.divine.panera.panera;

public class paneracli
{
	
	public static void main(String args[]) throws IOException {
		
		try {
			
			panera pan = new panera();
			
			for( int i = 1; i <= 1; i++ ) {
				
				System.out.println("===================================================================");
				System.out.println( i + "th running begin." );
				System.out.println("===================================================================");
				
				// sending a file to the server.
				String fileName = "/Users/water4063/Documents/secure_sw/panera/test.txt";
				pan.sendFile( fileName );
				
				// fileName, encryptedFilename, date
				int index = fileName.lastIndexOf("/");
	            String name = fileName.substring(index + 1);
	        	String encryptedFileName = pan.encryptFileName( name );
	        	String createDate = new SimpleDateFormat("yyyyMMdd/").format(System.currentTimeMillis( ));
            	
	        	System.out.println("target file info = " + name + ", [" + encryptedFileName + "], date = [" + createDate + "]" );
	        	
	        	// Save the file info to database.
	            pan.sendFileInfo( name, encryptedFileName, createDate );
	            
	        	// check cache
	            String fileServerIP = "";
				String fileServerPort = "";
				String[] arr;
				
	        	String searched = pan.checkCache( fileName, encryptedFileName, createDate );
	        	if( searched.length() > 0 ) {
	        		
	        		arr = panera.base64decode(searched).split( panera.DELIMITER );
	        		
	        		System.out.println("File info is in the cache = [" + searched + "]");
	        		
	        		fileServerIP      = arr[0];
					fileServerPort    = arr[1];
					fileName          = arr[2];
					encryptedFileName = arr[3];
					createDate        = arr[4];
	        	}
	        	else {
	        		
	        		System.out.println("Not Found the file info in the cache = [" + name + "]");
	        		
	        		// receiving a file from the server.
		            String fileInfo = pan.receiveFileInfo(name, createDate);
		            
					arr = fileInfo.split( panera.DELIMITER );
					
					encryptedFileName = arr[0];
					fileServerIP      = arr[1];
					fileServerPort    = arr[2];
	        	}
	        	
	        	System.out.println("FileName        = [" + name + "]");
	            System.out.println("encFileName     = [" + encryptedFileName + "]");
	            System.out.println("fileServerIP    = [" + fileServerIP + "]");
	            System.out.println("fileServerPort  = [" + fileServerPort + "]");
	            System.out.println("createDate      = [" + createDate + "]");
	        	
				// receiving a file from the server.
	            //fileName = "test.txt";
	            System.out.println("before receiveFile" + name + createDate );
	            pan.receiveFile( encryptedFileName + panera.DELIMITER + createDate );
	            
	            System.out.println("===================================================================");
	            System.out.println( i + "th running end.");
	            System.out.println("===================================================================");
	          
			}
			
		}
		catch( Exception e ) {
			System.err.println("erro on client : " + e.getLocalizedMessage() );
		}
	}
	
	
}