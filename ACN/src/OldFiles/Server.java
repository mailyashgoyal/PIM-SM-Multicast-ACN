/**
 * Include API's
 */
package OldFiles;
import java.net.*; 
/**
 * Class : Server: Sets up the UDP connection,It Receives get host name , port number , 
 * sequence number from the user and gets the data from the client .
 * @author Yash Goyal Dated : 24 Sept 2012
 *
 */
class Server { 

	//Declaring the Variables
	static String  hostName  = null;
	static String  portNumber  = null;
	static Integer SequenceInput = null;

	/**
	 * Main method :Sets up the UDP connection, receives and acknowledges the data
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception 
	{ 
		//Get the host Name from the user in command line
		try 
		{
			hostName = args[0];
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			System.out.println("Need argument: remoteHost");
			System.exit(-1);
		}

		//Get the port number in the command line
		try 
		{
			portNumber = args[1];
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			System.out.println("Provide port number");
			System.exit(-1);
		}

		//Check if the port number given by the user is of proper format or not, else again ask user input.
		Integer port = -1;
		if(isInteger(portNumber))
		{ port = Integer.parseInt(args[1]);}
		else
		{ 
			System.out.println("Need valid argument: PortNumber");
			System.exit(-1);
		}

		//Get the Max sequence number/ window/ frame size from the user
		if(isInteger(args[2]))
		{SequenceInput = Integer.parseInt(args[2]);}
		else
		{
			System.out.println("Need valid argument: SeqNumber");
			System.exit(-1);
		}

		//Setting up the server socket
		DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(port.toString()));

		//Initializing local variables
		Integer expectedSeqNo = 0;

		//Initializing the byte array to keep the send and receive data.
		byte[] receiveData = new byte[1024]; 
		byte[] sendData  = new byte[1024]; 
		Integer seqNo = -1;

		while(true) 
		{ 
			//Receive the packet from client
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
			serverSocket.receive(receivePacket); 
			//InetAddress IPAddress = receivePacket.getAddress();
			InetAddress IPAddress = receivePacket.getAddress(); 
		    int ports = receivePacket.getPort(); 


			//Display the received packet
			String sentence = new String(receivePacket.getData()); 
			System.out.println("Recv: " + sentence);


			//4 is taken because Data string size is 4 [D,A,T,A] Size == 4
			if(sentence.length() > 4)
			{
				String Akn = sentence.substring(5);
				int nextSpace = Akn.indexOf(" ");
				seqNo = Integer.parseInt(Akn.substring(0,nextSpace).trim());
			}
			
			//Extract the sequence number from the received data
			if(expectedSeqNo == seqNo )
			{
				if(expectedSeqNo <(SequenceInput -1))
				{
					expectedSeqNo++;
				}
				else 
				{
					expectedSeqNo =0;
				}
			}

			seqNo  = expectedSeqNo - 1;
			if(seqNo < 0)
			{seqNo = SequenceInput -1 ;}

			String sentAkn = "ACK " + seqNo.toString();
			sendData = sentAkn.getBytes();

			//Send the packet back to the client 
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, ports); 
			serverSocket.send(sendPacket); 
			System.out.println("Send: " + sentAkn);
			System.out.println("");
		}
	} 

	/**
	 * Method to check if the input provided by user is Integer or not
	 * @param input
	 * @return flag
	 */
	public static boolean isInteger(String input) 
	{
		//Handling number format exception if the number is not of type integer
		try 
		{
			Integer.parseInt(input);
			return true;
		} 
		//return true/false depending upon the user input
		catch (NumberFormatException e) 
		{ return false; }
	}
} 