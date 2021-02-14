package bot;

import bot.data.Filter;
import bot.data.Profile;
import bot.data.Stat;

import java.util.function.Consumer;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TeleGrindr extends AbilityBot {

    public TeleGrindr(String botToken, String username, int creatorId) {
        super(botToken, username);
        cId = creatorId;
    }

    @Override
    public int creatorId() {
        return cId;
    }

    public Ability iam() {
        return Ability
                .builder()
                .name("iam")
                .info("edits your profile")
                .input(0)
                .locality(Locality.GROUP)
                .privacy(Privacy.PUBLIC)
                .action(iamAction)
                .reply(iamReply, Flag.LOCATION)
                .enableStats()
                .build();
    }
    
    public Ability howis() {
        return Ability
                .builder()
                .name("howis")
                .info("displays a profile")
                .input(1)
                .locality(Locality.GROUP)
                .privacy(Privacy.PUBLIC)
                .action(howisAction)
                .enableStats()
                .build();
    }

    public Ability whois() {
        return Ability
                .builder()
                .name("whois")
                .info("searches for profiles")
                .input(0)
                .locality(Locality.GROUP)
                .privacy(Privacy.PUBLIC)
                .action(whoisAction)
                .enableStats()
                .build();
    }

    public void print(Profile p, Long chatId) {
        silent.sendMd(p.toString(), chatId);
        if (p.location != null) {
            SendLocation location = new SendLocation();
            location.setChatId(chatId.toString());
            location.setHorizontalAccuracy(p.location.getHorizontalAccuracy());
            location.setLatitude(p.location.getLatitude());
            location.setLongitude(p.location.getLongitude());
            try {
                execute(location);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    public void print(Stream<Profile> s, Long chatId) {
        silent.send(String.format(PEOPLECOUNTER, s.count())
                    + s.map(p -> p.toShortString())
                       .collect(Collectors.joining(String.format("%n"),
                                                   String.format("%n"), "")),
                    chatId);
    }

    private int cId;
    final private static char TAGPREFIX = '@';
    final private static String
        REMOVETAGARGUMENTPREFIX = "-",
        TAGARGUMENTREGEX = "([+-]?)#([0-9A-Za-z]+).*",
        STATARGUMENTREGEX = "(\\d+)(.+)",
        PROFILESTABLE = "Profiles_%d",
        UNKNOWNARGUMENT = "%s❓",
        LOCATIONLABEL = "📍",
        PEOPLECOUNTER = "👤 × %d";
    final private static Pattern
        TAGARGUMENTPATTERN = Pattern.compile(TAGARGUMENTREGEX),
        STATARGUMENTPATTERN = Pattern.compile(STATARGUMENTREGEX);
        
    private Profile getProfile(Long chatId, User user) {
        final Map<Integer, Profile> profiles =
            db.getMap(String.format(PROFILESTABLE, chatId));
        final Integer userId = user.getId();
        if (profiles.containsKey(userId)) {
            final Profile oldProfile = profiles.get(userId);
            oldProfile.user = user;
            return oldProfile;
        }
        final Profile newProfile = new Profile(user);
        profiles.put(user.getId(), newProfile);
        return newProfile;
    }

    private Profile setProfile(Long chatId, Profile profile) {
        return db.<Integer, Profile>getMap(String.format(PROFILESTABLE, chatId))
                 .put(profile.user.getId(), profile);
    }

    private boolean update(Profile profile, String argument) {
        Matcher matcher = TAGARGUMENTPATTERN.matcher(argument);
        if (matcher.matches()) {
            String tag = matcher.group(2);
            if (matcher.group(1).equals(REMOVETAGARGUMENTPREFIX))
                profile.removeTag(tag);
            else
                profile.addTag(tag);
        } else {
            matcher = STATARGUMENTPATTERN.matcher(argument);
            if (matcher.matches()) {
                final String uom = matcher.group(2);
                for (Stat stat : Stat.values())
                    if (stat.uom().equals(uom)) {
                        final int value = Integer.parseInt(matcher.group(1));
                        if (stat.validate(value))
                            profile.putStat(stat, value);
                        break;
                    }
            } else {
                matcher = Profile.EMOJIPATTERN.matcher(argument);
                if (matcher.matches())
                    profile.setEmoji(argument);
                else
                    return false;
            }
        }
        return true;
    }

    final private Consumer<MessageContext> iamAction = ctx -> {
        final Long chat = ctx.chatId();
        final Profile profile = getProfile(ctx.chatId(), ctx.user());
        for (String argument : ctx.arguments())     
            if (!update(profile, argument))
                silent.send(String.format(UNKNOWNARGUMENT, argument),
                            ctx.chatId());
        print(profile, chat);
        setProfile(chat, profile);
    };
    
    final private Consumer<Update> iamReply = upd -> {
        final Message message = upd.getMessage();
        final Profile profile = getProfile(upd.getMessage().getChatId(),
                                           upd.getMessage().getFrom());
        profile.location = message.getLocation();
        setProfile(message.getChatId(), profile);
        print(profile, upd.getMessage().getChatId());
    };

    final private Consumer<MessageContext> howisAction = ctx -> {
        final String arg = ctx.firstArg(),
                     tag = arg.substring(arg.charAt(0) == TAGPREFIX ? 1 : 0);
        final Long chat = ctx.chatId();
        Optional<Profile> profile = db
            .<Integer, Profile>getMap(String.format(PROFILESTABLE, chat))
            .values().stream().filter(
                p -> p != null && p.user != null &&
                     tag.equalsIgnoreCase(p.user.getUserName())
            ).findAny();
        if (profile.isPresent())
            print(profile.get(), chat);
        else
            silent.send(String.format(UNKNOWNARGUMENT, TAGPREFIX + tag), chat);
    };

    final private Consumer<MessageContext> whoisAction = ctx -> {
        final Long chat = ctx.chatId();
        final Location from = getProfile(chat, ctx.user()).location;
        if (from != null) {
            final Stream<Profile> results = db
                .<Integer, Profile>getMap(String.format(PROFILESTABLE, chat))
                .values().stream().filter(new Filter(ctx.arguments(), from));
        } else
            silent.send(String.format(UNKNOWNARGUMENT, LOCATIONLABEL), chat);
    };
}