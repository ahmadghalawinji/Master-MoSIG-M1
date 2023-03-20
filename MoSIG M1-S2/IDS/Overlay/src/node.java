import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.text.Style;

import java.util.*;

public class node {

    // Physical and Logical addresses for the nodes in the network
    private static String physicalAdd, logicalAddr, leftchild, rightchild;

    // List of all physical connections
    private static ArrayList<String> physicalConnection;

    private static ArrayList<String> requestMessagesRecv;

    private static String MessageType[] = { "request", "reply", "election", "normal" };

    // Table for all nodes in an overlay
    private static HashMap<String, String> routeTable;

    // to know if my leftchild and rightchild have been discovered
    private static Boolean connected;

    // varibles responsible for rabbitMq connectionS
    private static String queueName;
    private static String physicalExchange = "physicalExchange";
    private static ConnectionFactory factory;
    private static Connection connection;
    private static Channel channel;

    public static void main(String[] args) throws Exception {

        // Each node should have atleast physical address , logical address and one
        // connection
        if (args.length < 5) {
            System.err.println("usage: node physicalAdd logicalAdd leftchild rightchild conn1 conn2 conn3 ...");
            System.exit(1);
        }
        // initialize the node connections and send its status
        Initialize(args);
        // schedules the message after 3 sec
        System.out.println("Node started with Physical Address " + physicalAdd + " and Logical Address " + logicalAddr);
        Schedule();

        // Delivercall back for rabbitmq
        Delivercallback();

        Scanner scanner = new Scanner(System.in);

        String input;

        while (true) {

            input = scanner.nextLine();
            switch (input) {
                case "r":
                    if (connected) {
                        unicastMessages(physicalAdd, logicalAddr, routeTable.get(rightchild),
                                rightchild, "TOKEN",
                                MessageType[3], logicalAddr);
                    } else
                        System.out.println("Not conneted");

                    break;
                case "l":
                    if (connected) {
                        unicastMessages(physicalAdd, logicalAddr, routeTable.get(leftchild),
                                leftchild, "TOKEN",
                                MessageType[3], logicalAddr);
                    } else
                        System.out.println("Not conneted");

                    break;
                case "t":
                    System.out.println("Logical Add : Next Physical Add");
                    for (String key : routeTable.keySet()) {
                        System.out.println(key + "\t:\t" + routeTable.get(key));
                    }
                    break;

                default:
                    System.out.println("Help\nr: send right\nl:send left\nt display route table");
                    break;
            }
        }

    }

    /**
     * Initialize Method
     * 
     * This method initializes necessary variable for node network connectivity
     * including Physical Address, Logical Address,Left child, Right child in a
     * Logical Ring and all Physical Connection it has.Furthermore this method
     * initializes the RabbitMQ variable such as factory,connection, direct exchange
     * ,channel queue and binds the queue to the exchange.
     * 
     * @param conn
     * @throws Exception
     */
    private static void Initialize(String[] conn) throws Exception {

        physicalAdd = conn[0];
        logicalAddr = conn[1];
        leftchild = conn[2];
        rightchild = conn[3];

        factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare(physicalExchange, "direct");
        queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, physicalExchange, conn[0]);

        connected = false;
        routeTable = new HashMap<>();
        requestMessagesRecv = new ArrayList<String>();
        physicalConnection = new ArrayList<String>();

