import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;

public class requestHandle implements Runnable {

    private Socket clientSocket;
    private BufferedReader in = null;
    
    private static String serverIP;
	private static String serverPort;
	private static String workdir;
	private static String logPath;
	private static String cachePath;
	private static String storagePath;
	private static String mysql_driver;
	private static String mysql_url;
	private static String mysql_userinfo;
	
	public static final String DELIMITER = "#";
	
	private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger("panerasrv");
	
	public int readConfig() throws IOException {
		
		logger.info("readConfig begin.");
		
		Properties prop = new Properties();
		InputStream input = null;

		try {
			
			String strOS = System.getProperty("os.name").toLowerCase();
			
            String pconfig = System.getProperty("panera.config").toLowerCase();
			
		    if( pconfig == null) {
                logger.info("need file path for panera.config");
		    }
						
			System.out.println( strOS + " : " + pconfig );
			logger.info( strOS + " : " + pconfig );
					
			input = new FileInputStream( pconfig );

			// load a properties file
			prop.load( input );

			// get the property value and print it out
			serverIP = prop.getProperty("serverip");
			serverPort = prop.getProperty("serverport");
			workdir = prop.getProperty("workdir");
			logPath = workdir + prop.getProperty("logpath");
			cachePath = workdir + prop.getProperty("cachepath");
			storagePath = workdir + prop.getProperty("storage");
			mysql_driver = prop.getProperty("mysql_driver");
			mysql_url = prop.getProperty("mysql_url");
			mysql_userinfo = prop.getProperty("mysql_userinfo");
			
			logger.info( "serverIP = " + serverIP );
			logger.info( "serverPort = " + serverPort );
			logger.info( "workdir = " + workdir );
			logger.info( "logPath = " + logPath );
			logger.info( "cachePath = " + cachePath );
			logger.info( "storagePath = " + storagePath );
			
			logger.info( "mysql_driver = " + mysql_driver );
			logger.info( "mysql_url = " + mysql_url );
			logger.info( "mysql_userinfo = " + mysql_userinfo );

		} 
		catch( IOException ex ) {
			ex.printStackTrace();
			logger.error("IOException.");
			return 0;
		} 
		finally {
			if( input != null ) {
				try {
					input.close();
				} 
				catch( IOException e ) {
					e.printStackTrace();
					logger.info("IOException : input != null.");
				}
			}
			
		}
		
		logger.info("readConfig end.");
		
		return 1;
	}

	public requestHandle() throws IOException {
		logger.info("requestHandle called.");
        readConfig();
    }
	
	public String getServerPort() {
        return serverPort;
    }
	
    public requestHandle(Socket client) throws IOException {
    	logger.info("requestHandle(Socket client) called.");
    	
    	readConfig();
        this.clientSocket = client;
    }

