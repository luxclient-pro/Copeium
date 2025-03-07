package cope.client.util;

import org.json.JSONArray;
import org.json.JSONObject;

public class EmbedBuilder {
    private final JSONObject embed;

    public EmbedBuilder() {
        embed = new JSONObject();
    }

    public EmbedBuilder setTitle(String title) {
        embed.put("title", title);
        return this;
    }

    public EmbedBuilder setDescription(String description) {
        embed.put("description", description);
        return this;
    }

    public EmbedBuilder setColor(int color) {
        embed.put("color", color);
        return this;
    }

    public EmbedBuilder setFooter(String text) {
        JSONObject footer = new JSONObject();
        footer.put("text", text);
        embed.put("footer", footer);
        return this;
    }

    public JSONObject build() {
        return embed;
    }

    public JSONArray buildArray() {
        JSONArray array = new JSONArray();
        array.put(embed);
        return array;
    }
}
