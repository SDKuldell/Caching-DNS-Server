package dns;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;

public class DNSCache {
    /* TODO: fill me in! */
    private ArrayList<DNSRecord> recordCache;

    public DNSCache(){
      recordCache = new ArrayList<DNSRecord>();

    }

    public void addEntry(DNSRecord record){
      recordCache.add(record);
    }

    public void addEntries(ArrayList<DNSRecord> message) {
      recordCache.addAll(message);
    }

    public void checkCache(){
      Instant currentTime = Instant.now();
      
      for(DNSRecord comparisonRecord : recordCache) {

        Instant thenTimestamp = comparisonRecord.getTimeStamp();

        Duration diff = Duration.between(thenTimestamp, currentTime);

        if(diff.toSeconds() > comparisonRecord.getTTL()) {
          recordCache.remove(comparisonRecord);
        }
      }
      
    }

    public ArrayList<DNSRecord> returnRecords(String name_str, String type_str, String class_str) {
      ArrayList<DNSRecord> returnList = new ArrayList<DNSRecord>();

      for(DNSRecord comparisonRecord : recordCache){

        if(comparisonRecord.getName().equals(name_str) && comparisonRecord.getTypeStr().equals(type_str) &&comparisonRecord.getClassStr().equals(class_str)){
          returnList.add(comparisonRecord);
        }
      }

      return returnList;
    }

}