    @Override
    public void run() {
        
    	try {
            
        	in = new BufferedReader( new InputStreamReader( clientSocket.getInputStream() ) );
            
            String clientSelection;
            String sendFileName;
            
            while( ( clientSelection = in.readLine() ) != null ) {
            	
                switch( Integer.parseInt( clientSelection ) ) {
                    case 1:
                        receiveFile();
                        break;
                    case 2:
                        while( ( sendFileName = in.readLine() ) != null ) {
                            sendFile( sendFileName );
                        }

                        break;
                        
                    case 3:
                    	
                    	while( ( sendFileName = in.readLine() ) != null ) {
                            sendFile( sendFileName );
                        }
                        
                        break;
                    
                    case 4:
                    	
                        saveFileInfo();
                        
                        break;
                        
                    case 5:
                    	
                        selectFileInfo();
                        
                        break;
                        
                    default:
                        System.out.println("Incorrect command received.");
                        logger.error("Incorrect command received.");
                        break;
                }
                
                in.close();
                break;
            }

        } 
    	catch( IOException ex ) {
            logger.error(ex.getMessage());
        } catch (InstantiationException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
    	
    }
    
    public String encryptFileName(String str) {
		
		logger.info("encryptFileName begin.");
		
		String encrypted = ""; 
		
		try{
			
			MessageDigest sh = MessageDigest.getInstance("SHA-256"); 
			
			sh.update(str.getBytes()); 
			
			byte byteData[] = sh.digest();
			
			StringBuffer sb = new StringBuffer(); 
			
			for(int i = 0 ; i < byteData.length ; i++){
				sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
			}
			
			encrypted = sb.toString();
			
		}
		catch(NoSuchAlgorithmException e){
			e.printStackTrace(); 
			encrypted = null; 
			logger.error(e.getMessage());
		}
		
		logger.info("encryptFileName end.");
		
		return encrypted;
	}
    
public void selectFileInfo() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    	
    	logger.info("selectFileInfo begin.");
    	
    	try {
        	
            DataInputStream clientData = new DataInputStream( clientSocket.getInputStream() );

            String fileInfo = in.readLine();
            //clientData.readUTF();
            //clientData.close();
            
            String fileName = "";
            String createDate = "";
            
            String[] arr;
			arr = fileInfo.split( DELIMITER );
			
			fileName = arr[0];
			createDate = arr[1];
			
			logger.info("file info = " + fileName + "[" + createDate + "] received from the client");
            
            // save the received file info into database
            String result = "RET_OK";
            
            //
            try
            {
            	// create a mysql database connection
	            Class.forName(mysql_driver).newInstance();
	            
	            Connection conn = null;
	            
	            conn = DriverManager.getConnection( mysql_url + mysql_userinfo );
                
	            logger.info("데이터 베이스 접속이 성공했습니다."); 
	        
	            // the mysql insert statement
	            String query = " select encfilename, filePath from files where fileName = ? and createDate = ?";
	
	            // create the mysql insert preparedstatement
	            PreparedStatement preparedStmt = ((java.sql.Connection) conn).prepareStatement(query);
	            preparedStmt.setString (1, fileName);
	            preparedStmt.setString (2, createDate);
	            
	            // execute select SQL stetement
				ResultSet rs = preparedStmt.executeQuery();

				while( rs.next() ) {

					String encFileName = rs.getString("encfilename");
					String filePath = rs.getString("filepath");

					System.out.println("encFileName : " + encFileName);
					System.out.println("filePath    : " + filePath);
					
					logger.info("encFileName : " + encFileName);
					logger.info("filePath    : " + filePath);
					
					result = encFileName + DELIMITER + filePath;

				}
	          
                conn.close();
            }
            catch (SQLException ex) {
                // handle any errors
                logger.error("SQLException: " + ex.getMessage());
                logger.error("SQLState: " + ex.getSQLState());
                logger.error("VendorError: " + ex.getErrorCode());
                
                result = "RET_NG";
            }
              
            // Sending the result to client.
            byte[] buffers = new byte[(int) result.length()];
            
            OutputStream os = clientSocket.getOutputStream();
            DataOutputStream dos = new DataOutputStream( os );
           
            dos.writeUTF( result );
            dos.writeLong( buffers.length );
            dos.write( buffers, 0, buffers.length );
            dos.flush();
            
            clientData.close();

            System.out.println("sent the result [" + result + "] to the client.");
            logger.info("sent the result [" + result + "] to the client.");
        } 
        catch( IOException ex ) {
            System.err.println("Client error. Connection closed.");
            logger.error("Client error. Connection closed.");
        }

    	logger.info("selectFileInfo end.");
    }

	
    public void saveFileInfo() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    	
    	logger.info("saveFileInfo begin.");
    	
