package com.webtool.packet;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapDLT;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.protocol.tcpip.Http;

import java.util.ArrayList;
import java.util.List;
public class PackageCatcher {
    public static void main(String[] args) {
        start();
    }

    private static final String DEV_NAME = "\\Device\\NPF_{4F039C40-B2B8-4531-9C75-E6C79FCA3CB9}";

    private static void start() {
        StringBuilder errbuf = new StringBuilder();
        List<PcapIf> ifs = new ArrayList<PcapIf>();
        if (Pcap.findAllDevs(ifs, errbuf) != 0) {
            System.err.println("No interfaces found");
            return;
        }
        Pcap pcap = null;
        System.out.println("interfaces count"+ifs.size());
        for (PcapIf i : ifs) {
          System.out.println(i);
            errbuf.setLength(0);
            if ((pcap = Pcap.openLive(i.getName(), 64, Pcap.MODE_NON_PROMISCUOUS, 1000, errbuf)) == null) {
                System.err.printf("Capture open %s: %s\n", i.getName(), errbuf.toString());
            }

            if (pcap.datalink() == PcapDLT.EN10MB.getValue() && i.getFlags() == 0) {
                System.out.printf("Opened interface\n\t%s\n\t%s\n", i.getName(), i.getDescription());
                System.out.printf("Warnings='%s'\n", errbuf.toString());
                break;
            } else {
                pcap.close();
                pcap = null;
            }
        }
        if (pcap == null) {
            System.err.println("Unable to find interface");
            return;
        }
        pcap.loop(Pcap.LOOP_INFINATE, (PcapPacket packet, String userObject) -> {
            System.out.println(userObject);            // ???????
            System.out.println(packet);            // ???http????
             if (packet.hasHeader(Http.ID)) {
                 Http http = new Http();
                 packet.getHeader(http);                // ????????
                 System.out.println(http);                // ?????????
                 System.out.println(new String(http.getPayload()));
             }
        },"xuxd");
    }
}