package com.divine.panera;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Properties;

//for encrypt and decrypt
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import org.apache.logging.log4j.LogManager;

public class panera {
	private static Socket sock;
	private static PrintStream os;
	
	private String serverIP;
	private String serverPort;
	private String agentIP;
	private String agentPort;
	private String logPath;
	private String cachePath;
	private String workdir;
	
	public static final String DEFAULT_ENCODING = "UTF-8"; 
    static BASE64Encoder enc = new BASE64Encoder();
    static BASE64Decoder dec = new BASE64Decoder();
    public static final String DELIMITER = "#";
    public static final String SRT_SEND = "1";
    public static final String SRT_RECV = "2";
    public static final String SRT_SRVINF = "3";
    public static final String SRT_SAVEINF = "4";
    public static final String SRT_RCVINF = "5";
    
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger("panera");
	
    
	public panera() throws Exception {
		
		logger.info("panera.constructor begin.");
		
		// initialize the class : read configuration.
		readConfig();
		
		// ask file server info to agent server.
		askFileServers();
		
		logger.info("panera.constructor end.");
		
	}
	
	public static boolean isWindows( String str ) {

		return (str.indexOf("win") >= 0);

	}

	public static boolean isMac( String str ) {

		return (str.indexOf("mac") >= 0);

	}

	public static boolean isUnix( String str ) {

		return (str.indexOf("nix") >= 0 || str.indexOf("nux") >= 0 || str.indexOf("aix") > 0 );
		
	}

	public static boolean isSolaris( String str ) {

		return (str.indexOf("sunos") >= 0);

	}
	
	public int readConfig() throws IOException {
		
		logger.info("readConfig begin.");
		
		Properties prop = new Properties();
		InputStream input = null;

		try {
			
			String strOS = System.getProperty("os.name").toLowerCase();
			
			String pconfig = System.getProperty("panera.config").toLowerCase();
			
		    if( pconfig == null ) {
                logger.info("need file path for panera.config");
		    }
			
			logger.info( strOS + " : " + pconfig );
					
			input = new FileInputStream( pconfig );

			// load a properties file
			prop.load( input );

			// get the property value and print it out
			agentIP = prop.getProperty("agentip") ;
			agentPort = prop.getProperty("agentport") ;
			workdir = prop.getProperty("workdir");
			logPath = workdir + prop.getProperty("logpath") ;
			cachePath = workdir + prop.getProperty("cachepath") ;
			
			logger.info( "agent serverip = " + agentIP );
			logger.info( "agent serverport = " + agentPort );
			logger.info( "workdir = " + workdir );
			logger.info( "logpath = " + logPath );
			logger.info( "cachepath = " + cachePath );

		} 
		catch( IOException ex ) {
			ex.printStackTrace();
			logger.error( ex.getMessage() );
		} 
		
		input.close();
		
		logger.info("readConfig end.");
		
		return 1;
	}
	
	public String encryptFileName(String str) {
		
		logger.info("endcryptFileName begin.");
		
		String encrypted = ""; 
		
		try{
			
			MessageDigest sh = MessageDigest.getInstance("SHA-256"); 
			
			sh.update(str.getBytes()); 
			
			byte byteData[] = sh.digest();
			
			StringBuffer sb = new StringBuffer(); 
			
			for( int i = 0 ; i < byteData.length ; i++ ){
				sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
			}
			
			encrypted = sb.toString();
			
		}
		catch(NoSuchAlgorithmException e){
			e.printStackTrace(); 
			encrypted = null; 
			logger.error( e.getMessage() );
		}
		
		logger.info("encryptFileName end.");
		
		return encrypted;
	}

    public static String base64encode(String text) {
        try {
            return enc.encode(text.getBytes(DEFAULT_ENCODING));
        } 
        catch (UnsupportedEncodingException e) {
        	return null;
        }
    }

    public static String base64decode(String text) {
        try {
            return new String(dec.decodeBuffer(text), DEFAULT_ENCODING);
        } 
        catch (IOException e) {
            return null;
        }
    }
	
	public String checkCache( String fileName, String encryptedFileName, String createDate ) {
		
		logger.info("checkCache begin.");
		
		//String[] arr;
		
		String name = base64encode( serverIP + DELIMITER + serverPort + DELIMITER + fileName + DELIMITER + encryptedFileName + DELIMITER + createDate);
		
		File file = new File( cachePath + name );
		if( file.exists() && !file.isDirectory() ) { 
		 
//		    arr = base64decode( name ).split( DELIMITER );
//		    
//		    for(int i = 0; i < arr.length; i++ ) {
//		    	System.out.println(arr[i]);
//		    }
			logger.info("The file is in the cache = [" + name+ "]");
		    
		}
		else {
			logger.info("Faile to find the file in the cache = [" + cachePath + name+ "]");
			name = "";
		}
		
		logger.info("checkCache end.");
		
		return name;
	}
	
