import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

public class RingTopology {
    static Channel channel;
    private static final String chatExchange = "chatExchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();


        channel.queueDeclare(chatExchange, true, false, false, null);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");

            if (message.substring(0, messages_type[0].length()).equals(messages_type[0])
                    && !personalKey.equals(message.substring(messages_type[0].length() + 1))) {

                String control_message = messages_type[3] + ":" + personalKey;
                channel.basicPublish(chatExchange,
                        message.substring(messages_type[0].length() + 1), null,
                        control_message.getBytes("UTF-8"));

            }

        channel.basicPublish("", chatExchange, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes("UTF-8"));
        System.out.println(message);


        int size = 3;
        Node rings[] = new Node[size];
        for (int i = 0; i < size; i++)
            rings[i] = new Node(i);

        for (int i = 0; i < size; i++)
            rings[i].SetNeighbors(rings[(i + size - 1) % size], rings[(i + size + 1) % size]);

    };

    class Node implements Runnable {
        Channel nextChannel;
        int id;
        Node(int id) {
            this.id = id;

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            final Connection connection = factory.newConnection();
            channel = connection.createChannel();

            channel.queueDeclare(chatExchange, true, false, false, null);
            channel.basicQos(2);

            final Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                        byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");

                    System.out.println("Received" + message);
                    try {
                        
                    } finally {

                        
                    }
                }
            };
            channel.basicConsume(chatExchange, false, consumer);

        }



        public void run() {

        }
    }
}}
