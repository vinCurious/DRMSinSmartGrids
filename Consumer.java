import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
 
 /**
 * This Consumer class signifies the electricity consumers connected to particular sub-station.
 * 
 * @author Vinay Vasant More
 * @author Pratik Shirish Kulkarni
 *
 */

public class Consumer {

    static int currentlyUsed = 0;

    /**
     * The main program.
     * This program simulates the activities of consumer.
     * A consumer first connects to a station (lookup operation).
     *
     * Then it asks for electricity according to requirement.
     * (getEnergy())
     *
     * if the required units are entered as 0, client is disconnected from
     * station.
     * @param args    command line argument (ignored)
     */
    public static void main(String[] args) {
        String ip = args[0];
        try {
            Registry reg = LocateRegistry.getRegistry(ip, 12005);
            myInt myInt = (myInt) reg.lookup("rmi://" + ip + ":12005/mystring");
            System.out.println("Connected to station: " + ip);
            String myIp = InetAddress.getLocalHost().getHostAddress();
            Scanner in = new Scanner(System.in);
            String str, result;
            System.out.println("Enter units required (enter 0 to disconnect): ");
            while(!(str = in.next()).equals("0")) {
                int req = Integer.parseInt(str);
                if(req > (-1 * currentlyUsed)) {
                    result = (myInt.getEnergy(myIp + ":" + req));
                    currentlyUsed += result.equals("Done") || result.equals("warning") ? req : 0;
                    if (result.equals("error")) {
                        System.out.println("There was error in your request.");
                    } else {
                        if(result.equals("warning")) {
                            System.out.println(result + ": You have reached maximum allowed usage limit. You are now being charged twice per unit.");
                        }
                        System.out.println("Current use: " + currentlyUsed);
                    }
                } else {
                    System.out.println("There was error in your request.");
                }
                System.out.println("Enter units required (enter 0 to disconnect): ");
            }
            myInt.getEnergy(myIp + ":" + (-1 * currentlyUsed));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
