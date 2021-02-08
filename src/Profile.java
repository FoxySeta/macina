import java.util.Collections;
import java.util.EnumMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.telegram.telegrambots.meta.api.objects.Location;

enum Stat {

    AGE("yo", 14, 150), HEIGHT("cm", 50, 250), WEIGHT("kg", 25, 750);

    Stat(String uom, int min, int max) {
        if (min > max)
            throw new IllegalArgumentException(
                "Stat.Stat: min (" + min + ") > max (" + max + ")");
        this.uom = uom;
        this.min = min;
        this.max = max;
    }

    public String uom() {
        return uom;
    }

    public int min() {
        return min;
    }
    
    public int max() {
        return max;
    }

    public boolean validate(int value) {
        return min <= value && value <= max;
    }

    String uom;
    int min, max;
}

public class Profile {

    public static final String DEFAULTEMOJI = "😀",
                               USERIDREGEX = "[a-z0-9]{5,32}",
                               EMOJIREGEX = "([\\u20a0-\\u32ff\\ud83c"
                                          + "\\udc00-\\ud83d\\udeff\\udbb9"
                                          + "\\udce5-\\udbb9\\udcee])";
    public static final Pattern USERIDPATTERN = Pattern.compile(USERIDREGEX),
                                EMOJIPATTERN = Pattern.compile(EMOJIREGEX);

    public Profile(int id) {
        setId(id);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id < 0)
            throw new IllegalArgumentException(
                "Profile.setId: " + id + " is negative");
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        if (!USERIDPATTERN.matcher(userId).matches())
            throw new IllegalArgumentException(
                "Profile.setId: Telegram User IDs must follow " + USERIDREGEX);
        this.userId = userId;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        if (!EMOJIPATTERN.matcher(emoji).matches())
            throw new IllegalArgumentException(
                "Profile.setEmoji: " + emoji + " is no emoji");
        this.emoji = emoji;
    }

    public boolean containsStat(Stat key) {
        return stats.containsKey(key);
    }

    public Integer getStat(Stat key) {
        return stats.get(key);
    }

    public Integer putStat(Stat key, Integer value) {
        if (key.validate(value))
            return stats.put(key, value);
        throw new IllegalArgumentException(
            "Profile.putStat: " + value + " " + key.uom +
            " is no valid stat");
    }
    
    public Integer removeStat(Stat key) {
        return stats.remove(key);
    }
    
    public boolean containsTag(String tag) {
        return tags.contains(tag);
    }

    public SortedSet<String> unmodifiableTags() {
        return Collections.unmodifiableSortedSet(tags);
    }
    
    public boolean addTag(String tag) {
        return tags.add(tag);
    }
    
    public boolean removeTag(String tag) {
        return tags.remove(tag);
    }
    
    public Location location;

    int id;
    String userId, emoji = DEFAULTEMOJI;
    EnumMap<Stat, Integer> stats = new EnumMap<Stat, Integer>(Stat.class);
    TreeSet<String> tags = new TreeSet<String>();

}