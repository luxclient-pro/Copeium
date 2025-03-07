package cope.client.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import org.apache.commons.lang3.RandomStringUtils;
import cope.client.Cope;
import org.apache.commons.lang3.StringUtils;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AutoScoreboard extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTitle = settings.createGroup("Title Options");
    private final SettingGroup sgContent = settings.createGroup("Content Options");
    private static final String TICKET_ID_PLACEHOLDER = "{ticketid}";
    private final Setting<String> title = sgTitle.add(new StringSetting.Builder()
        .name("title")
        .description("Title of the scoreboard to create. Supports Starscript.")
        .defaultValue("Trolled!")
        .wide()
        .renderer(StarscriptTextBoxRenderer.class)
        .build()
    );

    private final Setting<String> titleColor = sgTitle.add(new StringSetting.Builder()
        .name("title-color")
        .description("Color of the title")
        .defaultValue("dark_red")
        .wide()
        .build()
    );

    private final Setting<List<String>> content = sgContent.add(new StringListSetting.Builder()
        .name("content")
        .description("Content of the scoreboard. Supports Starscript.")
            .defaultValue(Arrays.asList(
                "  █████████████████████████████████████████████████████████████████████████████████████████████████████",
                "",
                "           Copeium Loss Prevention Inc.                                                                                                                                Copeium Loss Prevention Inc.",
                "              Ticket ID # {ticketid}                                                                                                                                         Ticket ID # {ticketid} ",
                "",
                "               Complaints Server:                                                                                                                                               Complaints Server:",
                "             discord.gg/HQyNhNKwMv                                                                                                                                        discord.gg/HQyNhNKwMv",
                "",
                "      Destroyed @ {time} | {date}                                                                                                                          Destroyed @ {time} | {date}",
                "",
                "  █████████████████████████████████████████████████████████████████████████████████████████████████████"
            ))
        .renderer(StarscriptTextBoxRenderer.class)
        .build()
    );

    private final Setting<String> contentColor = sgContent.add(new StringSetting.Builder()
        .name("content-color")
        .description("Color of the content")
        .defaultValue("red")
        .build()
    );

    private final Setting<Boolean> useDelay = sgGeneral.add(new BoolSetting.Builder()
        .name("Use Command Delay")
        .description("Adds delay between commands to prevent kicks")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> commandDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Command Delay")
        .description("Ticks between each command")
        .defaultValue(2)
        .min(1)
        .sliderMax(20)
        .visible(useDelay::get)
        .build()
    );

    private int tickCounter = 0;
    private Queue<String> commandQueue = new LinkedList<>();

    public AutoScoreboard() {
        super(Cope.CATEGORY, "Cope-Scoreboard", "Automatically create a scoreboard using Starscript. Requires operator access.");
    }

    private String processStarscript(String input) {
        if (input == null || input.isEmpty()) return "";

        try {
            Script script = MeteorStarscript.compile(input);
            if (script == null) {
                error("Failed to compile Starscript: " + input);
                return input;
            }
            String result = MeteorStarscript.run(script);
            return result != null ? result : input;
        } catch (Exception e) {
            error("Error processing Starscript: " + e.getMessage());
            return input;
        }
    }

    @Override
    public void onActivate() {
        assert mc.player != null;
        if (!mc.player.hasPermissionLevel(2)) {
            toggle();
            error("No permission!");
            return;
        }

        try {
            String ticketId = getTicketNumber();
            String scoreboardName = RandomStringUtils.randomAlphabetic(10).toLowerCase();
            String processedTitle = processStarscript(title.get().replace(TICKET_ID_PLACEHOLDER, ticketId));
            String thecommand = "/scoreboard objectives add " + scoreboardName + " dummy {\"text\":\"" + processedTitle + "\",\"color\":\"" + titleColor.get() + "\"}";

            if (thecommand.length() > 256) {
                error("Title is too long. Shorten it by " + (thecommand.length() - 256) + " characters.");
                toggle();
                return;
            }

            if (useDelay.get()) {
                commandQueue.add(thecommand);
                commandQueue.add("/scoreboard objectives setdisplay sidebar " + scoreboardName);

                int i = content.get().size();
                for (String string : content.get()) {
                    String processedContent = processStarscript(string.replace(TICKET_ID_PLACEHOLDER, ticketId));
                    String randomName = RandomStringUtils.randomAlphabetic(10).toLowerCase();
                    commandQueue.add("/team add " + randomName);

                    String thecommand2 = "/team modify " + randomName + " suffix {\"text\":\" " + processedContent + "\"}";
                    if (thecommand2.length() <= 256) {
                        commandQueue.add(thecommand2);
                        commandQueue.add("/team modify " + randomName + " color " + contentColor.get());
                        commandQueue.add("/team join " + randomName + " " + i);
                        commandQueue.add("/scoreboard players set " + i + " " + scoreboardName + " " + i);
                    } else {
                        error("Content line too long: " + processedContent + ". Shorten it by " + (thecommand2.length() - 256) + " characters.");
                        toggle();
                        return;
                    }
                    i--;
                }
            } else {
                ChatUtils.sendPlayerMsg(thecommand);
                ChatUtils.sendPlayerMsg("/scoreboard objectives setdisplay sidebar " + scoreboardName);

                int i = content.get().size();
                for (String string : content.get()) {
                    String processedContent = processStarscript(string.replace(TICKET_ID_PLACEHOLDER, ticketId));
                    String randomName = RandomStringUtils.randomAlphabetic(10).toLowerCase();
                    ChatUtils.sendPlayerMsg("/team add " + randomName);

                    String thecommand2 = "/team modify " + randomName + " suffix {\"text\":\" " + processedContent + "\"}";
                    if (thecommand2.length() <= 256) {
                        ChatUtils.sendPlayerMsg(thecommand2);
                        ChatUtils.sendPlayerMsg("/team modify " + randomName + " color " + contentColor.get());
                        ChatUtils.sendPlayerMsg("/team join " + randomName + " " + i);
                        ChatUtils.sendPlayerMsg("/scoreboard players set " + i + " " + scoreboardName + " " + i);
                    } else {
                        error("Content line too long: " + processedContent + ". Shorten it by " + (thecommand2.length() - 256) + " characters.");
                        toggle();
                        return;
                    }
                    i--;
                }
                toggle();
                info("Created scoreboard.");
            }
        } catch (Exception e) {
            error("Error creating scoreboard: " + e.getMessage());
            toggle();
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (!useDelay.get()) return;

        if (!commandQueue.isEmpty()) {
            if (tickCounter >= commandDelay.get()) {
                ChatUtils.sendPlayerMsg(commandQueue.poll());
                tickCounter = 0;
            } else {
                tickCounter++;
            }
        } else {
            toggle();
            info("Created scoreboard.");
        }
    }

    private String getTicketNumber() {
        if (mc.getCurrentServerEntry() == null) return "OFFLINE";

        try {
            // Split IP and port
            String[] addressParts = mc.getCurrentServerEntry().address.split(":");
            String ip = addressParts[0];
            int port = addressParts.length > 1 ? Integer.parseInt(addressParts[1]) : 25565;

            // Convert IP to bytes
            Inet4Address address = (Inet4Address) Inet4Address.getByName(ip);
            byte[] bytes = address.getAddress();

            // Convert IP to a single number
            long ipNum = ((bytes[0] & 0xFFL) << 24) |
                ((bytes[1] & 0xFFL) << 16) |
                ((bytes[2] & 0xFFL) << 8) |
                (bytes[3] & 0xFFL);

            // Combine IP and port into one number
            long combined = (ipNum << 16) | (port & 0xFFFF);

            // Convert to base36
            return toBase36(combined);

        } catch (UnknownHostException e) {
            return StringUtils.abbreviate(mc.player.getName().getString(), 15);
        }
    }

    private String toBase36(long number) {
        final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder result = new StringBuilder();

        do {
            result.insert(0, ALPHABET.charAt((int)(number % 36)));
            number /= 36;
        } while (number > 0);

        return result.toString();
    }
}
