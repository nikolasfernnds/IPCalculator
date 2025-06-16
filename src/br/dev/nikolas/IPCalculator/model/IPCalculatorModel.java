package br.dev.nikolas.IPCalculator.model;

import java.util.ArrayList;
import java.util.List;

public class IPCalculatorModel {

    private final int[] ipParts;
    private final int cidr;
    private final int ipInt;
    private final int mask;
    private final int network;
    private final int broadcast;
    private final int firstIP;
    private final int lastIP;
    private final int totalHosts;
    private final int totalSubnets;

    public IPCalculatorModel(int[] ipParts, int cidr) {
        this.ipParts = ipParts;
        this.cidr = cidr;

        this.ipInt = (ipParts[0] << 24) | (ipParts[1] << 16) | (ipParts[2] << 8) | ipParts[3];
        this.mask = 0xFFFFFFFF << (32 - cidr);
        this.network = ipInt & mask;
        this.broadcast = network | ~mask;
        this.firstIP = network + 1;
        this.lastIP = broadcast - 1;
        this.totalHosts = (int) Math.pow(2, 32 - cidr) - 2;
        int classBits = getClassBits(ipParts[0]);
        int subnetBits = cidr - classBits;
        this.totalSubnets = subnetBits > 0 ? (int) Math.pow(2, subnetBits) : 1;
    }

    public String getIPClass() {
        int firstOctet = ipParts[0];
        if (firstOctet >= 1 && firstOctet <= 126) return "A";
        if (firstOctet >= 128 && firstOctet <= 191) return "B";
        if (firstOctet >= 192 && firstOctet <= 223) return "C";
        if (firstOctet >= 224 && firstOctet <= 239) return "D (Multicast)";
        return "E (Experimental)";
    }

    private int getClassBits(int firstOctet) {
        if (firstOctet >= 1 && firstOctet <= 126) return 8;
        if (firstOctet >= 128 && firstOctet <= 191) return 16;
        if (firstOctet >= 192 && firstOctet <= 223) return 24;
        return 0;
    }

    public String ipToString(int ip) {
        return ((ip >>> 24) & 0xFF) + "." +
               ((ip >>> 16) & 0xFF) + "." +
               ((ip >>> 8) & 0xFF) + "." +
               (ip & 0xFF);
    }

    public String maskToBinaryString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 3; i >= 0; i--) {
            int octet = (mask >>> (i * 8)) & 0xFF;
            sb.append(String.format("%8s", Integer.toBinaryString(octet)).replace(' ', '0'));
            if (i > 0) sb.append(".");
        }
        return sb.toString();
    }

    public List<String> generateSubnets() {
        List<String> subnets = new ArrayList<>();
        int classBits = getClassBits((network >>> 24) & 0xFF);
        int hostBits = 32 - cidr;
        int blockSize = (int) Math.pow(2, hostBits);

        for (int i = 0; i < totalSubnets; i++) {
            int subnetAddr = (network & (0xFFFFFFFF << (32 - classBits))) | (i << hostBits);
            subnets.add(ipToString(subnetAddr) + "/" + cidr);
        }

        return subnets;
    }

    public String getResultText() {
        StringBuilder sb = new StringBuilder();
        sb.append("IP: ").append(ipToString(ipInt)).append("/").append(cidr).append("\n");
        sb.append("Classe: ").append(getIPClass()).append("\n");
        sb.append("Máscara de sub-rede decimal: ").append(ipToString(mask)).append("\n");
        sb.append("Máscara de sub-rede binária: ").append(maskToBinaryString()).append("\n");
        sb.append("Endereço de rede: ").append(ipToString(network)).append("\n");
        sb.append("Endereço de broadcast: ").append(ipToString(broadcast)).append("\n");
        sb.append("Primeiro IP válido: ").append(ipToString(firstIP)).append("\n");
        sb.append("Último IP válido: ").append(ipToString(lastIP)).append("\n");
        sb.append("Total de hosts possíveis: ").append(totalHosts).append("\n");
        sb.append("Total de subredes possíveis: ").append(totalSubnets).append("\n");

        if (totalSubnets <= 256) {
            sb.append("\nSub-redes possíveis:\n");
            for (String subnet : generateSubnets()) {
                sb.append(" - ").append(subnet).append("\n");
            }
        } else {
            sb.append("\nNúmero de subredes muito grande para listar.\n");
        }

        return sb.toString();
    }
}
