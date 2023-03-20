import java.util.ArrayList;
import java.util.Collections;

import javax.swing.*;
import java.awt.*;

public class overlayMonitor {
    static int NbNodes;
    static String connections[][];
    static ArrayList<JTextField> Jtext;

    public static void main(String[] args) throws Exception {
        createTopologies();
        String cmd[] = new String[NbNodes];
        ArrayList<Integer> logicalAddress = new ArrayList<>();

        for (int i = 0; i < NbNodes; i++) {
            logicalAddress.add(i + 1);
        }
        Collections.shuffle(logicalAddress);

        // parameters are physicalAdd logicalAdd leftchild rightchild con1 con2 con3
        // ....
        for (int i = 0; i < NbNodes; i++) {
            if (i == 0) {
                cmd[i] = "gnome-terminal -- java -cp ./bin:./bin/amqp-client-5.14.2.jar:./bin/slf4j-api-1.7.36.jar:./bin/slf4j-simple-1.7.36.jar node "
                        + (i + 1)
                        + " " + logicalAddress.get(i) + " " + logicalAddress.get(NbNodes - 1) + " "
                        + logicalAddress.get(i + 1);
            } else if (i == NbNodes - 1) {
                cmd[i] = "gnome-terminal -- java -cp ./bin:./bin/amqp-client-5.14.2.jar:./bin/slf4j-api-1.7.36.jar:./bin/slf4j-simple-1.7.36.jar node "
                        + (i + 1)
                        + " " + logicalAddress.get(i) + " " + logicalAddress.get(i - 1) + " " + logicalAddress.get(0);
            } else
                cmd[i] = "gnome-terminal -- java -cp ./bin:./bin/amqp-client-5.14.2.jar:./bin/slf4j-api-1.7.36.jar:./bin/slf4j-simple-1.7.36.jar node "
                        + (i + 1)
                        + " " + logicalAddress.get(i) + " " + logicalAddress.get(i - 1) + " "
                        + logicalAddress.get(i + 1);

            for (int j = 0; j < NbNodes; j++) {
                if (connections[i][j].equals("1")) {
                    cmd[i] = cmd[i] + " " + (j + 1);
                }
            }
            System.out.println(cmd[i]);
        }

        for (int i = 0; i < NbNodes; i++) {
            Process process = Runtime.getRuntime().exec(cmd[i]);

        }
        System.out.println("Proceses initiated");
    }

    public static void createTopologies() {

        NbNodes = Integer.parseInt(JOptionPane.showInputDialog("Enter Number of NbNodes").trim());
        connections = new String[NbNodes][NbNodes];

        Jtext = new ArrayList<>();
        JPanel input = new JPanel();
        input.setLayout(new GridLayout(NbNodes + 1, NbNodes + 1));
        int counter = 1;
        for (int a = 0; a < (NbNodes + 1) * (NbNodes + 1); a++) {
            if (a <= NbNodes) {
                if (a == 0) {
                    input.add(new JLabel(""));
                } else
                    input.add(new JLabel(String.valueOf(a), SwingConstants.CENTER));
            } else if (a % (NbNodes + 1) == 0) {
                input.add(new JLabel(String.valueOf(counter), SwingConstants.CENTER));
                counter++;
            } else {
                JTextField content = new JTextField();
                Jtext.add(content);
                input.add(content);
            }

        }
        if (JOptionPane.showConfirmDialog(null, input, "Enter the matrix",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

            for (int i = 0; i < Jtext.size(); i++) {
                connections[i / NbNodes][i % NbNodes] = Jtext.get(i).getText();
            }
        }
    }

}
