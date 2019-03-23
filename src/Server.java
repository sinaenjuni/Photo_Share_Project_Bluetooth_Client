
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier; 



class Server{
	  
    Server(){ 
    	
    	
		log("Local Bluetooth device...\n");
        
    	LocalDevice local = null;
		try {
			
			local = LocalDevice.getLocalDevice();
		} catch (BluetoothStateException e2) {
			
		}   
		
    	log( "address: " + local.getBluetoothAddress() );
    	log( "name: " + local.getFriendlyName() );
    	
    	
    	Runnable r = new ServerRunable();
    	Thread thread = new Thread(r);
    	thread.start();
    	
    }
    
      
    private static void log(String msg) {  
    	
        System.out.println("["+(new Date()) + "] " + msg);  
    }

}


class ServerRunable implements Runnable{
	  
	//UUID for SPP
		final UUID uuid = new UUID("0000110100001000800000805F9B34FB", false);
	    final String CONNECTION_URL_FOR_SPP = "btspp://localhost:"
	    			+ uuid +";name=SPP Server";
	  
	    private StreamConnectionNotifier mStreamConnectionNotifier = null;  
	    private StreamConnection mStreamConnection = null; 
	    private int count = 0;
	    private boolean recvImage = false;
	   
	    
		@Override
		public void run() {

	    	try {
	    		
				mStreamConnectionNotifier = (StreamConnectionNotifier) Connector
							.open(CONNECTION_URL_FOR_SPP);
				
				log("Opened connection successful.");
			} catch (IOException e) {
				
				log("Could not open connection: " + e.getMessage());
				return;
			}
	   

	    	log("Server is now running.");

	    	
	    	
	        while(true){
	        	
	        	log("wait for client requests...");

				try {
					
					mStreamConnection = mStreamConnectionNotifier.acceptAndOpen();
				} catch (IOException e1) {
					
					log("Could not open connection: " + e1.getMessage() );
				}
				
	        	
				count++;
				log("현재 접속 중인 클라이언트 수: " + count);
				
									
		        new Receiver(mStreamConnection).start();
	        }
			
		}
		
	        
	    
	    class Receiver extends Thread {
	    	
	    	private InputStream mInputStream = null; 
	        private OutputStream mOutputStream = null; 
	        private String mRemoteDeviceString = null;
	        private StreamConnection mStreamConnection = null;
	        
	        
	        Receiver(StreamConnection streamConnection){
	        	
	        	mStreamConnection = streamConnection;

				try {
				    	
					mInputStream = mStreamConnection.openInputStream();
					mOutputStream = mStreamConnection.openOutputStream();
										
					log("Open streams...");
				} catch (IOException e) {
					
					log("Couldn't open Stream: " + e.getMessage());

					Thread.currentThread().interrupt();		
					return;
				}
				
				
				try {
			        	
						RemoteDevice remoteDevice 
							= RemoteDevice.getRemoteDevice(mStreamConnection);
						
				        mRemoteDeviceString = remoteDevice.getBluetoothAddress();
				        
						log("Remote device");
						log("address: "+ mRemoteDeviceString);
				        
					} catch (IOException e1) {
						
						log("Found device, but couldn't connect to it: " + e1.getMessage());
						return;
				}
				
				log("Client is connected...");
	        }
	        
	        
	      
	        
	        
	    	@Override
			public void run() {
	    		
				try {
					
		    		Reader mReader = new BufferedReader(new InputStreamReader
				         ( mInputStream, Charset.forName(StandardCharsets.UTF_8.name())));
					
		    		boolean isDisconnected = false;
		    		StringBuilder imageStringBuilder = null;
		    		
					Sender("에코 서버에 접속하셨습니다.");
					Sender( "보내신 문자를 에코해드립니다.");
		    		
					while(true){

						log("ready");
		
				        
			            StringBuilder stringBuilder = new StringBuilder();
			            int c = 0;
			            
			            
						while ( '\n' != (char)( c = mReader.read()) ) {
							
							if ( c == -1){
								
								log("Client has been disconnected");
								
								count--;
								log("현재 접속 중인 클라이언트 수: " + count);
								
								isDisconnected = true;
								Thread.currentThread().interrupt();
								
								break;
							}
							
							stringBuilder.append((char) c);
						}
		
			            if ( isDisconnected ) break;
			            
			            String recvMessage = stringBuilder.toString();
				        log( mRemoteDeviceString + ": [" + recvMessage +"]" );
				        
				        if (recvMessage.matches("Start.*")) {
				        	recvImage = true;

				        	log("start recv image "+ recvMessage.substring(5));
				        	int size = Integer.parseInt(recvMessage.substring(5));
				        
				    		DataInputStream dis = new DataInputStream(mInputStream);
				    		FileOutputStream fos = new FileOutputStream("testfile.jpg");
				    		byte[] buffer = new byte[4096];
				    		
				    		int read = 0;
				    		int totalRead = 0;

				    		while((read = dis.read(buffer)) > 0) {
				    			totalRead += read;

				    			System.out.println("read " + totalRead + "/" + size + "  bytes.");		
				    		
				    			fos.write(buffer, 0, read);
				    			
				    			if ( totalRead >= size) break;
				    		}
				    		
				    		log("end");
				    		
				    		fos.flush();
				    		fos.close();
				    		
				    		recvImage = false;
				    		recvMessage = "recv Image";
				        }
				        


				        if (recvImage == false )
				        	Sender(recvMessage);

					}
					
				} catch (IOException e) {
					
					log("Receiver closed" + e.getMessage());
				}
			}
	    	

	    	void Sender(String msg){
	        	
	            PrintWriter printWriter = new PrintWriter(new BufferedWriter
	            		(new OutputStreamWriter(mOutputStream, 
	            				Charset.forName(StandardCharsets.UTF_8.name()))));
	        	
	    		printWriter.write(msg+"\n");
	    		printWriter.flush();
	    		
	    		log( "Me : " + msg );
	    	}
		}
	    
	    
	    private static void log(String msg) {  
	    	
	        System.out.println("["+(new Date()) + "] " + msg);  
	    }
	        
	}  