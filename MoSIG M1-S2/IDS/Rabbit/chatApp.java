import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

public class chatApp {
    private static final String chatExchange = "chatExchange";

    private static final String exchange_key = "clients_key";
    private static String personalKey;
    static ArrayList<String> messagesStore;
    private static final String messages_type[] = { "joined", "normal", "leave", "status", "elected" };
    private static boolean synchronized_;
    private static boolean joined;
    static Channel channel;
    static String queueName;
    // private static String state;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: chatClient <<name>>");
            System.exit(1);
        }
        personalKey = args[0];
        // Initiate message box
        messagesStore = new ArrayList<>();
        synchronized_ = false;
        joined = false;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");

            if (message.substring(0, messages_type[0].length()).equals(messages_type[0])
                    && !personalKey.equals(message.substring(messages_type[0].length() + 1))) {

                String control_message = messages_type[3] + ":" + personalKey;
                channel.basicPublish(chatExchange,
                        message.substring(messages_type[0].length() + 1), null,
                        control_message.getBytes("UTF-8"));

            }
            // received the keys from others
            else if (message.substring(0, messages_type[3].length()).equals(messages_type[3])
                    && !exchange_key.equals(message.substring(messages_type[3].length() + 1))) {
                if (!synchronized_) {

                    String control_message = messages_type[4] + ":" + personalKey;
                    channel.basicPublish(chatExchange,
                            message.substring(messages_type[3].length() + 1), null,
                            control_message.getBytes("UTF-8"));
                    synchronized_ = true;
                }

            }
            // if am elected then start sending the messages.
            else if (message.substring(0, messages_type[4].length()).equals(messages_type[4])
                    && !exchange_key.equals(message.substring(messages_type[4].length() + 1))) {

                // System.out.println("Received :" + message);
                for (int i = 0; i < messagesStore.size(); i++) {
                    channel.basicPublish(chatExchange, message.substring(messages_type[4].length() + 1), null,
                            messagesStore.get(i).getBytes("UTF-8"));
                }
                channel.basicPublish(chatExchange,
                        message.substring(messages_type[4].length() + 1), null,
                        "finished".getBytes("UTF-8"));

            } else {

                if (message.equals("finished") && delivery.getEnvelope().getRoutingKey().equals(personalKey)) {
                    String send = args[0] + ":Joined";
                    channel.basicPublish(chatExchange,
                            exchange_key, null,
                            send.getBytes("UTF-8"));
                } else {

                    if (message.substring(0, messages_type[0].length()).equals(messages_type[0])) {
                        message = message.substring(messages_type[0].length() + 1) + ":Joined";
                    }
                    if (!message.substring(0, args[0].length()).equals(args[0])) {
                        System.out.println(message);
                    }

                    messagesStore.add(message);

                }

            }
        };

        Scanner iScanner = new Scanner(System.in);
        String message;
        int key;
        System.out.println("");
        System.out.println("************Enter help to view functionalities************");
        while (true) {

            message = iScanner.nextLine();

            // if entered help show help
            if (message.trim().equals("help")) {
                System.out.println("Enter \"Join\" to Join Chat");
                System.out.println("Enter \"Leave\" to Leave Chat");
                System.out.println("Otherwise enter message to be sent");
            }
            // if entered Join , Join the chat
            else if (message.trim().equals("Join")) {
                if (joined) {
                    System.out.println("Already in the group");
                } else {
                    message = messages_type[0] + ":" + personalKey;
                    channel = connection.createChannel();

                    channel.exchangeDeclare(chatExchange, "direct");
                    queueName = channel.queueDeclare().getQueue();
                    channel.queueBind(queueName, chatExchange, exchange_key);
                    channel.queueBind(queueName, chatExchange, personalKey);

                    channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
                    });
                    channel.basicPublish(chatExchange, exchange_key, null,
                            message.getBytes("UTF-8"));
                    joined = true;
                }

            }
            // if entered Leave
            else if (message.trim().equals("Leave")) {
                message = args[0] + " left";
                if (channel == null) {
                    System.out.println("No chat to leave");

                } else {
                    channel.basicPublish(chatExchange, exchange_key, null,
                            message.getBytes("UTF-8"));
                    channel.queueUnbind(queueName, chatExchange, exchange_key);
                    channel = null;
                }

            }
            // otherwise send messages in a chat
            else {
                message = args[0] + ":" + message;
                if (channel == null) {
                    System.out.println("Please Join the chat");

                } else {
                    channel.basicPublish(chatExchange, exchange_key, null,
                            message.getBytes("UTF-8"));
                }

            }
        }
    }
}
