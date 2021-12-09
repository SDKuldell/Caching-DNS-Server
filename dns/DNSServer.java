package dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import dns.DNSMessage;

public class DNSServer {
    
    final private int PORT = 53;
    final private int MAX_SIZE = 512;

    private DNSZone zone;
    /* TODO: add class variable for the cache :)*/
    private DNSCache cache;
    private HashMap<Integer,DNSMessage> outstandingQueries;


    private InetAddress nextServer;
    private int nextServerPort;

    public DNSServer(DNSZone zone) {
        this.zone = zone;
        this.outstandingQueries = new HashMap<Integer,DNSMessage>();
        this.cache = new DNSCache();

        try {
            nextServer = InetAddress.getByName("127.0.0.53");
        } catch(UnknownHostException e) {
            System.out.println("Should never get here.");
            System.exit(0);
        }
        nextServerPort = 53;

        /* TODO: add a DNSCache object :)*/

        System.out.printf("Starting server on port %d%n", PORT);
    }

    /* TODO: complete me! */
    private DatagramPacket handleQuery(DNSMessage query) {
        /* print the query message contents */
        System.out.println("Query received from " + query.getPacket().getSocketAddress());
        System.out.println(query);

        /* look for the record in our zone */
        boolean inZone = true;
        var records = zone.getRecords(query.getQuestionName(), query.getQuestionType(), query.getQuestionClass());

        /* TODO: look for the record in the cache if it's not in our zone */

        records.addAll(cache.returnRecords(query.getQuestionName(), query.getQuestionType(), query.getQuestionClass()));
        

        /* send the response back to the client if we found the record either in our zone or in the cache */
        if(records.size() != 0) {
            /* make a response message */
            var reply = new DNSMessage(query, records, inZone);

            /* print the response message contents */
            System.out.println("Reply to " + query.getPacket().getSocketAddress());
            System.out.println(reply);

            /* make and return a response packet */
            return new DatagramPacket(reply.getData(), reply.getDataLength(), query.getPacket().getSocketAddress());
        }

        /* if we didn't find the record, send to the next server (see nextServer and nextServerPort variables) */

        /* TODO: print the response message contents */
        	System.out.println("Forwarding Query to " + nextServer);
        	System.out.println(query);

        /* TODO: store the query so we can respond to it when we get a reply :)*/
          outstandingQueries.put(query.getID(), query);

        /* TODO: make and return a new DatagramPacket query packet to forward :)*/
         return new DatagramPacket(query.getData(), query.getDataLength(), nextServer, nextServerPort);
    }

    /* TODO: complete me! */
    private DatagramPacket handleReply(DNSMessage reply) {
        /* print the reply message contents */
        System.out.println("Reply received from " + reply.getPacket().getSocketAddress());
        System.out.println(reply);

        /* TODO: match the reply to the original query :)*/
        DNSMessage origQuery = outstandingQueries.get(reply.getID());

        /* TODO: add answers to the cache :)*/
        cache.addEntries(reply.getAnswers());

        /* TODO: remove the original query from the outstanding set :)*/
        outstandingQueries.remove(reply.getID());

        /* TODO: print the reply message again for consistency :)*/
        System.out.println("Forwarding reply to" + origQuery.getPacket().getSocketAddress());
        System.out.println(reply);

        /* TODO: make and return a new response packet to send to the original client :)*/
        return new DatagramPacket(reply.getData(), reply.getDataLength(), origQuery.getPacket().getSocketAddress());
    }

    private DatagramPacket handleMessage(DatagramPacket incomingPkt) {
        /* TODO: update the cache each time we receive a message, to remove any records with expired TTLs :)*/
        cache.checkCache();    

        /* create a DNS Message object that will parse the request packet data */
        var incomingMessage = new DNSMessage(incomingPkt);

        /* handle queries */
        if(incomingMessage.isQuery()) {
            return handleQuery(incomingMessage);
        }

        /* handle replies */
        else {
            return handleReply(incomingMessage);
        }
    }

    public void run() {
        try (
            var sock = new DatagramSocket(PORT, InetAddress.getLoopbackAddress());
        ) {
            /* keep reading packets one at a time, forever */
            while(true) {
                /* packet to store the incoming message */
                var in_packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);

                /* blocking call, read one packet */
                sock.receive(in_packet);

                /* handle this packet */
                var out_packet = handleMessage(in_packet);

                /* only send a response if there were no errors */
                if (out_packet != null) {
                    /* send the response */
                    sock.send(out_packet);
                }
            }
        } catch(IOException e) {
            /* Have to catch IOexceptions for most socket calls */
            System.out.println("Network error!");
        }
    }
        public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Usage: sudo java dns.DNSServer zone_file");
            System.exit(0);
        }
        var zone = new DNSZone(args[0]);
        var server = new DNSServer(zone);
        server.run();
    }
}
