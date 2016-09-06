import java.io.*;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This Station class acts like a smart grid sub-station.
 * 
 * @author Vinay Vasant More
 * @author Pratik Shirish Kulkarni
 *
 */

public class Station extends UnicastRemoteObject implements myInt, Runnable{

    String groupIp = "225.4.5.6";
    MulticastSocket multi;
    // HashMap to store data after analysing weekly peak values.
    HashMap<String, List<Integer>> data = new HashMap<>();

    // HashMap to store consumer's data.
    HashMap<String, Integer> consumers = new HashMap<>();

    // HashMap to store data of stations from which we are
    // borrowing electricity.
    HashMap<String, Integer> doners = new HashMap<>();

    // HashMap to store data of stations to which current station is
    // transferring electricity.
    HashMap<String, Integer> receivers = new HashMap<>();

    int currentUsage = 0;
    int allowedPerUser = 500;
    int stationCapacity = 2000;
    int threshold = (int) (0.8 * stationCapacity);
    boolean reqFinish = true;
    boolean helpreceived = false;

    /**
     * Station Constructor.
     * In this a station joins a multicast group of all stations.
     *
     * This group can be used to communicate in case of higher demand.
     * @throws Exception
     */
    protected Station() throws Exception {
        multi = new MulticastSocket(35001);
        multi.joinGroup(InetAddress.getByName(groupIp));
    }

    /**
     * This method will be called by consumer (as RMI call) for requesting
     * electricity.
     *
     * If requested units are in allowed range then station approves the request
     * else error message is sent back.
     *
     * While serving client requests station tracks its current distribution.
     * If it reaches defined threshold, then it asks other station to contribute.
     * These requests are sent as multicast messages.
     *
     * @param units    String     format consumerIP:units
     * @return    done, warning or error
     * @throws Exception
     */
    public String getEnergy(String units) throws Exception {
        String result = "";
        if (units != null) {
            String[] request = units.split(":");
            if (!consumers.containsKey(request[0])) {
                consumers.put(request[0], 0);
            }
            int current = consumers.get(request[0]);
            int demand = Integer.parseInt(request[1]);
            if (demand < 0) {
                if (demand < (-1 * current))
                    return "error";
                if (demand == (-1 * current)) {
                    consumers.remove(request[0]);
                }
                currentUsage += demand;
                reqFinish = (currentUsage < threshold);
                result = "Done";
            } else {
                if (demand + current >= allowedPerUser) {
                    result = "warning";
                    if (demand + currentUsage > stationCapacity) {
                        return "error";
                    } else if (demand + currentUsage >= threshold) {
                        helpreceived = false;
                        callForHelp(receivers.isEmpty());
                    }
                } else {
                    result = "Done";
                }
                currentUsage += demand;
            }
            consumers.put(request[0], current + demand);
        } else {
            result = "error";
        }
        System.out.println("Current usage: " + currentUsage + "  capacity: " + stationCapacity + "  threshold: " + threshold);
        return result;
    }

    /**
     * This method is called in case current station needs other stations
     * to contribute as it has reached threshold.
     *
     * Messages can be of 2 types
     * 1. Request : asking for electricity from other clients
     * 2. Withdraw: asking other station, to which some electricity was transferred,
     *              to return it back
     *
     * @param request    boolean
     * @throws Exception
     */
    public void callForHelp(boolean request) throws Exception {
        reqFinish = false;
        int help = 0;
        multi.leaveGroup(InetAddress.getByName(groupIp));
        DatagramSocket outSoc = new DatagramSocket(45001);
        byte[] buff = new byte[100];
        while (!helpreceived) {
            if (request) {
                buff = ("request:" + (int) (0.2 * stationCapacity)).getBytes();
                System.out.println("Sending request...\n");
            } else {
                buff = "withdraw".getBytes();
                System.out.println("Sending withdraw request...\n");
            }
            DatagramPacket out = new DatagramPacket(buff, buff.length);
            DatagramPacket in;
            out.setAddress(InetAddress.getByName(groupIp));
            out.setPort(35001);
            outSoc.send(out);

            buff = new byte[256];
            in = new DatagramPacket(buff, buff.length);
            try {
                outSoc.setSoTimeout(1000);
                outSoc.receive(in);
                String res = getdata(in.getData());
                if (getType(in.getData()).equals("response")) {
                    doners.put(in.getAddress().getHostAddress(), Integer.parseInt(res));
                    help = Integer.parseInt(res);
                    stationCapacity += help;
                    threshold = (int) (0.8 * stationCapacity);
                } else if (getType(in.getData()).equals("withdrawGrant")) {
                    help = receivers.get(in.getAddress().getHostAddress());
                    stationCapacity += help;
                    threshold = (int) (0.8 * stationCapacity);
                    receivers.remove(in.getAddress().getHostAddress());
                }

                buff = "done".getBytes();
                out = new DatagramPacket(buff, buff.length);
                out.setAddress(in.getAddress());
                out.setPort(in.getPort());
                outSoc.send(out);

                outSoc.close();

                System.out.println(help + " units received from : " + in.getAddress().getHostAddress());
                System.out.println("New Capacity : " + stationCapacity + "\tNew threshold: " + threshold + "\n");
                multi.joinGroup(InetAddress.getByName(groupIp));
                helpreceived = true;
            } catch (SocketTimeoutException e) {
                System.out.println("No response received.");
                request = true;
            }
        }
    }

