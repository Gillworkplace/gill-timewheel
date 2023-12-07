/**
 * Data
 *
 * @author gill
 * @version 2023/12/06
 **/
public class Data {

    public final String group;

    private final long actualTime;

    private final long standardTime;

    public Data(String group, long actualTime, long standardTime) {
        this.group = group;
        this.actualTime = actualTime;
        this.standardTime = standardTime;
    }

    public long getActualTime() {
        return actualTime;
    }

    public long getStandardTime() {
        return standardTime;
    }
}
