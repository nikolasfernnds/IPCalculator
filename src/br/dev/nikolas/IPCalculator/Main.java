package br.dev.nikolas.IPCalculator;

import javax.swing.SwingUtilities;
import br.dev.nikolas.IPCalculator.gui.IPCalculatorGUI;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IPCalculatorGUI().createAndShowGUI());
    }
}