    /**
     * Utility method to find type of message received.
     * @param buff    byte array
     * @return
     */
    public String getType(byte[] buff) {
        String str = new String(buff).trim();
        return str.split(":")[0];
    }

    /**
     * Utility method to get data of message received.
     * @param buff    byte array
     * @return
     */
    public String getdata(byte[] buff) {
        String str = new String(buff).trim();
        return str.split(":")[1];
    }

    /**
     * run method.
     * This method runs a thread that keeps listening on multicast socket
     * for incoming requests. And respond depending on type of request and
     * current load on the station.
     */
    @Override
    public void run() {
        String message;
        while(true) {
            try {
                // request
                byte[] buff = new byte[256];
                DatagramPacket in = new DatagramPacket(buff, buff.length);
                multi.receive(in);
                Date date = new Date();
                DateFormat format1=new SimpleDateFormat("HH:mm:ss");
                DateFormat format2=new SimpleDateFormat("EEEE");
                String finalDay = format2.format(date);
                int time = Integer.parseInt(format1.format(date).split(":")[0]);
                int nextOne = (time + 1) <= 23 ? time + 1 : 0;
                int nextTwo = nextOne + 1 <= 23 ? nextOne + 1 : 0;
                if (getType(in.getData()).equals("request")) {
                    int req = Integer.parseInt(getdata(in.getData()));

                    System.out.println("Request from : " + in.getAddress().getHostAddress() + " units requested: " + req);

                    if (reqFinish && (req + (3 * ((data.get(finalDay).get(nextOne) + data.get(finalDay).get(nextTwo)) / 2)) < threshold)) {
                        buff = ("response:" + req).getBytes();
                        final DatagramSocket outSoc = new DatagramSocket(45001);
                        DatagramPacket out = new DatagramPacket(buff, buff.length);
                        out.setAddress(in.getAddress());
                        out.setPort(in.getPort());
                        outSoc.send(out);
                        buff = new byte[256];
                        in = new DatagramPacket(buff, buff.length);
                        try {
                            outSoc.setSoTimeout(100);
                            outSoc.receive(in);
                            stationCapacity -= req;
                            threshold = (int) (0.8 * stationCapacity);
                            receivers.put(in.getAddress().getHostAddress(), req);

                            System.out.println("Transferring " + req + " units to " + in.getAddress().getHostAddress());
                            System.out.println("New Capacity: " + stationCapacity + "\tNew threshold: " + threshold);
                        } catch (SocketTimeoutException e) {
                            System.out.println("No response received...\n");
                        }
                        outSoc.close();
                    }
                    else {
                        System.out.println("Can't process request from: " + in.getAddress().getHostAddress());
                    }
                } else if(getType(in.getData()).equals("withdraw")) {
                    String doner = in.getAddress().getHostAddress();
                    if (doners.containsKey(doner)) {
                        System.out.println("Withdraw request from : " + in.getAddress().getHostAddress());
                        if (reqFinish && ((currentUsage / 3) + (data.get(finalDay).get(nextOne) + data.get(finalDay).get(nextTwo))) < threshold) {
                            buff = ("withdrawGrant:" + doners.get(doner)).getBytes();
                            final DatagramSocket outSoc = new DatagramSocket(45001);
                            DatagramPacket out = new DatagramPacket(buff, buff.length);
                            out.setAddress(in.getAddress());
                            out.setPort(in.getPort());
                            outSoc.send(out);

                            buff = new byte[256];
                            in = new DatagramPacket(buff, buff.length);
                            try {
                                outSoc.setSoTimeout(100);
                                outSoc.receive(in);
                                stationCapacity -= doners.get(doner);
                                threshold = (int) (0.8 * stationCapacity);

                                System.out.println("Transferring " + doners.get(doner) + " units to " + in.getAddress().getHostAddress());
                                System.out.println("New Capacity: " + stationCapacity + " New threshold: " + threshold);
                                doners.remove(doner);
                            } catch (SocketTimeoutException e) {
                                System.out.println("No response");
                            }
                            outSoc.close();
                        } else {
                            System.out.println("Can't process request from: " + in.getAddress().getHostAddress());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method loads the data received after analysis into the hashmap.
     * This is then used to predict consumption in next few hours. This information
     * is also used while deciding response to requests coming from other stations.
     *
     * @param fileName    file where the data is stored.
     */
    public void init(String fileName) {
        try{
            File file = new File(fileName);
            FileInputStream fileReader = new FileInputStream(file);
            DataInputStream input = new DataInputStream(fileReader);
            String str;
            String[] strArr;
            List<Integer> list;
            while((str = input.readLine()) != null) {
                strArr = str.split(",");
                String[] usage = strArr[1].split(":");
                if (data.containsKey(strArr[0])) {
                    list = data.get(strArr[0]);
                } else {
                    list = new ArrayList<>();
                    for(int i = 0; i < 24; i++) {
                        list.add(i, 0);
                    }
                }
                list.set(Integer.parseInt(usage[0]) - 1, Integer.parseInt(usage[1]));
                data.put(strArr[0], list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The main program. It starts the station.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Station address: " + ip);
            Registry stationsNamingRegistry = LocateRegistry.createRegistry(12005);
            Station myString = new Station();
            myString.init("/home/stu1/s9/psk7534/Courses/DS/STN_101.txt");
            stationsNamingRegistry.bind("rmi://" + ip + ":12005/mystring", myString);
            Thread t = new Thread(myString);
            t.start();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
    }
}
