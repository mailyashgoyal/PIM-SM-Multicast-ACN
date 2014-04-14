/**
 * Include the API's.
 */
import java.io.*; 
import java.net.*; 
import java.util.ArrayList;

/**
 * Client Class : Creates UDP connection and Implement Sliding Window protocol
 * @Input Takes hostName, Server Port, Window/Frame Size and inputString
 * @Output null
 * @author Yash Goyal, Dated : 24 Sept 2012
 *
 */
class Client
{ 
	//Declaring variables
	static int servPort = 1142;
	static String  hostName  = null;
	static DatagramSocket clientSocket ;
	static InetAddress IPAddress;

	/**
	 * Main class to execute sliding window protocol to a specific sever
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception 
	{
		//Get the host Name
		try 
		{
			hostName = args[0];
			//hostName = "net01";
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			System.out.println("Need argument: remoteHost");
			System.exit(-1);
		}

		//Initialize UDPSlideWindow Class to perform sliding window logic over input of specific strings
		UDPSlideWindow udp =  new UDPSlideWindow();

		// Initializing local variables
		clientSocket = new DatagramSocket(); 
		IPAddress = InetAddress.getByName(hostName);
		int windSize = 0;

		//Providing 1024 to receive and Send Data byte to be provided to server
		byte[] sendData = new byte[1024]; 
		byte[] receiveData = new byte[1024]; 

		System.out.println("Enter the port number");
		BufferedReader inPortNumber = new BufferedReader(new InputStreamReader(System.in)); 
		String portNumber = inPortNumber.readLine();
		while (isInteger(portNumber) == false)
		{
			System.out.println("please provide port number in correct format");
			inPortNumber = new BufferedReader(new InputStreamReader(System.in));
			portNumber = inPortNumber.readLine();
		}
		servPort = Integer.parseInt(portNumber);

		//Get the window Size from the user
		System.out.println("Enter the window size");
		BufferedReader inWindSize = new BufferedReader(new InputStreamReader(System.in)); 
		String windSizeInput = inWindSize.readLine();

		//Checking if the input provided by the user has the specific format(Integer) else user to again provide the
		//input in correct format
		while(isInteger(windSizeInput) == false)
		{
			System.out.println("Please provide numerical window size");
			windSizeInput = inWindSize.readLine();
		}
		windSize = Integer.parseInt(windSizeInput);

		//Get the Input String to be passed to the server from the user
		System.out.println("Provide Input String");
		//Let say we are taking input just once and then send the packets and also get the acknowledgement
		BufferedReader inFromUserToSend = new BufferedReader(new InputStreamReader(System.in));
		String input  = inFromUserToSend.readLine();

		//If the window size is greater than the input provided by the user, ask user to provide the input again.
		while (windSize > input.length())
		{
			System.out.println("Input provided is less than window size, Please provide input again");
			inFromUserToSend = new BufferedReader(new InputStreamReader(System.in));
			input  = inFromUserToSend.readLine();
		}
		//Acknowledging the input provided by the user and dispalying on the screen.
		System.out.println("Input is : " + input);

		//Variable for setting the input length 
		int stringTobeSent = input.length();

		// Handling the socket timeout exception
		try 
		{
			clientSocket.setSoTimeout(5000);
		}
		catch (SocketException e)
		{
			System.out.println("Socket Timeout Occured");
		}

		//Providing ipAddress, clientSocket and server port to UDPSlideWindow class
		udp.IPAddress = IPAddress;
		udp.clientSocket = clientSocket;
		udp.servPort = servPort;

		//Start the logic of sending the data to the server
		udp.PerformSliding(input, windSize, sendData,receiveData, stringTobeSent);

		//Close ClientSocket
		clientSocket.close(); 
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
		{  return false;}

	}

}

/**
 * Class: UDPSlideWindow: It create packets that needs to be send to the server.
 * Handles the data in such a way that only window size specific packets are sent to the server
 * Recieve acknowledgement from the server
 * Resend the data if there is some data loss or server time out 
 * @author Yash Goyal Dated : 2 Sept 2012
 *
 */
class UDPSlideWindow
{
	//Declaring Local Variables
	int charPositionSent = 0;
	DatagramSocket clientSocket ;
	InetAddress IPAddress;
	int SlidingCounter = -1;
	int servPort = 0;