	public void saveCache( String fileName, String encryptedFileName, String createDate ) {
		
		logger.info("saveCache begin.");
		
		String name = base64encode( serverIP + DELIMITER + serverPort + DELIMITER + fileName + DELIMITER + encryptedFileName + DELIMITER + createDate);
		
		try {

	        File file = new File( name );
	        
	        logger.info("cache info = [" + file.getAbsolutePath() + "]");
	
	        if( file.createNewFile() ) {
	        	logger.info("File is created!");
	        }
	        else {
	        	logger.info("File already exists");
	        }
	        
	        // move to the cache directory
	        if( file.renameTo( new File( cachePath + name ) ) ) {
     		    logger.info("File is moved successful!! [" + name + "]");
     	    }
     	    else {
     	 	    logger.info("File is failed to move! [" + name + "]");
     	    }
		
	    } 
		catch (IOException e) {
	      e.printStackTrace();
	      logger.info( e.getMessage() );
		}
		
		logger.info("saveCache end.");
		
	}
	
public String receiveFileInfo(String fileName, String createDate ) throws IOException {
		
		logger.info("receiveFileInfo begin.");
		
		String result = "";
		
		// establish connection to server
		try {
			sock = new Socket( agentIP, Integer.parseInt( agentPort ) );
		}
		catch(Exception e) {
			logger.error( "Can not connect to agent server, try again later." + e.getMessage() );
			System.exit(1);
		}
		
		os = new PrintStream( sock.getOutputStream() );
		
		String fileInfo = fileName + DELIMITER + createDate;
		
		os.println(SRT_RCVINF);
		os.println(fileInfo);
        
		// read the result
		try {
        	
            InputStream in = sock.getInputStream();

            DataInputStream clientData = new DataInputStream( in );
            
            result = clientData.readUTF();
            
            in.close();
            
            logger.info("ReceiveInfo result = [" + result + "] received from Agent Server.");
        } 
        catch( IOException ex ) {
            logger.error( ex.getMessage() );
        }
		
		sock.close();
		
		logger.info("receiveFileInfo end.");
		
		return result;
	}

	
	public void sendFileInfo(String fileName, String encryptedFileName, String createDate ) throws IOException {
		
		logger.info("sendFileInfo begin.");
		
		// establish connection to server
		try {
			sock = new Socket( agentIP, Integer.parseInt( agentPort ) );
		}
		catch(Exception e) {
			logger.error( "Can not connect to agent server, try again later." + e.getMessage() );
			
			System.exit(1);
		}
		
		os = new PrintStream( sock.getOutputStream() );
		
		String fileInfo = serverIP + DELIMITER + serverPort + DELIMITER + fileName + DELIMITER + encryptedFileName + DELIMITER + createDate;
		
		os.println(SRT_SAVEINF);
		os.println(fileInfo);
        
		// read the result
		try {
        	
            InputStream in = sock.getInputStream();

            DataInputStream clientData = new DataInputStream( in );
            
            String result = clientData.readUTF();
            
            in.close();

            logger.info("SaveInfo result = [" + result + "] received from Agent Server.");
        } 
        catch( IOException ex ) {
            logger.error( ex.getMessage() );
        }
		
		sock.close();
		
		logger.info("sendFileInfo end.");
	}
	
	public void sendFile(String fileName) throws Exception {
		
		logger.info("sendFile begin.");
		
		// establish connection to server
		try {
			sock = new Socket( serverIP, Integer.parseInt( serverPort ) );
		}
		catch(Exception e) {
			logger.error( "Can not connect to server, try again later." + e.getMessage() );
			System.exit(1);
		}
		
		os = new PrintStream( sock.getOutputStream() );
		os.println(SRT_SEND);
		
        try {
        	
        	// encrypt file name by SHA256
            int index = fileName.lastIndexOf("/");
            String name = fileName.substring(index + 1);
        	String encryptedFileName = encryptFileName( name );
        	
        	logger.info("encrypted file name = " + name + "[" + encryptedFileName + "]" );
        	logger.info("sendFile filePath = " + fileName);
        	
            File filePath = new File( fileName );
            byte[] buffers = new byte[(int) filePath.length()];

            FileInputStream fis = new FileInputStream( filePath );
            BufferedInputStream bis = new BufferedInputStream( fis );
            //bis.read(filearray, 0, filearray.length);

            DataInputStream dis = new DataInputStream( bis );
            dis.readFully( buffers, 0, buffers.length );
            dis.close();
            
            OutputStream os = sock.getOutputStream();

            // Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream( os );
            
            dos.writeUTF( filePath.getName() );
            dos.writeLong( buffers.length );
            dos.write( buffers, 0, buffers.length );
            dos.flush();
            
            logger.info("File " + fileName + " sent to Server.");
            
            // Save the file info to cache
            String createDate = new SimpleDateFormat("yyyyMMdd/").format(System.currentTimeMillis( ));
            saveCache( fileName, encryptedFileName, createDate );
            
        } 
        catch( Exception e ) {
            logger.error( "Error on sendFile " + e.getMessage() );
        }
        
        sock.close();
        
        logger.info("sendFile end.");
    }

