import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class panerasrv {
	
	private static ServerSocket serverSocket;
	private static Socket clientSocket = null;
	
	public static void main(String args[]) throws IOException {
		
		requestHandle rh = new requestHandle();
		
		// check license
		if( !rh.checkLicense() ) {
			System.err.println("License is not allowed.");
			System.exit(1);
		}
		
		
		try {
			serverSocket = new ServerSocket( Integer.parseInt( "7405" ) );
			System.out.println("Server started.");
		}
		catch( Exception e ) {
			System.err.println("Port already in used.");
			System.exit(1);
		}
		
		// Handling the clients requests.
		while( true ) {
			
			try{
				clientSocket = serverSocket.accept();
				
				System.out.println("Accepted connection : " + clientSocket );
				
				Thread t = new Thread( new requestHandle( clientSocket ) );
				
				t.start();
			}
			catch( Exception e ){
				System.out.println("Error in connection attempt");
			}
		}
		
	}
	
}
 