	//Default Constructor
	UDPSlideWindow()
	{
	}

	/**
	 * PerformSliding Method iterate for the input provided by the user on specific window frames.
	 * Then provide sequence number to the data and then send the data to the server.
	 * It then receives the acknowledgement from server. Parse the input and check if the sequence number provided
	 * by the server is same as what was sent to it. 
	 * in case of any discrepancy between the data, this method resends the data to the server
	 * @param input
	 * @param windSize
	 * @param sendData
	 * @param receiveData
	 * @param stringTobeSent
	 * @throws IOException
	 */
	public void PerformSliding(String input,  int windSize,byte[] sendData,byte[] receiveData , int stringTobeSent ) throws IOException
	{
		//Getting the Sequencing of all the characters of the input
		int NumOfWindow = input.length()/ windSize;
		int SeqArray[] = new int[input.length()];
		int total = 0;
		for(int q =0 ;q < NumOfWindow ; q++)
		{
			for(int w = 0; w < windSize ;w++)
			{
				SeqArray[total] = w;
				total++;
			}
		}
		for(int q= 0 ;q < (input.length() -total);q++)
		{
			SeqArray[total + q] = q;
		}

		// Counter used for checking if the data is received by server for the very first time or not
		SlidingCounter++;

		//divide the input in the groups of frame size
		int windIterator = 0;

		//Initializing local variables
		int initialStringLength = stringTobeSent;
		boolean UnexpectedReturn = false;
		for(int j = 0; j < initialStringLength ;j++)
		{
			//Array stores all the received acknowledgments
			ArrayList<Integer> returnarray = new ArrayList<Integer>();
			
			//WindIterator is used so as to take care of the last few left characters which are less than the window size frame
			if(stringTobeSent < windSize)
			{
				windIterator = stringTobeSent;
			}
			else
			{
				windIterator = windSize;
			}
			System.out.println("");
			//Sending the data and respective sequence numbers to the server
			for (int i=0 ; i < windIterator ;i++)
			{
				//String displayString = "DATA " + i + " " +input.charAt((input.length() - stringTobeSent) +  i);
				String displayString = "DATA " + SeqArray[(input.length() - stringTobeSent) +  i] + " " +input.charAt((input.length() - stringTobeSent) +  i);
				System.out.println(displayString);
				sendData = displayString.getBytes();

				SendData(sendData,i);
			}
			System.out.println("");
			int MaxSeqNoRecieved = -1;
			//Getting the recieved acknowledgements or the ERR or null(socket time out) values 
			for (int i=0 ; i < windIterator ;i++)
			{
				String modifiedSentence = null;
				if( i==0 && j ==0 && SlidingCounter == 0)
				{
					//For the very first time, if the recieved data is -1 then we will see and re send the data
					modifiedSentence = ReceiveData(input, windSize, sendData, receiveData, stringTobeSent, true);
					if(modifiedSentence != null)
					{int nextSpace = modifiedSentence.indexOf(" ");
					if(MaxSeqNoRecieved < Integer.parseInt(modifiedSentence.substring(nextSpace +1).trim()))
						MaxSeqNoRecieved= Integer.parseInt(modifiedSentence.substring(nextSpace +1).trim());}
				}
				else
				{
					//When the received data is proper
					modifiedSentence = ReceiveData(input, windSize, sendData, receiveData, stringTobeSent, false);
					if(modifiedSentence != null) {int nextSpace = modifiedSentence.indexOf(" ");
					if(MaxSeqNoRecieved < Integer.parseInt(modifiedSentence.substring(nextSpace +1).trim()))
						MaxSeqNoRecieved = Integer.parseInt(modifiedSentence.substring(nextSpace +1).trim());}
				}

				//If the received data contains some error then re send the data
				if(modifiedSentence == null || modifiedSentence.startsWith("ACK -1") || modifiedSentence.startsWith("ERR"))
				{
					//Unexpected return due to incorrect data, session time out , send the data
					UnexpectedReturn = true;
					System.out.println("Recieved ACK -1/ERR, so sending data again");
					if(MaxSeqNoRecieved != -1)
					{
						//If we received ack in between then increase the window size
						//charPositionSent = (input.length() - stringTobeSent) +  lastRecdAk;
						stringTobeSent = input.length() - MaxSeqNoRecieved - 1 -(SlidingCounter*windSize);

					}
					//Minimize the j iterator and begin from the beginning
					j--;
					break;
				}
				
				//If there was an unexpected return then do not follow the code to get the sequence and add them into array. 
				if(UnexpectedReturn == false)
				{
					int nextSpace = modifiedSentence.indexOf(" ");
					Integer ackSeqNo = Integer.parseInt(modifiedSentence.substring(nextSpace +1).trim());

					if(i == ackSeqNo);
					{
						System.out.println(modifiedSentence );
					}
					returnarray.add(ackSeqNo);
				}
			}
			if(UnexpectedReturn == false)
			{
				if(returnarray.size() > 0)
				{
					//check Individual input
					int counter = 0;
					int NotRecievedCounter = 0;
					int lastRecdAk = -1;
					for (int i = 0; i< returnarray.size(); i++)
					{
						//Check how many proper and improper inputs we have encountered and store the value
						if(Integer.parseInt(returnarray.get(i).toString()) == i)
						{
							counter++;
							lastRecdAk = Integer.parseInt(returnarray.get(i).toString()) + 1;
						}
						else
						{
							if(lastRecdAk > i)
							{
								//lastRecdAk = lastRecdAk;
							}
							else
							{
								lastRecdAk = Integer.parseInt(returnarray.get(i).toString()) + 1;
							}
							NotRecievedCounter++;
						}
					}

					//Calculate the remaining strings to be send to the server again
					charPositionSent = (input.length() - stringTobeSent) +  lastRecdAk;
					stringTobeSent = input.length() - charPositionSent;

					//If everything is smooth
					if (counter == windIterator && NotRecievedCounter == 0)
					{
						System.out.println("All packets recieved for a window");
					}
					//If we have got some mismatch then call the method recursivly
					else if(NotRecievedCounter > 0 )
					{
						PerformSliding(input, windSize, sendData, receiveData,stringTobeSent);
					}
					j = charPositionSent;
				}
				else
				{
					//Didn't get anything acknowledged so re send data
					PerformSliding(input, windSize, sendData, receiveData, stringTobeSent);
				}
			}
			UnexpectedReturn = false;
		}
	}