    public void receiveFile(String fileName) throws Exception {
    	
    	logger.info("receiveFile begin");
    	
    	try {
    		sock = new Socket( serverIP, Integer.parseInt( serverPort ) );
		}
		catch( Exception e ) {
			logger.error( "Can not connect to server, try again later." + e.getMessage() );
			
			System.exit(1);
		}
		
		os = new PrintStream( sock.getOutputStream() );
		
		os.println(SRT_RECV);
		os.println( fileName );
    	
        try {
        	
            int bytesRead;
            
            InputStream in = sock.getInputStream();

            DataInputStream clientData = new DataInputStream( in );
            
            fileName = clientData.readUTF();
            OutputStream output = new FileOutputStream( fileName );
            
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            
            while( size > 0 && ( bytesRead = clientData.read( buffer, 0, (int) Math.min( buffer.length, size ) ) ) != -1 ) {
                output.write( buffer, 0, bytesRead );
                size -= bytesRead;
            }

            output.close();
            in.close();

            logger.info("File " + fileName + " received from File Server.");
        } 
        catch( IOException ex ) {
            logger.error( ex.getMessage() );
        }
        
        sock.close();
        
        logger.info("receiveFile end.");
    }
    
    public void askFileServers() throws Exception {
    	
    	logger.info("askFileServers begin.");
    	
    	try {
    		sock = new Socket( agentIP, Integer.parseInt( agentPort ) );
		}
		catch( Exception e ) {
			logger.error( "Can not connect to server, try again later." + e.getMessage() );
			
			System.exit(1);
		}
		
    	String fileName = "fileserver.config";
    	
		os = new PrintStream( sock.getOutputStream() );
		
		os.println(SRT_SRVINF);
		os.println( fileName );
    	
		try {
        	
            int bytesRead;
            
            InputStream in = sock.getInputStream();

            DataInputStream clientData = new DataInputStream( in );
            
            fileName = clientData.readUTF();
            OutputStream output = new FileOutputStream( fileName );
            
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            
            while( size > 0 && ( bytesRead = clientData.read( buffer, 0, (int) Math.min( buffer.length, size ) ) ) != -1 ) {
                output.write( buffer, 0, bytesRead );
                size -= bytesRead;
            }
            
            output.close();
            in.close();

            logger.info("File " + fileName + " received from Server.");
        } 
        catch( IOException ex ) {
            logger.error( ex.getMessage() );
        }
        
        sock.close();
        
        // move the file to the client directory
        File file = new File( fileName );
        File fileToMove = new File( workdir + fileName );

        boolean isMoved = file .renameTo( fileToMove );
        if( isMoved )
        	setFileServers();
        
		logger.info("askFileServers end.");
			
    }
    
    public int setFileServers() throws IOException {
		
		logger.info("setFileServers begin.");
		
		Properties prop = new Properties();
		InputStream input = null;

		try {
			
			String strOS = System.getProperty("os.name").toLowerCase();
			
			String pconfig = "";
			
			if( isWindows(strOS) ) {
				pconfig = workdir + "fileserver.config";
			} 
			else {
				pconfig = workdir + "fileserver.config";
			}
			
			logger.info( strOS + " : " + pconfig );
					
			input = new FileInputStream( pconfig );

			// load a properties file
			prop.load( input );

			// get the property value and print it out
			int fileServerCnt = Integer.parseInt( prop.getProperty("fileservercnt") );
						
			if( fileServerCnt > 0 ) {

				String[] fileServerIP   = new String[fileServerCnt];
				String[] fileServerPort = new String[fileServerCnt];
				
				for( int i = 0; i < fileServerCnt; i++ ) {
					fileServerIP[i] = prop.getProperty( "fileserverip" + (i + 1) );
					fileServerPort[i] = prop.getProperty( "fileserverport" + (i + 1) );
				}
				
				serverIP = fileServerIP[0];
				serverPort = fileServerPort[0];
				
				logger.info("serverIP : " + serverIP );
				logger.info("serverPort : " + serverPort );
			}

		} 
		catch( IOException ex ) {
			ex.printStackTrace();
			logger.error( ex.getMessage() );
		} 
		
		input.close();
		
		logger.info("setFileServer end.");
		
		return 1;
	}
    
} // class panera