        for (int i = 4; i < conn.length; i++) {
            physicalConnection.add(conn[i]);
        }
    }

    /**
     * Schedule Method
     * 
     * This method schedules two broadcast messages for a node requesting for
     * Physical Address where it's right and left children are located.The
     * broadcasts are sent 10 seconds after the nodes have started and sends it
     * repeatedly after every 30 seconds until it gets them
     * 
     */

    private static void Schedule() {

        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {

                if (routeTable.containsKey(rightchild) && routeTable.containsKey(leftchild)
                        && !connected) {

                    unicastMessages(physicalAdd, logicalAddr, routeTable.get(rightchild),
                            rightchild, "", MessageType[2]);
                }

            }
        }, 5000, 10000);

        t.schedule(new TimerTask() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                broadcastMessages(physicalAdd, logicalAddr, leftchild,
                        MessageType[0]);

                broadcastMessages(physicalAdd, logicalAddr, rightchild,
                        MessageType[0]);

            }

        }, 10000);

    }

    /**
     * Delivercallback Method
     * 
     * This method sets the delivercallback for the RabbitMQ and consumes the
     * messages from the queue
     * 
     * @throws Exception
     */

    private static void Delivercallback() throws Exception {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                ByteArrayInputStream byteStream = new ByteArrayInputStream(delivery.getBody());
                ObjectInputStream objStream = new ObjectInputStream(byteStream);
                message msg = (message) objStream.readObject();
                objStream.close();
                // System.out.println(msg.getMessage());
                requestReplymessages(msg);
                connectingMessage(msg);

                ringMessages(msg);

            } catch (Exception e) {
                System.out.println("Failed \n" + e);

            }
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {

        });
    }

    /**
     * broadcastMessages Method
     * 
     * This method sends a broadcast a message to all Physically connected nodes
     * 
     * @param sourcePhysAddr
     * @param sourceLogicalAddr
     * @param message
     * @param type
     */

    private static void broadcastMessages(String sourcePhysAddr, String sourceLogicalAddr, String message,
            String type) {

        message msg = new message(sourcePhysAddr, sourceLogicalAddr, "ALL", "ALL",
                message, type);

        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(byteStream);

            objStream.writeObject(msg);
            objStream.flush();
            objStream.close();
            for (int i = 0; i < physicalConnection.size(); i++) {
                channel.basicPublish(physicalExchange, physicalConnection.get(i), null,
                        byteStream.toByteArray());
            }

        } catch (Exception e) {
            System.out.println("Reached the end");
        }
    }

    /**
     * propagateMessages Method
     * 
     * This method forwards a message it received to its direct connected nodes
     * except to the node it received the message. Before forwarding it updates its
     * routing table with the source logical address of the message and the physical
     * address it received the message from and updates the message's source
     * physical address i.e(Store and forward).
     * 
     * @param msg
     */
    private static void propagateMessages(message msg) {

        String receivedFrom = msg.getSourcePhysAdd();

        if (!routeTable.containsKey(msg.getSourceLogicAdd())) {

            routeTable.put(msg.getSourceLogicAdd(), msg.getSourcePhysAdd());
        } else if (Integer.parseInt(routeTable.get(msg.getSourceLogicAdd())) > Integer
                .parseInt(msg.getSourcePhysAdd())) {
            System.out
                    .println("replace " + routeTable.get(msg.getSourceLogicAdd()) + " with " + msg.getSourcePhysAdd()
                            + " at " + logicalAddr);
            routeTable.replace(msg.getSourceLogicAdd(), msg.getSourcePhysAdd());
        }
        msg.setSourcePhysAdd(physicalAdd);
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
            objStream.writeObject(msg);
            objStream.flush();
            objStream.close();
            for (int i = 0; i < physicalConnection.size(); i++) {
                if (physicalConnection.get(i).equals(receivedFrom)) {
                    continue;
                }
                channel.basicPublish(physicalExchange, physicalConnection.get(i), null,
                        byteStream.toByteArray());

            }
        } catch (

        Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * unicastMessages Method
     * 
     * This Method is responsible for sending unicast messages from one node to the
     * other
     * 
     * @param sourcePhysAddr
     * @param sourceLogicalAddr
     * @param destinationPhysAddr
     * @param destinationLogicalAddr
     * @param message
     * @param type
     */
    private static void unicastMessages(String sourcePhysAddr, String sourceLogicalAddr, String destinationPhysAddr,
            String destinationLogicalAddr, String message, String type) {

        message msg = new message(sourcePhysAddr, sourceLogicalAddr, destinationPhysAddr, destinationLogicalAddr,
                message, type);
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
            objStream.writeObject(msg);
            objStream.flush();
            objStream.close();
            channel.basicPublish(physicalExchange, destinationPhysAddr, null,
                    byteStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void unicastMessages(String sourcePhysAddr, String sourceLogicalAddr, String destinationPhysAddr,
            String destinationLogicalAddr, String message, String type, String initiator) {

        message msg = new message(sourcePhysAddr, sourceLogicalAddr, destinationPhysAddr, destinationLogicalAddr,
                message, type, initiator);
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
            objStream.writeObject(msg);
            objStream.flush();
            objStream.close();
            channel.basicPublish(physicalExchange, destinationPhysAddr, null,
                    byteStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * requestReplymessages Method
     * 
     * This method is responsible for responding to the request for the node's left
     * and right neighbor and reply messages from the neighbor nodes.
     * 
     * @param msg
     */

    private static void requestReplymessages(message msg) {
        if (msg.getMsgtype().equals(MessageType[0]) &&
                msg.getMessage().equals(logicalAddr)) {

            unicastMessages(physicalAdd, logicalAddr, msg.getSourcePhysAdd(),
                    msg.getSourceLogicAdd(), msg.getMessage(),
                    MessageType[1]);
        } else if (msg.getMsgtype().equals(MessageType[0]) &&
                !msg.getMessage().equals(logicalAddr)
                && !requestMessagesRecv
                        .contains(msg.getSourceLogicAdd() + ":" + msg.getMsgtype() + ":" +
                                msg.getMessage())
                && !msg.getDestinationLogicAdd().equals(logicalAddr)) {
            requestMessagesRecv.add(msg.getSourceLogicAdd() + ":" + msg.getMsgtype() +
                    ":" + msg.getMessage());

            propagateMessages(msg);
        } else if (msg.getMsgtype().equals(MessageType[1]) && msg.getDestinationLogicAdd().equals(logicalAddr)) {

            if (msg.getMessage().equals(leftchild) && !routeTable.containsKey(leftchild)) {
                routeTable.put(leftchild, msg.getSourcePhysAdd());

            } else if (msg.getMessage().equals(rightchild) && !routeTable.containsKey(rightchild)) {
                routeTable.put(rightchild, msg.getSourcePhysAdd());
            }

        } else if (msg.getMsgtype().equals(MessageType[1]) &&
                !msg.getDestinationLogicAdd().equals(logicalAddr)) {

            unicastMessages(physicalAdd, msg.getSourceLogicAdd(),
                    routeTable.get(msg.getDestinationLogicAdd()),
                    msg.getDestinationLogicAdd(),
                    msg.getMessage(),
                    MessageType[1]);
        }
    }

    /**
     * connectingMessage Method
     * 
     * This Method processes the connection messages which are exchanged once a node
     * gets response from its left and right neighbor in the ring.
     * 
     * @param msg
     */

    private static void connectingMessage(message msg) {

        if (msg.getMsgtype().equals(MessageType[2]) && msg.getDestinationLogicAdd().equals(logicalAddr)
                && !msg.getSourceLogicAdd().equals(logicalAddr) && routeTable.containsKey(rightchild)) {
            unicastMessages(physicalAdd, msg.getSourceLogicAdd(),
                    routeTable.get(rightchild),
                    rightchild,
                    msg.getMessage(),
                    MessageType[2]);

        }

        else if (msg.getMsgtype().equals(MessageType[2]) && !msg.getDestinationLogicAdd().equals(logicalAddr)
                && routeTable.containsKey(msg.getDestinationLogicAdd())) {

            unicastMessages(physicalAdd, msg.getSourceLogicAdd(),
                    routeTable.get(msg.getDestinationLogicAdd()),
                    msg.getDestinationLogicAdd(),
                    msg.getMessage(),
                    MessageType[2]);

        } else if (msg.getMsgtype().equals(MessageType[2])
                && msg.getSourceLogicAdd().equals(logicalAddr)) {

            connected = true;
            System.out.println("Connected to " + rightchild + " and " + leftchild);
        }
    }

    /**
     * ringMessages Method
     * 
     * This Method proceses the messages which are exchanged once a ring is
     * established.
     * 
     * @param msg
     */
    private static void ringMessages(message msg) {
        if (msg.getMsgtype().equals(MessageType[3])
                && msg.getInitiator().equals(logicalAddr) && msg.getDestinationLogicAdd().equals(logicalAddr)) {

            System.out.println("Ring Complete");

        } else if (msg.getMsgtype().equals(MessageType[3]) && msg.getDestinationLogicAdd().equals(logicalAddr)
                && !msg.getInitiator().equals(logicalAddr)) {
            try {
                System.out.println("\nReceived [x] : " + msg.getMessage() + " from " +
                        msg.getSourceLogicAdd() + " via "
                        + msg.getSourcePhysAdd());

                Thread.sleep(2000);

            } catch (Exception e) {
                System.out.println("Thread Sleep Failed" + e);
            }
            if (msg.getSourceLogicAdd().equals(leftchild)) {
                msg.setSourceLogicalAdd(logicalAddr);
                unicastMessages(physicalAdd, msg.getSourceLogicAdd(), routeTable.get(rightchild),
                        rightchild, msg.getMessage(), msg.getMsgtype(), msg.getInitiator());
            } else {
                msg.setSourceLogicalAdd(logicalAddr);
                unicastMessages(physicalAdd, msg.getSourceLogicAdd(), routeTable.get(leftchild),
                        leftchild, msg.getMessage(), msg.getMsgtype(), msg.getInitiator());
            }

        } else if (msg.getMsgtype().equals(MessageType[3]) && !msg.getDestinationLogicAdd().equals(logicalAddr)) {
            unicastMessages(physicalAdd, msg.getSourceLogicAdd(), routeTable.get(msg.getDestinationLogicAdd()),
                    msg.getDestinationLogicAdd(), msg.getMessage(), msg.getMsgtype(), msg.getInitiator());
        }
    }
}