	/**
	 * Method : Send Data : Sends the data to the server.
	 * @param sendData
	 * @param i
	 * @throws IOException
	 */
	public void SendData(byte[] sendData,int i) throws IOException
	{
		// we need to re send data..
		//1) when the acknowledge is not received
		//2) the time out occurs
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, servPort); 
		clientSocket.send(sendPacket); 
	}

    /**
     * Method : ReceiveData: Receives the data from the server and checks if it proper or not.
     * @return
     * @throws IOException
     */
	public String ReceiveData(String input,  int windSize,byte[] sendData,byte[] receiveData , int stringTobeSent, boolean isFirst) throws IOException
	{
		String returnMessage = null;
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
		
		//Setting the time out for the server.
		try
		{
			clientSocket.receive(receivePacket);
		}
		catch (SocketTimeoutException se)
		{
			//Return to the parent method if there is a time out.
			System.out.println("Socket time out sending again");
			return returnMessage;
		}
		returnMessage = new String(receivePacket.getData());
		if(isFirst == true && SlidingCounter == 0)
		{
			//Check if we are getting proper data for the very first time or not.
			int nextSpace = returnMessage.indexOf(" ");
			Integer ackSeqNo = Integer.parseInt(returnMessage.substring(nextSpace +1).trim());
			if(ackSeqNo == -1)
			{
				return returnMessage;
			}
		}
		return returnMessage;
	}



}
