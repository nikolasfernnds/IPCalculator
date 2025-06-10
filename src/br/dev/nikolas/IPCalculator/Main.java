package br.dev.nikolas.IPCalculator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList; // Embora não usado no snippet, mantido da original
import java.util.List;     // Embora não usado no snippet, mantido da original

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
        gbc.insets = new Insets(5, 5, 5, 5); // Essas insets se aplicam a todos os componentes, mas vamos otimizar
        gbc.anchor = GridBagConstraints.WEST; // Alinhar componentes à esquerda

        // --- INÍCIO DA CORREÇÃO: Agrupando os octetos e pontos em um JPanel separado ---

        // JLabel para "Endereço IP:"
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Endereço IP:"), gbc);

        // Criar um JPanel para agrupar os campos de octeto e os pontos
        // Usamos FlowLayout com pequenos espaçamentos horizontais (hgap) e verticais (vgap)
        JPanel ipOctetGroupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); // hgap=2, vgap=0

        for (int i = 0; i < 4; i++) {
            octets[i] = new JTextField(3); // JTextField de 3 colunas para cada octeto
            ipOctetGroupPanel.add(octets[i]); // Adiciona o campo de texto ao painel de grupo
            if (i < 3) {
                ipOctetGroupPanel.add(new JLabel(".")); // Adiciona o ponto ao painel de grupo
            }
        }

        // Adiciona o JPanel de grupo de octetos ao inputPanel principal
        gbc.gridx = 1; // Coloca o grupo de octetos na próxima coluna
        gbc.gridwidth = GridBagConstraints.REMAINDER; // Faz com que o grupo ocupe o restante da linha
        gbc.fill = GridBagConstraints.HORIZONTAL; // Permite que o grupo se estique horizontalmente
        inputPanel.add(ipOctetGroupPanel, gbc);

        // --- FIM DA CORREÇÃO ---

        // Resetar gridwidth e fill para os próximos componentes, para que não herdem as configurações do ipOctetGroupPanel
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 0;
        gbc.gridy++; // Move para a próxima linha para o CIDR
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
            // A validação do CIDR aqui pode precisar de ajuste dependendo do seu objetivo.
            // Para redes /31 e /32, o conceito de "hosts usáveis" muda.
            // O código original limitava a 30, estou deixando assim para manter sua lógica.
            if (cidr < 1 || cidr > 32) { // Alterado para 32 para permitir /31 e /32, se quiser. Se não, mantenha 30.
                throw new IllegalArgumentException("CIDR deve estar entre 1 e 32.");
            }


            long ipLong = (long)ipParts[0] << 24 | (long)ipParts[1] << 16 | (long)ipParts[2] << 8 | ipParts[3];
            long mask = 0xFFFFFFFFL << (32 - cidr);
            long network = ipLong & mask;
            long broadcast = network | ~mask;

            long firstIP = -1; // Usar -1 para indicar que pode não existir
            long lastIP = -1; // Usar -1 para indicar que pode não existir
            long totalHosts = (1L << (32 - cidr)); // Total de endereços na rede

            if (cidr <= 30) { // Para redes com hosts usáveis (excluindo /31 e /32)
                firstIP = network + 1;
                lastIP = broadcast - 1;
                totalHosts -= 2; // Exclui endereço de rede e broadcast
            } else if (cidr == 31) { // Rede ponto a ponto, ambos IPs usáveis
                firstIP = network;
                lastIP = broadcast;
                totalHosts = 2; // Ambos IPs são usáveis
            } else if (cidr == 32) { // Rede de host único
                firstIP = network;
                lastIP = network;
                totalHosts = 1; // O próprio IP é o host
            }


            // Contagem de subredes baseado na classe e no CIDR
            int classBits = getClassBits(ipParts[0]);
            int subnetBits = cidr - classBits;
            int totalSubnets = subnetBits > 0 ? (int) Math.pow(2, subnetBits) : 1;
            // Se CIDR for menor que o default da classe (ex: 192.168.1.0/8), o calculo de subredes muda
            if (cidr < classBits) { // Se o CIDR é menor que a máscara padrão da classe
                totalSubnets = 1; // Considera como uma única super-rede ou rede principal
            } else {
                totalSubnets = (int) Math.pow(2, cidr - classBits);
            }
            if (totalSubnets == 0) totalSubnets = 1; // Garante mínimo de 1 sub-rede


            resultArea.setText("");
            resultArea.append("IP: " + ipToString(ipLong) + "/" + cidr + "\n");
            resultArea.append("Classe: " + getIPClass(ipParts[0]) + "\n");
            resultArea.append("Máscara de sub-rede decimal: " + ipToString(mask) + "\n");
            resultArea.append("Máscara de sub-rede binária: " + maskToBinaryString(mask) + "\n");
            resultArea.append("Endereço de rede: " + ipToString(network) + "\n");
            resultArea.append("Endereço de broadcast: " + ipToString(broadcast) + "\n");

            if (totalHosts > 0) { // Apenas mostra se houver hosts usáveis
                resultArea.append("Primeiro IP válido: " + ipToString(firstIP) + "\n");
                resultArea.append("Último IP válido: " + ipToString(lastIP) + "\n");
            } else {
                 resultArea.append("Não há hosts usáveis nesta rede.\n");
            }
            resultArea.append("Número de hosts possíveis: " + totalHosts + "\n");
            resultArea.append("Total de subredes possíveis: " + totalSubnets + "\n");

            // Listar subredes (exemplo simples, baseado no número de subredes)
            if (totalSubnets > 1 && totalSubnets <= 256) { // Evitar lista muito longa e listar apenas se houver mais de 1
                resultArea.append("\nSub-redes possíveis:\n");
                List<String> subnets = generateSubnets(network, cidr, totalSubnets);
                for (String subnet : subnets) {
                    resultArea.append(" - " + subnet + "\n");
                }
            } else if (totalSubnets > 256) {
                resultArea.append("\nNúmero de subredes muito grande para listar.\n");
            }

        } catch (NumberFormatException e) {
            resultArea.setText("Erro: Entrada inválida. Preencha todos os campos corretamente com números.");
        } catch (IllegalArgumentException e) {
            resultArea.setText("Erro: " + e.getMessage());
        } catch (Exception e) {
            resultArea.setText("Erro inesperado: " + e.getMessage() + "\n" + e.getClass().getName());
            e.printStackTrace(); // Para depuração
        }
    }

    private String getIPClass(int firstOctet) {
        if (firstOctet >= 1 && firstOctet <= 126) return "A";
        if (firstOctet >= 128 && firstOctet <= 191) return "B";
        if (firstOctet >= 192 && firstOctet <= 223) return "C";
        if (firstOctet >= 224 && firstOctet <= 239) return "D (Multicast)";
        if (firstOctet >= 240 && firstOctet <= 255) return "E (Experimental)"; // Adicionado classe E
        return "Inválida/Reservada"; // Para 0.x.x.x ou outros casos
    }

    // Retorna os bits fixos de rede para cada classe (A=8, B=16, C=24)
    private int getClassBits(int firstOctet) {
        if (firstOctet >= 1 && firstOctet <= 126) return 8;
        if (firstOctet >= 128 && firstOctet <= 191) return 16;
        if (firstOctet >= 192 && firstOctet <= 223) return 24;
        return 0; // Para classes D, E ou IPs inválidos/reservados, não há bits de classe padrão para subnetting tradicional
    }

    private String ipToString(long ip) { // Usando long para evitar overflow com máscara
        return ((ip >>> 24) & 0xFF) + "." +
                ((ip >>> 16) & 0xFF) + "." +
                ((ip >>> 8) & 0xFF) + "." +
                (ip & 0xFF);
    }

    // Converte a máscara para formato binário em 4 octetos
    private String maskToBinaryString(long mask) { // Usando long para evitar overflow
        StringBuilder sb = new StringBuilder();
        for (int i = 3; i >= 0; i--) {
            long octet = (mask >>> (i * 8)) & 0xFF;
            sb.append(String.format("%8s", Long.toBinaryString(octet)).replace(' ', '0'));
            if (i > 0) sb.append(".");
        }
        return sb.toString();
    }

    // Gera subredes (endereços de rede) para o número de subredes especificado
    private List<String> generateSubnets(long network, int cidr, int totalSubnets) {
        List<String> subnets = new ArrayList<>();
        int hostBits = 32 - cidr;

        // Tamanho do bloco da subrede em IPs
        // Aumenta o tamanho da rede original para o próximo bloco de sub-rede
        long currentSubnetNetwork = network & (0xFFFFFFFFL << (32 - cidr)); // Garante que comece no limite da rede

        for (int i = 0; i < totalSubnets; i++) {
            subnets.add(ipToString(currentSubnetNetwork) + "/" + cidr);
            currentSubnetNetwork += (1L << hostBits); // Adiciona o tamanho do bloco para a próxima sub-rede
        }

        return subnets;
    }
}