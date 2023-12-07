import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CsvUtil
 *
 * @author gill
 * @version 2023/12/06
 **/
public class CsvUtil {

    public static <T> void export(List<T> data, Class<T> clazz) {
        File file = new File("./data.csv");
        try (FileWriter writer = new FileWriter(file)) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
            }

            // 字段头
            writer.write(
                Arrays.stream(fields).map(Field::getName).collect(Collectors.joining(",")) + System.lineSeparator());

            // 实际数据
            for (T d : data) {
                String line = Arrays.stream(fields).map(field -> {
                    try {
                        return field.get(d);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }).map(String::valueOf).collect(Collectors.joining(","));
                writer.write(line + System.lineSeparator());
            }

            // 标准数据

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
