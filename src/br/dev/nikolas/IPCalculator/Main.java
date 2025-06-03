package br.dev.nikolas.IPCalculator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IPCalculatorGUI().createAndShowGUI());
    }
}

class IPCalculatorGUI {

    private JTextField[] octets = new JTextField[4];
    private JTextField cidrField;
    private JTextArea resultArea;

    public void createAndShowGUI() {
        JFrame frame = new JFrame("Calculadora de IP - Java");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Entrada de IP"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        inputPanel.add(new JLabel("Endereço IP:"), gbc);
        gbc.gridx++;

        for (int i = 0; i < 4; i++) {
            octets[i] = new JTextField(3);
            inputPanel.add(octets[i], gbc);
            gbc.gridx++;
            if (i < 3) {
                inputPanel.add(new JLabel("."), gbc);
                gbc.gridx++;
            }
        }

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("CIDR (/): "), gbc);
        gbc.gridx++;
        cidrField = new JTextField(3);
        inputPanel.add(cidrField, gbc);

        gbc.gridx++;
        JButton calculateBtn = new JButton("Calcular");
        inputPanel.add(calculateBtn, gbc);

        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("Resultado"));
        resultPanel.add(scrollPane, BorderLayout.CENTER);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(resultPanel, BorderLayout.CENTER);

        calculateBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                calculate();
            }
        });

        frame.setVisible(true);
    }

    private void calculate() {
        try {
            int[] ipParts = new int[4];
            for (int i = 0; i < 4; i++) {
                ipParts[i] = Integer.parseInt(octets[i].getText());
                if (ipParts[i] < 0 || ipParts[i] > 255) {
                    throw new IllegalArgumentException("Octeto " + (i + 1) + " inválido.");
                }
            }

            int cidr = Integer.parseInt(cidrField.getText());
            if (cidr < 1 || cidr > 30) {
                throw new IllegalArgumentException("CIDR deve estar entre 1 e 30.");
            }

            int ipInt = (ipParts[0] << 24) | (ipParts[1] << 16) | (ipParts[2] << 8) | ipParts[3];
            int mask = 0xFFFFFFFF << (32 - cidr);
            int network = ipInt & mask;
            int broadcast = network | ~mask;

            int firstIP = network + 1;
            int lastIP = broadcast - 1;
            int totalHosts = (int) Math.pow(2, 32 - cidr) - 2;

            // Contagem de subredes baseado na classe e no CIDR
            int classBits = getClassBits(ipParts[0]);
            int subnetBits = cidr - classBits;
            int totalSubnets = subnetBits > 0 ? (int) Math.pow(2, subnetBits) : 1;

            resultArea.setText("");
            resultArea.append("IP: " + ipToString(ipInt) + "/" + cidr + "\n");
            resultArea.append("Classe: " + getIPClass(ipParts[0]) + "\n");
            resultArea.append("Máscara de sub-rede decimal: " + ipToString(mask) + "\n");
            resultArea.append("Máscara de sub-rede binária: " + maskToBinaryString(mask) + "\n");
            resultArea.append("Endereço de rede: " + ipToString(network) + "\n");
            resultArea.append("Endereço de broadcast: " + ipToString(broadcast) + "\n");
            resultArea.append("Primeiro IP válido: " + ipToString(firstIP) + "\n");
            resultArea.append("Último IP válido: " + ipToString(lastIP) + "\n");
            resultArea.append("Total de hosts possíveis: " + totalHosts + "\n");
            resultArea.append("Total de subredes possíveis: " + totalSubnets + "\n");

            // Listar subredes (exemplo simples, baseado no número de subredes)
            if (totalSubnets <= 256) { // Evitar lista muito longa
                resultArea.append("\nSub-redes possíveis:\n");
                List<String> subnets = generateSubnets(network, cidr, totalSubnets);
                for (String subnet : subnets) {
                    resultArea.append(" - " + subnet + "\n");
                }
            } else {
                resultArea.append("\nNúmero de subredes muito grande para listar.\n");
            }

        } catch (NumberFormatException e) {
            resultArea.setText("Erro: Entrada inválida. Preencha todos os campos corretamente.");
        } catch (IllegalArgumentException e) {
            resultArea.setText("Erro: " + e.getMessage());
        } catch (Exception e) {
            resultArea.setText("Erro inesperado: " + e.getMessage());
        }
    }

    private String getIPClass(int firstOctet) {
        if (firstOctet >= 1 && firstOctet <= 126) return "A";
        if (firstOctet >= 128 && firstOctet <= 191) return "B";
        if (firstOctet >= 192 && firstOctet <= 223) return "C";
        if (firstOctet >= 224 && firstOctet <= 239) return "D (Multicast)";
        return "E (Experimental)";
    }

    // Retorna os bits fixos de rede para cada classe (A=8, B=16, C=24)
    private int getClassBits(int firstOctet) {
        if (firstOctet >= 1 && firstOctet <= 126) return 8;
        if (firstOctet >= 128 && firstOctet <= 191) return 16;
        if (firstOctet >= 192 && firstOctet <= 223) return 24;
        return 0; // Classes D e E não são normalmente usadas para subnetting
    }

    private String ipToString(int ip) {
        return ((ip >>> 24) & 0xFF) + "." +
               ((ip >>> 16) & 0xFF) + "." +
               ((ip >>> 8) & 0xFF) + "." +
               (ip & 0xFF);
    }

    // Converte a máscara para formato binário em 4 octetos
    private String maskToBinaryString(int mask) {
        StringBuilder sb = new StringBuilder();
        for (int i = 3; i >= 0; i--) {
            int octet = (mask >>> (i * 8)) & 0xFF;
            sb.append(String.format("%8s", Integer.toBinaryString(octet)).replace(' ', '0'));
            if (i > 0) sb.append(".");
        }
        return sb.toString();
    }

    // Gera subredes (endereços de rede) para o número de subredes especificado
    private List<String> generateSubnets(int network, int cidr, int totalSubnets) {
        List<String> subnets = new ArrayList<>();
        int classBits = getClassBits((network >>> 24) & 0xFF);
        int subnetBits = cidr - classBits;
        int hostBits = 32 - cidr;

        // Tamanho do bloco da subrede em IPs
        int blockSize = (int) Math.pow(2, hostBits);

        for (int i = 0; i < totalSubnets; i++) {
            int subnetAddr = (network & (0xFFFFFFFF << (32 - classBits))) | (i << hostBits);
            subnets.add(ipToString(subnetAddr) + "/" + cidr);
        }

        return subnets;
    }
}
