package dns;

import java.util.Map;
import java.util.HashMap;

public class DNSRecord {

    private String name;
    private int ttl;
    private int class_num;
    private String class_str;
    private int type_num;
    private String type_str;
    private int data_length;
    private String data;

    private static HashMap<Integer,String> classes;
    private static HashMap<Integer,String> types;
    static {
        classes = new HashMap<Integer,String>();
        classes.put(1,"IN");

        types = new HashMap<Integer,String>();
        types.put(1,"A");
        types.put(5,"CNAME");
    }
    
    /* TODO: add something to track when this record object was stored */

    private void setDataLength() {
        if (this.type_num == 1) {
            this.data_length = 4;
        } else if (this.type_num == 5) {
            this.data_length = data.length() + 1;
        } else {
            System.out.println("This server only handles A and CNAME records.");
            System.exit(0);
        }
    }

    public DNSRecord(String name, int ttl, String class_str, String type_str, String data) {
        this.name = name;
        this.ttl = ttl;
        this.class_str = class_str;
        this.type_str = type_str;
        this.data = data;

        this.class_num = 0;
        for (Map.Entry<Integer, String> entry : classes.entrySet()) {
            if(entry.getValue().equals(class_str)) {
                this.class_num = entry.getKey();
            }
        }

        this.type_num = 0;
        for (Map.Entry<Integer, String> entry : types.entrySet()) {
            if(entry.getValue().equals(type_str)) {
                this.type_num = entry.getKey();
            }
        }

        setDataLength();
    }

    public DNSRecord(String name, int ttl, int class_num, int type_num, String data) {
        this.name = name;
        this.ttl = ttl;
        this.class_num = class_num;
        this.type_num = type_num;
        this.data = data;

        if(classes.containsKey(class_num)) {
            class_str = classes.get(class_num);
        } else {
            class_str = String.format("%d", class_num);
        }

        if(types.containsKey(type_num)) {
            type_str = types.get(type_num);
        } else {
            type_str = String.format("%d", type_num);
        }

        setDataLength();
    }

    public String getName() {
        return name;
    }

    public int getTTL() {
        return ttl;
    }

    public int getClassNum() {
        return class_num;
    }

    public String getClassStr() {
        return class_str;
    }

    public int getTypeNum() {
        return type_num;
    }

    public String getTypeStr() {
        return type_str;
    }

    public int getDataLength() {
        return data_length;
    }

    public String getData() {
        return data;
    }
}
