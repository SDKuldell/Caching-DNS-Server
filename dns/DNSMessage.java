package dns;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.HashMap;

public class DNSMessage {

    private DatagramPacket pkt;
    private byte[] data;
    private int data_length;

    private int next_byte;

    private int id;
    private int flags;
    private int flag_qr;
    private int flag_opcode;
    private int flag_aa;
    private int flag_tc;
    private int flag_rd;
    private int flag_ra;
    private int flag_rcode;

    private int num_questions;
    private int num_answers;
    private int num_auth_rrs;
    private int num_additional_rrs;

    private static HashMap<Integer,String> classes;
    private static HashMap<Integer,String> types;
    static {
        classes = new HashMap<Integer,String>();
        classes.put(1,"IN");

        types = new HashMap<Integer,String>();
        types.put(1,"A");
        types.put(2,"NS");
        types.put(5,"CNAME");
        types.put(6,"SOA");
        types.put(12,"PTR");
        types.put(28,"AAAA");
    }

    private String question_name;
    private int question_type;
    private String question_type_str;
    private int question_class;
    private String question_class_str;

    private ArrayList<DNSRecord> answers;

    public DNSMessage(DatagramPacket pkt) {
        this.pkt = pkt;
        next_byte = 0;
        parseHeader();
        parseFlags();
        parseQuestions();
        this.answers = new ArrayList<DNSRecord>();
        parseAnswers();
    }

    public DNSMessage(DNSMessage request, ArrayList<DNSRecord> answers, boolean isAuthoritative) {
        this.answers = answers;
        createHeader(request, isAuthoritative);
        createQuestions(request);
        createBuffer();
    }

    private void createHeader(DNSMessage request, boolean isAuthoritative) {
        this.id = request.id;
        this.flag_qr = 1;
        this.flag_opcode = request.flag_opcode; 
        if(isAuthoritative) {
            this.flag_aa = 1;
        } else {
            this.flag_aa = 0;
        }

        this.flag_tc = 0;
        this.flag_rd = request.flag_rd;
        this.flag_ra = 1;
        this.num_questions = request.num_questions;
        this.num_auth_rrs = 0;
        this.num_additional_rrs = 0;

        if(answers.size() != 0) {
            this.flag_rcode = 0;
            this.num_answers = answers.size();
        } else {
            this.flag_rcode = 3;
            this.num_answers = 0;
        }

        createFlags();
    }

    private void createFlags() {
        flags = (flag_qr & 0x1) << 15;
        flags |= (flag_opcode & 0xf) << 11;
        flags |= (flag_aa & 0x1) << 10;
        flags |= (flag_tc & 0x1) << 9;
        flags |= (flag_rd & 0x1) << 8;
        flags |= (flag_ra & 0x1) << 7;
        flags |= (flag_rcode & 0xf);
    }

    private void createQuestions(DNSMessage request) {
        if(num_questions != 1) {
            return;
        }
        this.question_name = request.question_name;
        this.question_type = request.question_type;
        this.question_type_str = request.question_type_str;
        this.question_class = request.question_class;
        this.question_class_str = request.question_class_str;
    }

    private void writeInt(int i) {
        data[data_length] = (byte)((i & 0xff000000) >> 24);
        data[data_length+1] = (byte)((i & 0xff0000) >> 16);
        data[data_length+2] = (byte)((i & 0xff00) >> 8);
        data[data_length+3] = (byte)(i & 0xff);
        data_length += 4;
    }

    private void writeShort(int s) {
        data[data_length] = (byte)((s & 0xff00) >> 8);
        data[data_length+1] = (byte)(s & 0xff);
        data_length += 2;
    }

    private void writeByte(int b) {
        data[data_length] = (byte)b;
        data_length += 1;
    }

    private void writeIP(String ip) {
        var octets = ip.split("\\.");
        for(String octet : octets) {
            writeByte(Integer.parseInt(octet));
        }
    }

    private void writeName(String s) {
        var labels = s.split("\\.");
        for (String label : labels) {
            var len = label.length();
            writeByte(len);
            for(int i=0; i<label.length(); i++) {
                writeByte(label.charAt(i));
            }
        }
        writeByte(0);
    }

    private void writeQuestion() {
        if(num_questions != 1) {
            return; 
        }
        writeName(question_name);
        writeShort(question_type);
        writeShort(question_class);
    }

    private void writeAnswer() {
        for(var record : answers) {
            writeName(record.getName());
            writeShort(record.getTypeNum());
            writeShort(record.getClassNum());
            writeInt(record.getTTL());
            writeShort(record.getDataLength());
            if(record.getTypeNum() == 1) {
                writeIP(record.getData());
            } else {
                writeName(record.getData());
            }
        }
    }

    private void createBuffer() {
        data = new byte[512];
        data_length = 0;

        writeShort(id);
        writeShort(flags);
        writeShort(num_questions);
        writeShort(num_answers);
        writeShort(num_auth_rrs);
        writeShort(num_additional_rrs);

        writeQuestion();
        writeAnswer();
    }

    private int bytesToShort(byte b0, byte b1) {
        int i0 = b0 & 0xff;
        int i1 = b1 & 0xff;
        return (i0 << 8) | i1;
    }

    private int bytesToInt(byte b0, byte b1, byte b2, byte b3) {
        int i0 = b0 & 0xff;
        int i1 = b1 & 0xff;
        int i2 = b2 & 0xff;
        int i3 = b3 & 0xff;
        return (i0 << 24) | (i1 << 16) | (i2 << 8) | i3;
    }