    	try {
        	
            DataInputStream clientData = new DataInputStream( clientSocket.getInputStream() );

            String fileInfo = in.readLine();
            //clientData.readUTF();
            //clientData.close();
            
            String serverIP = "";
            String serverPort = "";
            String fileName = "";
            String encryptedFileName = "";
            String createDate = "";
            
            String[] arr;
			arr = fileInfo.split( DELIMITER );
			
			serverIP = arr[0];
			serverPort = arr[1];
			fileName = arr[2];
			encryptedFileName = arr[3];
			createDate = arr[4];
			
			logger.info("serverIP, serverPort = [" + serverIP + "," + serverPort + "]"); 
            logger.info("file info = " + fileName + "[" + encryptedFileName + "]" + "[" + createDate + "] received from the client");
            
            // save the received file info into database
            String result = "RET_OK";

            //
            try
            {
            	// create a mysql database connection
	            Class.forName(mysql_driver).newInstance();
	            
	            Connection conn = null;
	            
	            conn = DriverManager.getConnection( mysql_url + mysql_userinfo );
                
	            logger.info("데이터 베이스 접속이 성공했습니다.");
	        
	            // the mysql insert statement
	            String query = " insert into files (filename, encfilename, filepath, createdate)" + " values (?, ?, ?, ?)";
	
	            // create the mysql insert preparedstatement
	            PreparedStatement preparedStmt = ((java.sql.Connection) conn).prepareStatement(query);
	            preparedStmt.setString (1, fileName);
	            preparedStmt.setString (2, encryptedFileName);
	            preparedStmt.setString (3, serverIP + DELIMITER + serverPort);
	            preparedStmt.setString (4, createDate);
	          
	            // execute the preparedstatement
	            preparedStmt.execute();
	          
                conn.close();
            }
            catch (SQLException ex) {
                // handle any errors
                logger.error("SQLException: " + ex.getMessage());
                logger.error("SQLState: " + ex.getSQLState());
                logger.error("VendorError: " + ex.getErrorCode());
                
                if( ex.getSQLState().equals("23000") )
                	result = "RET_OK:23000";
                else
                	result = "RET_NG";
            }
              
            //
            
            // Sending the result to client.
            byte[] buffers = new byte[(int) result.length()];
            
            OutputStream os = clientSocket.getOutputStream();
            DataOutputStream dos = new DataOutputStream( os );
           
            dos.writeUTF( result );
            dos.writeLong( buffers.length );
            dos.write( buffers, 0, buffers.length );
            dos.flush();
            
            clientData.close();

            System.out.println("sent the result [" + result + "] to the client.");
            logger.info("sent the result [" + result + "] to the client.");
        } 
        catch( IOException ex ) {
            System.err.println("Client error. Connection closed.");
            logger.error("Client error. Connection closed.");
        }

