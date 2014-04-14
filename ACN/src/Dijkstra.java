import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

class Vertex implements Comparable<Vertex>
{
    public  String name;
    public Edge[] adjacencies;
    public double minDistance = Double.POSITIVE_INFINITY;
    public Vertex previous;
    public Vertex(String argName) { name = argName; }
    public String toString() { return name; }
    public int compareTo(Vertex other)
    {
        return Double.compare(minDistance, other.minDistance);
    }

}


class Edge
{
    public Vertex target;
    public double weight;
    public Edge(Vertex argTarget, double argWeight)
    { target = argTarget; weight = argWeight; }
}

class Dijkstra
{
    public static void computePaths(Vertex source)
    {
    	 source.minDistance = 0.;
         PriorityQueue<Vertex> vertexQueue = new PriorityQueue<Vertex>();
       	vertexQueue.add(source);

 	while (!vertexQueue.isEmpty()) {
 	    Vertex u = vertexQueue.poll();

             // Visit each edge exiting u
             for (Edge e : u.adjacencies)
             {
                 Vertex v = e.target;
                 double weight = e.weight;
                 double distanceThroughU = u.minDistance + weight;
 		if (distanceThroughU < v.minDistance) {
 		    vertexQueue.remove(v);
 		    v.minDistance = distanceThroughU ;
 		    v.previous = u;
 		    vertexQueue.add(v);
 		}
             }}
	
    }

    public static List<Vertex> getShortestPathTo(Vertex target)
    {   
    	
        List<Vertex> path = new ArrayList<Vertex>();
        for (Vertex vertex = target; vertex != null; vertex = vertex.previous)
            path.add(vertex);

        Collections.reverse(path);
        return path;
    }


    public Hashtable<String,String> dijkstralgo(int routerID) throws IOException
    {
    	FileReader in = new FileReader("C:\\workspace\\MCAST\\src\\MainPackage\\data.txt");
    	BufferedReader reader = new BufferedReader(in);
		String sentence="";
		sentence=reader.readLine();
		int size=Integer.parseInt(sentence);
		int [][] mat = new int[size+1][size+1];
		for(int i=0;i<=size;i++)
		for(int j=0;j<=size;j++)
		mat[i][j]=0;
        for(int i=0;i<size;i++)
		{
			sentence=reader.readLine();
			String strings[] = sentence.split(" ");
			for(int j=0;j<size;j++)
			{
				//list.add();
				//System.out.println(strings[j]);
				mat[i][j]=Integer.parseInt(strings[j]);
			}
		
		}
        reader.close();
	Vertex[] V=new Vertex[size];
	for (int i=0; i<size;i++){
		V[i] = new Vertex(""+i);
	}
    
	for(int i=0; i<size; i++){
		
	V[i].adjacencies = new Edge[size];
	for(int j=0;j<size;j++){
		V[i].adjacencies[j]= new Edge(V[j], mat[i][j]);
           
	}}
     
        computePaths(V[routerID]);
        Hashtable<String, String> nexthop= new Hashtable<String, String>();
        
        
    for(int l=0; l<size; l++)
    {
    	    List<Vertex> path = getShortestPathTo(V[l]);
    		   System.out.println("Path: " + path);
    		   java.util.Iterator<Vertex> itr= path.iterator();
    		   String[] arr = new String[size];
    		   int j=path.size();
    		   for(int i=0;i<path.size();i++){
    		   Object ele=itr.next();
    		   arr[i]=ele.toString();
    		   System.out.println(""+arr[i]);
    		   
    		   }
    		  
         	   for(int k=0;k<path.size();k++)
         	   {
         		   int m=0;
         		   if(k==0)m=0;
         		   else m=m+1;
         	   nexthop.put(new String(arr[0]+arr[j]), new String(arr[m]));
               System.out.println("hey"+nexthop.get(arr[0]+arr[j]));
         	   }
    	 }
    return nexthop;
    }
    	
    }
    
