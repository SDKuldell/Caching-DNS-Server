package dns;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.NumberFormatException;
import java.util.ArrayList;
import java.util.Scanner;

public class DNSZone {
    
    private ArrayList<DNSRecord> records;
    
    public DNSZone(String zonefile_name) {
        records = new ArrayList<DNSRecord>();
        parseFile(zonefile_name);
    }

    private void parseFile(String zonefile_name) {
        try (var scanner = new Scanner(new File(zonefile_name))) {
            while(scanner.hasNextLine()) {
                var tokens = scanner.nextLine().split("\\s+");
                if(tokens.length != 5) {
                    System.out.println("Error in zone file: format incorrect.");
                    System.exit(0);
                }

                var record_name = tokens[0];
                var record_ttl = Integer.parseInt(tokens[1]);
                var record_class = tokens[2];
                var record_type = tokens[3];
                var record_data = tokens[4];

                if(!record_class.equals("IN")) {
                    System.out.println("Error in zone file: non-IN record found.");
                    System.exit(0);
                }

                var record = new DNSRecord(record_name, record_ttl, record_class, record_type, record_data);

                records.add(record);
            }
        } catch(NumberFormatException e) {
            System.out.println("Error in zone file: format incorrect.");
            System.exit(0);
        } catch(FileNotFoundException e) {
            System.out.println("Error: zone file not found.");
            System.exit(0);
        }
    }

    public ArrayList<DNSRecord> getRecords(String name, String type, String rclass) {
        var matches = new ArrayList<DNSRecord>();

        for (var record : records) {
            if(record.getName().equals(name) &&
               record.getClassStr().equals(rclass) &&
               record.getTypeStr().equals(type)) {

                matches.add(record);
            }
        }

        return matches;
    }
}
