/**
 * 
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream.GetField;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * @author 
 *
 */
public class Router {

	//Declaring the Variables
	static String  hostName  = null;
	static String  portNumber  = null;
	static String routerId = null;

	static String configfile;
	static String config_rp;
	static String config_topo;
	static int maxRouters = 0;
	static HashMap<String, Socket> activeClients = new HashMap<String, Socket>();
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		//Get CommandLine Arguments :: Router Id, Configuration Files
		GetCommandLineArgs(args);

		//Read Files
		ArrayList<ArrayList<String>> configArray =  readConfig();
		ArrayList<ArrayList<String>> topoArray  = readConfigTopo();
		ArrayList<ArrayList<String>> rpArray  = readConfigRp();

		ArrayList<RoutingTable> forwardingTable = new ArrayList<RoutingTable>();

		//create socket
		ServerSocket welcomeSocket = null;
		Socket clientSocket = null;
		for(int i=0; i <configArray.size() ;i++)
		{
			//get port number and host name
			if(Integer.parseInt((configArray.get(i)).get(0)) == Integer.parseInt(routerId))
			{
				hostName = (configArray.get(i)).get(1);
				portNumber = (configArray.get(i)).get(2);
				try {
					welcomeSocket = new ServerSocket(Integer.parseInt(portNumber));
				} catch (IOException e) {

					e.printStackTrace();
				}
			}

			if(welcomeSocket != null)
			{
				// this will create socket from the file... 
				//as soon as the socket gets created for the first time for the file it exits.
				break;
			}
		}

		//in that we need to have all the host registered.// means allow for the connection between all the hosts connected to that router.
		// check n the file the number of hosts for that router and accept the connection. and also store the multicast group name.

