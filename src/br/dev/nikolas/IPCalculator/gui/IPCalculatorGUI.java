package br.dev.nikolas.IPCalculator.gui;

import javax.swing.*;
import br.dev.nikolas.IPCalculator.model.IPCalculatorModel;
import java.awt.*;
import java.awt.event.ActionEvent;

public class IPCalculatorGUI {

    private final JTextField[] octets = new JTextField[4];
    private final JTextField cidrField = new JTextField(3);
    private final JTextArea resultArea = new JTextArea(20, 60);

    public void createAndShowGUI() {
        JFrame frame = new JFrame("Calculadora de IP - Java");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JPanel inputPanel = buildInputPanel();
        JScrollPane scrollPane = new JScrollPane(resultArea);
        resultArea.setEditable(false);

        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("Resultado"));
        resultPanel.add(scrollPane, BorderLayout.CENTER);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(resultPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Entrada de IP"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        panel.add(new JLabel("Endereço IP:"), gbc);
        gbc.gridx++;

        for (int i = 0; i < 4; i++) {
            octets[i] = new JTextField(3);
            panel.add(octets[i], gbc);
            gbc.gridx++;
            if (i < 3) {
                panel.add(new JLabel("."), gbc);
                gbc.gridx++;
            }
        }

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("CIDR (/): "), gbc);
        gbc.gridx++;
        panel.add(cidrField, gbc);

        gbc.gridx++;
        JButton calculateBtn = new JButton("Calcular");
        calculateBtn.addActionListener(this::calculate);
        panel.add(calculateBtn, gbc);

        return panel;
    }

    private void calculate(ActionEvent e) {
        try {
            int[] ipParts = new int[4];
            for (int i = 0; i < 4; i++) {
                ipParts[i] = Integer.parseInt(octets[i].getText());
                if (ipParts[i] < 0 || ipParts[i] > 255) throw new IllegalArgumentException("Octeto " + (i + 1) + " inválido.");
            }

            int cidr = Integer.parseInt(cidrField.getText());
            if (cidr < 1 || cidr > 30) throw new IllegalArgumentException("CIDR deve estar entre 1 e 30.");

            IPCalculatorModel model = new IPCalculatorModel(ipParts, cidr);
            resultArea.setText(model.getResultText());

        } catch (NumberFormatException ex) {
            resultArea.setText("Erro: Entrada inválida. Preencha todos os campos corretamente.");
        } catch (IllegalArgumentException ex) {
            resultArea.setText("Erro: " + ex.getMessage());
        } catch (Exception ex) {
            resultArea.setText("Erro inesperado: " + ex.getMessage());
        }
    }
}