    	logger.info("saveFileInfo end.");
    }
    
	public void receiveFile() {
		
		logger.info("receiveFile begin.");
		
        try {
        	
            int bytesRead;

            DataInputStream clientData = new DataInputStream( clientSocket.getInputStream() );

            String fileName = clientData.readUTF();
            String encryptedFileName = encryptFileName(fileName);
            
            logger.info("encrypted file name = " + fileName + "[" + encryptedFileName + "]" );
            
            OutputStream output = new FileOutputStream( encryptedFileName );
            
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            
            while( size > 0 && ( bytesRead = clientData.read( buffer, 0, (int) Math.min( buffer.length, size ) ) ) != -1 ) {
                output.write( buffer, 0, bytesRead );
                size -= bytesRead;
            }

            output.close();
            clientData.close();
            
            // move the received file to storage
            try{

            	// if not exist, make a new directory.
            	String date = new SimpleDateFormat("yyyyMMdd/").format(System.currentTimeMillis( ));
            	String strDir = storagePath + date;
            	
            	logger.info( "the storage path = " + strDir );
            	
            	File dir = new File( strDir );
                
                // attempt to create the directory here
                boolean successful = dir.mkdir();
                if( successful )
                {
                  // creating the directory succeeded
                  logger.info("directory was created successfully");
                }
                else
                {
                  // creating the directory failed
                  logger.info("failed trying to create the directory");
                }
                
         	    File afile =new File(encryptedFileName);

	     	    String newFileName = strDir + encryptedFileName;
	     	    if( afile.renameTo( new File( newFileName ) ) ) {
	     		  logger.info("File is moved successful!");
	     	    }
	     	    else{
	     	 	  logger.info("File is failed to move! [" + newFileName + "]");
	     	    }

         	}
            catch(Exception e){
            	e.printStackTrace();
            	logger.error(e.getMessage());
         	}

            System.out.println("File " + fileName + " received from client.");
            logger.info("File " + fileName + " received from client.");
        } 
        catch( IOException ex ) {
            System.err.println("Client error. Connection closed.");
            logger.error("Client error. Connection closed." + ex.getMessage());
        }
        
        logger.info("receiveFile end.");
        
    }

    public void sendFile(String fileName) {
    	
    	logger.info("sendFile begin.");
    	
        try {
        	
        	String arr[];
        	String name;
        	String createDate;
        	String strDir;
        	
            // handle file read
        	// set the file path from storage
        	if( fileName.indexOf(".config") != -1 ) {
        		strDir = workdir;
        		name = fileName;
        		createDate = "";
        	}
        	else {
        		arr = fileName.split( DELIMITER );
            	name = arr[0];
            	createDate = arr[1];
            	strDir = storagePath + createDate;
        	}
        	
        	logger.info("sendFile filename =   [" + name + "]");
        	logger.info("sendFile createDate = [" + createDate + "]");
        	logger.info( "the storage path =   [" + strDir + "]");
        	
            File filePath = new File( strDir + name );
            byte[] buffers = new byte[(int) filePath.length()];

            FileInputStream fis = new FileInputStream( filePath );
            BufferedInputStream bis = new BufferedInputStream( fis );
            //bis.read(filearray, 0, filearray.length);

            DataInputStream dis = new DataInputStream( bis );
            dis.readFully( buffers, 0, buffers.length );
            dis.close();
            
            // handle file send over socket
            OutputStream os = clientSocket.getOutputStream();

            // Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream( os ); 
            
            dos.writeUTF( filePath.getName() );
            dos.writeLong( buffers.length );
            dos.write( buffers, 0, buffers.length );
            dos.flush();
            
            System.out.println("File " + fileName + " sent to client.");
            logger.info("File " + fileName + " sent to client.");
        } 
        catch( Exception e ) {
            System.err.println("File does not exist !!! = " + fileName );
            logger.error("File does not exist !!! = " + fileName + e.getMessage());
        } 
        
        logger.info("sendFile end.");
    }
    
    private String makeLicense() {
    	
    	logger.info("makeLicense begin.");
    	
    	String strLicense = "";
    	String encrypted = "";
    	
    	try {

    		//ip = InetAddress.getLocalHost();
    		//System.out.println("Current IP address : " + ip.getHostAddress());

    		//NetworkInterface network = NetworkInterface.getByInetAddress(ip);
    		
    		InetAddress ip = InetAddress.getLocalHost();
            
    		logger.info("Current IP address : " + ip.getHostAddress());

    		byte[] mac;//network.getHardwareAddress();
    		StringBuilder sb = new StringBuilder();
    		
    		Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
		    while( networks.hasMoreElements() ) {
		       NetworkInterface network = networks.nextElement();
		       mac = network.getHardwareAddress();
		
		       if( mac != null ) {
		           
		           for( int i = 0; i < mac.length; i++ ) {
		               sb.append( String.format("%02X%s", mac[i], ( i < mac.length - 1 ) ? "-" : "") );
		           }
		           
		           logger.info("Current MAC address : " + sb );
		       }
		    }
    		//
    		
    		
    		//StringBuilder sb = new StringBuilder();
    		
//    		for (int i = 0; i < mac.length; i++) {
//    			sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
//    		}
    		
    		logger.info("Current MAC address : " + sb.toString() );
    		
    		strLicense = ip.getHostAddress() + DELIMITER + sb.toString() + DELIMITER + "sow4063";
    		
    		// SHA-256 encoding.
    		encrypted = encryptFileName(strLicense);
    		
    		logger.info("License string : " + strLicense);
    		logger.info("Encrypted license string : " + encrypted);

    	} 
    	catch (UnknownHostException e) {
    		e.printStackTrace();
    		logger.error(e.getMessage());
    	} 
    	catch (SocketException e){
    		e.printStackTrace();
    		logger.error(e.getMessage());
    	}
    	
    	logger.info("makekLicense end.");
    	
    	return encrypted;
    }
    
    public boolean checkLicense() throws IOException {
    	
    	logger.info("checkLicense begin.");
    	
    	boolean result = false;
    	
    	// read configuration
    	readConfig();
    	
    	// license file checking.
    	String path = workdir + "srv.lic";
    	
    	File file = new File( path );
		if( !file.exists() || !file.isFile() ) { 
			System.out.println("The License file does not existed.[" + path + "]");
			logger.error("The License file does not existed.[" + path + "]");
		    return result;
		}
    	
    	// make license string for this server.
    	String str = makeLicense();
    	
    	// check if the license file is correct.
    	FileReader fr = new FileReader(file);
    	BufferedReader br = new BufferedReader(fr);

    	String strLicense = br.readLine();
    	
    	logger.info("checkLicense. made = [" + str + "]");
		logger.info("checkLicense. file = [" + strLicense + "]");
		
    	if( str.equals(strLicense) ) {
    		result = true;
    	}
    	
    	br.close();
    	
    	logger.info("checkLicense end. rc=[" + result + "]");
    	
    	return result;
    }
}