		try {
			//Ask user if he wants to send the join or whatever request or to receive
			//keep the option available every time so that he can communicate with the other router

			// Ask user what he would like to do

			/*
			System.out.println("what Operation Press \n 1: Join \n 2: Prune \n 3: Multicast");
			BufferedReader userInput =  new BufferedReader(new InputStreamReader(System.in));
			String input  = userInput.readLine();
			Send(input, clientSocket);
			 */

			// fetch from router host connection file
			HashMap<String, Socket>  hostConnection = getConnectedRouterHostname(routerId); 

			while(true) 
			{
				Socket connectionSocket = welcomeSocket.accept();
				//always have receive command open
				BufferedReader inputRecieved = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 
				String sentence = inputRecieved.readLine();
				/// this command will read all the inputs from other routers

				if(sentence.length() > 4)
				{
					// till two spaces as every message has 2 spaces
					int firstSpace = sentence.indexOf(" ");
					String reqRecived =  sentence.substring(0, firstSpace);
					String remainingString = sentence.substring(firstSpace);
					int secSpace = remainingString.indexOf(" ");

					ArrayList<String> mgroupsOfRouter = GetMulticastGrouspOfRpRouter();
					if(reqRecived == "JOIN")
					{
						//Join(myId, rpId, mgroup)
						// read the data...
						// id of router to join the group
						String myId = remainingString.substring(0,secSpace);
						String remainingString2 = sentence.substring(secSpace);
						int thirdSpace = remainingString2.indexOf(" ");
						String rpId = remainingString.substring(0,thirdSpace);
						String mGroup = remainingString.substring(thirdSpace);

						//check from the table if it is a rpid itself 
						//and the mgroup s also the same as of the rpId
						if(routerId == rpId)
						{
							// means the router is the rpId of the group
							//add the router to the join the group in rpid file.
							PrintWriter out = new PrintWriter(new BufferedWriter
									(new FileWriter("C:\\Users\\Yash\\workspace\\ACN\\src\\mgroupfile+.txt", true)));
							BufferedWriter br = new BufferedWriter(out);
							String outline= myId+" "+mGroup+"\n";
							br.write(outline);
							br.close();
						}
						else
						{
							// the next router connection
							//HashMap<String, Socket>  connect = getConnectedRouterHostname(routerId); 
							// if the router is not the rpid then pass it on.
							// perform a join send message
							//get the id of nex hop router so that we can have connection with that router
							String nexthopRouter = DjisktaAlgo(routerId,  getRpIdfromMGroupId( mGroup));
							hostConnection  = getConnectedRouterHostname(nexthopRouter);
							Socket sck = hostConnection.get(nexthopRouter);
							JoinSend(sck, mGroup);


							boolean flag = false; 
							for(int i=0; i < forwardingTable.size(); i ++)
							{
								if(forwardingTable.get(i).SendingId == "*" && forwardingTable.get(i).MGroup == mGroup)
								{
									forwardingTable.get(i).Nexthop.add(myId);
									flag = true;
								}
							}
							if(flag == false)
							{
								//create a forwarding table
								RoutingTable rt = new RoutingTable();
								rt.SendingId = "*";
								rt.MGroup = mGroup;
								rt.Nexthop = new ArrayList<String>();
								rt.Nexthop.add(myId);
								rt.Hosts = null;
								forwardingTable.add(rt);
							}
							// get the data fom register message							
						}
					}
					else if(reqRecived == "PRUNE")
					{
						//Prune(myId, rpId, mgroup)
						// id of router to prune from the group
						String myId = remainingString.substring(0,secSpace);
						String remainingString2 = sentence.substring(secSpace);
						int thirdSpace = remainingString2.indexOf(" ");
						String rpId = remainingString.substring(0,thirdSpace);
						String mGroup = remainingString.substring(thirdSpace);

						//check from the table if it is a rpid itself 
						//and the mgroup s also the same as of the rpId
						int indexToBeRemoved = -1;
						for(int i=0; i < forwardingTable.size(); i ++)
						{
							if(forwardingTable.get(i).SendingId == "*" && forwardingTable.get(i).MGroup == mGroup
									&& forwardingTable.get(i).Hosts.size() == 1 && forwardingTable.get(i).Nexthop.size() == 1 
									&& forwardingTable.get(i).Hosts.get(0) == hostName)
							{
								indexToBeRemoved = i;
							}
						}
						if( indexToBeRemoved != -1)
						{
							forwardingTable.remove(indexToBeRemoved);
						}

						String inputtoberemoved = myId+" "+mGroup;
						// means the router is the rpId of the group							
						// remove from file
						FileReader in = new FileReader("C:\\Users\\Yash\\workspace\\ACN\\src\\mgroupfile+.txt");
						File filename = new File("C:\\Users\\Yash\\workspace\\ACN\\src\\mgroupfile+.txt");
						File tempFile = new File("myTempFile");
						BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
						BufferedReader reader = new BufferedReader(in);
						String sentence1="";
						while((
								sentence1=reader.readLine()) != null)
						{
							// trim newline when comparing with lineToRemove
							if(sentence1.equals(inputtoberemoved)) continue;
							writer.write(sentence1);
						}
						reader.close();
						writer.close();
						tempFile.delete();
						boolean successful = tempFile.renameTo(filename);

						if(routerId != rpId)
						{
							//forward it to next router
							String nexthopRouter = DjisktaAlgo(routerId,  getRpIdfromMGroupId( mGroup));
							hostConnection  = getConnectedRouterHostname(nexthopRouter);
							Socket sck = hostConnection.get(nexthopRouter);

							String outline = "PRUNE " + myId +" "+ rpId +" "+ mGroup;
							DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
							outToServer.writeBytes(outline);
						}
					}
					else if(reqRecived == "MCAST")
					{
						//						3 things....
						//						1) check if the router id is rp or not
						//						if its rp then display all things to hosts and foward to connected routers
						//						2)if it is not an rp then check if it is coming fom rp or goint to rp
						//						3)if it is coming from rp or going towards rp
						//						4) check if it is source specific join

						//mcast(myId,srcId,mgroup,data)
						String myId = remainingString.substring(0,secSpace);
						String remainingString2 = sentence.substring(secSpace);
						int thirdSpace = remainingString2.indexOf(" ");
						String srcId = remainingString.substring(0,thirdSpace);
						String remainingString3 = sentence.substring(thirdSpace);
						int fourthSpace = remainingString3.indexOf(" ");
						String mGroup = remainingString.substring(0,fourthSpace);
						String data =remainingString3.substring(fourthSpace);

						String rpId = getRpIdfromMGroupId(mGroup);

						//--------------------------------------------------------------------------------------------------------------
						int getIndex = -1;
						String fromRP = null;
						for(int i =0 ; i <forwardingTable.size(); i ++)
						{
							// get all the  hosts as well as nect hops
							if(forwardingTable.get(i).SendingId ==  srcId && forwardingTable.get(i).MGroup == mGroup)
							{
								getIndex = i ;
							}
						}
						if(getIndex == -1)
						{
							for(int i =0 ; i <forwardingTable.size(); i ++)
							{
								// get all the  hosts as well as nect hops
								if(forwardingTable.get(i).SendingId ==  "*" && forwardingTable.get(i).MGroup == mGroup)
								{
									getIndex = i ;
									fromRP = "true";
									break;
								}
							}
						}
						if(getIndex != -1)
						{
							// it has an entry in forwading table
							if(forwardingTable.get(getIndex).Hosts != null && (forwardingTable.get(getIndex).Hosts).size() > 0)
							{
								for(int i = 0 ; i  <(forwardingTable.get(getIndex).Hosts).size() ; i++)
								{
									hostConnection  = getConnectedRouterHostname(forwardingTable.get(getIndex).Hosts.get(i));
									Socket sck = hostConnection.get(forwardingTable.get(getIndex).Hosts.get(i));

									DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
									outToServer.writeBytes("MCAST " + routerId +" "+ srcId +" "+ mGroup + " "+ data);
								}
							}
							if(forwardingTable.get(getIndex).Nexthop != null && (forwardingTable.get(getIndex).Nexthop).size() > 0)
							{
								for(int i = 0 ; i  <(forwardingTable.get(getIndex).Nexthop).size() ; i++)
								{
									hostConnection  = getConnectedRouterHostname(forwardingTable.get(getIndex).Nexthop.get(i));
									Socket sck = hostConnection.get(forwardingTable.get(getIndex).Nexthop.get(i));

									DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
									outToServer.writeBytes("MCAST " + routerId +" "+ srcId +" "+ mGroup + " "+ data);
								}
							}
						}








						/*
						//-------------------------------------------------------------------------------------------------------------
						if(routerId == rpId)
						{
							int getIndex =-1;
							for(int i =0 ; i <forwardingTable.size(); i ++)
							{
								// get all the  hosts as well as nect hops
								if(forwardingTable.get(i).SendingId ==  "*" && forwardingTable.get(i).MGroup == mGroup)
								{
									getIndex = i ;
									break;
								}
								else if(forwardingTable.get(i).SendingId !=  "*" && forwardingTable.get(i).MGroup == mGroup)
								{
									getIndex = i ;
									break;
								}
							}
							if(getIndex != -1)
							{
								// it has an entry in forwading table
								for(int i = 0 ; i  <(forwardingTable.get(getIndex).Hosts).size() ; i++)
								{
									hostConnection  = getConnectedRouterHostname(forwardingTable.get(getIndex).Hosts.get(i));
									Socket sck = hostConnection.get(forwardingTable.get(getIndex).Hosts.get(i));

									DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
									outToServer.writeBytes("MCAST " + routerId +" "+ srcId +" "+ mGroup + " "+ data);
								}
								for(int i = 0 ; i  <(forwardingTable.get(getIndex).Nexthop).size() ; i++)
								{
									hostConnection  = getConnectedRouterHostname(forwardingTable.get(getIndex).Nexthop.get(i));
									Socket sck = hostConnection.get(forwardingTable.get(getIndex).Nexthop.get(i));

									DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
									outToServer.writeBytes("MCAST " + routerId +" "+ srcId +" "+ mGroup + " "+ data);
								}
							}
						}
						else if(routerId == srcId)
						{
							int getIndex =-1;
							for(int i =0 ; i <forwardingTable.size(); i ++)
							{
								// get all the  hosts as well as nect hops
								if(forwardingTable.get(i).SendingId ==  "*" && forwardingTable.get(i).MGroup == mGroup &&
										forwardingTable.get(i).Nexthop == null)
								{
									getIndex = i ;
									break;
								}
							}
							for(int i = 0 ; i  <(forwardingTable.get(getIndex).Hosts).size() ; i++)
							{
								hostConnection  = getConnectedRouterHostname(forwardingTable.get(getIndex).Hosts.get(i));
								Socket sck = hostConnection.get(forwardingTable.get(getIndex).Hosts.get(i));

								DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
								outToServer.writeBytes("MCAST " + routerId +" "+ srcId +" "+ mGroup + " "+ data);
							}
						}
						else 
						{
							// forward it to next router depending upon from rp or from othr router
							int getIndex =-1;
							String fromRP = null; 
							for(int i =0 ; i <forwardingTable.size(); i ++)
							{
								// get all the  hosts as well as nect hops
								if(forwardingTable.get(i).SendingId ==  "*" && forwardingTable.get(i).MGroup == mGroup)
								{
									getIndex = i ;
									fromRP = "true";
									break;
								}
								else if(forwardingTable.get(i).SendingId !=  "*" && forwardingTable.get(i).MGroup == mGroup)
								{
									getIndex = i ;
									fromRP = "false";
									break;
								}
							}
							if(fromRP != null && fromRP == "true")
							{
								// forward it and also display to hosts
								// it has an entry in forwading table
								for(int i = 0 ; i  <(forwardingTable.get(getIndex).Hosts).size() ; i++)
								{
									hostConnection  = getConnectedRouterHostname(forwardingTable.get(getIndex).Hosts.get(i));
									Socket sck = hostConnection.get(forwardingTable.get(getIndex).Hosts.get(i));

									DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
									outToServer.writeBytes("MCAST " + routerId +" "+ srcId +" "+ mGroup + " "+ data);
								}
								for(int i = 0 ; i  <(forwardingTable.get(getIndex).Nexthop).size() ; i++)
								{
									hostConnection  = getConnectedRouterHostname(forwardingTable.get(getIndex).Nexthop.get(i));
									Socket sck = hostConnection.get(forwardingTable.get(getIndex).Nexthop.get(i));

									DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
									outToServer.writeBytes("MCAST " + routerId +" "+ srcId +" "+ mGroup + " "+ data);
								}
							}
							else if(fromRP != null && fromRP == "false")
							{
								// foward it
								hostConnection  = getConnectedRouterHostname(forwardingTable.get(getIndex).Hosts.get(i));
								Socket sck = hostConnection.get(forwardingTable.get(getIndex).Hosts.get(i));

								DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
								outToServer.writeBytes("MCAST " + routerId +" "+ srcId +" "+ mGroup + " "+ data);
							}
						}
						//--------------------------------------------------------------------------------------------------------
						HashMap<String, Socket>  connect = getConnectedRouterHostname(routerId); 

						//check the forwarding table
						ArrayList<ArrayList<String>> fwdTable = readForwardingTable();
						String entry = null;
						if(!fwdTable.contains(entry))
						{
							// if entry does not exists
							// check its its rpid and perform action
							// send ssjoin register

							// it is same asd the condion of not having entry in mgroups of router
							//have to remove this check
						}
						else
						{


							// if entry exists
							//check if the router is the rpid of the mcast which is being sent
							//check its is rpid and perform action
							if(mgroupsOfRouter.size() != 0 && mgroupsOfRouter.contains(mGroup))
							{
								if (rpId == routerId)
								{
									//by checking the forwarding table
									//the conditon to check will be *
									//loop for all the connected routers

									// if it is rpid, send mcast message to all other routers
									String output = "MCAST " + routerId + " "+srcId+" "+ mGroup+ " "+ data;
									DataOutputStream outToServer = new DataOutputStream((connect.get(routerId)).getOutputStream());
									outToServer.writeBytes(output);
								}
								else
								{
									// Router is added in the group but its not rp. so it has to forwad the data

									if(rpId == srcId)
									{
										// check the forwarding state and send it to other routers
										//also we have to check here. if the message is coming fom rp or from some other source
										String output = "MCAST " + routerId + " "+srcId+" "+ mGroup+ " "+ data;
										DataOutputStream outToServer = new DataOutputStream((connect.get(routerId)).getOutputStream());
										outToServer.writeBytes(output);
									}
									else
									{
										// data coming from some other router
										String output = "MCAST " + routerId + " "+srcId+" "+ mGroup+ " "+ data;
										DataOutputStream outToServer = new DataOutputStream((connect.get(routerId)).getOutputStream());
										outToServer.writeBytes(output);
									}
								}
							}
							else
							{
								// check if the message is received from rpid
								if(srcId == rpId)// it is then send it to hosts, after checking if router is in that group
								{
									// conditions needs to be changed
									// get the host id of the connected host
									// get the host id from the file 
									boolean flag  = false;
									String hostId  =  null;// from forwarding table
									ArrayList<ArrayList<String>> mgroupHostfile = readMGroupHost();
									for(int i=0 ; i < mgroupHostfile.size() ; i++)
									{
										if((readMGroupHost().get(i)).get(0) == hostId && (readMGroupHost().get(i)).get(1) == mGroup)
										{
											HashMap<String, Socket> sockt = getConnectedRouterHostname(hostId);
											String output = "MCAST " + routerId + " "+srcId+" "+ mGroup+ " "+ data;
											DataOutputStream outToServer = new DataOutputStream(sockt.get(hostId).getOutputStream());
											outToServer.writeBytes(output);
										}
									}
								}
								else
								{
									// send it to next hop router

									Socket sck;
									//connectedRouters: get this frm forwarding table
									ArrayList<String> connectedRouters =  getConnectedRouter(myId);
									// check algorithm if next router is this or not 
									//if it is
									HashMap<String, Socket> nexthop = getConnectedRouterHostname(myId);
									sck = nexthop.get(myId);
									// if not
									String nextid = DjisktaAlgo(routerId, mGroup);
									HashMap<String, Socket> nexthop2mcastgrp =   getConnectedRouterHostname(nextid);//id from algo:  routerIds);
									sck = nexthop2mcastgrp.get(myId);

									String output = "MCAST " + routerId + " "+srcId+" "+ mGroup+ " "+ data;
									DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
									outToServer.writeBytes(output);
								}
							}
						}*/
					}
					else if(reqRecived == "SSJOIN")
					{
						//ssjoin(myId,srcId,mgroup)
						String myId = remainingString.substring(0,secSpace);
						String remainingString2 = sentence.substring(secSpace);
						int thirdSpace = remainingString2.indexOf(" ");
						String srcId = remainingString.substring(0,thirdSpace);
						String mGroup = remainingString.substring(thirdSpace);

						// check if router is rp
						ArrayList<ArrayList<String>> rpfile = readConfigRp();
						// get the rpId from the file
						String rpId = getRpIdfromMGroupId(mGroup);

						if (rpId != routerId)
						{
							//{
							// router will never recive ssJoin
							// send the ssjoin message
							//							String output = "SSJOIN " +rpId+ " "+srcId+" "+ mGroup;
							//							HashMap<String, Socket>  connect = getConnectedRouterHostname(routerId); 
							//
							//							DataOutputStream outToServer = new DataOutputStream(connect.get(routerId).getOutputStream());
							//							outToServer.writeBytes(output);
							//}
							// forward the ssjoin message
							// and create a router table source specific entry
							RoutingTable rt = new RoutingTable();
							rt.SendingId = srcId;
							rt.MGroup = mGroup;
							rt.Nexthop = new ArrayList<String>();
							rt.Nexthop.add(routerId);
							// get the data fom register message
							rt.Hosts = null;
							forwardingTable.add(rt);
							String output = "SSJOIN "+ routerId+ " " +srcId+" "+ mGroup;

							String nexthopRouter = DjisktaAlgo(routerId,srcId);
							hostConnection  = getConnectedRouterHostname(nexthopRouter);
							Socket sck = hostConnection.get(nexthopRouter);
							//							
							//							HashMap<String, Socket>  connect = getConnectedRouterHostname(routerId); 

							DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
							outToServer.writeBytes(output);
						}
					}
					else if(reqRecived == "REGISTER")
					{
						//register(srcId, rpId,mgroup,data)
						String srcId = remainingString.substring(0,secSpace);
						String remainingString2 = sentence.substring(secSpace);
						int thirdSpace = remainingString2.indexOf(" ");
						String rpId = remainingString.substring(0,thirdSpace);
						String remainingString3 = sentence.substring(thirdSpace);
						String mGroup = remainingString.substring(0,thirdSpace);
						String data =remainingString3.substring(0,thirdSpace);

						//create a table for host only when the router is rpid... 
						// fo every other router the router needs to forward it as it is.

						if (routerId == rpId)
						{
							PrintWriter out = new PrintWriter(new BufferedWriter
									(new FileWriter("C:\\Users\\Yash\\workspace\\ACN\\src\\mgroupfile+.txt", true)));
							BufferedWriter br = new BufferedWriter(out);
							String outline= hostName+" "+mGroup+"\n";
							br.write(outline);
							br.close();

							// then send SSJoin message
							RoutingTable rt = new RoutingTable();
							rt.SendingId = srcId;
							rt.MGroup = mGroup;
							// todo: HAVE TO SEE THIS ENTRY
							rt.Nexthop = new ArrayList<String>();
							rt.Nexthop.add(routerId);
							// get the data fom register message
							rt.Hosts = null;
							forwardingTable.add(rt);

							String nexthopRouter = DjisktaAlgo(routerId,  getRpIdfromMGroupId( mGroup));
							hostConnection  = getConnectedRouterHostname(nexthopRouter);
							Socket sck = hostConnection.get(nexthopRouter);

							String output = "SSJOIN "+ routerId+ " " +srcId+" "+ mGroup;
							DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
							outToServer.writeBytes(output);
						}
						else
						{
							String nexthopRouter = DjisktaAlgo(routerId,  getRpIdfromMGroupId( mGroup));
							hostConnection  = getConnectedRouterHostname(nexthopRouter);
							Socket sck = hostConnection.get(nexthopRouter);

							String output = "REGISTER " +srcId+" "+ rpId +" "+ mGroup+ " "+ data;
							DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
							outToServer.writeBytes(output);

							/*
							Socket sck;
							ArrayList<String> connectedRouters =  getConnectedRouter(rpId);
							// check algorithm if next router is this or not 
							//if it is
							HashMap<String, Socket> nexthop =   getConnectedRouterHostname(rpId);
							sck = nexthop.get(rpId);
							// if not
							String nextid = DjisktaAlgo(routerId, mGroup);
							HashMap<String, Socket> nexthop2mcastgrp =   getConnectedRouterHostname(nextid);//id from algo:  routerIds);
							sck = nexthop2mcastgrp.get(rpId);

							String output = "REGISTER " +srcId+" "+ rpId +" "+ mGroup+ " "+ data;
							DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
							outToServer.writeBytes(output);
							 */
						}

						// keep this socket in thread and maintain all ins and out
					}
					else if (reqRecived  == "LEAVE")
					{
						//LEAVE <myID> <mgroup> 
						String myId = remainingString.substring(0,secSpace);
						String mgroup = sentence.substring(secSpace);

						boolean flagContains  = false;
						boolean flag2 = false;
						ArrayList<ArrayList<String>> mgroupHostfile = readMGroupHost();
						for(int i=0 ; i < mgroupHostfile.size() ; i++)
						{
							if((mgroupHostfile.get(i)).get(0) == myId && (mgroupHostfile.get(i)).get(1) == mgroup )
							{
								// it contains 
								flagContains = true;
							}
							if((mgroupHostfile.get(i)).get(0) != myId && (mgroupHostfile.get(i)).get(1) == mgroup )
							{
								// it is not the only one subscribed
								flag2 = true;
							}
						}
						if(flagContains == true && flag2 == true )
						{

							// remove the entry of host from mgroup table
							FileReader in = new FileReader("C:\\Users\\Yash\\workspace\\ACN\\src\\mgroupfile+.txt");
							File filename = new File("C:\\Users\\Yash\\workspace\\ACN\\src\\mgroupfile+.txt");
							File tempFile = new File("myTempFile");
							BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
							BufferedReader reader = new BufferedReader(in);
							String sentence1="";
							while((
									sentence1=reader.readLine()) != null)
							{

								// trim newline when comparing with lineToRemove
								if(sentence1.equals((myId + " " + mgroup) )) continue;
								writer.write(sentence1);
							}
							reader.close();
							writer.close();
							tempFile.delete();
							boolean successful = tempFile.renameTo(filename);
						}
						else if(flagContains == true && flag2 == false )
						{
							// send a prune message
							ArrayList<ArrayList<String>> rpfile = readConfigRp();
							// get the rpId from the file
							// append the id of router where message is sent get from config file also get the port number
							// append the request router id while sending
							String rpId = getRpIdfromMGroupId(mgroup);
							String outline = "PRUNE " +myId+" "+ rpId +" "+ mgroup;
							String nexthopId = DjisktaAlgo(routerId, mgroup);
							HashMap<String, Socket>  connect = getConnectedRouterHostname(nexthopId); 
							// get the route id frm djikstra
							DataOutputStream outToServer = new DataOutputStream(connect.get(routerId).getOutputStream());
							outToServer.writeBytes(outline);
						}
						else
						{
							// no need to do anything because the req is invalid
						}
					}
					else if(reqRecived == "SEND")
					{
						//SEND <myID> <mgroup> <data>
						String myId = remainingString.substring(0,secSpace);
						String remainingString2 = sentence.substring(secSpace);
						int thirdSpace = remainingString2.indexOf(" ");
						String mgroup = remainingString.substring(0,thirdSpace);
						String data = sentence.substring(thirdSpace);

						String rpId = getRpIdfromMGroupId(mgroup);

						// router gets send from host
						// get the host specific socket
						if(mgroupsOfRouter.size() != 0 && mgroupsOfRouter.contains(mgroup))
						{
							//check the mgroups table if the entry exists else add the entry in mgroups of the router
							boolean flag  = false;
							ArrayList<ArrayList<String>> mgroupHostfile = readMGroupHost();
							for(int i=0 ; i < mgroupHostfile.size() ; i++)
							{
								if((mgroupHostfile.get(i)).get(0) == myId && (mgroupHostfile.get(i)).get(1) == mgroup )
								{
									// it contains 
									flag = true;
								}
							}
							if(flag == true)
							{
								// send the mcast message
								// append the id of router where message is sent get from config file also get the port number
								// append the request router id while sending
								if(routerId == rpId)
								{
									// read for the forwarding table all the next hop routers
									for (int i=0 ; i<forwardingTable.size();i++)
									{
										if(forwardingTable.get(i).SendingId == "*" && forwardingTable.get(i).MGroup == mgroup)
										{
											for(int j =0 ; j < forwardingTable.get(i).Hosts.size() ; j++)
											{
												hostConnection  = getConnectedRouterHostname(forwardingTable.get(i).Hosts.get(j));
												Socket sck = hostConnection.get(forwardingTable.get(i).Hosts.get(j));

												DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
												outToServer.writeBytes("MCAST " + routerId +" "+ routerId +" "+ mgroup + " "+ data);
											}
											break;
										}										
									}
								}
								else
								{
									//forward it to next router
									String nexthopRouter = DjisktaAlgo(routerId,  getRpIdfromMGroupId( mgroup));
									hostConnection  = getConnectedRouterHostname(nexthopRouter);
									Socket sck = hostConnection.get(nexthopRouter);

									DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
									outToServer.writeBytes("MCAST " + routerId +" "+ routerId +" "+ mgroup + " "+ data);
								}
							}
							else
							{
								//create a table for host
								PrintWriter out = new PrintWriter(new BufferedWriter
										(new FileWriter("C:\\Users\\Yash\\workspace\\ACN\\src\\mgroupfile+.txt", true)));
								BufferedWriter br = new BufferedWriter(out);
								String outline= ""+hostName+" "+ mgroup+"\n";
								br.write(outline);
								br.close();

								//create a forwarding table
								RoutingTable rt = new RoutingTable();
								rt.SendingId = "*";
								rt.MGroup = mgroup;
								rt.Nexthop = new ArrayList<String>();
								rt.Nexthop.add(myId);
								rt.Hosts = null;
								forwardingTable.add(rt);

								String nexthopRouter = DjisktaAlgo(routerId,  getRpIdfromMGroupId( mgroup));
								hostConnection  = getConnectedRouterHostname(nexthopRouter);
								Socket sck = hostConnection.get(nexthopRouter);
								// will be send to next hop router
								DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
								outToServer.writeBytes("MCAST " + routerId +" "+ routerId +" "+ mgroup + " "+ data);
							}
						}
						else
						{
							// router is not a member of that group also means host is nt thr in the grp too then implement  register
							String nexthopRouter = DjisktaAlgo(routerId,  getRpIdfromMGroupId( mgroup));
							hostConnection  = getConnectedRouterHostname(nexthopRouter);
							Socket sck = hostConnection.get(nexthopRouter);

							String output = "REGISTER " +routerId+" "+ rpId +" "+ mgroup+ " "+ data;
							DataOutputStream outToServer = new DataOutputStream(sck.getOutputStream());
							outToServer.writeBytes(output);
						}
					}
					else if (reqRecived == "REPORT")
					{
						//REPORT <myID> <mgroup> 
						String hostId = remainingString.substring(0,secSpace);
						String mGroup = sentence.substring(secSpace);

						// add entry in mgroups file is the host is not in the groupfile of the router
						int counter =0;
						for(int i=0; i <readMGroupHost().size(); i++)
						{
							if((readMGroupHost().get(i)).get(0) == hostId && (readMGroupHost().get(i)).get(1) == mGroup)
							{
								// do nothing
							}
							else if((readMGroupHost().get(i)).get(0) != hostId && (readMGroupHost().get(i)).get(1) == mGroup)
							{
								counter ++; 
							}
							else if((readMGroupHost().get(i)).get(0) != hostId && (readMGroupHost().get(i)).get(1) != mGroup)
							{
								// perform a join send message
								//get the id of nex hop router so that we can have connection with that router
								String nexthopRouter = DjisktaAlgo(routerId,  getRpIdfromMGroupId( mGroup));
								hostConnection  = getConnectedRouterHostname(nexthopRouter);
								Socket sck = hostConnection.get(nexthopRouter);
								JoinSend(sck, mGroup);
								break;
							}
						}
						if(counter == readMGroupHost().size())
						{
							// check if id is pesent in the mgroupfile
							//add entry in router local file
							PrintWriter out = new PrintWriter(new BufferedWriter
									(new FileWriter("C:\\Users\\Yash\\workspace\\ACN\\src\\mgroupfile+.txt", true)));
							BufferedWriter br = new BufferedWriter(out);
							String outline= hostId +" "+mGroup+"\n";
							br.write(outline);
							br.close();

							//create a forwarding table
							// get the data fom register message
							boolean flag = false; 
							for(int i=0; i < forwardingTable.size(); i ++)
							{
								if(forwardingTable.get(i).SendingId == "*" && forwardingTable.get(i).MGroup == mGroup)
								{
									forwardingTable.get(i).Hosts.add(hostId);
									flag = true;
								}
							}
							if(flag == false)
							{
								RoutingTable rt = new RoutingTable();
								rt.SendingId = "*";
								rt.MGroup = mGroup;
								rt.Nexthop = null;
								rt.Hosts = new ArrayList<String>();
								rt.Hosts.add(hostId);
								forwardingTable.add(rt);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{

		}
		welcomeSocket.close();
	}

	void Recieve()
	{}

	static void JoinSend(Socket clientSocket,String mgroupId) throws IOException
	{
		// append the id of router where message is sent get from config file also get the port number
		// append the request router id while sending

		String rpId = getRpIdfromMGroupId(mgroupId); 

		String sentence = "JOIN " +routerId+" "+ rpId +" "+ mgroupId;
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		outToServer.writeBytes(sentence);
	}

	static String getRpIdfromMGroupId(String mGroupId) throws IOException
	{
		ArrayList<ArrayList<String>> rpfile = readConfigRp();
		String rpId = null;
		// get the rpId from the file

		for(int i =0 ;i < rpfile.size();i++)
		{
			if((rpfile.get(i)).get(0) == mGroupId)
			{
				rpId =  (rpfile.get(i)).get(1);
				break;
			}
		}
		return rpId;
	}

	static void PruneSend(Socket clientSocket) throws IOException
	{
		// join the router to some other router
		System.out.println("provide mgroup Id to leave");
		BufferedReader in =  new BufferedReader(new InputStreamReader(System.in));
		String mgroupId  = in.readLine();

		// get the rpId from the file
		// append the id of router where message is sent get from config file also get the port number
		// append the request router id while sending
		String rpId =  getRpIdfromMGroupId(mgroupId);

		String sentence = "PRUNE " +routerId+" "+ rpId +" "+ mgroupId;
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		outToServer.writeBytes(sentence);
	}

	static void MulticastSend(Socket clientSocket) throws IOException
	{
		// join the router to some other router
		System.out.println("provide mgroup Id for sending the data");
		BufferedReader in =  new BufferedReader(new InputStreamReader(System.in));
		String mgroupId  = in.readLine();

		System.out.println("provide data to be sent");
		in =  new BufferedReader(new InputStreamReader(System.in));
		String data  = in.readLine();

		// append the id of router where message is sent get from config file also get the port number
		// append the request router id while sending
		String rpId =  getRpIdfromMGroupId(mgroupId);
		String sourceId = null; 

		//if originating from this router 
		String sentence = "MCAST " + routerId +" "+ routerId +" "+ mgroupId + " " + data;
		// else if we are forwarding the mcast then we need to check topology
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		sentence = "MCAST " + routerId +" "+ sourceId +" "+ mgroupId + " "+ data;
		outToServer.writeBytes(sentence);
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

	public static void Send(String input,Socket clientSocket) throws IOException
	{
		// check config file if the other router is present or not..
		// if present
		if(Integer.parseInt(input) == 1)
		{
			// join the router to some other router
			// join the router to some other router
			System.out.println("provide mgroup Id to join");
			BufferedReader in =  new BufferedReader(new InputStreamReader(System.in));
			String mgroupId  = in.readLine();

			JoinSend(clientSocket, mgroupId);
		}
		else if(input == "2")
		{
			//Remove itself from a multicast group
			PruneSend(clientSocket);
		}
		else if(input == "3")
		{
			// send a multicast message
			MulticastSend(clientSocket);
		}
	}

	public static HashMap<String, Socket>  getConnectedRouterHostname(String routerId) throws IOException
	{
		HashMap<String, Socket> socktsconnected = new HashMap<String,Socket>();
		// the client sockets should have port number as well as host name of all the connected routers
		ArrayList<ArrayList<String>> configArray =  readConfig();

		for(int i=0 ;i < configArray.size(); i++)
		{
			if((configArray.get(i)).get(0) == routerId)
			{
				// means we have reached the correct router
				// get the clients port number and host name
				String hostName = 	(configArray.get(i)).get(1);
				String portNumber  = (configArray.get(i)).get(2);
				if(activeClients.isEmpty() == false && activeClients.containsKey(routerId))
				{
					socktsconnected.put(routerId, activeClients.get(routerId));
				}
				else
				{
					Socket clientSocket = new Socket(hostName, Integer.parseInt(portNumber));
					socktsconnected.put(routerId, clientSocket);
				}
			}
		}
		return socktsconnected;
	}

	public static ArrayList<String> getConnectedRouter(String routerId) throws IOException
	{
		ArrayList<String> connectedRouters = new ArrayList<String>();
		ArrayList<String> allconnects = new ArrayList<String>();
		ArrayList<ArrayList<String>> topoArray  = readConfigTopo();

		for(int i=0 ; i< topoArray.size() ; i++)
		{
			if(i == Integer.parseInt(routerId))
			{
				allconnects = topoArray.get(i);
			}
		}

		for(Integer i=0 ; i< allconnects.size() ; i++)
		{
			// means router having direct connection
			if(Integer.parseInt(allconnects.get(i)) == 1)
			{
				connectedRouters.add(i.toString());
			}
		}
		return connectedRouters;
	}

	public void mGroupReadFunc(String input) throws IOException{
		FileReader in = new FileReader("C:\\workspace\\MCAST\\mgroupfile+.txt");
		File filename = new File("C:\\workspace\\MCAST\\mgroupfile+.txt");
		File tempFile = new File("myTempFile");
		BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
		BufferedReader reader = new BufferedReader(in);
		String sentence="";
		while((
				sentence=reader.readLine()) != null)
		{

			// trim newline when comparing with lineToRemove

			if(sentence.equals(input)) continue;
			writer.write(sentence);

		}
		reader.close();
		writer.close();
		tempFile.delete();
		boolean successful = tempFile.renameTo(filename);
	}

	public static ArrayList<ArrayList<String>> readConfig() throws IOException
	{		
		ArrayList<ArrayList<String>> st = new ArrayList<ArrayList<String>>();
		FileInputStream fstream =null;
		try {fstream = new FileInputStream(configfile+".txt");}
		catch (FileNotFoundException e)
		{e.printStackTrace();}
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine ;

		strLine = br.readLine();
		while (strLine != null)   
		{
			String ss = null;
			ArrayList<String> string = new ArrayList<String>();
			// get the substring and put in another array
			for (int i =0 ; i <3 ;i ++)
			{
				int spc = 0;
				spc = strLine.indexOf(" ");
				if(spc == -1)
				{
					ss = strLine.substring(0);
					string.add( ss);
				}
				else
				{
					ss = strLine.substring(0,spc);
					string.add( ss);
					strLine = strLine.substring((ss.length()+1));
				}
			}
			st.add(string);
			strLine = br.readLine();
		}
		br.close();
		fstream.close();
		return st;
	}

	public static ArrayList<ArrayList<String>> readConfigTopo() throws IOException
	{
		ArrayList<ArrayList<String>> st = new ArrayList<ArrayList<String>>();
		FileInputStream fstream =null;
		try {fstream = new FileInputStream(config_topo+".txt");}
		catch (FileNotFoundException e)
		{e.printStackTrace();}
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine ;
		int k = 0;

		strLine = br.readLine();
		while (strLine != null)   
		{
			if(k == 0)
			{
				// first line tell about number of routers
				maxRouters = Integer.parseInt(strLine);
				k++;
			}
			else
			{
				String ss = null;
				ArrayList<String> string = new ArrayList<String>();
				// get the substring and put in another array
				for (int i =0 ; i <maxRouters ;i ++)
				{
					int spc = 0;
					spc = strLine.indexOf(" ");
					if(spc == -1)
					{
						ss = strLine.substring(0);
						string.add( ss);
					}
					else
					{
						ss = strLine.substring(0,spc);
						string.add( ss);
						strLine = strLine.substring((ss.length()+1));
					}
				}
				st.add(string);
				strLine = br.readLine();
			}
		}
		br.close();
		fstream.close();
		return st;
	}

	public static ArrayList<ArrayList<String>> readForwardingTable() throws IOException
	{
		ArrayList<ArrayList<String>> st = new ArrayList<ArrayList<String>>();
		FileInputStream fstream =null;
		try {fstream = new FileInputStream("C:\\Users\\Yash\\workspace\\ACN\\src\\forwardingTable+.txt");}
		catch (FileNotFoundException e)
		{e.printStackTrace();}
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine ;

		strLine = br.readLine();
		while (strLine != null)   
		{

		}
		br.close();
		fstream.close();
		return st;
	}

	public static ArrayList<ArrayList<String>> readConfigRp() throws IOException
	{
		ArrayList<ArrayList<String>> st = new ArrayList<ArrayList<String>>();
		FileInputStream fstream =null;
		try {fstream = new FileInputStream(config_rp+".txt");}
		catch (FileNotFoundException e)
		{e.printStackTrace();}
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine ;

		strLine = br.readLine();
		while (strLine != null)   
		{
			String ss = null;
			ArrayList<String> string = new ArrayList<String>();
			// get the substring and put in another array
			for (int i =0 ; i <3 ;i ++)
			{
				int spc = 0;
				spc = strLine.indexOf(" ");
				if(spc == -1)
				{
					ss = strLine.substring(0);
					string.add( ss);
				}
				else
				{
					ss = strLine.substring(0,spc);
					string.add( ss);
					strLine = strLine.substring((ss.length()+1));
				}
			}
			st.add(string);
			strLine = br.readLine();
		}
		br.close();
		fstream.close();
		return st;
	}

	public static ArrayList<ArrayList<String>> readMGroupHost() throws IOException
	{
		ArrayList<ArrayList<String>> st = new ArrayList<ArrayList<String>>();
		FileInputStream fstream =null;
		try {fstream = new FileInputStream("mgroup"+".txt");}
		catch (FileNotFoundException e)
		{e.printStackTrace();}
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine ;

		strLine = br.readLine();
		while (strLine != null)   
		{
			String ss = null;
			ArrayList<String> string = new ArrayList<String>();
			// get the substring and put in another array
			for (int i =0 ; i <2 ;i ++)
			{
				int spc = 0;
				spc = strLine.indexOf(" ");
				if(spc == -1)
				{
					ss = strLine.substring(0);
					string.add( ss);
				}
				else
				{
					ss = strLine.substring(0,spc);
					string.add( ss);
					strLine = strLine.substring((ss.length()+1));
				}
			}
			st.add(string);
			strLine = br.readLine();
		}
		br.close();
		fstream.close();
		return st;
	}

	public static void GetCommandLineArgs(String[] args)
	{
		//Get the router Id from the command line
		try 
		{
			//Check if the router Id given by the user is of proper format or not, else again ask user input.
			if(isInteger(args[0]))
			{ 
				routerId = args[0];
			}
			else
			{ 
				System.out.println("Need valid argument: Router Id");
				System.exit(-1);
			}
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			System.out.println("Provide Router Id");
			System.exit(-1);
		}
		try 
		{
			configfile = args[1];
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			configfile = "C:\\Users\\Yash\\workspace\\ACN\\src\\configfile";
		}
		try 
		{
			config_rp = args[2];
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			config_rp = "C:\\Users\\Yash\\workspace\\ACN\\src\\config_rp";
		}
		try 
		{
			config_topo = args[3];
		} 
		catch (ArrayIndexOutOfBoundsException e) 
		{
			config_topo = "C:\\Users\\Yash\\workspace\\ACN\\src\\config_topo";
		}
	}

	public static String DjisktaAlgo(String src, String dest) throws NumberFormatException, IOException
	{
		Dijkstra dijkstra = new Dijkstra();
		Hashtable<String, String> nexthops = dijkstra.dijkstralgo(Integer.parseInt(routerId));

		String key = src+dest;
		String nexthopRouter = nexthops.get(key);
		return nexthopRouter;
	}

	public static ArrayList<String> GetMulticastGrouspOfRpRouter() throws IOException
	{
		ArrayList<String> mgroupsOfRouter = new ArrayList<String>();
		ArrayList<ArrayList<String>> rpArray  = readConfigRp();

		for(int i= 0; i < rpArray.size() ;i++)
		{
			if((rpArray.get(i)).get(1)== routerId)
			{
				mgroupsOfRouter.add((rpArray.get(i)).get(0));
			}
		}
		return mgroupsOfRouter;
	}
}

class RoutingTable 
{
	public String SendingId;
	public String MGroup;
	public ArrayList<String> Nexthop;
	public ArrayList<String> Hosts;
	public RoutingTable()
	{
	}
}