    private void parseHeader() {
        data = pkt.getData();
        data_length = pkt.getLength();

        id = bytesToShort(data[0], data[1]);
        flags = bytesToShort(data[2], data[3]);
        num_questions = bytesToShort(data[4], data[5]);
        num_answers = bytesToShort(data[6], data[7]);
        num_auth_rrs = bytesToShort(data[8], data[9]);
        num_additional_rrs = bytesToShort(data[10], data[11]);
        next_byte = 12;
    }

    private void parseFlags() {
        flag_qr = flags >> 15 & 0x1;
        flag_opcode = flags >> 11 & 0xf;
        flag_aa = flags >> 10 & 0x1;
        flag_tc = flags >> 9 & 0x1;
        flag_rd = flags >> 8 & 0x1;
        flag_ra = flags >> 7 & 0x1;
        flag_rcode = flags & 0xf;
    }

    private int parseShort() {
        int s = bytesToShort(data[next_byte], data[next_byte+1]);
        next_byte += 2;
        return s;
    }

    private int parseInt() {
        int i = bytesToInt(data[next_byte], data[next_byte+1], data[next_byte+2], data[next_byte+3]);
        next_byte += 4;
        return i;
    }

    private String parseCompressedName() {
        int offset = parseShort() & 0x3fff;

        int save_next_byte = next_byte;
        next_byte = offset;
        var name = parseName();

        next_byte = save_next_byte;
        return name;
    }

    private String parseName() {
        String name = "";
        int next_label_len = data[next_byte] & 0xff;

        /* have to handle label compresion */
        if(next_label_len >= 192) {
            return parseCompressedName();
        }

        while(next_label_len != 0) {

            int i;
            for(i=next_byte+1; i <= next_byte+next_label_len; i++) {
                name += (char)data[i];
            }

            name += ".";
            next_byte = i;
            next_label_len = data[next_byte] & 0xff;
        }

        name = name.substring(0, name.length()-1);
        next_byte++;
        return name;
    }

    private String parseIP() {
        String ip = "";
        for(int i = 0; i < 4; i++) {
            int b = data[next_byte] & 0xff;
            ip += String.valueOf(b);
            next_byte++;
            if (i != 3) {
                ip += ".";
            }
        }
        return ip;
    }

    private void parseQuestions() {
        if(num_questions != 1) {
            System.out.println("Warning, unexpected number of questions.");
            return;
        }

        question_name = parseName();
        question_type = parseShort();
        question_class = parseShort(); 

        if(types.containsKey(question_type)) {
            question_type_str = types.get(question_type);
        } else {
            question_type_str = String.format("%d", question_type);
        }

        if(classes.containsKey(question_class)) {
            question_class_str = classes.get(question_class);
        } else {
            question_class_str = String.format("%d", question_class);
        }
    }

    private void parseAnswers() {
        for(int i = 0; i < num_answers; i++) {
            var answer_name = parseName();
            var answer_type = parseShort();
            var answer_class = parseShort();
            var answer_ttl = parseInt();
            var answer_rdlength = parseShort();
            String answer_data = "";
            if(answer_type == 1) {
                answer_data = parseIP();
            } else {
                answer_data = parseName();
            }
            var record = new DNSRecord(answer_name, answer_ttl, answer_class, answer_type, answer_data);
            answers.add(record);
        }
    }

    public String toString() {
        var sb = new StringBuilder();
        sb.append(String.format("ID: 0x%04X%n",id));
        sb.append(String.format("Flags: 0x%04X%n",flags));
        if(flag_qr == 0 && flag_opcode == 0) {
            sb.append(String.format("- Standard Query%n"));
        } else if(flag_qr == 1 && flag_rcode == 0) {
            sb.append(String.format("- Standard Response%n"));
        } else if(flag_qr == 1 && flag_rcode == 3) {
            sb.append(String.format("- Response NXDomain%n"));
        } else {
            sb.append(String.format("- Unexpected QR/opcode%n"));
        }
        if(flag_aa == 1) {
            sb.append(String.format("- Authoritative Answer%n"));
        }
        if(flag_rd == 1) {
            sb.append(String.format("- Recursion Requested%n"));
        }
        if(flag_ra == 1) {
            sb.append(String.format("- Recursion Available%n"));
        }
        sb.append(String.format("# Questions: %d%n",num_questions));
        sb.append(String.format("# Answers: %d%n",num_answers));
        sb.append(String.format("# Authority RRs: %d%n",num_auth_rrs));
        sb.append(String.format("# Additional RRs: %d%n",num_additional_rrs));
        if(num_questions == 1) {
            sb.append(String.format("Questions:%n"));
            sb.append(String.format("- %s, %s, %s%n", question_name, question_type_str, question_class_str));
        }
        if(num_answers != 0) {
            sb.append(String.format("Answers:%n"));
            for(var answer : answers) {
              sb.append(String.format("- %s, %s, %s, %d, %s%n", answer.getName(), answer.getTypeStr(), answer.getClassStr(), answer.getTTL(), answer.getData()));
            }
        }
        return sb.toString();
    }

    public boolean isQuery() {
        if(flag_qr == 0) {
            return true;
        }
        return false;
    }

    public int getID() {
        return id;
    }

    public String getQuestionName() {
        return question_name;
    }

    public String getQuestionType() {
        return question_type_str;
    }

    public String getQuestionClass() {
        return question_class_str;
    }

    public ArrayList<DNSRecord> getAnswers() {
        return answers;
    }

    public byte[] getData() {
        return data;
    }

    public int getDataLength() {
        return data_length;
    }

    public DatagramPacket getPacket() {
        return pkt;
    }
}
