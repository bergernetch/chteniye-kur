import java.util.logging.*;
import java.util.Date;
import java.text.SimpleDateFormat;

public class VerySimpleFormatter extends Formatter {

    private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    @Override
    public String format(final LogRecord record) {

// implement timezone handling, see https://stackoverflow.com/questions/2891361/how-to-set-time-zone-of-a-java-util-date

        return String.format(
                "%1$s %2$s\n",
                new SimpleDateFormat(PATTERN).format(new Date(record.getMillis())),
                formatMessage(record));
    }
}
